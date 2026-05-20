package com.example.donorrequestmanagementsystem.engine.datastructure;

import java.util.HashMap;
import java.util.Map;

public class RouteGraph {
    private final Map<String, GraphNode> nodes;

    public RouteGraph() {
        this.nodes = new HashMap<>();
    }

    public void addNode(String id, double x, double y, GraphNode.NodeType type) {
        if (!nodes.containsKey(id)) {
            nodes.put(id, new GraphNode(id, x, y, type));
        }
    }

    /**
     * Creates an undirected road connection between two physical points.
     */
    public void addUndirectedEdge(String sourceId, String destId, double weight) {
        GraphNode u = nodes.get(sourceId);
        GraphNode v = nodes.get(destId);

        if (u != null && v != null) {
            u.addEdge(v, weight);
            v.addEdge(u, weight);
        }
    }

    public GraphNode getNode(String id) {
        return nodes.get(id);
    }

    public Map<String, GraphNode> getAllNodes() {
        return nodes;
    }
}