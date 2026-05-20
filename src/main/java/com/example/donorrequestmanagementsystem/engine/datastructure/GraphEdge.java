package com.example.donorrequestmanagementsystem.engine.datastructure;

public class GraphEdge {
    private final GraphNode source;
    private final GraphNode destination;
    private final double weight; // The actual road distance or transit time

    public GraphEdge(GraphNode source, GraphNode destination, double weight) {
        this.source = source;
        this.destination = destination;
        this.weight = weight;
    }

    public GraphNode getSource() { return source; }
    public GraphNode getDestination() { return destination; }
    public double getWeight() { return weight; }
}