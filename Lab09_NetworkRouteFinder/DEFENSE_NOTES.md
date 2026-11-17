# Lab 09 — Defense Notes

## 30-second explanation

The graph is an adjacency list plus a set of undirected endpoint pairs. BFS uses a queue and parent links to return a fewest-edge path. DFS uses a visited set and a recursion path to return the first path by neighbor insertion order. Cycle detection uses colors: gray means the vertex is active on the recursion stack; black means finished. For an undirected edge, the reverse link to the DFS parent is skipped because it is the same physical edge.

## Permanent vs temporary state

| Permanent | Temporary |
|---|---|
| `adjacency` | BFS `queue`, `visited`, `parent`, `distance` |
| `undirectedEdges` | DFS `visited`, `path` / recursion stack |
| | cycle `color` / `inStack` |

Only `addVertex` and `addEdge` change permanent state. BFS, DFS, and `hasCycle()` are read-only on the graph. The before/after snapshot test proves this.

## Questions you should be ready to answer

**Why does BFS return a shortest path?**

BFS discovers vertices in increasing edge-count layers. A vertex is marked visited when it is enqueued, so its first parent is fixed at the smallest distance. When the target is dequeued, following parent links gives a minimum-edge route.

**Why is DFS not shortest?**

DFS follows the first unvisited neighbor deeply before trying other branches. In the lab graph it returns `A, B, D, C, E, F` (5 edges), while BFS returns `A, B, D, F` (3 edges). DFS answers “is there a path?” not “what is the fewest-edge path?”

**Why mark visited on enqueue instead of dequeue?**

If a vertex is marked only when dequeued, another vertex can enqueue it again before that happens. In the lab graph, `D` can be enqueued by both `B` and `C`, and `parent[D]` can be overwritten. The exact expected path `[A, B, D, F]` would become `[A, C, D, F]`. Marking on enqueue prevents duplicate frontier entries and preserves first discovery.

**Why store `undirectedEdges`?**

An adjacency list stores both directions of an undirected edge. When DFS at `B` sees its parent `A`, that reverse entry is the same physical edge and must be skipped. But a directed edge `B -> A` would be a real second arc. The metadata tells `hasCycle()` which case it is looking at.

**Why is a visited neighbor not always a directed cycle?**

A visited vertex can be BLACK/finished. A directed edge to a finished vertex is a cross or forward edge, not a back-edge. Only a GRAY/inStack neighbor proves a directed cycle. For undirected edges, a visited non-parent neighbor does close a cycle.

**Why check every component?**

A graph can have a tree in component 1 and a cycle in component 2. If `hasCycle()` stops after the first component, it returns `false` incorrectly. The outer loop starts a new DFS at every WHITE vertex; `testCycleInLaterComponent` proves this.

**Why does BFS's shortest guarantee need equal edge costs?**

BFS compares edge counts. With weights, a 5-edge route can cost less than a 2-edge route. BFS's layer order no longer matches cost order; Dijkstra is needed for nonnegative weights.

**What is the complexity?**

`O(V + E)` time for BFS, DFS, and cycle detection, because each vertex is discovered once and each adjacency entry is scanned once in a full traversal. An undirected edge has two adjacency entries; a directed edge has one. Auxiliary space is `O(V)` for visited/parent/distance/queue/path/color. Permanent adjacency storage is `O(V + E)`.

**Why does `addEdge` cost more than O(1)?**

The duplicate/conflict check scans the neighbor lists with `contains`, so it costs `O(deg(u) + deg(v))`. Adding the entry itself is amortized `O(1)` for an `ArrayList`.

## Known limitation to state honestly

The workbook rule “directed edge to black = not a cycle” is correct for pure directed graphs, and the parent-edge rule is correct for pure undirected graphs. A mixed graph can be subtler. For example:

```text
A—B undirected, B—C undirected, A -> C directed
```

This contains the cycle `A -> C -> B -> A`, but the required workbook rule sees `C` as black when `A -> C` is examined and returns `false`. The implementation follows the workbook exactly and discloses this limitation. Do not claim general mixed-graph cycle completeness.

## Conflict rule

The project uses the strict interpretation: one relation per unordered pair. After directed `A -> B`, adding undirected `A-B` or directed `B -> A` throws. Duplicate `A -> B` is idempotent. A rejected call leaves `adjacency` and `undirectedEdges` unchanged.

## Demo commands

```powershell
cd C:\Users\xamgd\Downloads\CpE211-Labs\Lab09_NetworkRouteFinder
.\run-tests.bat
java -cp out NetworkRouteFinderDemo
```

Expected test summary:

```text
SUMMARY: 20 passed, 0 failed
```

## AI-use honesty

This project was completed with AI assistance. The student selected the assignment and key decisions, but did not independently author all code. The student must review the code, the collaborative undirected-tree assertion, and these notes before the live defense. Do not present AI-generated work as independently authored.
