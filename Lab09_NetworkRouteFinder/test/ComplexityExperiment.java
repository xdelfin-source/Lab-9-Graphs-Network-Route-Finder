import java.util.List;

/**
 * Actual measured counts for the Lab 09 complexity audit.
 *
 * Counter definitions:
 *   visited  - a vertex is counted the first time it is marked visited:
 *              BFS enqueue, DFS entry, or cycle-DFS entry.
 *   scan     - one adjacency-list entry is examined. An undirected physical
 *              edge contributes two entries (one at each endpoint); a directed
 *              edge contributes one.
 *   maxQueue - largest BFS queue observed.
 *   maxPath  - largest DFS path / recursion stack observed.
 *
 * The program prints real output from NetworkRouteFinder; no numbers below are
 * hand-written predictions.
 */
public class ComplexityExperiment {

    public static void main(String[] args) {
        printDefinitions();
        runLabGraph();

        System.out.println();
        System.out.println("=== SHAPE 1: sparse undirected path ===");
        for (int n : new int[] {100, 400, 1600}) {
            runPath(n);
        }

        System.out.println();
        System.out.println("=== SHAPE 2: dense undirected grid (mesh) ===");
        for (int k : new int[] {10, 20, 30}) {
            runGrid(k);
        }

        System.out.println();
        System.out.println("=== SHAPE 3: dense directed acyclic graph ===");
        for (int n : new int[] {20, 40, 80}) {
            runDag(n);
        }

        System.out.println();
        System.out.println("=== SHAPE 4: disconnected multi-component trees ===");
        for (int components : new int[] {2, 4, 8}) {
            runDisconnected(components, 100, 5);
        }

        System.out.println();
        System.out.println("=== CYCLE IN A LATER COMPONENT ===");
        runLaterComponentCycle();

        System.out.println();
        System.out.println("=== LAB GRAPH A-F EVIDENCE ===");
        runLabGraphEvidence();

        System.out.println();
        System.out.println("=== DOCUMENTED MIXED-GRAPH LIMITATION ===");
        runMixedGraphLimitation();
    }

    private static void printDefinitions() {
        System.out.println("COMPLEXITY EXPERIMENT - Lab 09 Network Route Finder");
        System.out.println("visited = first discovery/enqueue/entry count");
        System.out.println("scan    = adjacency-list entries examined");
        System.out.println("maxQueue= largest BFS frontier; maxPath = largest DFS recursion stack");
        System.out.println("An undirected edge appears twice in adjacency; a directed edge appears once.");
    }

    private static void runPath(int n) {
        NetworkRouteFinder graph = new NetworkRouteFinder();
        for (int i = 0; i + 1 < n; i++) {
            graph.addEdge("P" + i, "P" + (i + 1), true);
        }
        graph.addVertex("UNREACHABLE");

        String start = "P0";
        String target = "P" + (n - 1);

        System.out.printf("%nPATH n=%d  predicted V=%d E=%d adjacencyEntries=%d%n",
                n, n + 1, n - 1, 2 * (n - 1));
        System.out.printf("  actual V=%d E=%d adjacencyEntries=%d%n",
                graph.getVertexCount(), graph.getUndirectedEdgeCount(), graph.getAdjacencyEntryCount());

        List<String> path = graph.breadthFirstSearch(start, "UNREACHABLE");
        System.out.printf("  BFS to isolated target: vis=%d scan=%d maxQueue=%d path=%s%n",
                graph.getLastVisitedCount(), graph.getLastEdgeScanCount(),
                graph.getLastMaxQueueSize(), path);

        path = graph.breadthFirstSearch(start, target);
        System.out.printf("  BFS to farthest target: vis=%d scan=%d maxQueue=%d pathEdges=%d%n",
                graph.getLastVisitedCount(), graph.getLastEdgeScanCount(),
                graph.getLastMaxQueueSize(), path.size() - 1);

        path = graph.depthFirstSearch(start, "UNREACHABLE");
        System.out.printf("  DFS to isolated target: vis=%d scan=%d maxPath=%d path=%s%n",
                graph.getLastVisitedCount(), graph.getLastEdgeScanCount(),
                graph.getLastMaxPathLength(), path);

        path = graph.depthFirstSearch(start, target);
        System.out.printf("  DFS to farthest target: vis=%d scan=%d maxPath=%d pathEdges=%d%n",
                graph.getLastVisitedCount(), graph.getLastEdgeScanCount(),
                graph.getLastMaxPathLength(), path.size() - 1);

        boolean cycle = graph.hasCycle();
        System.out.printf("  hasCycle: result=%s vis=%d scan=%d%n",
                cycle, graph.getLastVisitedCount(), graph.getLastEdgeScanCount());
    }

    private static void runGrid(int k) {
        NetworkRouteFinder graph = new NetworkRouteFinder();
        for (int r = 0; r < k; r++) {
            for (int c = 0; c < k; c++) {
                graph.addVertex(cell(r, c));
            }
        }
        for (int r = 0; r < k; r++) {
            for (int c = 0; c < k; c++) {
                if (c + 1 < k) {
                    graph.addEdge(cell(r, c), cell(r, c + 1), true);
                }
                if (r + 1 < k) {
                    graph.addEdge(cell(r, c), cell(r + 1, c), true);
                }
            }
        }
        graph.addVertex("UNREACHABLE");

        String start = cell(0, 0);
        String target = cell(k - 1, k - 1);
        int vertices = k * k;
        int edges = 2 * k * (k - 1);

        System.out.printf("%nGRID %dx%d  predicted V=%d E=%d adjacencyEntries=%d%n",
                k, k, vertices + 1, edges, 2 * edges);
        System.out.printf("  actual V=%d E=%d adjacencyEntries=%d%n",
                graph.getVertexCount(), graph.getUndirectedEdgeCount(), graph.getAdjacencyEntryCount());

        List<String> path = graph.breadthFirstSearch(start, "UNREACHABLE");
        System.out.printf("  BFS to isolated target: vis=%d scan=%d maxQueue=%d path=%s%n",
                graph.getLastVisitedCount(), graph.getLastEdgeScanCount(),
                graph.getLastMaxQueueSize(), path);

        path = graph.breadthFirstSearch(start, target);
        System.out.printf("  BFS corner-to-corner: vis=%d scan=%d maxQueue=%d pathEdges=%d%n",
                graph.getLastVisitedCount(), graph.getLastEdgeScanCount(),
                graph.getLastMaxQueueSize(), path.size() - 1);

        path = graph.depthFirstSearch(start, "UNREACHABLE");
        System.out.printf("  DFS to isolated target: vis=%d scan=%d maxPath=%d path=%s%n",
                graph.getLastVisitedCount(), graph.getLastEdgeScanCount(),
                graph.getLastMaxPathLength(), path);

        path = graph.depthFirstSearch(start, target);
        System.out.printf("  DFS corner-to-corner: vis=%d scan=%d maxPath=%d pathEdges=%d%n",
                graph.getLastVisitedCount(), graph.getLastEdgeScanCount(),
                graph.getLastMaxPathLength(), path.size() - 1);

        boolean cycle = graph.hasCycle();
        System.out.printf("  hasCycle: result=%s vis=%d scan=%d (early exit expected)%n",
                cycle, graph.getLastVisitedCount(), graph.getLastEdgeScanCount());
    }

    private static void runDag(int n) {
        NetworkRouteFinder graph = new NetworkRouteFinder();
        for (int i = 0; i < n; i++) {
            graph.addVertex("D" + i);
        }
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                graph.addEdge("D" + i, "D" + j, false);
            }
        }
        graph.addVertex("UNREACHABLE");

        int edges = n * (n - 1) / 2;
        System.out.printf("%nDAG n=%d  predicted V=%d E=%d adjacencyEntries=%d%n",
                n, n + 1, edges, edges);
        System.out.printf("  actual V=%d E_directed=%d adjacencyEntries=%d%n",
                graph.getVertexCount(), graph.getDirectedEdgeCount(), graph.getAdjacencyEntryCount());

        String start = "D0";
        String target = "D" + (n - 1);

        List<String> path = graph.breadthFirstSearch(start, "UNREACHABLE");
        System.out.printf("  BFS to isolated target: vis=%d scan=%d maxQueue=%d path=%s%n",
                graph.getLastVisitedCount(), graph.getLastEdgeScanCount(),
                graph.getLastMaxQueueSize(), path);

        path = graph.breadthFirstSearch(start, target);
        System.out.printf("  BFS shortest path: vis=%d scan=%d maxQueue=%d pathEdges=%d%n",
                graph.getLastVisitedCount(), graph.getLastEdgeScanCount(),
                graph.getLastMaxQueueSize(), path.size() - 1);

        path = graph.depthFirstSearch(start, "UNREACHABLE");
        System.out.printf("  DFS to isolated target: vis=%d scan=%d maxPath=%d path=%s%n",
                graph.getLastVisitedCount(), graph.getLastEdgeScanCount(),
                graph.getLastMaxPathLength(), path);

        path = graph.depthFirstSearch(start, target);
        System.out.printf("  DFS path: vis=%d scan=%d maxPath=%d pathEdges=%d%n",
                graph.getLastVisitedCount(), graph.getLastEdgeScanCount(),
                graph.getLastMaxPathLength(), path.size() - 1);

        boolean cycle = graph.hasCycle();
        System.out.printf("  hasCycle: result=%s vis=%d scan=%d (all black-edge scans)%n",
                cycle, graph.getLastVisitedCount(), graph.getLastEdgeScanCount());
    }

    private static void runDisconnected(int components, int size, int isolated) {
        NetworkRouteFinder graph = new NetworkRouteFinder();
        for (int c = 0; c < components; c++) {
            for (int v = 0; v < size; v++) {
                graph.addVertex(componentVertex(c, v));
            }
            for (int v = 0; v + 1 < size; v++) {
                graph.addEdge(componentVertex(c, v), componentVertex(c, v + 1), true);
            }
        }
        for (int i = 0; i < isolated; i++) {
            graph.addVertex("I" + i);
        }

        int vertices = components * size + isolated;
        int edges = components * (size - 1);

        System.out.printf("%nDISCONNECTED components=%d size=%d isolated=%d  predicted V=%d E=%d adjacencyEntries=%d%n",
                components, size, isolated, vertices, edges, 2 * edges);
        System.out.printf("  actual V=%d E=%d adjacencyEntries=%d%n",
                graph.getVertexCount(), graph.getUndirectedEdgeCount(), graph.getAdjacencyEntryCount());

        String start = componentVertex(0, 0);
        String target = componentVertex(1, 0);

        List<String> path = graph.breadthFirstSearch(start, target);
        System.out.printf("  BFS to other component: vis=%d scan=%d maxQueue=%d path=%s%n",
                graph.getLastVisitedCount(), graph.getLastEdgeScanCount(),
                graph.getLastMaxQueueSize(), path);

        path = graph.depthFirstSearch(start, target);
        System.out.printf("  DFS to other component: vis=%d scan=%d maxPath=%d path=%s%n",
                graph.getLastVisitedCount(), graph.getLastEdgeScanCount(),
                graph.getLastMaxPathLength(), path);

        boolean cycle = graph.hasCycle();
        System.out.printf("  hasCycle: result=%s vis=%d scan=%d (all components inspected)%n",
                cycle, graph.getLastVisitedCount(), graph.getLastEdgeScanCount());
    }

    private static void runLaterComponentCycle() {
        NetworkRouteFinder graph = new NetworkRouteFinder();
        for (int c = 0; c < 2; c++) {
            for (int v = 0; v < 50; v++) {
                graph.addVertex(componentVertex(c, v));
            }
            for (int v = 0; v + 1 < 50; v++) {
                graph.addEdge(componentVertex(c, v), componentVertex(c, v + 1), true);
            }
        }
        graph.addEdge("T0", "T1", true);
        graph.addEdge("T1", "T2", true);
        graph.addEdge("T2", "T0", true);

        System.out.printf("V=%d E=%d adjacencyEntries=%d%n",
                graph.getVertexCount(), graph.getUndirectedEdgeCount(), graph.getAdjacencyEntryCount());
        boolean cycle = graph.hasCycle();
        System.out.printf("hasCycle: result=%s vis=%d scan=%d (two tree components, then triangle)%n",
                cycle, graph.getLastVisitedCount(), graph.getLastEdgeScanCount());
    }

    private static void runLabGraph() {
        NetworkRouteFinder graph = buildLabGraph();

        System.out.printf("%nLAB GRAPH V=%d E=%d adjacencyEntries=%d%n",
                graph.getVertexCount(), graph.getUndirectedEdgeCount(), graph.getAdjacencyEntryCount());

        List<String> bfs = graph.breadthFirstSearch("A", "F");
        System.out.printf("BFS A->F: %s edges=%d vis=%d scan=%d maxQueue=%d%n",
                bfs, bfs.size() - 1, graph.getLastVisitedCount(), graph.getLastEdgeScanCount(),
                graph.getLastMaxQueueSize());

        List<String> dfs = graph.depthFirstSearch("A", "F");
        System.out.printf("DFS A->F: %s edges=%d vis=%d scan=%d maxPath=%d%n",
                dfs, dfs.size() - 1, graph.getLastVisitedCount(), graph.getLastEdgeScanCount(),
                graph.getLastMaxPathLength());

        boolean cycle = graph.hasCycle();
        System.out.printf("hasCycle: result=%s vis=%d scan=%d%n",
                cycle, graph.getLastVisitedCount(), graph.getLastEdgeScanCount());
    }

    private static void runLabGraphEvidence() {
        NetworkRouteFinder graph = buildLabGraph();

        List<String> bfs = graph.breadthFirstSearch("A", "F");
        System.out.printf("BFS A->F = %s, edges=%d, vis=%d, scan=%d, maxQueue=%d%n",
                bfs, bfs.size() - 1, graph.getLastVisitedCount(), graph.getLastEdgeScanCount(),
                graph.getLastMaxQueueSize());

        List<String> dfs = graph.depthFirstSearch("A", "F");
        System.out.printf("DFS A->F = %s, edges=%d, vis=%d, scan=%d, maxPath=%d%n",
                dfs, dfs.size() - 1, graph.getLastVisitedCount(), graph.getLastEdgeScanCount(),
                graph.getLastMaxPathLength());

        System.out.printf("hasCycle = %s, vis=%d, scan=%d%n",
                graph.hasCycle(), graph.getLastVisitedCount(), graph.getLastEdgeScanCount());

        NetworkRouteFinder dag = new NetworkRouteFinder();
        dag.addEdge("A", "B", false);
        dag.addEdge("A", "C", false);
        dag.addEdge("B", "D", false);
        dag.addEdge("C", "D", false);
        System.out.printf("DAG directed-to-black hasCycle = %s%n", dag.hasCycle());

        NetworkRouteFinder directedCycle = new NetworkRouteFinder();
        directedCycle.addEdge("A", "B", false);
        directedCycle.addEdge("B", "C", false);
        directedCycle.addEdge("C", "A", false);
        System.out.printf("directed-gray-back-edge hasCycle = %s%n", directedCycle.hasCycle());
    }

    private static void runMixedGraphLimitation() {
        NetworkRouteFinder graph = new NetworkRouteFinder();
        graph.addEdge("A", "B", true);
        graph.addEdge("B", "C", true);
        graph.addEdge("A", "C", false);

        System.out.println("Graph: A-B undirected, B-C undirected, A->C directed.");
        System.out.println("This mixed graph contains the cycle A->C->B->A.");
        System.out.printf("Workbook black-edge rule returns hasCycle=%s; this limitation is disclosed, not hidden.%n",
                graph.hasCycle());
    }

    private static NetworkRouteFinder buildLabGraph() {
        NetworkRouteFinder graph = new NetworkRouteFinder();
        graph.addEdge("A", "B", true);
        graph.addEdge("A", "C", true);
        graph.addEdge("B", "D", true);
        graph.addEdge("C", "D", true);
        graph.addEdge("C", "E", true);
        graph.addEdge("D", "F", true);
        graph.addEdge("E", "F", true);
        return graph;
    }

    private static String cell(int row, int column) {
        return "R" + row + "C" + column;
    }

    private static String componentVertex(int component, int vertex) {
        return "C" + component + "V" + vertex;
    }
}
