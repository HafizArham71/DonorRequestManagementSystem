module com.example.donorrequestmanagementsystem {
    // Require core JavaFX graphic dependency modules
    requires javafx.controls;
    requires javafx.fxml;

    // Allow JavaFX components to read and execute classes inside these directories
    exports com.example.donorrequestmanagementsystem;
    exports com.example.donorrequestmanagementsystem.engine.ui;

    // CRITICAL: Open your UI package to let JavaFX reflectively launch MainDashboard
    opens com.example.donorrequestmanagementsystem.engine.ui to javafx.graphics, javafx.fxml;
}