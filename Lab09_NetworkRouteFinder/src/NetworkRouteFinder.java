import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Network Route Finder.
 *
 * <p>Permanent state:
 * <ul>
 *   <li>{@code adjacency} - each vertex mapped to its neighbor list in insertion order.</li>
 *   <li>{@code undirectedEdges} - sorted endpoint pairs for physical undirected links.</li>
 * </ul>
 *
 * <p>Temporary state (visited, parent, distance, queue, path, color) is created
 * inside each traversal and never mutates the permanent graph.
 */
public class NetworkRouteFinder {

    private final Map<String, List<String>> adjacency;
    private final Set<List<String>> undirectedEdges;

    // Instrumentation for the complexity audit. These fields describe the most
    // recent traversal only; they are not part of the graph representation.
    private int lastVisitedCount;
    private int lastEdgeScanCount;
    private int lastMaxQueueSize;
    private int lastMaxPathLength;

    private enum Color {
        WHITE,
        GRAY,
        BLACK
    }

    public NetworkRouteFinder() {
        adjacency = new HashMap<>();
        undirectedEdges = new HashSet<>();
    }

    /**
     * Adds a vertex. Duplicate addition is harmless. Rejects null.
     */
    public void addVertex(String vertex) {
        if (vertex == null) {
            throw new IllegalArgumentException("vertex must not be null");
        }
        ensureVertex(vertex);
    }

    /**
     * Adds an undirected link when {@code bidirectional} is true, otherwise
     * only the directed edge {@code u -> v}. Missing endpoints are auto-added.
     *
     * <p>A duplicate relation is idempotent. A conflicting relation throws
     * before either permanent field changes.
     */
    public void addEdge(String u, String v, boolean bidirectional) {
        if (u == null || v == null) {
            throw new IllegalArgumentException("edge endpoints must not be null");
        }

        if (u.equals(v)) {
            addSelfLoop(u, bidirectional);
            return;
        }

        List<String> neighborsU = adjacency.get(u);
        List<String> neighborsV = adjacency.get(v);
        boolean hasUV = neighborsU != null && neighborsU.contains(v);
        boolean hasVU = neighborsV != null && neighborsV.contains(u);
        boolean isUndirected = undirectedEdges.contains(edgePair(u, v));

        // Check conflicts before ensureVertex or any list insertion. This keeps
        // a rejected call from leaving a partial mutation behind.
        if (bidirectional) {
            if (isUndirected) {
                return; // exact duplicate
            }
            if (hasUV || hasVU) {
                throw new IllegalStateException(
                        "conflicting relation between " + u + " and " + v
                                + ": a directed edge already exists");
            }
        } else {
            if (isUndirected) {
                throw new IllegalStateException(
                        "conflicting relation between " + u + " and " + v
                                + ": an undirected edge already exists");
            }
            if (hasUV) {
                return; // exact directed duplicate
            }
            if (hasVU) {
                throw new IllegalStateException(
                        "conflicting direction: " + v + " -> " + u + " already exists");
            }
        }

        ensureVertex(u);
        ensureVertex(v);

        if (bidirectional) {
            adjacency.get(u).add(v);
            adjacency.get(v).add(u);
            undirectedEdges.add(edgePair(u, v));
        } else {
            adjacency.get(u).add(v);
        }
    }

    /**
     * Returns the inclusive shortest-by-edge-count path from start to target.
     * Returns an empty list for null, missing, or unreachable endpoints.
     * Returns {@code [start]} when start equals target.
     */
    public List<String> breadthFirstSearch(String start, String target) {
        resetTraversalStats();

        if (start == null || target == null) {
            return List.of();
        }
        if (!adjacency.containsKey(start) || !adjacency.containsKey(target)) {
            return List.of();
        }
        if (start.equals(target)) {
            lastVisitedCount = 1;
            lastMaxQueueSize = 1;
            return List.of(start);
        }

        Set<String> visited = new HashSet<>();
        Map<String, String> parent = new HashMap<>();
        Map<String, Integer> distance = new HashMap<>();
        Deque<String> queue = new ArrayDeque<>();

        queue.addLast(start);
        visited.add(start);
        distance.put(start, 0);
        lastVisitedCount = 1;
        lastMaxQueueSize = 1;

        boolean found = false;

        while (!queue.isEmpty()) {
            String current = queue.removeFirst();

            if (current.equals(target)) {
                found = true;
                break;
            }

            for (String neighbor : adjacency.get(current)) {
                lastEdgeScanCount++;

                // Mark on enqueue, not on dequeue. The first discovery owns
                // the parent link, so later discoveries cannot overwrite it.
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    lastVisitedCount++;
                    parent.put(neighbor, current);
                    distance.put(neighbor, distance.get(current) + 1);
                    queue.addLast(neighbor);
                    lastMaxQueueSize = Math.max(lastMaxQueueSize, queue.size());
                }
            }
        }

        if (!found) {
            return List.of();
        }

        LinkedList<String> path = new LinkedList<>();
        String current = target;
        while (current != null) {
            path.addFirst(current);
            current = parent.get(current);
        }
        return List.copyOf(path);
    }

    /**
     * Returns the first DFS path using neighbor insertion order. This path is
     * valid but not promised shortest. Returns an empty list for null, missing,
     * or unreachable endpoints. Returns {@code [start]} when start equals target.
     */
    public List<String> depthFirstSearch(String start, String target) {
        resetTraversalStats();

        if (start == null || target == null) {
            return List.of();
        }
        if (!adjacency.containsKey(start) || !adjacency.containsKey(target)) {
            return List.of();
        }
        if (start.equals(target)) {
            lastVisitedCount = 1;
            lastMaxPathLength = 1;
            return List.of(start);
        }

        Set<String> visited = new HashSet<>();
        List<String> path = new ArrayList<>();

        boolean found = depthFirstSearchFrom(start, target, visited, path);
        return found ? List.copyOf(path) : List.of();
    }

    private boolean depthFirstSearchFrom(
            String current,
            String target,
            Set<String> visited,
            List<String> path) {

        visited.add(current);
        lastVisitedCount++;
        path.add(current);
        lastMaxPathLength = Math.max(lastMaxPathLength, path.size());

        if (current.equals(target)) {
            return true;
        }

        for (String neighbor : adjacency.get(current)) {
            lastEdgeScanCount++;
            if (!visited.contains(neighbor)) {
                if (depthFirstSearchFrom(neighbor, target, visited, path)) {
                    return true;
                }
            }
        }

        // Only the current path is unwound. The vertex stays in visited, which
        // is why a visited vertex and the recursion stack are different states.
        path.remove(path.size() - 1);
        return false;
    }

    /**
     * Returns true when any component contains a cycle. The outer loop starts a
     * new DFS at every white vertex, so later components are inspected too.
     */
    public boolean hasCycle() {
        resetTraversalStats();

        Map<String, Color> color = new HashMap<>();
        for (String vertex : adjacency.keySet()) {
            color.put(vertex, Color.WHITE);
        }

        for (String vertex : adjacency.keySet()) {
            if (color.get(vertex) == Color.WHITE) {
                if (hasCycleFrom(vertex, null, color)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasCycleFrom(String current, String parent, Map<String, Color> color) {
        color.put(current, Color.GRAY); // GRAY means active / inStack
        lastVisitedCount++;

        for (String neighbor : adjacency.get(current)) {
            lastEdgeScanCount++;

            if (neighbor.equals(current)) {
                return true; // self-loop
            }

            boolean undirected = undirectedEdges.contains(edgePair(current, neighbor));

            // The reverse direction of the same physical undirected edge is not
            // a second edge. Skip it only when the metadata says it is undirected.
            if (undirected && neighbor.equals(parent)) {
                continue;
            }

            Color neighborColor = color.get(neighbor);

            if (neighborColor == Color.WHITE) {
                if (hasCycleFrom(neighbor, current, color)) {
                    return true;
                }
            } else if (undirected) {
                // An undirected visited neighbor other than the parent closes a cycle.
                return true;
            } else if (neighborColor == Color.GRAY) {
                // Directed back-edge to an active vertex is a cycle.
                return true;
            }
            // A directed edge to a BLACK / finished vertex is not a cycle.
        }

        color.put(current, Color.BLACK);
        return false;
    }

    /**
     * Returns an independent copy of the adjacency lists.
     */
    public Map<String, List<String>> adjacencySnapshot() {
        Map<String, List<String>> copy = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : adjacency.entrySet()) {
            copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return copy;
    }

    /**
     * Returns an independent snapshot of undirected edges. Each edge is a
     * sorted two-element list, e.g. {@code [A, B]}.
     */
    public Set<List<String>> undirectedEdgesSnapshot() {
        return new HashSet<>(undirectedEdges);
    }

    public int getVertexCount() {
        return adjacency.size();
    }

    public int getAdjacencyEntryCount() {
        int count = 0;
        for (List<String> neighbors : adjacency.values()) {
            count += neighbors.size();
        }
        return count;
    }

    public int getUndirectedEdgeCount() {
        return undirectedEdges.size();
    }

    public int getDirectedEdgeCount() {
        int count = 0;
        for (Map.Entry<String, List<String>> entry : adjacency.entrySet()) {
            for (String neighbor : entry.getValue()) {
                if (!undirectedEdges.contains(edgePair(entry.getKey(), neighbor))) {
                    count++;
                }
            }
        }
        return count;
    }

    public int getLastVisitedCount() {
        return lastVisitedCount;
    }

    public int getLastEdgeScanCount() {
        return lastEdgeScanCount;
    }

    public int getLastMaxQueueSize() {
        return lastMaxQueueSize;
    }

    public int getLastMaxPathLength() {
        return lastMaxPathLength;
    }

    private void addSelfLoop(String vertex, boolean bidirectional) {
        boolean hasLoop = adjacency.containsKey(vertex)
                && adjacency.get(vertex).contains(vertex);
        boolean isUndirected = undirectedEdges.contains(edgePair(vertex, vertex));

        if (bidirectional) {
            if (isUndirected) {
                return; // exact duplicate
            }
            if (hasLoop) {
                throw new IllegalStateException(
                        "conflicting relation: a directed self-loop already exists at " + vertex);
            }
        } else {
            if (isUndirected) {
                throw new IllegalStateException(
                        "conflicting relation: an undirected self-loop already exists at " + vertex);
            }
            if (hasLoop) {
                return; // exact duplicate
            }
        }

        ensureVertex(vertex);
        adjacency.get(vertex).add(vertex); // exactly one entry, even for undirected
        if (bidirectional) {
            undirectedEdges.add(edgePair(vertex, vertex));
        }
    }

    private void ensureVertex(String vertex) {
        adjacency.computeIfAbsent(vertex, key -> new ArrayList<>());
    }

    private static List<String> edgePair(String u, String v) {
        if (u.compareTo(v) <= 0) {
            return List.of(u, v);
        }
        return List.of(v, u);
    }

    private void resetTraversalStats() {
        lastVisitedCount = 0;
        lastEdgeScanCount = 0;
        lastMaxQueueSize = 0;
        lastMaxPathLength = 0;
    }
}
