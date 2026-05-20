package com.example.donorrequestmanagementsystem;

import com.example.donorrequestmanagementsystem.engine.ui.MainDashboard;

public class Launcher {
    public static void main(String[] args) {
        // Explicitly passing the target class redirects the runtime execution
        // to bypass premature JavaFX module visibility configuration checks.
        MainDashboard.main(args);
    }
}