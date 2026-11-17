import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Dependency-free acceptance tests for Lab 09 Network Route Finder.
 *
 * These tests were written before the BUILD phase replaced the starter TODOs.
 * They are designed to fail with UnsupportedOperationException until the graph
 * operations exist, then verify the workbook's behavioral contract.
 */
public class LabTestRunner {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        run("testEmptyGraph", LabTestRunner::testEmptyGraph);
        run("testSingleVertexAndStartEqualsTarget", LabTestRunner::testSingleVertexAndStartEqualsTarget);
        run("testNullAndMissingInputs", LabTestRunner::testNullAndMissingInputs);
        run("testLabGraphRepresentation", LabTestRunner::testLabGraphRepresentation);
        run("testBfsShortestPath", LabTestRunner::testBfsShortestPath);
        run("testDfsPathAndNotShortest", LabTestRunner::testDfsPathAndNotShortest);
        run("testDfsBacktracks", LabTestRunner::testDfsBacktracks);
        run("testDisconnectedAndIsolated", LabTestRunner::testDisconnectedAndIsolated);
        run("testStudentAuthoredUndirectedTreeIsNotACycle", LabTestRunner::testStudentAuthoredUndirectedTreeIsNotACycle);
        run("testUndirectedPathIsNotCycle", LabTestRunner::testUndirectedPathIsNotCycle);
        run("testUndirectedCycle", LabTestRunner::testUndirectedCycle);
        run("testSelfLoop", LabTestRunner::testSelfLoop);
        run("testDirectedAcyclicBlackEdge", LabTestRunner::testDirectedAcyclicBlackEdge);
        run("testDirectedCycle", LabTestRunner::testDirectedCycle);
        run("testCycleInLaterComponent", LabTestRunner::testCycleInLaterComponent);
        run("testDirectedEdgeNoReverse", LabTestRunner::testDirectedEdgeNoReverse);
        run("testDuplicateRelationsIdempotent", LabTestRunner::testDuplicateRelationsIdempotent);
        run("testConflictingRelationsAtomic", LabTestRunner::testConflictingRelationsAtomic);
        run("testTraversalImmutability", LabTestRunner::testTraversalImmutability);
        run("testMixedGraphCoexistence", LabTestRunner::testMixedGraphCoexistence);

        System.out.println();
        System.out.println("SUMMARY: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testEmptyGraph() {
        NetworkRouteFinder graph = new NetworkRouteFinder();

        check(graph.getVertexCount() == 0, "empty graph should have 0 vertices");
        check(graph.getAdjacencyEntryCount() == 0, "empty graph should have 0 adjacency entries");
        check(graph.getUndirectedEdgeCount() == 0, "empty graph should have 0 undirected edges");
        check(graph.adjacencySnapshot().isEmpty(), "empty graph snapshot should be empty");
        check(graph.undirectedEdgesSnapshot().isEmpty(), "empty graph undirected snapshot should be empty");

        check(graph.breadthFirstSearch("A", "B").isEmpty(), "BFS on empty graph should return []");
        check(graph.depthFirstSearch("A", "B").isEmpty(), "DFS on empty graph should return []");
        check(!graph.hasCycle(), "empty graph should not have a cycle");
    }

    private static void testSingleVertexAndStartEqualsTarget() {
        NetworkRouteFinder graph = new NetworkRouteFinder();
        graph.addVertex("A");
        graph.addVertex("A");

        check(graph.getVertexCount() == 1, "duplicate addVertex should not create a second vertex");
        checkEquals(Map.of("A", List.<String>of()), graph.adjacencySnapshot(),
                "single vertex should have an empty neighbor list");
        checkEquals(List.of("A"), graph.breadthFirstSearch("A", "A"),
                "BFS start == target should return [A]");
        checkEquals(List.of("A"), graph.depthFirstSearch("A", "A"),
                "DFS start == target should return [A]");
        check(!graph.hasCycle(), "single vertex should not have a cycle");
    }

    private static void testNullAndMissingInputs() {
        NetworkRouteFinder graph = new NetworkRouteFinder();

        expectThrows(IllegalArgumentException.class,
                () -> graph.addVertex(null), "addVertex(null) should reject null");
        expectThrows(IllegalArgumentException.class,
                () -> graph.addEdge(null, "B", true), "addEdge(null, B, true) should reject null");
        expectThrows(IllegalArgumentException.class,
                () -> graph.addEdge("A", null, false), "addEdge(A, null, false) should reject null");

        check(graph.breadthFirstSearch(null, "A").isEmpty(), "BFS(null, A) should return []");
        check(graph.breadthFirstSearch("A", null).isEmpty(), "BFS(A, null) should return []");
        check(graph.breadthFirstSearch("MISSING", "A").isEmpty(), "BFS missing start should return []");
        check(graph.breadthFirstSearch("A", "MISSING").isEmpty(), "BFS missing target should return []");
        check(graph.depthFirstSearch(null, "A").isEmpty(), "DFS(null, A) should return []");
        check(graph.depthFirstSearch("A", null).isEmpty(), "DFS(A, null) should return []");
        check(graph.depthFirstSearch("MISSING", "A").isEmpty(), "DFS missing start should return []");
        check(graph.depthFirstSearch("A", "MISSING").isEmpty(), "DFS missing target should return []");
    }

    private static void testLabGraphRepresentation() {
        NetworkRouteFinder graph = buildLabGraph();

        check(graph.getVertexCount() == 6, "lab graph should have 6 vertices");
        check(graph.getAdjacencyEntryCount() == 14,
                "lab graph should have 14 adjacency entries (7 undirected edges x 2)");
        check(graph.getUndirectedEdgeCount() == 7, "lab graph should have 7 undirected edges");
        check(graph.getDirectedEdgeCount() == 0, "lab graph has no directed-only edges");

        Map<String, List<String>> expectedAdjacency = Map.of(
                "A", List.of("B", "C"),
                "B", List.of("A", "D"),
                "C", List.of("A", "D", "E"),
                "D", List.of("B", "C", "F"),
                "E", List.of("C", "F"),
                "F", List.of("D", "E"));

        Set<List<String>> expectedUndirected = Set.of(
                List.of("A", "B"),
                List.of("A", "C"),
                List.of("B", "D"),
                List.of("C", "D"),
                List.of("C", "E"),
                List.of("D", "F"),
                List.of("E", "F"));

        checkEquals(expectedAdjacency, graph.adjacencySnapshot(),
                "lab graph adjacency snapshot should match the workbook");
        checkEquals(expectedUndirected, graph.undirectedEdgesSnapshot(),
                "lab graph undirected metadata should match the workbook");
    }

    private static void testBfsShortestPath() {
        NetworkRouteFinder graph = buildLabGraph();

        List<String> path = graph.breadthFirstSearch("A", "F");
        checkEquals(List.of("A", "B", "D", "F"), path,
                "BFS A -> F should use the fewest-edge path");
        check(path.size() - 1 == 3, "BFS A -> F should have 3 edges, but had " + (path.size() - 1));
        assertSimplePath(graph, path, "BFS A -> F");

        checkEquals(List.of("A", "C"), graph.breadthFirstSearch("A", "C"),
                "BFS A -> C should discover C directly from A");
        checkEquals(List.of("A", "B", "D"), graph.breadthFirstSearch("A", "D"),
                "BFS A -> D should use B before C because B is inserted first");
        checkEquals(List.of("A", "C", "E"), graph.breadthFirstSearch("A", "E"),
                "BFS A -> E should use the direct C route");
    }

    private static void testDfsPathAndNotShortest() {
        NetworkRouteFinder graph = buildLabGraph();

        List<String> dfs = graph.depthFirstSearch("A", "F");
        checkEquals(List.of("A", "B", "D", "C", "E", "F"), dfs,
                "DFS A -> F should follow insertion order through the lab graph");
        check(dfs.size() - 1 == 5, "DFS A -> F should have 5 edges, but had " + (dfs.size() - 1));
        assertSimplePath(graph, dfs, "DFS A -> F");

        List<String> bfs = graph.breadthFirstSearch("A", "F");
        check(dfs.size() - 1 >= bfs.size() - 1,
                "DFS must not be claimed shorter than BFS; DFS=" + (dfs.size() - 1)
                        + " edges, BFS=" + (bfs.size() - 1) + " edges");

        checkEquals(List.of("A", "B", "D", "C"), graph.depthFirstSearch("A", "C"),
                "DFS A -> C should follow the first branch even though BFS is shorter");
        checkEquals(List.of("A", "B", "D", "C", "E"), graph.depthFirstSearch("A", "E"),
                "DFS A -> E should follow the deep branch");
        check(graph.getLastMaxPathLength() == 5,
                "DFS A -> E should have a maximum recursion depth of 5, but was "
                        + graph.getLastMaxPathLength());
    }

    private static void testDfsBacktracks() {
        NetworkRouteFinder graph = new NetworkRouteFinder();
        graph.addEdge("A", "B", true);
        graph.addEdge("B", "C", true);   // dead-end branch
        graph.addEdge("A", "D", true);
        graph.addEdge("D", "E", true);

        checkEquals(List.of("A", "D", "E"), graph.depthFirstSearch("A", "E"),
                "DFS should backtrack out of the failed A-B-C branch and then reach E");
        checkEquals(List.of("A", "B", "C"), graph.depthFirstSearch("A", "C"),
                "DFS should still return the direct branch to C");
        check(!graph.hasCycle(), "the backtracking test graph is a tree, not a cycle");
    }

    private static void testDisconnectedAndIsolated() {
        NetworkRouteFinder graph = new NetworkRouteFinder();
        graph.addEdge("A", "B", true);
        graph.addEdge("C", "D", true);
        graph.addVertex("E");

        check(graph.getVertexCount() == 5, "disconnected graph should have 5 vertices");
        check(graph.breadthFirstSearch("A", "D").isEmpty(),
                "BFS should return [] when the target is in another component");
        check(graph.getLastVisitedCount() == 2,
                "BFS from A should visit only the A-B component, but visited "
                        + graph.getLastVisitedCount());
        check(graph.getLastEdgeScanCount() == 2,
                "BFS from A should scan both A-B entries, but scanned "
                        + graph.getLastEdgeScanCount());

        check(graph.depthFirstSearch("A", "D").isEmpty(),
                "DFS should return [] when the target is in another component");
        check(graph.breadthFirstSearch("A", "E").isEmpty(),
                "BFS should return [] for an isolated unreachable vertex");
        checkEquals(List.of("E"), graph.breadthFirstSearch("E", "E"),
                "BFS from an isolated vertex to itself should return [E]");
        check(!graph.hasCycle(), "two tree components and an isolated vertex have no cycle");
        check(graph.getLastVisitedCount() == 5,
                "hasCycle should inspect all 5 vertices, but visited " + graph.getLastVisitedCount());
        check(graph.getLastEdgeScanCount() == 4,
                "hasCycle should scan the 4 undirected adjacency entries, but scanned "
                        + graph.getLastEdgeScanCount());
    }

    /**
     * STUDENT-SELECTED TEST CASE.
     *
     * The student chose the undirected-tree parent-edge case. The assertion was
     * drafted collaboratively by the agent because the student did not supply
     * the final line in the conversation. The student must review or replace it
     * before submission. This is not labeled as independently student-authored.
     */
    private static void testStudentAuthoredUndirectedTreeIsNotACycle() {
        NetworkRouteFinder graph = new NetworkRouteFinder();
        graph.addEdge("A", "B", true);
        graph.addEdge("B", "C", true);
        graph.addEdge("B", "D", true);

        check(!graph.hasCycle(),
                "An undirected tree must not be reported as a cycle; the reverse "
                        + "link to the parent is the same physical edge.");
    }

    private static void testUndirectedPathIsNotCycle() {
        NetworkRouteFinder graph = new NetworkRouteFinder();
        graph.addEdge("A", "B", true);
        graph.addEdge("B", "C", true);
        graph.addEdge("C", "D", true);
        graph.addEdge("D", "E", true);

        check(!graph.hasCycle(), "a 5-vertex undirected path has no cycle");
        check(graph.getLastVisitedCount() == 5,
                "cycle detection on a path should visit all 5 vertices, but visited "
                        + graph.getLastVisitedCount());
        check(graph.getLastEdgeScanCount() == 8,
                "cycle detection on a path should scan 8 undirected adjacency entries, but scanned "
                        + graph.getLastEdgeScanCount());
    }

    private static void testUndirectedCycle() {
        NetworkRouteFinder triangle = new NetworkRouteFinder();
        triangle.addEdge("A", "B", true);
        triangle.addEdge("B", "C", true);
        triangle.addEdge("C", "A", true);
        check(triangle.hasCycle(), "an undirected triangle should be a cycle");

        NetworkRouteFinder squareWithTail = new NetworkRouteFinder();
        squareWithTail.addEdge("A", "B", true);
        squareWithTail.addEdge("B", "C", true);
        squareWithTail.addEdge("C", "D", true);
        squareWithTail.addEdge("D", "B", true);
        check(squareWithTail.hasCycle(), "an undirected cycle attached to a tail should be a cycle");
    }

    private static void testSelfLoop() {
        NetworkRouteFinder directed = new NetworkRouteFinder();
        directed.addEdge("A", "A", false);
        check(directed.hasCycle(), "a directed self-loop should be a cycle");
        checkEquals(Map.of("A", List.of("A")), directed.adjacencySnapshot(),
                "a directed self-loop should add one adjacency entry");
        check(directed.getUndirectedEdgeCount() == 0,
                "a directed self-loop should not add undirected metadata");
        check(directed.getDirectedEdgeCount() == 1,
                "a directed self-loop should count as one directed edge");

        directed.addEdge("A", "A", false);
        check(directed.getAdjacencyEntryCount() == 1,
                "duplicate directed self-loop should remain one adjacency entry");

        Map<String, List<String>> beforeDirected = directed.adjacencySnapshot();
        Set<List<String>> beforeDirectedEdges = directed.undirectedEdgesSnapshot();
        expectThrows(IllegalStateException.class,
                () -> directed.addEdge("A", "A", true),
                "adding an undirected self-loop over a directed one should conflict");
        checkEquals(beforeDirected, directed.adjacencySnapshot(),
                "failed directed/undirected self-loop conflict must not mutate adjacency");
        checkEquals(beforeDirectedEdges, directed.undirectedEdgesSnapshot(),
                "failed directed/undirected self-loop conflict must not mutate metadata");

        NetworkRouteFinder undirected = new NetworkRouteFinder();
        undirected.addEdge("B", "B", true);
        check(undirected.hasCycle(), "an undirected self-loop should be a cycle");
        checkEquals(Map.of("B", List.of("B")), undirected.adjacencySnapshot(),
                "an undirected self-loop should add one adjacency entry, not two");
        checkEquals(Set.of(List.of("B", "B")), undirected.undirectedEdgesSnapshot(),
                "an undirected self-loop should add one metadata pair");
        check(undirected.getAdjacencyEntryCount() == 1,
                "an undirected self-loop should have exactly one adjacency entry");

        undirected.addEdge("B", "B", true);
        check(undirected.getAdjacencyEntryCount() == 1,
                "duplicate undirected self-loop should remain one adjacency entry");

        Map<String, List<String>> beforeUndirected = undirected.adjacencySnapshot();
        Set<List<String>> beforeUndirectedEdges = undirected.undirectedEdgesSnapshot();
        expectThrows(IllegalStateException.class,
                () -> undirected.addEdge("B", "B", false),
                "adding a directed self-loop over an undirected one should conflict");
        checkEquals(beforeUndirected, undirected.adjacencySnapshot(),
                "failed undirected/directed self-loop conflict must not mutate adjacency");
        checkEquals(beforeUndirectedEdges, undirected.undirectedEdgesSnapshot(),
                "failed undirected/directed self-loop conflict must not mutate metadata");
    }

    private static void testDirectedAcyclicBlackEdge() {
        NetworkRouteFinder graph = new NetworkRouteFinder();
        graph.addEdge("A", "B", false);
        graph.addEdge("A", "C", false);
        graph.addEdge("B", "D", false);
        graph.addEdge("C", "D", false);

        check(!graph.hasCycle(),
                "a DAG with a directed edge to a finished/black vertex is not a cycle");
        check(graph.getDirectedEdgeCount() == 4, "the DAG should have 4 directed edges");
        check(graph.getUndirectedEdgeCount() == 0, "the DAG should have no undirected metadata");
        check(graph.getAdjacencyEntryCount() == 4, "the DAG should have 4 adjacency entries");

        NetworkRouteFinder forwardEdge = new NetworkRouteFinder();
        forwardEdge.addEdge("A", "B", false);
        forwardEdge.addEdge("B", "C", false);
        forwardEdge.addEdge("A", "C", false);
        check(!forwardEdge.hasCycle(),
                "a directed forward edge A -> C to a black vertex in a DAG is not a cycle");
    }

    private static void testDirectedCycle() {
        NetworkRouteFinder graph = new NetworkRouteFinder();
        graph.addEdge("A", "B", false);
        graph.addEdge("B", "C", false);
        graph.addEdge("C", "A", false);

        check(graph.hasCycle(),
                "a directed back-edge to a gray/inStack vertex should be a cycle");

        NetworkRouteFinder longer = new NetworkRouteFinder();
        longer.addEdge("A", "B", false);
        longer.addEdge("B", "C", false);
        longer.addEdge("C", "D", false);
        longer.addEdge("D", "B", false);
        check(longer.hasCycle(), "a directed cycle with a tail should be detected");
    }

    private static void testCycleInLaterComponent() {
        NetworkRouteFinder graph = new NetworkRouteFinder();
        graph.addEdge("A", "B", true);
        graph.addEdge("B", "C", true);   // component 1: tree

        graph.addEdge("D", "E", true);
        graph.addEdge("E", "F", true);
        graph.addEdge("F", "D", true);   // component 2: triangle

        check(graph.hasCycle(), "a cycle in the second component must be found");
        check(graph.breadthFirstSearch("A", "F").isEmpty(),
                "the two components are disconnected for BFS");
    }

    private static void testDirectedEdgeNoReverse() {
        NetworkRouteFinder graph = new NetworkRouteFinder();
        graph.addEdge("A", "B", false);

        checkEquals(Map.of("A", List.of("B"), "B", List.<String>of()),
                graph.adjacencySnapshot(),
                "directed A -> B should auto-add B with an empty list and no reverse entry");
        check(graph.getAdjacencyEntryCount() == 1, "directed A -> B has one adjacency entry");
        check(graph.getDirectedEdgeCount() == 1, "directed A -> B has one directed edge");
        check(graph.getUndirectedEdgeCount() == 0, "directed A -> B has no undirected metadata");

        checkEquals(List.of("A", "B"), graph.breadthFirstSearch("A", "B"),
                "forward BFS A -> B should use the directed edge");
        check(graph.breadthFirstSearch("B", "A").isEmpty(),
                "BFS B -> A must be empty because no reverse edge exists");
        check(graph.depthFirstSearch("B", "A").isEmpty(),
                "DFS B -> A must be empty because no reverse edge exists");
        check(!graph.hasCycle(), "a single directed edge is not a cycle");
    }

    private static void testDuplicateRelationsIdempotent() {
        NetworkRouteFinder graph = new NetworkRouteFinder();
        graph.addEdge("A", "B", true);

        Map<String, List<String>> beforeAdjacency = graph.adjacencySnapshot();
        Set<List<String>> beforeUndirected = graph.undirectedEdgesSnapshot();

        graph.addEdge("A", "B", true);
        graph.addEdge("B", "A", true);

        checkEquals(beforeAdjacency, graph.adjacencySnapshot(),
                "duplicate undirected relation must not duplicate neighbor entries");
        checkEquals(beforeUndirected, graph.undirectedEdgesSnapshot(),
                "duplicate undirected relation must not duplicate metadata");
        check(graph.getAdjacencyEntryCount() == 2,
                "A-B undirected should have exactly 2 adjacency entries");

        graph.addEdge("C", "D", false);
        graph.addEdge("C", "D", false);

        check(graph.getAdjacencyEntryCount() == 3,
                "duplicate directed relation should add only one entry to the existing 2");
        check(graph.getDirectedEdgeCount() == 1,
                "C -> D should count as one directed edge after duplicate calls");
        checkEquals(List.of("D"), graph.adjacencySnapshot().get("C"),
                "C should have one D neighbor after duplicate calls");
        check(graph.adjacencySnapshot().get("D").isEmpty(),
                "D should still have an empty list because C -> D is directed");
    }

    private static void testConflictingRelationsAtomic() {
        NetworkRouteFinder directed = new NetworkRouteFinder();
        directed.addEdge("A", "B", false);

        Map<String, List<String>> adjacentBefore = directed.adjacencySnapshot();
        Set<List<String>> undirectedBefore = directed.undirectedEdgesSnapshot();

        expectThrows(IllegalStateException.class,
                () -> directed.addEdge("A", "B", true),
                "adding an undirected A-B over directed A -> B should conflict");
        checkEquals(adjacentBefore, directed.adjacencySnapshot(),
                "failed directed-to-undirected conflict must not mutate adjacency");
        checkEquals(undirectedBefore, directed.undirectedEdgesSnapshot(),
                "failed directed-to-undirected conflict must not mutate metadata");

        expectThrows(IllegalStateException.class,
                () -> directed.addEdge("B", "A", false),
                "adding directed B -> A over directed A -> B should conflict under the strict rule");
        checkEquals(adjacentBefore, directed.adjacencySnapshot(),
                "failed reverse-direction conflict must not mutate adjacency");
        checkEquals(undirectedBefore, directed.undirectedEdgesSnapshot(),
                "failed reverse-direction conflict must not mutate metadata");

        expectThrows(IllegalStateException.class,
                () -> directed.addEdge("B", "A", true),
                "adding an undirected B-A over directed A -> B should conflict");
        checkEquals(adjacentBefore, directed.adjacencySnapshot(),
                "failed reverse undirected conflict must not mutate adjacency");
        checkEquals(undirectedBefore, directed.undirectedEdgesSnapshot(),
                "failed reverse undirected conflict must not mutate metadata");

        NetworkRouteFinder undirected = new NetworkRouteFinder();
        undirected.addEdge("C", "D", true);

        Map<String, List<String>> undirectedAdjacencyBefore = undirected.adjacencySnapshot();
        Set<List<String>> undirectedMetadataBefore = undirected.undirectedEdgesSnapshot();

        expectThrows(IllegalStateException.class,
                () -> undirected.addEdge("C", "D", false),
                "adding directed C -> D over undirected C-D should conflict");
        expectThrows(IllegalStateException.class,
                () -> undirected.addEdge("D", "C", false),
                "adding directed D -> C over undirected C-D should conflict");

        checkEquals(undirectedAdjacencyBefore, undirected.adjacencySnapshot(),
                "failed undirected-to-directed conflicts must not mutate adjacency");
        checkEquals(undirectedMetadataBefore, undirected.undirectedEdgesSnapshot(),
                "failed undirected-to-directed conflicts must not mutate metadata");
    }

    private static void testTraversalImmutability() {
        NetworkRouteFinder graph = buildLabGraph();
        Map<String, List<String>> adjacencyBefore = graph.adjacencySnapshot();
        Set<List<String>> undirectedBefore = graph.undirectedEdgesSnapshot();

        graph.breadthFirstSearch("A", "F");
        graph.depthFirstSearch("A", "F");
        graph.hasCycle();

        checkEquals(adjacencyBefore, graph.adjacencySnapshot(),
                "BFS, DFS, and hasCycle must not mutate adjacency");
        checkEquals(undirectedBefore, graph.undirectedEdgesSnapshot(),
                "BFS, DFS, and hasCycle must not mutate undirected metadata");

        graph.breadthFirstSearch(null, "A");
        graph.breadthFirstSearch("A", null);
        graph.breadthFirstSearch("MISSING", "A");
        graph.depthFirstSearch("A", "MISSING");

        checkEquals(adjacencyBefore, graph.adjacencySnapshot(),
                "invalid traversal inputs must not mutate adjacency");
        checkEquals(undirectedBefore, graph.undirectedEdgesSnapshot(),
                "invalid traversal inputs must not mutate undirected metadata");
    }

    private static void testMixedGraphCoexistence() {
        NetworkRouteFinder graph = new NetworkRouteFinder();
        graph.addEdge("A", "B", true);
        graph.addEdge("B", "C", false);
        graph.addEdge("C", "D", false);

        check(graph.getUndirectedEdgeCount() == 1, "mixed graph should have one undirected edge");
        check(graph.getDirectedEdgeCount() == 2, "mixed graph should have two directed edges");
        check(graph.getAdjacencyEntryCount() == 4,
                "mixed graph should have 4 adjacency entries (2 for A-B, 1 each for B-C and C-D)");
        check(!graph.hasCycle(), "the test mixed graph is a DAG");
        checkEquals(List.of("A", "B", "C", "D"), graph.breadthFirstSearch("A", "D"),
                "BFS should cross the undirected then directed edges");
        checkEquals(List.of("A", "B", "C", "D"), graph.depthFirstSearch("A", "D"),
                "DFS should cross the undirected then directed edges");
        check(graph.breadthFirstSearch("D", "A").isEmpty(),
                "the directed edges should not create a reverse route");
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

    private static void assertSimplePath(NetworkRouteFinder graph, List<String> path, String label) {
        check(!path.isEmpty(), label + " should not be empty");
        check(new HashSet<>(path).size() == path.size(),
                label + " should not repeat vertices, but was " + path);

        Map<String, List<String>> adjacency = graph.adjacencySnapshot();
        for (int i = 0; i + 1 < path.size(); i++) {
            String from = path.get(i);
            String to = path.get(i + 1);
            check(adjacency.containsKey(from) && adjacency.get(from).contains(to),
                    label + " has a non-adjacent step " + from + " -> " + to);
        }
    }

    private static void run(String name, Runnable test) {
        try {
            test.run();
            passed++;
            System.out.println("PASS: " + name);
        } catch (Throwable failure) {
            failed++;
            System.out.println("FAIL: " + name);
            System.out.println("      " + failure);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void checkEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + "\n      expected: " + expected + "\n      actual:   " + actual);
        }
    }

    private static void expectThrows(Class<? extends Throwable> expected, Runnable action, String message) {
        boolean thrown = false;
        try {
            action.run();
        } catch (Throwable failure) {
            thrown = true;
            if (!expected.isInstance(failure)) {
                throw new AssertionError(message + "\n      expected: " + expected.getSimpleName()
                        + "\n      actual:   " + failure.getClass().getSimpleName()
                        + ": " + failure.getMessage(), failure);
            }
        }
        if (!thrown) {
            throw new AssertionError(message + "\n      no exception was thrown");
        }
    }
}
