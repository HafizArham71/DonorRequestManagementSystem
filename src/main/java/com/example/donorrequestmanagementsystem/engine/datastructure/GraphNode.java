package com.example.donorrequestmanagementsystem.engine.datastructure;

import java.util.ArrayList;
import java.util.List;

public class GraphNode {
    public enum NodeType { HOSPITAL, DONOR, INTERSECTION }

    private final String id;
    private final double x; // Physical X position on the visual map
    private final double y; // Physical Y position on the visual map
    private final NodeType type;
    private final List<GraphEdge> edges; // Adjacency list representation

    public GraphNode(String id, double x, double y, NodeType type) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.type = type;
        this.edges = new ArrayList<>();
    }

    public String getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public NodeType getType() { return type; }
    public List<GraphEdge> getEdges() { return edges; }

    public void addEdge(GraphNode destination, double distance) {
        this.edges.add(new GraphEdge(this, destination, distance));
    }
}