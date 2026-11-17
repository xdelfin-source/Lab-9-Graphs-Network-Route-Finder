# Lab 09 — Network Route Finder

CpE 211 · Module 2 · Plan → Test → Build → Audit

This project implements an unweighted graph with adjacency lists, BFS shortest-by-edge path reconstruction, DFS path finding, and directed/undirected cycle detection.

## Project location and isolation

```text
C:\Users\xamgd\Downloads\CpE211-Labs\Lab09_NetworkRouteFinder
```

The parent folder also contains Lab 7 and Lab 8. This lab is isolated:

- it compiles only its own `src/` and `test/` into its own `out/`;
- `run-tests.bat` changes to its own folder first;
- there is no generic `Main.java`;
- the parent `.vscode/settings.json` is not modified.

Open `Lab09_NetworkRouteFinder` directly in VS Code so the lab-local `.vscode/settings.json` applies.

## Quick start (Windows)

```powershell
cd C:\Users\xamgd\Downloads\CpE211-Labs\Lab09_NetworkRouteFinder
.\run-tests.bat
```

Expected final line:

```text
SUMMARY: 20 passed, 0 failed
```

Manual compile and test:

```powershell
$sources = @(Get-ChildItem src,test -Filter *.java | Select-Object -ExpandProperty FullName)
javac -d out @sources
java -ea -cp out LabTestRunner
```

Bash / macOS / Linux:

```bash
cd Lab09_NetworkRouteFinder
javac -d out src/*.java test/*.java
java -ea -cp out LabTestRunner
```

Run the defense demo:

```powershell
java -cp out NetworkRouteFinderDemo
```

Run the complexity experiment:

```powershell
.\run-complexity.bat
```

The measured output is also saved in `docs/complexity-results.txt`.

## Files

```text
src/NetworkRouteFinder.java          core graph, BFS, DFS, cycle detection
src/NetworkRouteFinderDemo.java      optional defense demo
test/LabTestRunner.java              dependency-free acceptance tests
test/ComplexityExperiment.java        actual measured counts
docs/initial-test-output.txt          pre-implementation TODO failures
docs/final-test-output.txt            final 20/20 test run
docs/complexity-results.txt           measured experiment output
AGENT_LOG.md                          full Plan → Test → Build → Audit record
DEFENSE_NOTES.md                      short live-defense study guide
```

## API

```java
new NetworkRouteFinder()
addVertex(String vertex)
addEdge(String u, String v, boolean bidirectional)
breadthFirstSearch(String start, String target)  // List<String>
depthFirstSearch(String start, String target)    // List<String>
hasCycle()                                       // boolean
```

Diagnostic methods used by tests and the audit:

```java
adjacencySnapshot()
undirectedEdgesSnapshot()
getVertexCount()
getAdjacencyEntryCount()
getUndirectedEdgeCount()
getDirectedEdgeCount()
getLastVisitedCount()
getLastEdgeScanCount()
getLastMaxQueueSize()
getLastMaxPathLength()
```

## Behavioral contract

- `null` vertices/endpoints are rejected with `IllegalArgumentException`.
- Missing endpoints are auto-added by `addEdge`.
- `bidirectional == true` adds both adjacency entries and one sorted metadata pair.
- `bidirectional == false` adds only `u -> v`; it does not create `v -> u`.
- Duplicate relations are idempotent.
- Conflicting relations throw `IllegalStateException` before either permanent field changes.
- Conflict rule is strict: one relation per unordered pair. After directed `A -> B`, both `addEdge("A","B",true)` and `addEdge("B","A",false)` are conflicts.
- BFS marks visited on enqueue and assigns a parent only on first discovery.
- DFS uses insertion order and is not asserted to be shortest.
- `hasCycle()` visits every white component; it can return as soon as a cycle is proven.
- Traversals do not mutate `adjacency` or `undirectedEdges`; snapshots prove this.

## Lab graph result

```text
A—B, A—C, B—D, C—D, C—E, D—F, E—F
V = 6, E = 7, adjacency entries = 14
BFS A -> F = [A, B, D, F]             3 edges, shortest
DFS A -> F = [A, B, D, C, E, F]       5 edges, valid but not shortest
hasCycle() = true                     e.g. A-B-D-C-A
```

## Complexity summary

The measured scan count is the number of adjacency-list entries examined. An undirected edge has two entries; a directed edge has one.

| Shape | V | E | Adjacency entries | BFS full scan | DFS full scan | Cycle scan |
|---|---:|---:|---:|---:|---:|---|
| Path n=1600 | 1601 | 1599 | 3198 | 3198 | 3198 | 3198 (acyclic, all components) |
| Grid 30×30 | 901 | 1740 | 3480 | 3480 | 3480 | 46 (cycle found early) |
| DAG n=80 | 81 | 3160 | 3160 | 3160 | 3160 | 3160 (all black-edge scans) |
| Disconnected 8×100 + 5 isolated | 805 | 792 | 1584 | 198 (one component) | 198 (one component) | 1584 (all components) |

The counts match `O(V + E)` work: every vertex in the explored component is discovered once, and every adjacency entry in that component is scanned once for a full traversal. Early exit explains smaller counts. Permanent storage is `O(V + E)`; temporary BFS/DFS/cycle state is `O(V)`.

## Known limitation (disclosed, not hidden)

The workbook's rule “directed edge to a finished/black vertex is not a cycle” is correct for pure directed graphs, and its case split is correct for pure undirected graphs. A mixed graph can be more subtle.

Example:

```text
A—B undirected, B—C undirected, A->C directed
```

This contains the cycle `A -> C -> B -> A`, but the required workbook rule sees `C` as black when `A -> C` is examined and returns `false`. The implementation follows the workbook exactly. This limitation is recorded in `AGENT_LOG.md` and printed by `ComplexityExperiment`.

## Student contribution and AI disclosure

The student selected the assignment, project location, strict conflict rule, undirected-tree test case, and the decision to follow the workbook's cycle rules. The undirected-tree assertion was drafted collaboratively by the agent because the student did not supply the final line in the conversation; it is not labeled as independently student-authored. The student must review or replace it before submission.

This project was completed with AI assistance. It is not claimed as independently authored by the student. See `AGENT_LOG.md` for the full record.

No GitHub publication is performed by this project.
