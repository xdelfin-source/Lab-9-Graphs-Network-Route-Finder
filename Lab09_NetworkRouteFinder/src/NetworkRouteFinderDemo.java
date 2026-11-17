import java.util.List;

/**
 * Optional defense demo. This class is not part of the graded API; it simply
 * prints the workbook graph and the three required questions.
 */
public class NetworkRouteFinderDemo {

    public static void main(String[] args) {
        NetworkRouteFinder graph = new NetworkRouteFinder();
        graph.addEdge("A", "B", true);
        graph.addEdge("A", "C", true);
        graph.addEdge("B", "D", true);
        graph.addEdge("C", "D", true);
        graph.addEdge("C", "E", true);
        graph.addEdge("D", "F", true);
        graph.addEdge("E", "F", true);

        System.out.println("Lab 09 Network Route Finder demo");
        System.out.println("V=" + graph.getVertexCount()
                + " E=" + graph.getUndirectedEdgeCount()
                + " adjacencyEntries=" + graph.getAdjacencyEntryCount());

        System.out.println();
        System.out.println("Adjacency:");
        graph.adjacencySnapshot().forEach(
                (vertex, neighbors) -> System.out.println("  " + vertex + " -> " + neighbors));

        System.out.println();
        System.out.println("Undirected edges: " + graph.undirectedEdgesSnapshot());

        List<String> bfs = graph.breadthFirstSearch("A", "F");
        System.out.println();
        System.out.println("BFS A -> F: " + bfs + " (" + (bfs.size() - 1) + " edges)");
        System.out.println("  fewest-edge guarantee: unweighted graph");

        List<String> dfs = graph.depthFirstSearch("A", "F");
        System.out.println("DFS A -> F: " + dfs + " (" + (dfs.size() - 1) + " edges)");
        System.out.println("  valid route, not promised shortest");

        System.out.println();
        System.out.println("hasCycle(): " + graph.hasCycle());
        System.out.println("  the lab graph has a cycle, e.g. A-B-D-C-A");
    }
}
