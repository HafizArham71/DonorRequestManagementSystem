package com.example.donorrequestmanagementsystem.engine.datastructure;

import com.example.donorrequestmanagementsystem.engine.model.RoutingResult;
import com.example.donorrequestmanagementsystem.engine.model.EmergencyRequest;

import java.util.*;

public class DijkstraEngine {

    private static class PathNode implements Comparable<PathNode> {
        String id;
        double currentShortestDistance;

        PathNode(String id, double currentShortestDistance) {
            this.id = id;
            this.currentShortestDistance = currentShortestDistance;
        }

        @Override
        public int compareTo(PathNode o) {
            return Double.compare(this.currentShortestDistance, o.currentShortestDistance);
        }
    }

    public static List<RoutingResult> findTopThreeDonors(RouteGraph graph, String hospitalNodeId) {
        GraphNode startNode = graph.getNode(hospitalNodeId);
        if (startNode == null) {
            throw new IllegalArgumentException("Target hospital node does not exist inside the routing matrix.");
        }

        Map<String, Double> distances = new HashMap<>();
        Map<String, String> parentNodes = new HashMap<>();
        PriorityQueue<PathNode> pq = new PriorityQueue<>();

        for (String nodeKey : graph.getAllNodes().keySet()) {
            distances.put(nodeKey, Double.MAX_VALUE);
        }

        distances.put(hospitalNodeId, 0.0);
        pq.add(new PathNode(hospitalNodeId, 0.0));

        while (!pq.isEmpty()) {
            PathNode current = pq.poll();
            String currentId = current.id;
            double currentDist = current.currentShortestDistance;

            if (currentDist > distances.get(currentId)) continue;

            GraphNode nodeObj = graph.getNode(currentId);
            if (nodeObj == null) continue;

            for (GraphEdge edge : nodeObj.getEdges()) {
                GraphNode neighbor = edge.getDestination();
                double newDist = currentDist + edge.getWeight();

                if (newDist < distances.get(neighbor.getId())) {
                    distances.put(neighbor.getId(), newDist);
                    parentNodes.put(neighbor.getId(), currentId);
                    pq.add(new PathNode(neighbor.getId(), newDist));
                }
            }
        }

        List<RoutingResult> donorMatches = new ArrayList<>();
        for (GraphNode node : graph.getAllNodes().values()) {
            if (node.getType() == GraphNode.NodeType.DONOR && distances.get(node.getId()) < Double.MAX_VALUE) {
                List<String> constructedPath = reconstructPath(parentNodes, node.getId(), hospitalNodeId);
                donorMatches.add(new RoutingResult(node, distances.get(node.getId()), constructedPath));
            }
        }

        donorMatches.sort(Comparator.comparingDouble(RoutingResult::getTotalDistance));

        if (donorMatches.size() > 3) {
            return donorMatches.subList(0, 3);
        }
        return donorMatches;
    }

    private static List<String> reconstructPath(Map<String, String> parents, String targetId, String startId) {
        LinkedList<String> path = new LinkedList<>();
        String step = targetId;

        if (parents.get(step) == null && !step.equals(startId)) {
            return path;
        }

        while (step != null) {
            path.addFirst(step);
            step = parents.get(step);
        }
        return path;
    }
}