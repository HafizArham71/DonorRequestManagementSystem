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
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainDashboard extends Application {

    private final RouteGraph cityGraph = new RouteGraph();
    private final EmergencyRequestHeap requestHeap = new EmergencyRequestHeap(30);
    private final Map<Donor.BloodType, List<Donor>> bloodRegistry = new HashMap<>();

    private Canvas mapCanvas;
    private ListView<String> heapListView;
    private VBox logContentArea;
    private List<String> highlightedRoutePath = null;

    private int requestSequenceCounter = 1;
    private int customNodeCounter = 1;
    private String selectedNodeTypeToAdd = "INTERSECTION";

    @Override
    public void start(Stage primaryStage) {
        initializePhysicalTopology();

        HBox mainLayoutContainer = new HBox(0);
        mainLayoutContainer.setStyle("-fx-background-color: #090d16;");

        // ================================================================
        // 1. LEFT CONTROLS (SCROLLABLE UI CONTROL DOCK)
        // ================================================================
        VBox leftPanelContent = new VBox(15);
        leftPanelContent.setPadding(new Insets(20));
        leftPanelContent.setStyle("-fx-background-color: #0f172a;");

        VBox brandingBox = new VBox(2);
        Label title = new Label("NEXUS CORE");
        title.setStyle("-fx-font-family: 'Helvetica'; -fx-font-weight: bold; -fx-font-size: 20px; -fx-text-fill: #f8fafc;");
        Label subTitle = new Label("EMERGENCY LOGISTICS MONITOR");
        subTitle.setStyle("-fx-font-family: 'Helvetica'; -fx-font-weight: bold; -fx-font-size: 9px; -fx-text-fill: #38bdf8; -fx-letter-spacing: 1px;");
        brandingBox.getChildren().addAll(title, subTitle);
        leftPanelContent.getChildren().add(brandingBox);

        VBox mapToolCard = createStyledFormCard("TOPOLOGY INFRASTRUCTURE TOOL");
        ToggleGroup toolGroup = new ToggleGroup();
        RadioButton rbIntersection = createStyledRadioButton("Intersection Mode", toolGroup, true, "INTERSECTION");
        RadioButton rbHospital = createStyledRadioButton("Hospital Hub Mode", toolGroup, false, "HOSPITAL");
        mapToolCard.getChildren().addAll(rbIntersection, rbHospital);
        leftPanelContent.getChildren().add(mapToolCard);

        VBox donorCard = createStyledFormCard("PROVISION NEW DONOR NODE");
        TextField txtDonorName = createStyledTextField("Full Name");
        TextField txtDonorPhone = createStyledTextField("Phone (e.g., 03001234567)");

        ComboBox<String> cbBloodGroup = new ComboBox<>();
        cbBloodGroup.getItems().addAll("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-");
        cbBloodGroup.setPromptText("Select Blood Profile");
        cbBloodGroup.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> cbDonorNode = new ComboBox<>();
        refreshNodeSelectorDropdowns(cbDonorNode, null);
        cbDonorNode.setPromptText("Bind to Node Anchor");
        cbDonorNode.setMaxWidth(Double.MAX_VALUE);

        Button btnRegisterDonor = createStyledButton("REGISTER DONOR ASSET", "#2563eb", "#3b82f6");
        donorCard.getChildren().addAll(txtDonorName, txtDonorPhone, cbBloodGroup, cbDonorNode, btnRegisterDonor);
        leftPanelContent.getChildren().add(donorCard);

        VBox emergencyCard = createStyledFormCard("EMERGENCY INTAKE PROTOCOL");
        TextField txtHospitalName = createStyledTextField("Receiving Health Facility");

        ComboBox<String> cbReqBlood = new ComboBox<>();
        cbReqBlood.getItems().addAll("O+", "O-", "A+", "A-", "B+", "B-", "AB+", "AB-");
        cbReqBlood.setPromptText("Required Phenotype");
        cbReqBlood.setMaxWidth(Double.MAX_VALUE);

        ComboBox<EmergencyRequest.UrgencyLevel> cbUrgency = new ComboBox<>();
        cbUrgency.getItems().addAll(EmergencyRequest.UrgencyLevel.values());
        cbUrgency.setPromptText("Assign Priority Urgency");
        cbUrgency.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> cbHospitalNode = new ComboBox<>();
        refreshNodeSelectorDropdowns(null, cbHospitalNode);
        cbHospitalNode.setPromptText("Target Destination Node");
        cbHospitalNode.setMaxWidth(Double.MAX_VALUE);

        Button btnRouteTrigger = createStyledButton("RUN DIJKSTRA PATHFINDER", "#dc2626", "#ef4444");
        emergencyCard.getChildren().addAll(txtHospitalName, cbReqBlood, cbUrgency, cbHospitalNode, btnRouteTrigger);
        leftPanelContent.getChildren().add(emergencyCard);

        ScrollPane leftScrollWrapper = new ScrollPane(leftPanelContent);
        leftScrollWrapper.setFitToWidth(true);
        leftScrollWrapper.setPrefWidth(350);
        leftScrollWrapper.setMinWidth(320);
        leftScrollWrapper.setMaxWidth(380);
        leftScrollWrapper.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScrollWrapper.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        leftScrollWrapper.setStyle("-fx-background: #0f172a; -fx-background-color: transparent; -fx-border-color: #334155; -fx-border-width: 0 1 0 0;");
        mainLayoutContainer.getChildren().add(leftScrollWrapper);

        // ================================================================
        // 2. CENTER PANEL (INTERACTIVE VECTOR CANVAS GRAPH)
        // ================================================================
        VBox canvasPanelBox = new VBox(10);
        canvasPanelBox.setPadding(new Insets(20));
        HBox.setHgrow(canvasPanelBox, Priority.ALWAYS);

        Label mapContextTitle = new Label("GEOSPATIAL TOPOLOGY NETWORK VECTOR");
        mapContextTitle.setStyle("-fx-font-family: 'Helvetica'; -fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #64748b; -fx-letter-spacing: 1px;");

        mapCanvas = new Canvas(500, 500);
        Pane canvasHolder = new Pane(mapCanvas);
        canvasHolder.setStyle("-fx-background-color: #020617; -fx-border-color: #1e293b; -fx-border-radius: 8; -fx-background-radius: 8; -fx-border-width: 1;");
        VBox.setVgrow(canvasHolder, Priority.ALWAYS);

        mapCanvas.widthProperty().bind(canvasHolder.widthProperty());
        mapCanvas.heightProperty().bind(canvasHolder.heightProperty());
        mapCanvas.widthProperty().addListener(evt -> drawMapInfrastructure());
        mapCanvas.heightProperty().addListener(evt -> drawMapInfrastructure());

        canvasPanelBox.getChildren().addAll(mapContextTitle, canvasHolder);
        mainLayoutContainer.getChildren().add(canvasPanelBox);

        // ================================================================
        // 3. RIGHT PANEL (HEAP MONITOR & REALTIME OUTPUT LOGS)
        // ================================================================
        VBox telemetryPanel = new VBox(15);
        telemetryPanel.setPadding(new Insets(20));
        telemetryPanel.setPrefWidth(320);
        telemetryPanel.setMinWidth(280);

        Label queueContextTitle = new Label("HEAP SYSTEM SORT MONITOR");
        queueContextTitle.setStyle("-fx-font-family: 'Helvetica'; -fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #64748b; -fx-letter-spacing: 1px;");

        heapListView = new ListView<>();
        heapListView.setStyle("-fx-background-color: #0f172a; -fx-text-fill: #e2e8f0; -fx-border-color: #1e293b; -fx-border-radius: 6; -fx-background-radius: 6;");
        VBox.setVgrow(heapListView, Priority.ALWAYS);

        VBox analyticalTerminalCard = new VBox(10);
        analyticalTerminalCard.setPadding(new Insets(12));
        analyticalTerminalCard.setStyle("-fx-background-color: #0f172a; -fx-border-color: #10b981; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");

        Label terminalTitle = new Label("REAL-TIME DISPATCH METRICS");
        terminalTitle.setStyle("-fx-font-family: 'Helvetica'; -fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #10b981;");

        logContentArea = new VBox(5);
        Label defaultLogLine = new Label("System engines idle.\nAwaiting tracking vector triggers.");
        defaultLogLine.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px; -fx-text-fill: #475569;");
        logContentArea.getChildren().add(defaultLogLine);
        analyticalTerminalCard.getChildren().addAll(terminalTitle, logContentArea);

        telemetryPanel.getChildren().addAll(queueContextTitle, heapListView, analyticalTerminalCard);
        mainLayoutContainer.getChildren().add(telemetryPanel);

        // ================================================================
        // EVENT PIPELINE HANDLERS
        // ================================================================

        toolGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT != null) selectedNodeTypeToAdd = (String) newT.getUserData();
        });

        mapCanvas.setOnMouseClicked(event -> {
            double clickX = event.getX();
            double clickY = event.getY();

            if (clickX < 20 || clickX > mapCanvas.getWidth() - 20 || clickY < 20 || clickY > mapCanvas.getHeight() - 20) return;

            String systemGeneratedId = (selectedNodeTypeToAdd.equals("HOSPITAL") ? "Hosp_Custom_" : "Node_") + customNodeCounter++;
            GraphNode.NodeType targetType = selectedNodeTypeToAdd.equals("HOSPITAL") ? GraphNode.NodeType.HOSPITAL : GraphNode.NodeType.INTERSECTION;

            cityGraph.addNode(systemGeneratedId, clickX, clickY, targetType);

            for (GraphNode existingNode : cityGraph.getAllNodes().values()) {
                if (existingNode.getId().equals(systemGeneratedId)) continue;
                double distance = Math.sqrt(Math.pow(existingNode.getX() - clickX, 2) + Math.pow(existingNode.getY() - clickY, 2));
                if (distance < 180.0) {
                    cityGraph.addUndirectedEdge(systemGeneratedId, existingNode.getId(), distance / 50.0);
                }
            }

            refreshNodeSelectorDropdowns(cbDonorNode, cbHospitalNode);
            drawMapInfrastructure();
        });

        // VALIDATED REGISTER DONOR BACKEND TRANSITION PIPELINE
        btnRegisterDonor.setOnAction(e -> {
            String name = txtDonorName.getText().trim();
            String phone = txtDonorPhone.getText().trim();
            String bgStr = cbBloodGroup.getValue();
            String nodeTarget = cbDonorNode.getValue();

            if (name.isEmpty() || phone.isEmpty() || bgStr == null || nodeTarget == null) {
                showWarning("Fields Empty", "Please populate all matching configurations.");
                return;
            }

            try {
                GraphNode nodeRef = cityGraph.getNode(nodeTarget);
                if (nodeRef != null) {
                    Donor.BloodType parsedEnum = Donor.BloodType.fromString(bgStr);
                    String donorId = "DNR-" + (System.currentTimeMillis() % 1000);

                    // Invoking clean explicit validation constructor
                    Donor individualDonorProfile = new Donor(donorId, name, parsedEnum, phone, nodeTarget);

                    cityGraph.addNode(nodeTarget, nodeRef.getX(), nodeRef.getY(), GraphNode.NodeType.DONOR);
                    bloodRegistry.computeIfAbsent(parsedEnum, k -> new ArrayList<>()).add(individualDonorProfile);
                }
                txtDonorName.clear();
                txtDonorPhone.clear();
                refreshNodeSelectorDropdowns(cbDonorNode, cbHospitalNode);
                drawMapInfrastructure();
            } catch (IllegalArgumentException ex) {
                showWarning("Validation Error", ex.getMessage());
            }
        });

        // VALIDATED DISPATCH INTAKE PATHFINDER PIPELINE
        btnRouteTrigger.setOnAction(e -> {
            String hospital = txtHospitalName.getText().trim();
            String bgStr = cbReqBlood.getValue();
            EmergencyRequest.UrgencyLevel severity = cbUrgency.getValue();
            String targetDestinationNode = cbHospitalNode.getValue();

            if (hospital.isEmpty() || bgStr == null || severity == null || targetDestinationNode == null) {
                showWarning("Fields Empty", "Complete metric selection configurations.");
                return;
            }

            try {
                String idToken = "REQ-" + String.format("%03d", requestSequenceCounter++);
                Donor.BloodType parsedEnum = Donor.BloodType.fromString(bgStr);

                EmergencyRequest emergencyRequest = new EmergencyRequest(idToken, hospital, parsedEnum, targetDestinationNode, severity);
                requestHeap.insert(emergencyRequest);
                refreshHeapUI();

                List<RoutingResult> runtimeCalculatedRoutes = DijkstraEngine.findTopThreeDonors(cityGraph, targetDestinationNode);
                updateOutputConsole(runtimeCalculatedRoutes);
            } catch (IllegalArgumentException ex) {
                showWarning("Validation Error", ex.getMessage());
            }
        });

        Scene scene = new Scene(mainLayoutContainer, 1200, 720);
        primaryStage.setTitle("Nexus Emergency Dispatch Engine Layout Workspace");
        primaryStage.setScene(scene);
        primaryStage.show();

        drawMapInfrastructure();
    }

    private void drawMapInfrastructure() {
        if (mapCanvas == null) return;
        GraphicsContext gc = mapCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, mapCanvas.getWidth(), mapCanvas.getHeight());

        gc.setStroke(Color.web("#0f172a"));
        gc.setLineWidth(1);
        for (int x = 0; x < mapCanvas.getWidth(); x += 40) gc.strokeLine(x, 0, x, mapCanvas.getHeight());
        for (int y = 0; y < mapCanvas.getHeight(); y += 40) gc.strokeLine(0, y, mapCanvas.getWidth(), y);

        gc.setStroke(Color.web("#334155"));
        gc.setLineWidth(1.5);
        for (GraphNode node : cityGraph.getAllNodes().values()) {
            for (GraphEdge edge : node.getEdges()) {
                gc.strokeLine(node.getX(), node.getY(), edge.getDestination().getX(), edge.getDestination().getY());
            }
        }

        if (highlightedRoutePath != null && highlightedRoutePath.size() > 1) {
            gc.setStroke(Color.web("#10b981"));
            gc.setLineWidth(4);

            DropShadow pathGlow = new DropShadow();
            pathGlow.setRadius(10.0);
            pathGlow.setColor(Color.web("#10b981"));
            gc.setEffect(pathGlow);

            for (int i = 0; i < highlightedRoutePath.size() - 1; i++) {
                GraphNode current = cityGraph.getNode(highlightedRoutePath.get(i));
                GraphNode next = cityGraph.getNode(highlightedRoutePath.get(i + 1));
                if (current != null && next != null) {
                    gc.strokeLine(current.getX(), current.getY(), next.getX(), next.getY());
                }
            }
            gc.setEffect(null);
        }

        for (GraphNode node : cityGraph.getAllNodes().values()) {
            if (node.getType() == GraphNode.NodeType.HOSPITAL) {
                gc.setFill(Color.web("#ef4444"));
                gc.fillRect(node.getX() - 7, node.getY() - 7, 14, 14);
            } else if (node.getType() == GraphNode.NodeType.DONOR) {
                gc.setFill(Color.web("#3b82f6"));
                gc.fillOval(node.getX() - 6, node.getY() - 6, 12, 12);
            } else {
                gc.setFill(Color.web("#475569"));
                gc.fillOval(node.getX() - 4, node.getY() - 4, 8, 8);
            }

            gc.setFill(Color.web("#94a3b8"));
            gc.setFont(javafx.scene.text.Font.font("Consolas", 10));
            gc.fillText(node.getId(), node.getX() + 10, node.getY() + 4);
        }
    }

    private void refreshNodeSelectorDropdowns(ComboBox<String> donorDrop, ComboBox<String> hospDrop) {
        if (donorDrop != null) donorDrop.getItems().clear();
        if (hospDrop != null) hospDrop.getItems().clear();

        for (GraphNode node : cityGraph.getAllNodes().values()) {
            if (donorDrop != null && node.getType() == GraphNode.NodeType.INTERSECTION) {
                donorDrop.getItems().add(node.getId());
            }
            if (hospDrop != null && node.getType() == GraphNode.NodeType.HOSPITAL) {
                hospDrop.getItems().add(node.getId());
            }
        }
    }

    private void refreshHeapUI() {
        heapListView.getItems().clear();
        EmergencyRequestHeap visualizationCache = new EmergencyRequestHeap(30);

        while (!requestHeap.isEmpty()) {
            EmergencyRequest trackingRef = requestHeap.poll();
            visualizationCache.insert(trackingRef);
            heapListView.getItems().add(String.format(" [%s] %s -> %s",
                    trackingRef.getUrgency() != null ? trackingRef.getUrgency().name() : "LOW",
                    trackingRef.getRequestId(), trackingRef.getHospitalName()));
        }

        while (!visualizationCache.isEmpty()) {
            requestHeap.insert(visualizationCache.poll());
        }
    }

    private void updateOutputConsole(List<RoutingResult> calculatedResults) {
        logContentArea.getChildren().clear();
        if (calculatedResults == null || calculatedResults.isEmpty()) {
            Label nullAlert = new Label("❌ NO PATH VECTOR DETECTED");
            nullAlert.setStyle("-fx-font-family: 'Consolas'; -fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #ef4444;");
            logContentArea.getChildren().add(nullAlert);
            highlightedRoutePath = null;
            drawMapInfrastructure();
            return;
        }

        for (int idx = 0; idx < Math.min(calculatedResults.size(), 2); idx++) {
            RoutingResult singleTrace = calculatedResults.get(idx);
            String outputReportString = String.format("RANK %d [%s]\n Metric: %.2f km\n Vector: %s",
                    idx + 1, singleTrace.getDonorNode().getId(), singleTrace.getTotalDistance(),
                    String.join("->", singleTrace.getCompletePath())
            );

            Label telemetryMetricsBlockLabel = new Label(outputReportString);
            telemetryMetricsBlockLabel.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");
            telemetryMetricsBlockLabel.setTextFill(idx == 0 ? Color.web("#34d399") : Color.web("#64748b"));
            logContentArea.getChildren().add(telemetryMetricsBlockLabel);
        }

        highlightedRoutePath = calculatedResults.get(0).getCompletePath();
        drawMapInfrastructure();
    }

    private void initializePhysicalTopology() {
        cityGraph.addNode("Node_A", 80, 120, GraphNode.NodeType.INTERSECTION);
        cityGraph.addNode("Node_B", 240, 90, GraphNode.NodeType.INTERSECTION);
        cityGraph.addNode("Node_C", 380, 200, GraphNode.NodeType.INTERSECTION);
        cityGraph.addNode("Node_D", 180, 320, GraphNode.NodeType.INTERSECTION);
        cityGraph.addNode("Hosp_Jinnah", 480, 140, GraphNode.NodeType.HOSPITAL);
        cityGraph.addNode("Hosp_Mayo", 460, 340, GraphNode.NodeType.HOSPITAL);

        cityGraph.addUndirectedEdge("Node_A", "Node_B", 3.8);
        cityGraph.addUndirectedEdge("Node_B", "Node_C", 2.9);
        cityGraph.addUndirectedEdge("Node_A", "Node_C", 6.4);
        cityGraph.addUndirectedEdge("Node_C", "Hosp_Jinnah", 1.9);
        cityGraph.addUndirectedEdge("Node_D", "Node_C", 3.5);
        cityGraph.addUndirectedEdge("Node_D", "Hosp_Mayo", 4.2);
        cityGraph.addUndirectedEdge("Hosp_Jinnah", "Hosp_Mayo", 5.7);
    }

    private void showWarning(String modalHeader, String modalMessageContext) {
        Alert dialogueAlertBox = new Alert(Alert.AlertType.WARNING);
        dialogueAlertBox.setTitle(modalHeader);
        dialogueAlertBox.setHeaderText(null);
        dialogueAlertBox.setContentText(modalMessageContext);
        dialogueAlertBox.showAndWait();
    }

    private VBox createStyledFormCard(String titleText) {
        VBox componentCardLayoutBox = new VBox(8);
        componentCardLayoutBox.setPadding(new Insets(12));
        componentCardLayoutBox.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-radius: 6; -fx-background-radius: 6;");

        Label structuralSectionTitleLabel = new Label(titleText);
        structuralSectionTitleLabel.setStyle("-fx-font-family: 'Helvetica'; -fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #38bdf8;");
        componentCardLayoutBox.getChildren().add(structuralSectionTitleLabel);
        return componentCardLayoutBox;
    }

    private TextField createStyledTextField(String waterMarkPrompt) {
        TextField structuralInputField = new TextField();
        structuralInputField.setPromptText(waterMarkPrompt);
        structuralInputField.setStyle("-fx-background-color: #0f172a; -fx-text-fill: #f8fafc; -fx-prompt-text-fill: #475569; -fx-border-color: #334155; -fx-border-radius: 4; -fx-background-radius: 4; -fx-padding: 5;");
        return structuralInputField;
    }

    private RadioButton createStyledRadioButton(String labelText, ToggleGroup group, boolean selectDefault, String identityValue) {
        RadioButton rb = new RadioButton(labelText);
        rb.setToggleGroup(group);
        rb.setSelected(selectDefault);
        rb.setUserData(identityValue);
        rb.setTextFill(Color.web("#94a3b8"));
        rb.setStyle("-fx-font-family: 'Helvetica'; -fx-font-size: 11px; -fx-cursor: hand;");
        return rb;
    }

    private Button createStyledButton(String text, String normalColor, String hoverColor) {
        Button primaryActionBtn = new Button(text);
        primaryActionBtn.setMaxWidth(Double.MAX_VALUE);
        primaryActionBtn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: #ffffff; -fx-font-family: 'Helvetica'; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 6 0 6 0; -fx-border-radius: 4; -fx-background-radius: 4;", normalColor));
        primaryActionBtn.setOnMouseEntered(e -> primaryActionBtn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: #ffffff; -fx-font-family: 'Helvetica'; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 6 0 6 0; -fx-border-radius: 4; -fx-background-radius: 4;", hoverColor)));
        primaryActionBtn.setOnMouseExited(e -> primaryActionBtn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: #ffffff; -fx-font-family: 'Helvetica'; -fx-font-weight: bold; -fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 6 0 6 0; -fx-border-radius: 4; -fx-background-radius: 4;", normalColor)));
        return primaryActionBtn;
    }

    public static void main(String[] args) {
        launch(args);
    }
}