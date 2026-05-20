package com.example.donorrequestmanagementsystem.engine.ui;

import com.example.donorrequestmanagementsystem.engine.datastructure.DijkstraEngine;
import com.example.donorrequestmanagementsystem.engine.datastructure.GraphNode;
import com.example.donorrequestmanagementsystem.engine.datastructure.RouteGraph;
import com.example.donorrequestmanagementsystem.engine.datastructure.GraphEdge;
import com.example.donorrequestmanagementsystem.engine.datastructure.EmergencyRequestHeap;
import com.example.donorrequestmanagementsystem.engine.model.Donor;
import com.example.donorrequestmanagementsystem.engine.model.EmergencyRequest;
import com.example.donorrequestmanagementsystem.engine.model.RoutingResult;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainDashboard extends Application {

    // Concrete Architecture Integrations
    private final RouteGraph cityGraph = new RouteGraph();
    private final EmergencyRequestHeap requestHeap = new EmergencyRequestHeap(15);
    private final Map<String, List<Donor>> bloodRegistry = new HashMap<>();

    private Canvas mapCanvas;
    private ListView<String> heapListView;
    private VBox logContentArea;
    private List<String> highlightedRoutePath = null;

    private int requestSequenceCounter = 1;

    @Override
    public void start(Stage primaryStage) {
        initializePhysicalTopology();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0F172A;");

        // --- TOP NAVIGATION HEADER ---
        HBox header = new HBox(20);
        header.setPadding(new Insets(15, 25, 15, 25));
        header.setStyle("-fx-background-color: #1E293B; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("SMART EMERGENCY DONOR ROUTING PLATFORM");
        title.setFont(Font.font("Helvetica", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#F8FAFC"));

        Label statusLabel = new Label("● ENGINE ACTIVE");
        statusLabel.setFont(Font.font("Helvetica", FontWeight.SEMI_BOLD, 12));
        statusLabel.setTextFill(Color.web("#10B981"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label regNo = new Label("REG NO: SP25-BCS-017(B)");
        regNo.setFont(Font.font("Consolas", 12));
        regNo.setTextFill(Color.web("#94A3B8"));

        header.getChildren().addAll(title, statusLabel, spacer, regNo);
        root.setTop(header);

        // --- LEFT PANEL: CONTROL FORMS ---
        VBox leftControlPanel = new VBox(20);
        leftControlPanel.setPadding(new Insets(20));
        leftControlPanel.setPrefWidth(380);
        leftControlPanel.setStyle("-fx-background-color: #1E293B; -fx-border-color: #334155; -fx-border-width: 0 1 0 0;");

        // Form 1: Donor Registration
        VBox donorCard = createStyledFormCard("DONOR REGISTRATION SYSTEM");
        TextField txtDonorName = createStyledTextField("Full Name");
        ComboBox<String> cbBloodGroup = new ComboBox<>();
        cbBloodGroup.getItems().addAll("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-");
        cbBloodGroup.setPromptText("Select Blood Group");
        cbBloodGroup.setMaxWidth(Double.MAX_VALUE);
        cbBloodGroup.setStyle("-fx-background-color: #334155; -fx-text-fill: white;");

        TextField txtDonorPhone = createStyledTextField("Phone (e.g., 03001234567)");

        ComboBox<String> cbDonorNode = new ComboBox<>();
        cbDonorNode.getItems().addAll("Node_A", "Node_B", "Node_C", "Node_D");
        cbDonorNode.setPromptText("Select Location Node");
        cbDonorNode.setMaxWidth(Double.MAX_VALUE);
        cbDonorNode.setStyle("-fx-background-color: #334155;");

        Button btnRegisterDonor = createStyledButton("REGISTER DONOR ENGINES", "#3B82F6");
        donorCard.getChildren().addAll(txtDonorName, cbBloodGroup, txtDonorPhone, cbDonorNode, btnRegisterDonor);

        // Form 2: Emergency Intake
        VBox emergencyCard = createStyledFormCard("EMERGENCY INTAKE PORTAL");
        TextField txtHospitalName = createStyledTextField("Target Hospital Name");

        ComboBox<String> cbReqBlood = new ComboBox<>();
        cbReqBlood.getItems().addAll("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-");
        cbReqBlood.setPromptText("Required Blood Group");
        cbReqBlood.setMaxWidth(Double.MAX_VALUE);
        cbReqBlood.setStyle("-fx-background-color: #334155;");

        ComboBox<EmergencyRequest.UrgencyLevel> cbUrgency = new ComboBox<>();
        cbUrgency.getItems().addAll(EmergencyRequest.UrgencyLevel.values());
        cbUrgency.setPromptText("Urgency Matrix Severity");
        cbUrgency.setMaxWidth(Double.MAX_VALUE);
        cbUrgency.setStyle("-fx-background-color: #334155;");

        ComboBox<String> cbHospitalNode = new ComboBox<>();
        cbHospitalNode.getItems().addAll("Hosp_Jinnah", "Hosp_Mayo");
        cbHospitalNode.setPromptText("Select Target Hospital Node");
        cbHospitalNode.setMaxWidth(Double.MAX_VALUE);
        cbHospitalNode.setStyle("-fx-background-color: #334155;");

        Button btnRouteTrigger = createStyledButton("TRIGGER OPTIMIZED MATCHING", "#EF4444");
        emergencyCard.getChildren().addAll(txtHospitalName, cbReqBlood, cbUrgency, cbHospitalNode, btnRouteTrigger);

        leftControlPanel.getChildren().addAll(donorCard, emergencyCard);
        root.setLeft(leftControlPanel);

        // --- CENTER PANEL: INFRASTRUCTURE MAP CANVAS ---
        VBox centerPanel = new VBox(10);
        centerPanel.setPadding(new Insets(20));
        VBox.setVgrow(centerPanel, Priority.ALWAYS);

        Label mapLabel = new Label("LIVE VECTOR INFRASTRUCTURE MAP (DIJKSTRA GEOMETRY)");
        mapLabel.setFont(Font.font("Helvetica", FontWeight.BOLD, 14));
        mapLabel.setTextFill(Color.web("#94A3B8"));

        mapCanvas = new Canvas(750, 500);
        StackPane canvasHolder = new StackPane(mapCanvas);
        canvasHolder.setStyle("-fx-background-color: #0B0F19; -fx-border-color: #334155; -fx-border-width: 2; -fx-border-radius: 8; -fx-background-radius: 8;");
        VBox.setVgrow(canvasHolder, Priority.ALWAYS);

        centerPanel.getChildren().addAll(mapLabel, canvasHolder);
        root.setCenter(centerPanel);

        // --- RIGHT PANEL: DISPATCH QUEUE & TERMINAL OUTPUT ---
        VBox rightMetricsPanel = new VBox(20);
        rightMetricsPanel.setPadding(new Insets(20));
        rightMetricsPanel.setPrefWidth(340);
        rightMetricsPanel.setStyle("-fx-background-color: #1E293B; -fx-border-color: #334155; -fx-border-width: 0 0 0 1;");

        Label queueLabel = new Label("HEAP REQUEST STREAM PRIORITY");
        queueLabel.setFont(Font.font("Helvetica", FontWeight.BOLD, 13));
        queueLabel.setTextFill(Color.web("#94A3B8"));

        heapListView = new ListView<>();
        heapListView.setStyle("-fx-background-color: #0F172A; -fx-text-fill: #F8FAFC; -fx-border-color: #334155;");
        VBox.setVgrow(heapListView, Priority.ALWAYS);

        VBox systemOutputLog = new VBox(10);
        systemOutputLog.setPadding(new Insets(12));
        systemOutputLog.setStyle("-fx-background-color: #0F172A; -fx-border-color: #10B981; -fx-border-radius: 6;");

        Label logTitle = new Label("REAL-TIME DISPATCH ROUTE ENGINE");
        logTitle.setFont(Font.font("Helvetica", FontWeight.BOLD, 11));
        logTitle.setTextFill(Color.web("#10B981"));

        logContentArea = new VBox(4);
        Label fallbackMessage = new Label("System Idling...\nAwaiting Target Path Parameter Triggers.");
        fallbackMessage.setFont(Font.font("Consolas", 11));
        fallbackMessage.setTextFill(Color.web("#64748B"));
        logContentArea.getChildren().add(fallbackMessage);

        systemOutputLog.getChildren().addAll(logTitle, logContentArea);
        rightMetricsPanel.getChildren().addAll(queueLabel, heapListView, systemOutputLog);
        root.setRight(rightMetricsPanel);

        // ================================================================
        // INTERACTIVE PIPELINE ACTION PROCESSING BINDINGS
        // ================================================================

        btnRegisterDonor.setOnAction(e -> {
            String name = txtDonorName.getText();
            String bgStr = cbBloodGroup.getValue();
            String phone = txtDonorPhone.getText();
            String targetNode = cbDonorNode.getValue();

            if (name.isEmpty() || bgStr == null || targetNode == null) {
                showWarning("Input Error", "All donor enrollment input metrics must be specified.");
                return;
            }

            // Morph underlying intersection point coordinates to reference a donor system asset
            GraphNode node = cityGraph.getNode(targetNode);
            if (node != null) {
                cityGraph.addNode(targetNode, node.getX(), node.getY(), GraphNode.NodeType.DONOR);
            }

            txtDonorName.clear();
            txtDonorPhone.clear();
            drawMapInfrastructure();
        });

        btnRouteTrigger.setOnAction(e -> {
            String hospital = txtHospitalName.getText();
            String bgStr = cbReqBlood.getValue();
            EmergencyRequest.UrgencyLevel urgency = cbUrgency.getValue();
            String hospitalNode = cbHospitalNode.getValue();

            if (hospital.isEmpty() || bgStr == null || urgency == null || hospitalNode == null) {
                showWarning("Parameters Missing", "Please select all emergency verification inputs.");
                return;
            }

            String reqId = "REQ-" + String.format("%03d", requestSequenceCounter++);
            // Temporary assignment mapping layer to populate required properties
            EmergencyRequest request = new EmergencyRequest(reqId, hospital, null, hospitalNode, urgency);
            requestHeap.insert(request);

            // Sync structural priority view
            refreshHeapUI();

            // Run Dijkstra Calculations directly against core nodes
            List<RoutingResult> routes = DijkstraEngine.findTopThreeDonors(cityGraph, hospitalNode);
            updateOutputConsole(routes);
        });

        Scene scene = new Scene(root, 1450, 880);
        primaryStage.setTitle("Smart Emergency Donor Matching & Routing Infrastructure Engine");
        primaryStage.setScene(scene);
        primaryStage.show();

        drawMapInfrastructure();
    }

    private void drawMapInfrastructure() {
        GraphicsContext gc = mapCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, mapCanvas.getWidth(), mapCanvas.getHeight());

        // Trace structural road links
        gc.setStroke(Color.web("#334155"));
        gc.setLineWidth(2);
        for (GraphNode node : cityGraph.getAllNodes().values()) {
            for (GraphEdge edge : node.getEdges()) {
                gc.strokeLine(node.getX(), node.getY(), edge.getDestination().getX(), edge.getDestination().getY());
            }
        }

        // Overlay calculated dynamic tracking path route lines
        if (highlightedRoutePath != null && highlightedRoutePath.size() > 1) {
            gc.setStroke(Color.web("#10B981")); // Emerald Green tracker line
            gc.setLineWidth(4);
            for (int i = 0; i < highlightedRoutePath.size() - 1; i++) {
                GraphNode current = cityGraph.getNode(highlightedRoutePath.get(i));
                GraphNode next = cityGraph.getNode(highlightedRoutePath.get(i + 1));
                if (current != null && next != null) {
                    gc.strokeLine(current.getX(), current.getY(), next.getX(), next.getY());
                }
            }
        }

        // Draw structural location point symbols
        for (GraphNode node : cityGraph.getAllNodes().values()) {
            if (node.getType() == GraphNode.NodeType.HOSPITAL) {
                gc.setFill(Color.web("#EF4444")); // Crimson Block
                gc.fillRect(node.getX() - 8, node.getY() - 8, 16, 16);
            } else if (node.getType() == GraphNode.NodeType.DONOR) {
                gc.setFill(Color.web("#3B82F6")); // Cyber Blue Circle
                gc.fillOval(node.getX() - 7, node.getY() - 7, 14, 14);
            } else {
                gc.setFill(Color.web("#64748B")); // Structural Hub
                gc.fillOval(node.getX() - 5, node.getY() - 5, 10, 10);
            }

            gc.setFill(Color.web("#94A3B8"));
            gc.setFont(Font.font("Consolas", FontWeight.BOLD, 10));
            gc.fillText(node.getId(), node.getX() + 12, node.getY() + 4);
        }
    }

    private void refreshHeapUI() {
        heapListView.getItems().clear();
        EmergencyRequestHeap backupHeap = new EmergencyRequestHeap(15);

        while (!requestHeap.isEmpty()) {
            EmergencyRequest req = requestHeap.poll();
            backupHeap.insert(req);
            heapListView.getItems().add(String.format("[%s] %s - %s",
                    req.getUrgency().name(), req.getRequestId(), req.getHospitalName()));
        }

        while (!backupHeap.isEmpty()) {
            requestHeap.insert(backupHeap.poll());
        }
    }

    private void updateOutputConsole(List<RoutingResult> routes) {
        logContentArea.getChildren().clear();
        if (routes.isEmpty()) {
            Label err = new Label("CRITICAL: No matching logistical paths found.");
            err.setTextFill(Color.web("#EF4444"));
            logContentArea.getChildren().add(err);
            highlightedRoutePath = null;
            drawMapInfrastructure();
            return;
        }

        for (int i = 0; i < Math.min(routes.size(), 3); i++) {
            RoutingResult res = routes.get(i);
            Label pathLabel = new Label(String.format("RANK %d: Target %s (%.1f km)\nPATH: %s",
                    i + 1, res.getDonorNode().getId(), res.getTotalDistance(), String.join(" -> ", res.getCompletePath())));
            pathLabel.setFont(Font.font("Consolas", 10));
            pathLabel.setTextFill(i == 0 ? Color.web("#34D399") : Color.web("#CBD5E1"));
            logContentArea.getChildren().add(pathLabel);
        }

        // Extract and assign the absolute shortest path target sequence to highlight on the canvas vector map
        highlightedRoutePath = routes.get(0).getCompletePath();
        drawMapInfrastructure();
    }

    private void initializePhysicalTopology() {
        // Map anchor coordinates
        cityGraph.addNode("Node_A", 100, 150, GraphNode.NodeType.INTERSECTION);
        cityGraph.addNode("Node_B", 260, 100, GraphNode.NodeType.INTERSECTION);
        cityGraph.addNode("Node_C", 400, 250, GraphNode.NodeType.INTERSECTION);
        cityGraph.addNode("Node_D", 200, 400, GraphNode.NodeType.INTERSECTION);

        cityGraph.addNode("Hosp_Jinnah", 600, 180, GraphNode.NodeType.HOSPITAL);
        cityGraph.addNode("Hosp_Mayo", 640, 420, GraphNode.NodeType.HOSPITAL);

        // Map interconnected road distances
        cityGraph.addUndirectedEdge("Node_A", "Node_B", 4.2);
        cityGraph.addUndirectedEdge("Node_B", "Node_C", 3.1);
        cityGraph.addUndirectedEdge("Node_A", "Node_C", 7.8);
        cityGraph.addUndirectedEdge("Node_C", "Hosp_Jinnah", 2.5);
        cityGraph.addUndirectedEdge("Node_D", "Node_C", 4.0);
        cityGraph.addUndirectedEdge("Node_D", "Hosp_Mayo", 5.1);
        cityGraph.addUndirectedEdge("Hosp_Jinnah", "Hosp_Mayo", 6.8);
    }

    private void showWarning(String heading, String body) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(heading);
        alert.setHeaderText(null);
        alert.setContentText(body);
        alert.showAndWait();
    }

    private VBox createStyledFormCard(String titleText) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #0F172A; -fx-border-color: #334155; -fx-border-radius: 6; -fx-background-radius: 6;");
        Label cardTitle = new Label(titleText);
        cardTitle.setFont(Font.font("Helvetica", FontWeight.BOLD, 12));
        cardTitle.setTextFill(Color.web("#38BDF8"));
        card.getChildren().add(cardTitle);
        return card;
    }

    private TextField createStyledTextField(String promptText) {
        TextField tf = new TextField();
        tf.setPromptText(promptText);
        tf.setStyle("-fx-background-color: #1E293B; -fx-text-fill: #F8FAFC; -fx-prompt-text-fill: #64748B; -fx-border-color: #334155; -fx-border-radius: 4; -fx-background-radius: 4;");
        return tf;
    }

    private Button createStyledButton(String text, String hexColor) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setFont(Font.font("Helvetica", FontWeight.BOLD, 11));
        btn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 8 0 8 0;", hexColor));
        return btn;
    }

    public static void main(String[] args) {
        launch(args);
    }
}