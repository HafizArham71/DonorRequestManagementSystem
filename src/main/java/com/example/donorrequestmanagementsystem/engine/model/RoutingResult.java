package com.example.donorrequestmanagementsystem.engine.model;

import com.example.donorrequestmanagementsystem.engine.datastructure.GraphNode;
import java.util.List;

public class RoutingResult {
    private final GraphNode donorNode;
    private final double totalDistance;
    private final List<String> completePath;

    public RoutingResult(GraphNode donorNode, double totalDistance, List<String> completePath) {
        this.donorNode = donorNode;
        this.totalDistance = totalDistance;
        this.completePath = completePath;
    }

    public GraphNode getDonorNode() { return donorNode; }
    public double getTotalDistance() { return totalDistance; }
    public List<String> getCompletePath() { return completePath; }
}