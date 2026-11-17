# Lab 09 · Network Route Finder — Agent Log

Course: CpE 211  
Workbook: `02_Graph_Network_Route_Finder_Lab.docx`  
Workbook header: LAB 2 · MODULE 2 — Network Route Finder  
Lecturer: Engr. Jamie Eduardo Rosal, MSCpE  
Workflow: Plan → Test → Build → Audit

> AI-use disclosure: This project was completed with AI assistance in VS Code through OpenCode. The student selected the assignment, reviewed the plan, chose the conflict rule and the student test case, and must review the final code and explanations before submission. AI-generated work is not claimed as independently authored by the student. The student-authored-test section records the exact division of work honestly.

---

## Phase 1 — PLAN

### 1.1 Scope and source of requirements

The implementation follows the workbook's technical contract and the user's Lab 9 master prompt. No requirements beyond those documents were invented.

| API / concept | Required behavior |
|---|---|
| `addVertex(v)` | Reject `null`; duplicate addition is harmless (idempotent). |
| `addEdge(u, v, bidirectional)` | Reject `null`; auto-add missing endpoints; `true` adds an undirected link, `false` adds `u -> v` only. |
| Duplicates | The same relation is idempotent; a conflicting relation throws before either permanent field changes. |
| `breadthFirstSearch(s, t)` | Inclusive shortest-by-edge-count path; empty if absent or unreachable; `[s]` if `s.equals(t)`. |
| `depthFirstSearch(s, t)` | First DFS path by neighbor insertion order; not promised shortest. |
| `hasCycle()` | Inspect every component; distinguish an undirected parent edge from a directed back-edge. |

### 1.2 Project location and isolation

Project folder:

```text
C:\Users\xamgd\Downloads\CpE211-Labs\Lab09_NetworkRouteFinder
```

The parent folder contains Lab 7 and Lab 8. To avoid collisions:

- Lab 9 compiles only its own `src/` and `test/` into its own `out/`.
- `run-tests.bat` and `run-tests.sh` change to the script's own folder before compiling or running.
- The core class is uniquely named `NetworkRouteFinder`; the optional demo is `NetworkRouteFinderDemo`.
- No generic `Main.java` is created.
- The parent `.vscode/settings.json` is left untouched because it currently points at `lab 7`.
- A lab-local `.vscode/settings.json` points at this lab's `src`, `test`, and `out`.

Planned layout:

```text
Lab09_NetworkRouteFinder/
  src/
    NetworkRouteFinder.java
    NetworkRouteFinderDemo.java
  test/
    LabTestRunner.java
    ComplexityExperiment.java
  docs/
    02_Graph_Network_Route_Finder_Lab.docx
    initial-test-output.txt
    final-test-output.txt
    complexity-results.txt
  .vscode/
    settings.json
  AGENT_LOG.md
  README.md
  DEFENSE_NOTES.md
  run-tests.bat
  run-tests.sh
  run-complexity.bat
```

### 1.3 Permanent vs temporary state

| State | Lifetime | Changes when | Used by |
|---|---|---|---|
| `adjacency` | Entire graph | `addVertex`, `addEdge` | Every operation; neighbor scans |
| `undirectedEdges` | Entire graph | `addEdge(..., true)` | Conflict detection; undirected parent-edge check |
| `visited` | One traversal | First discovery | BFS, DFS, cycle check |
| `parent` | One traversal | First discovery | Path reconstruction; undirected parent |
| `distance` | One BFS | First discovery | BFS distance evidence |
| `queue` | One BFS | Enqueue / dequeue | BFS frontier |
| `path` / recursion stack | One DFS | Enter / backtrack | DFS route and cycle state |
| `color` / `inStack` | One cycle check | Enter / finish | Directed cycle detection |

Permanent fields survive between calls. Temporary traversal fields are local variables or locals passed to a helper. BFS, DFS, and `hasCycle()` are read-only with respect to `adjacency` and `undirectedEdges`.

### 1.4 Lab graph and mutation ledger

Lab graph, all bidirectional:

```text
A—B, A—C, B—D, C—D, C—E, D—F, E—F
```

`V = 6`, `E = 7`, adjacency entries `= 2E = 14`.

| Call | `adjacency` delta | `undirectedEdges` delta |
|---|---|---|
| `addEdge(A,B,true)` | `A:[B]`, `B:[A]` | `+[A,B]` |
| `addEdge(A,C,true)` | `A:[B,C]`, `C:[A]` | `+[A,C]` |
| `addEdge(B,D,true)` | `B:[A,D]`, `D:[B]` | `+[B,D]` |
| `addEdge(C,D,true)` | `C:[A,D]`, `D:[B,C]` | `+[C,D]` |
| `addEdge(C,E,true)` | `C:[A,D,E]`, `E:[C]` | `+[C,E]` |
| `addEdge(D,F,true)` | `D:[B,C,F]`, `F:[D]` | `+[D,F]` |
| `addEdge(E,F,true)` | `E:[C,F]`, `F:[D,E]` | `+[E,F]` |

Final permanent state:

```text
adjacency = {
  A: [B, C],       B: [A, D],
  C: [A, D, E],    D: [B, C, F],
  E: [C, F],       F: [D, E]
}
undirectedEdges = { [A,B], [A,C], [B,D], [C,D], [C,E], [D,F], [E,F] }
```

#### Checkpoint 1 — representation snapshot

Start from the workbook's example:

```text
empty:
  adjacency = {}
  undirectedEdges = {}

addEdge("A","B",true):
  adjacency = { A: [B], B: [A] }
  undirectedEdges = { [A,B] }

then addEdge("B","D",false):
  adjacency = { A: [B], B: [A, D], D: [] }
  undirectedEdges = { [A,B] }        // no D -> B
```

Only `adjacency[B]` and vertex `D`'s empty list changed. The undirected metadata did not gain `[B,D]`, because `B -> D` is a directed arc.

### 1.5 BFS trace — Checkpoint 2

BFS uses local `queue`, `visited`, `parent`, and `distance`. A vertex is marked `visited` when enqueued, not when dequeued. Its `parent` is assigned only on first discovery.

| Step | Dequeue | Discover | Queue after | New parent / distance |
|---|---|---|---|---|
| 1 | A | B, C | `[B, C]` | `B <- A`, `C <- A`; distance 1 |
| 2 | B | D | `[C, D]` | `D <- B`; distance 2 |
| 3 | C | E | `[D, E]` | `E <- C`; distance 2 |
| 4 | D | F | `[E, F]` | `F <- D`; distance 3 |
| 5 | E | — | `[F]` | F already discovered |
| 6 | F | — | `[]` | target reached |

Parent map:

```text
parent = { A: null, B: A, C: A, D: B, E: C, F: D }
```

Path reconstruction:

```text
F -> D -> B -> A        reverse
A -> B -> D -> F        inclusive route
3 edges: minimum for this unweighted graph
```

Tie-breaking: `A`'s neighbor list is `[B, C]`. BFS enqueues `B` before `C`, so `D` is discovered from `B` before `C` is dequeued. `D` is enqueued before `E`, so `F` is discovered from `D` before `E` is dequeued. The alternative `A -> C -> E -> F` also has 3 edges but is not returned.

Before and after BFS, `adjacency` and `undirectedEdges` must be identical. Only the local queue, visited set, parent map, and distance map change.

### 1.6 DFS trace — Checkpoint 3

DFS uses a local `visited` set and a local `path` (the current recursion stack). Neighbors are examined in insertion order. `visited` remembers vertices after backtracking; `path` only contains the active recursion route.

| Step | Enter | Path / recursion stack | Visited | Next decision |
|---|---|---|---|---|
| 1 | A | `[A]` | `{A}` | B is the first unvisited neighbor |
| 2 | B | `[A,B]` | `{A,B}` | A visited; go to D |
| 3 | D | `[A,B,D]` | `{A,B,D}` | B visited; go to C |
| 4 | C | `[A,B,D,C]` | `{A,B,C,D}` | A, D visited; go to E |
| 5 | E | `[A,B,D,C,E]` | `{A,B,C,D,E}` | C visited; go to F |
| 6 | F | `[A,B,D,C,E,F]` | all | target found; 5 edges |

At the moment `F` is found, the recursion stack is `[A, B, D, C, E, F]`. The visited set still contains every entered vertex after backtracking, but the path shrinks when a branch fails. The DFS route is valid but not shortest:

| Traversal | Route from A to F | Edges | Guarantee |
|---|---|---|---|
| BFS | A, B, D, F | 3 | Fewest edges on an unweighted graph |
| DFS | A, B, D, C, E, F | 5 | A valid path; not promised shortest |

### 1.7 Cycle detection state cases

`hasCycle()` loops over every vertex. A white vertex starts a new component. `GRAY` means the vertex is active in the current recursion stack (`inStack`); `BLACK` means finished.

| Case | State observation | Decision |
|---|---|---|
| Self-loop `u -> u` | Neighbor equals current vertex | Cycle |
| Undirected reverse to parent | `neighbor.equals(parent[current])` and `[current, neighbor]` is in `undirectedEdges` | Skip; same physical edge |
| Undirected visited non-parent | The edge is undirected and the visited neighbor is not the parent | Cycle |
| Directed edge to gray | Neighbor color is GRAY (`inStack`) | Cycle (back edge) |
| Directed edge to black | Neighbor color is BLACK (finished) | Not a cycle |
| Disconnected component | Outer loop starts at every white vertex | Must inspect all components |

The implementation stores undirected edges as sorted two-element lists, e.g. `[A, B]`. This makes `[A, B]` and `[B, A]` the same metadata without a custom edge class.

Scope note: the workbook's black-edge rule is correct for pure directed graphs and its case split is correct for pure undirected graphs. A mixed graph can have a directed arc that closes a path through undirected edges and is not caught by the black rule. The required workbook behavior is implemented exactly; this limitation is disclosed in the Audit section rather than hidden.

### 1.8 Pseudocode

```text
addVertex(v):
    if v is null: throw IllegalArgumentException
    if adjacency does not contain v:
        adjacency[v] = new empty list

addEdge(u, v, bidirectional):
    if u is null or v is null: throw IllegalArgumentException

    if u == v:
        reject/accept according to the self-loop conflict rules
        add exactly one adjacency entry
        add [u,u] metadata only when bidirectional
        return

    hasUV = adjacency[u] contains v
    hasVU = adjacency[v] contains u
    isUndirected = undirectedEdges contains sortedPair(u,v)

    if bidirectional:
        if isUndirected: return                 // exact duplicate
        if hasUV or hasVU: throw conflict       // directed relation exists
    else:
        if isUndirected: throw conflict         // undirected relation exists
        if hasUV: return                        // exact directed duplicate
        if hasVU: throw conflict                // strict reversed-direction rule

    ensureVertex(u)
    ensureVertex(v)

    if bidirectional:
        adjacency[u].add(v)
        adjacency[v].add(u)
        undirectedEdges.add(sortedPair(u,v))
    else:
        adjacency[u].add(v)
```

```text
breadthFirstSearch(s, t):
    if s or t is null: return empty
    if s or t is missing: return empty
    if s == t: return [s]

    queue, visited, parent, distance = new local structures
    queue.addLast(s); visited.add(s); distance[s] = 0

    while queue is not empty:
        current = queue.removeFirst()
        if current == t: break
        for neighbor in adjacency[current] in insertion order:
            if neighbor not in visited:
                visited.add(neighbor)           // mark on enqueue
                parent[neighbor] = current      // first discovery only
                distance[neighbor] = distance[current] + 1
                queue.addLast(neighbor)

    if t not visited: return empty
    reconstruct t -> parent -> ... -> s, then reverse
    return inclusive path
```

```text
depthFirstSearch(s, t):
    if s or t is null: return empty
    if s or t is missing: return empty
    if s == t: return [s]

    visited = new local set
    path = new local list
    if dfs(s, t, visited, path): return copy of path
    return empty

dfs(current, target, visited, path):
    visited.add(current)
    path.add(current)
    if current == target: return true
    for neighbor in adjacency[current] in insertion order:
        if neighbor not in visited:
            if dfs(neighbor, target, visited, path): return true
    path.remove(last)
    return false
```

```text
hasCycle():
    color = WHITE for every vertex
    for each vertex v:
        if color[v] == WHITE:
            if cycleDfs(v, null, color): return true
    return false

cycleDfs(current, parent, color):
    color[current] = GRAY
    for neighbor in adjacency[current]:
        if neighbor == current: return true
        if [current,neighbor] is undirected and neighbor == parent:
            continue
        if color[neighbor] == WHITE:
            if cycleDfs(neighbor, current, color): return true
        else if [current,neighbor] is undirected:
            return true
        else if color[neighbor] == GRAY:
            return true
        // directed edge to BLACK: not a cycle
    color[current] = BLACK
    return false
```

### 1.9 Edge cases

- Empty graph: BFS/DFS return empty; `hasCycle()` is false.
- Single vertex: `start == target` returns `[s]`; `hasCycle()` is false.
- Missing start or target: return empty, do not throw.
- Disconnected target: return empty.
- Isolated vertices: no neighbor scans; cycle detection handles them.
- Multiple components: cycle detection must reach later components.
- Self-loop: directed and undirected forms are cycles; add only one adjacency entry.
- Undirected tree: reverse link to the DFS parent is the same physical edge and must be skipped.
- Undirected cycle: a visited non-parent undirected neighbor is a cycle.
- Directed gray back-edge: cycle.
- Directed black cross/forward edge in a DAG: not a cycle.
- Duplicate relation: idempotent, no repeated neighbor entry.
- Conflicting relation: throw before changing `adjacency` or `undirectedEdges`.
- Directed edge: no automatic reverse route.
- DFS dead end: backtrack without removing vertices from `visited`.
- `start == target` with a self-loop: still returns `[s]`; `hasCycle()` separately reports the loop.

### 1.10 Predicted complexity

Let `V` be vertices, `E` physical edges, and `A` adjacency entries (`A = 2E` for an undirected graph, `A = E` for a directed graph).

| Operation | Typical / expected time | Auxiliary space | Evidence to collect |
|---|---|---|---|
| Adjacency storage | `O(V + E)` | `O(V + E)` permanent space | 14 entries for `V=6, E=7` undirected |
| `addVertex` | `O(1)` average | `O(1)` | `HashMap` lookup/insert |
| `addEdge` | `O(deg(u) + deg(v))` | `O(1)` | Duplicate/conflict checks scan neighbor lists |
| BFS | `O(V + A)` = `O(V + E)` | `O(V)` | Visit vertices once; scan adjacency entries once |
| DFS | `O(V + A)` = `O(V + E)` | `O(V)` | Same work; recursion/path depth `O(V)` |
| Cycle detection | `O(V + A)` = `O(V + E)` | `O(V)` | Color map and recursion stack |
| Vertex map lookup | `O(1)` average | `O(1)` expected | `HashMap`; `O(V)` worst-case collisions |

Permanent graph storage and temporary traversal storage are separate. BFS's queue, visited, parent, and distance are `O(V)`; DFS's visited/path/recursion stack is `O(V)`; cycle detection's color map and recursion stack is `O(V)`. The snapshots used by tests are temporary copies, not part of traversal auxiliary space.

### 1.11 Plan-agent challenge and decisions

The Plan agent reviewed the plan without modifying files. Recommendations and decisions:

| Challenge | Exact state that could change | Decision |
|---|---|---|
| Undirected self-loop `addEdge("A","A",true)` can add `A` twice to its own list. | `adjacency["A"]` could become `[A, A]`; `undirectedEdges` could get duplicate metadata. | Special-case `u.equals(v)`; add one adjacency entry and one pair. Add tests. |
| Conflict checking after auto-adding endpoints can leave mutation behind. | A rejected call could add a new vertex or neighbor before throwing. | Perform all conflict checks before `ensureVertex` and before any list insertion. Test snapshots before/after. |
| BFS marked on dequeue can enqueue a vertex twice and overwrite its parent. | In the diamond `A-B, A-C, B-D, C-D`, `parent[D]` could become `C` instead of `B`; the returned path changes. | Mark visited on enqueue and assign parent only on first discovery. Test exact `[A,B,D,F]`. |
| DFS using `path` as `visited` can miss vertices after backtracking. | After a dead-end branch, a vertex removed from `path` would be treated as unvisited and could be re-entered. | Keep `visited` separate; remove only from `path` on backtrack. Add a dead-end test. |
| `hasCycle()` stopping after the first component misses a later cycle. | Component 1 is a tree, component 2 has the cycle; the boolean stays `false`. | Outer loop over every white vertex. Test a cycle in the second/third component. |
| The workbook's black-edge rule is not a complete mixed-graph cycle test. | Undirected path `A-B-C` plus directed `A -> C`: `C` is black when `A -> C` is examined, so the required rule says no cycle, although the mixed graph has one. | Follow the workbook exactly and disclose the limitation. Do not claim general mixed-graph correctness. |
| Strict reverse-direction rule is an interpretation of “conflicting direction.” | After directed `A -> B`, calling `addEdge("B","A",false)` could either add a 2-cycle or throw. | Student chose: throw. Document and test. |

Student decisions recorded:

- Project location: inside `CpE211-Labs`, isolated from Labs 7 and 8.
- Conflict rule: strict one-relation-per-unordered-pair.
- Student test case: undirected tree parent edge.
- Mixed-graph scope: follow workbook, document limitation.

### 1.12 Student-authored test contribution

The student selected the **undirected tree parent edge** case. The assertion text was not independently supplied in the conversation, so it is recorded as **collaborative**: student-selected case, agent-drafted assertion, student review required. It is not labeled as independently student-authored.

Planned shape:

```java
private static void testStudentAuthoredUndirectedTreeIsNotACycle() {
    NetworkRouteFinder graph = new NetworkRouteFinder();
    graph.addEdge("A", "B", true);
    graph.addEdge("B", "C", true);
    graph.addEdge("B", "D", true);

    check(!graph.hasCycle(),
            "An undirected tree must not be reported as a cycle; the reverse "
          + "link to the parent is the same physical edge.");
}
```

The student should replace or approve this assertion before submission. The test is meaningful because a naive “any visited neighbor is a cycle” implementation returns `true` for this tree.

---

## Phase 2 — TEST

### 2.1 Test-first setup

No instructor-provided starter files or tests were found in the workspace. The workbook was copied into `docs/` for reference. A starter `src/NetworkRouteFinder.java` was created with the required public signatures; every operation threw `UnsupportedOperationException("TODO: ...")` so the test runner could compile and fail for the intended behavioral reason.

`test/LabTestRunner.java` was written before the BUILD implementation. The baseline command was:

```powershell
$files = @(Get-ChildItem -Path src,test -Filter *.java | Select-Object -ExpandProperty FullName)
javac -d out @files
java -cp out LabTestRunner
```

The compile succeeded. The run produced **0 passed, 20 failed**. The full output is preserved in `docs/initial-test-output.txt`.

### 2.2 Exact initial failing output

```text
FAIL: testEmptyGraph
      java.lang.UnsupportedOperationException: TODO: implement getVertexCount
FAIL: testSingleVertexAndStartEqualsTarget
      java.lang.UnsupportedOperationException: TODO: implement addVertex
FAIL: testNullAndMissingInputs
      java.lang.AssertionError: addVertex(null) should reject null
      expected: IllegalArgumentException
      actual:   UnsupportedOperationException: TODO: implement addVertex
FAIL: testLabGraphRepresentation
      java.lang.UnsupportedOperationException: TODO: implement addEdge
FAIL: testBfsShortestPath
      java.lang.UnsupportedOperationException: TODO: implement addEdge
FAIL: testDfsPathAndNotShortest
      java.lang.UnsupportedOperationException: TODO: implement addEdge
FAIL: testDfsBacktracks
      java.lang.UnsupportedOperationException: TODO: implement addEdge
FAIL: testDisconnectedAndIsolated
      java.lang.UnsupportedOperationException: TODO: implement addEdge
FAIL: testStudentAuthoredUndirectedTreeIsNotACycle
      java.lang.UnsupportedOperationException: TODO: implement addEdge
FAIL: testUndirectedPathIsNotCycle
      java.lang.UnsupportedOperationException: TODO: implement addEdge
FAIL: testUndirectedCycle
      java.lang.UnsupportedOperationException: TODO: implement addEdge
FAIL: testSelfLoop
      java.lang.UnsupportedOperationException: TODO: implement addEdge
FAIL: testDirectedAcyclicBlackEdge
      java.lang.UnsupportedOperationException: TODO: implement addEdge
FAIL: testDirectedCycle
      java.lang.UnsupportedOperationException: TODO: implement addEdge
FAIL: testCycleInLaterComponent
      java.lang.UnsupportedOperationException: TODO: implement addEdge
FAIL: testDirectedEdgeNoReverse
      java.lang.UnsupportedOperationException: TODO: implement addEdge
FAIL: testDuplicateRelationsIdempotent
      java.lang.UnsupportedOperationException: TODO: implement addEdge
FAIL: testConflictingRelationsAtomic
      java.lang.UnsupportedOperationException: TODO: implement addEdge
FAIL: testTraversalImmutability
      java.lang.UnsupportedOperationException: TODO: implement addEdge
FAIL: testMixedGraphCoexistence
      java.lang.UnsupportedOperationException: TODO: implement addEdge

SUMMARY: 0 passed, 20 failed
```

The failures are not compile errors. They are the intended “not implemented yet” behavior.

### 2.3 Acceptance-test matrix

| Matrix area | Tests | Verified behavior |
|---|---|---|
| Empty / single | `testEmptyGraph`, `testSingleVertexAndStartEqualsTarget`, `testNullAndMissingInputs` | Empty graph, one vertex, duplicate `addVertex`, `start == target` -> `[s]`, null rejection |
| Path | `testLabGraphRepresentation`, `testBfsShortestPath`, `testDfsPathAndNotShortest`, `testDfsBacktracks` | 6 vertices, 7 edges, 14 entries; BFS 3 edges; DFS 5 edges; DFS not claimed shortest; backtracking |
| Disconnected | `testDisconnectedAndIsolated`, `testCycleInLaterComponent` | Unreachable target returns empty; isolated vertices; component-only BFS; later-component cycle |
| Cycle | `testStudentAuthoredUndirectedTreeIsNotACycle`, `testUndirectedPathIsNotCycle`, `testUndirectedCycle`, `testSelfLoop`, `testDirectedAcyclicBlackEdge`, `testDirectedCycle` | Tree parent edge not a cycle; undirected cycle; directed/undirected self-loop; gray back-edge; black edge not a cycle |
| Direction and mutation | `testDirectedEdgeNoReverse`, `testDuplicateRelationsIdempotent`, `testConflictingRelationsAtomic`, `testTraversalImmutability`, `testMixedGraphCoexistence` | No automatic reverse route; idempotency; conflict atomicity; before/after snapshots identical |

No instructor-provided test was deleted, weakened, or had its expected result changed. No instructor tests were supplied.

### 2.4 Student-authored test contribution

The student selected the undirected-tree parent-edge case. The final assertion was drafted collaboratively by the agent because the student did not supply the line in the conversation. It is labeled as **collaborative**, not independently student-authored. The student must review or replace it before submission.

```java
private static void testStudentAuthoredUndirectedTreeIsNotACycle() {
    NetworkRouteFinder graph = new NetworkRouteFinder();
    graph.addEdge("A", "B", true);
    graph.addEdge("B", "C", true);
    graph.addEdge("B", "D", true);

    check(!graph.hasCycle(),
            "An undirected tree must not be reported as a cycle; the reverse "
                    + "link to the parent is the same physical edge.");
}
```

Why it is meaningful: a naive implementation that treats every visited neighbor as a cycle returns `true` for this tree. The test fails before implementation and passes only when the parent-edge case is handled.

---

## Phase 3 — BUILD

### 3.1 Implementation summary

Permanent fields:

```java
private final Map<String, List<String>> adjacency;
private final Set<List<String>> undirectedEdges;
```

`undirectedEdges` stores sorted two-element lists. `[A, B]` and `[B, A]` are the same metadata pair. This avoids a custom edge class while keeping conflict checks and parent-edge checks readable.

Implementation decisions:

- `addVertex` rejects null and uses `computeIfAbsent`, so duplicate additions are idempotent.
- `addEdge` validates nulls, checks conflicts before `ensureVertex`, then adds the relation. A rejected call cannot leave a partial mutation.
- Undirected self-loops add exactly one adjacency entry and one metadata pair, not two.
- Directed edges add only `u -> v`; the destination is auto-added with an empty list if absent.
- Strict conflict rule: after directed `A -> B`, both an undirected `A-B` and a directed `B -> A` throw `IllegalStateException`.
- BFS uses local `visited`, `parent`, `distance`, and `queue`. It marks visited on enqueue and assigns parent only on first discovery.
- DFS uses local `visited` and `path`; `path` is unwound on backtracking while `visited` remains.
- `hasCycle` uses `WHITE`/`GRAY`/`BLACK`. `GRAY` is the active recursion stack (`inStack`). Undirected parent edges are skipped only when the pair is in `undirectedEdges`.
- Snapshots return independent copies. Traversals are read-only on permanent fields.
- Audit counters are reset at the start of each traversal. They describe only the most recent call.

### 3.2 Checkpoint 1 — representation snapshot

After `addEdge("A","B",true)`:

```text
adjacency = { A: [B], B: [A] }
undirectedEdges = { [A,B] }
```

After `addEdge("B","D",false)`:

```text
adjacency = { A: [B], B: [A, D], D: [] }
undirectedEdges = { [A,B] }        // no [B,D]
```

Only `adjacency[B]` gained `D`, and vertex `D` was auto-added with an empty list. The metadata did not change because `B -> D` is directed. `testDirectedEdgeNoReverse` verifies the same invariant, including that `BFS("B","A")` and `DFS("B","A")` are empty.

### 3.3 Checkpoint 2 — BFS evidence

The implementation reproduces the Plan trace:

| Step | Dequeue | Discover | Queue after | Parent / distance |
|---|---|---|---|---|
| 1 | A | B, C | `[B, C]` | `B <- A`, `C <- A`; distance 1 |
| 2 | B | D | `[C, D]` | `D <- B`; distance 2 |
| 3 | C | E | `[D, E]` | `E <- C`; distance 2 |
| 4 | D | F | `[E, F]` | `F <- D`; distance 3 |
| 5 | E | — | `[F]` | F already discovered |
| 6 | F | — | `[]` | target reached |

Reconstructed path:

```text
parent = { A: null, B: A, C: A, D: B, E: C, F: D }
F -> D -> B -> A  (reverse)
A -> B -> D -> F  (inclusive, 3 edges)
```

The alternative `A -> C -> E -> F` also has 3 edges but is not returned because `B` is inserted before `C`, and `D` is inserted before `E`. The test expects the exact `[A, B, D, F]` path, so a dequeue-marking implementation that overwrites `parent[D]` with `C` fails.

Measured BFS evidence for the lab graph: `vis=6`, `scan=12`, `maxQueue=2`, path edges `3`. The full 14 adjacency entries are not scanned because BFS stops when `F` is dequeued; `F`'s two entries remain unexamined. This is an early-exit optimization, not a missing edge.

### 3.4 Checkpoint 3 — DFS and cycle evidence

DFS state when `F` is found:

```text
path / recursion stack = [A, B, D, C, E, F]
visited                = {A, B, C, D, E, F}
edges                  = 5
```

The visited set is not the path. After a failed branch, `path` shrinks but `visited` keeps the entered vertices. `testDfsBacktracks` verifies this with `A-B-C` as a dead end and `A-D-E` as the successful branch.

Cycle state cases verified by tests:

| Case | State observation | Result |
|---|---|---|
| `A -> A` directed self-loop | Neighbor equals current | cycle |
| `B—B` undirected self-loop | One adjacency entry, one metadata pair, neighbor equals current | cycle |
| Undirected tree `A-B, B-C, B-D` | Reverse neighbor equals parent and pair is in `undirectedEdges` | not a cycle |
| Undirected triangle | Visited non-parent undirected neighbor | cycle |
| Directed DAG `A->B, A->C, B->D, C->D` | `C -> D` reaches a BLACK/finished vertex | not a cycle |
| Directed cycle `A->B, B->C, C->A` | `C -> A` reaches a GRAY/inStack vertex | cycle |
| Cycle in second component | Outer loop starts a new DFS after the first component finishes | cycle found |

### 3.5 Part A — model the network

The lab graph is undirected/bidirectional:

```text
adjacency = {
  A: [B, C],       B: [A, D],
  C: [A, D, E],    D: [B, C, F],
  E: [C, F],       F: [D, E]
}
undirectedEdges = { [A,B], [A,C], [B,D], [C,D], [C,E], [D,F], [E,F] }
V = 6, E = 7, adjacency entries = 14
```

One shortest `A -> F` route is `A -> B -> D -> F` (3 edges). A tie is possible: `A -> C -> E -> F` also has 3 edges. Insertion order decides: `A` lists `B` before `C`; `B` discovers `D` before `C` is dequeued; `D` discovers `F` before `E` is dequeued. The returned path is therefore `[A, B, D, F]`.

### 3.6 Part B — implement and test

The graph operations, BFS, DFS, and cycle detection are implemented. The acceptance matrix in section 2.3 records the test inputs, expected behavior, and why each case matters. The student-selected undirected-tree test is in section 2.4. No expected result was changed to force a pass.

### 3.7 Part C — audit state

- BFS state after the final dequeue: `queue=[]`, `visited={A,B,C,D,E,F}`, `parent={A:null, B:A, C:A, D:B, E:C, F:D}`, `distance={A:0, B:1, C:1, D:2, E:2, F:3}`.
- DFS state at the target: `visited={A,B,C,D,E,F}`, `inStack/path=[A,B,D,C,E,F]`, `parent` tree `A->B->D->C->E->F`, `queue` not used.
- Cycle state for `A->B->C->A`: when `C` examines `A`, `color[A]=GRAY`, so `A` is inStack and the directed edge is a back-edge.
- Cycle state for the DAG `A->B, A->C, B->D, C->D`: when `C` examines `D`, `color[D]=BLACK`; `D` is finished, so the directed edge is not a cycle.
- Why a visited neighbor is not always a directed cycle: a visited vertex can be BLACK/finished. In a directed graph, reaching a finished vertex is a cross or forward edge. Only a GRAY/inStack neighbor proves a directed back-edge. For undirected edges, however, a visited non-parent neighbor does close a cycle because the physical edge can be traversed in either direction.
- Why BFS's shortest guarantee needs equal edge cost: BFS discovers vertices in nondecreasing edge-count layers. The first time the target is dequeued, it has the fewest edges. If edges have different weights, a route with more edges can cost less, so BFS's layer order no longer matches cost order. Dijkstra's algorithm (or Bellman-Ford for negative weights) is needed.

### 3.8 Checkpoint 4 — agent audit, accepted and rejected suggestions

| Suggestion | Decision | Evidence |
|---|---|---|
| Check conflicts before `ensureVertex` or any list insertion. | Accepted | `testConflictingRelationsAtomic` compares adjacency and undirected snapshots before/after every rejected call. |
| Keep `visited` separate from the DFS `path` and remove only from `path` on backtracking. | Accepted | `testDfsBacktracks` finds `A -> D -> E` after the `A-B-C` branch fails. |
| Start a new DFS at every WHITE vertex. | Accepted | `testCycleInLaterComponent` finds a triangle in the second component. |
| Mark BFS vertices visited on dequeue to simplify the loop. | Rejected | It can enqueue `D` twice in the lab graph and overwrite `parent[D]` with `C`; the exact expected path `[A,B,D,F]` would change. `testBfsShortestPath` fails. |
| Treat every visited neighbor as a cycle. | Rejected | It falsely flags the undirected tree in `testStudentAuthoredUndirectedTreeIsNotACycle` and the directed black edge in `testDirectedAcyclicBlackEdge`. |
| Use BFS's route for DFS so both return the shortest path. | Rejected | The workbook requires the first DFS path by insertion order. `testDfsPathAndNotShortest` expects `[A,B,D,C,E,F]` (5 edges), not BFS's `[A,B,D,F]` (3 edges). |

### 3.9 Build verification

`run-tests.bat` compiles the lab-local sources and runs the acceptance suite:

```text
SUMMARY: 20 passed, 0 failed
```

Exit code: `0`. The final output is preserved in `docs/final-test-output.txt`.

The optional demo also runs:

```text
BFS A -> F: [A, B, D, F] (3 edges)
DFS A -> F: [A, B, D, C, E, F] (5 edges)
hasCycle(): true
```

---

## Phase 4 — COMPLEXITY AUDIT

### 4.1 Counter definitions

The experiment instrumented `NetworkRouteFinder` with four counters. They are reset at the start of every public traversal and describe only the most recent call.

| Counter | Exact meaning |
|---|---|
| `lastVisitedCount` | Number of vertices marked visited for the first time: BFS enqueue, DFS entry, or cycle-DFS entry. |
| `lastEdgeScanCount` | Number of adjacency-list entries examined. One undirected physical edge contributes two entries (one at each endpoint); one directed edge contributes one. |
| `lastMaxQueueSize` | Largest number of vertices in the BFS queue at any moment. |
| `lastMaxPathLength` | Largest DFS `path` / recursion-stack size. |

These are diagnostic fields, not permanent graph state. The permanent graph is still only `adjacency` and `undirectedEdges`.

### 4.2 Actual measured results

All numbers below come from running `ComplexityExperiment`; the raw output is in `docs/complexity-results.txt`.

The lab-graph row uses the A-to-F traversal. Every other row uses a full traversal to an isolated target, so the source component is exhausted before the empty result is returned.

| Shape and size | V | E | Adj. entries | BFS evidence: vis / scan / maxQueue | DFS evidence: vis / scan / maxPath | hasCycle: result / vis / scan |
|---|---:|---:|---:|---|---|---|
| Lab graph | 6 | 7 | 14 | 6 / 12 / 2 | 6 / 10 / 6 | true / 4 / 6 |
| Path n=100 | 101 | 99 | 198 | 100 / 198 / 1 | 100 / 198 / 100 | false / 101 / 198 |
| Path n=400 | 401 | 399 | 798 | 400 / 798 / 1 | 400 / 798 / 400 | false / 401 / 798 |
| Path n=1600 | 1601 | 1599 | 3198 | 1600 / 3198 / 1 | 1600 / 3198 / 1600 | false / 1601 / 3198 |
| Grid 10×10 | 101 | 180 | 360 | 100 / 360 / 10 | 100 / 360 / 100 | true / 15 / 25 |
| Grid 20×20 | 401 | 760 | 1520 | 400 / 1520 / 20 | 400 / 1520 / 400 | true / 18 / 20 |
| Grid 30×30 | 901 | 1740 | 3480 | 900 / 3480 / 30 | 900 / 3480 / 900 | true / 44 / 46 |
| DAG n=20 | 21 | 190 | 190 | 20 / 190 / 19 | 20 / 190 / 20 | false / 21 / 190 |
| DAG n=40 | 41 | 780 | 780 | 40 / 780 / 39 | 40 / 780 / 40 | false / 41 / 780 |
| DAG n=80 | 81 | 3160 | 3160 | 80 / 3160 / 79 | 80 / 3160 / 80 | false / 81 / 3160 |
| Disconnected 2×100 + 5 isolated | 205 | 198 | 396 | 100 / 198 / 1 | 100 / 198 / 100 | false / 205 / 396 |
| Disconnected 4×100 + 5 isolated | 405 | 396 | 792 | 100 / 198 / 1 | 100 / 198 / 100 | false / 405 / 792 |
| Disconnected 8×100 + 5 isolated | 805 | 792 | 1584 | 100 / 198 / 1 | 100 / 198 / 100 | false / 805 / 1584 |

Additional measured rows:

- **Path corner-to-corner:** BFS scans `2E - 1` (197, 797, 3197) because the target's adjacency is not scanned after early exit. DFS scans the same amount on a path.
- **Grid corner-to-corner:** BFS path has 18, 38, 58 edges (the true shortest corner distance). DFS path has 90, 380, 870 edges because it follows a snake; it is valid but far from shortest. DFS visits 91, 381, 871 vertices before finding the target, so it stops before scanning every entry.
- **DAG shortest path:** BFS path is 1 edge because `D0 -> D(n-1)` exists; DFS path is `n-1` edges. This is direct evidence that DFS is not a shortest-path algorithm.
- **Cycle in a later component:** `V=103, E=101, adjacencyEntries=202`; `hasCycle()` returns `true` with `vis=103, scan=201`. It scans the two tree components before finding the triangle, and returns as soon as the cycle is proven.
- **Lab graph cycle:** `hasCycle()` returns `true` with `vis=4, scan=6`, finding `A-B-D-C-A` before visiting `E` or `F`.

### 4.3 Predicted vs measured

Prediction: full BFS/DFS work is `O(V + A)`, where `A` is the number of adjacency entries. For an undirected graph `A = 2E`; for a directed graph `A = E`.

Observed:

- Every full undirected traversal scanned exactly `2E` entries: path 198/798/3198, grid 360/1520/3480, disconnected 396/792/1584.
- Every full directed traversal scanned exactly `E` entries: DAG 190/780/3160.
- Every full traversal visited exactly the vertices in the explored component(s): `n` for path source component, `k^2` for grid, `n` for DAG, `100` for one disconnected component, `V` for all-component cycle detection.
- The `hasCycle()` row for the acyclic path and disconnected graphs scanned all `2E` entries after visiting all `V` vertices. The grid rows are smaller because a cycle was found early. Early exit does not violate the bound; it is less than the worst-case work.

The dense DAG is important: `E = n(n-1)/2`, so `O(V + E)` becomes `O(V^2)` for that shape. The observed scan count equals `E`, not `V`. The bound follows the actual number of adjacency entries, not a memorized “linear in vertices” claim.

`addEdge` is not `O(1)`: duplicate/conflict checks use `List.contains`, so a call costs `O(deg(u) + deg(v))`. `addVertex` and `HashMap` lookup are `O(1)` average. The workbook's `O(1)` row is specifically about vertex-map lookup, not the full edge operation.

### 4.4 Permanent vs temporary space

Permanent graph storage:

- `adjacency`: `V` keys plus `A` neighbor entries.
- `undirectedEdges`: one sorted pair per undirected physical edge.
- Total: `O(V + E)`.

Temporary traversal storage:

- BFS: `visited`, `parent`, and `distance` maps plus `queue`, each `O(V)`.
- DFS: `visited` set, `path` list, and recursion stack, each `O(V)`.
- `hasCycle`: color map plus recursion stack, each `O(V)`.
- Observed maxima support this: path `maxQueue=1` and `maxPath=n`; grid `maxQueue=k` and `maxPath=k^2`; DAG `maxQueue=n-1` and `maxPath=n`; disconnected `maxQueue=1` and `maxPath=100`.

The snapshots used by tests are independent copies and therefore also `O(V + E)` temporarily, but they are test/audit tools, not part of BFS/DFS/cycle auxiliary space.

### 4.5 Differences and limitations

- BFS/DFS stop when the target is dequeued/entered, so a target's adjacency may not be scanned. This explains `2E - 1` path scans and the smaller grid DFS corner counts.
- `hasCycle()` returns as soon as a cycle is proven. Grid counts are therefore much smaller than `2E`. Acyclic graphs still force a full scan, which is the relevant worst-case evidence.
- `HashMap` iteration order is unspecified. It affects which component or vertex is visited first and how quickly a cycle is found, but not correctness. The later-component experiment still proves all components are reachable by the outer loop.
- The strict conflict rule is a deliberate interpretation of “conflicting direction.” It prevents antiparallel directed pairs. If the instructor expects such pairs to be legal, that rule is the one place to revisit.
- The mixed-graph limitation is real and disclosed: `A—B` undirected, `B—C` undirected, `A -> C` directed contains the cycle `A -> C -> B -> A`, but the workbook's black-edge rule returns `false`. The experiment prints this exact result. The required workbook behavior is followed; the implementation does not claim general mixed-graph cycle completeness.

---

## Phase 5 — VERIFY AND AUDIT

### 5.1 Final commands and results

From inside the lab folder:

```powershell
cd C:\Users\xamgd\Downloads\CpE211-Labs\Lab09_NetworkRouteFinder
.\run-tests.bat
```

Final output:

```text
PASS: testEmptyGraph
PASS: testSingleVertexAndStartEqualsTarget
PASS: testNullAndMissingInputs
PASS: testLabGraphRepresentation
PASS: testBfsShortestPath
PASS: testDfsPathAndNotShortest
PASS: testDfsBacktracks
PASS: testDisconnectedAndIsolated
PASS: testStudentAuthoredUndirectedTreeIsNotACycle
PASS: testUndirectedPathIsNotCycle
PASS: testUndirectedCycle
PASS: testSelfLoop
PASS: testDirectedAcyclicBlackEdge
PASS: testDirectedCycle
PASS: testCycleInLaterComponent
PASS: testDirectedEdgeNoReverse
PASS: testDuplicateRelationsIdempotent
PASS: testConflictingRelationsAtomic
PASS: testTraversalImmutability
PASS: testMixedGraphCoexistence

SUMMARY: 20 passed, 0 failed
```

Exit code: `0`. Raw output: `docs/final-test-output.txt`.

The equivalent manual compile/run form is:

```powershell
$sources = @(Get-ChildItem src,test -Filter *.java | Select-Object -ExpandProperty FullName)
javac -d out @sources
java -ea -cp out LabTestRunner
```

Bash form:

```bash
javac -d out src/*.java test/*.java
java -ea -cp out LabTestRunner
```

Complexity run:

```powershell
.\run-complexity.bat
```

Exit code: `0`. Raw output: `docs/complexity-results.txt`.

Demo run:

```text
Lab 09 Network Route Finder demo
V=6 E=7 adjacencyEntries=14
BFS A -> F: [A, B, D, F] (3 edges)
DFS A -> F: [A, B, D, C, E, F] (5 edges)
hasCycle(): true
```

### 5.2 Audit checklist

- [x] All connected components inspected: `testCycleInLaterComponent` and disconnected experiments.
- [x] Traversals do not mutate permanent fields: `testTraversalImmutability` compares before/after snapshots after BFS, DFS, and cycle detection.
- [x] BFS marks visited on enqueue and assigns parent on first discovery: exact `[A,B,D,F]` test plus trace.
- [x] DFS uses insertion order and is not claimed shortest: exact 5-edge DFS path versus 3-edge BFS path.
- [x] Self-loop, undirected tree, undirected cycle, directed gray, and directed black cases tested.
- [x] Duplicate relations idempotent; conflicts throw before mutation.
- [x] Directed edge creates no reverse route.
- [x] Complexity tied to observed scans and visits, not a memorized formula.
- [x] No instructor-provided test was removed or weakened; none were supplied.
- [x] No GitHub publication performed.
- [ ] Student review of the collaborative undirected-tree assertion.
- [ ] Student can explain and modify the implementation without the agent.

### 5.3 Reflection

**Why does BFS return a shortest path here, and what changes with weights?**

BFS explores in layers: distance 0, then 1, then 2, and so on. The first time a vertex is enqueued, it receives the fewest-edge distance from the start, and its parent is fixed. When the target is dequeued, its parent chain is therefore a minimum-edge route. In this lab every edge has equal cost, so fewest edges equals least cost. With weights, a path with more edges can cost less, so layer order no longer matches cost order. A priority-queue algorithm such as Dijkstra is needed for nonnegative weights; negative weights require Bellman-Ford or a equivalent algorithm.

**How did the cycle logic distinguish an undirected parent edge from a directed back-edge?**

It uses two different pieces of state. For an undirected edge, the reverse direction to `parent[current]` is the same physical edge, so it is skipped only when `[current, neighbor]` is in `undirectedEdges`. Any other visited neighbor reached by an undirected edge closes a cycle. For a directed edge, there is no physical reverse edge to skip. Instead, the color state matters: a neighbor that is GRAY is still on the active recursion stack (`inStack`), so the directed edge is a back-edge and a cycle; a neighbor that is BLACK is finished, so a directed edge to it is a cross/forward edge and not a cycle by itself. The outer loop starts a new DFS at every WHITE vertex so a cycle in a later component is not missed.

### 5.4 Known unresolved issues and honest disclosure

- The mixed-graph limitation in section 4.5 is not fixed because the student chose to follow the workbook's case split exactly. It is disclosed in the README and printed by the experiment.
- The strict reverse-direction conflict rule is an interpretation of the workbook's “conflicting direction” wording. It is tested and documented; it may need revision if the instructor expects antiparallel directed pairs.
- The student-selected undirected-tree assertion was drafted collaboratively by the agent. It is not claimed as independently student-authored. The student must review or replace it.
- No instructor starter files or instructor tests were present, so there was nothing supplied to preserve. The project uses its own standard starter and acceptance runner.

### 5.5 AI-use disclosure

This project was completed with AI assistance in VS Code through OpenCode. The agent created the project structure, starter, implementation, tests, experiment, README, and logs. The student chose the assignment and key decisions, but did not independently author all code. The student must review the work and be able to explain it during the live defense. No GitHub publication was performed.

### 5.6 Post-verification fix — Lab 9 test compile error (2026-11-18)

**Reported symptom.** The Lab 9 test files appeared not to compile in the
parent `CpE211-Labs` VS Code workspace.

**Diagnosis.** The test sources were not the cause. Compiling them with the
Lab 9 sources from inside the lab folder already succeeded:

```text
javac -Xlint:all -Werror -d <temp> src/*.java test/*.java
exit 0
```

The compile errors came from the parent workspace configuration. Its
`.vscode/settings.json` listed `Lab09_NetworkRouteFinder/test` without
`Lab09_NetworkRouteFinder/src`, and also listed `lab 7/test`. Both
`lab 7/test/LabTestRunner.java` and
`Lab09_NetworkRouteFinder/test/LabTestRunner.java` declare
`public class LabTestRunner` in the default package. Compiling that mixed
source set produced:

```text
Lab09_NetworkRouteFinder\test\LabTestRunner.java:15: error: duplicate class: LabTestRunner
Lab09_NetworkRouteFinder\test\ComplexityExperiment.java:70: error: cannot find symbol
        NetworkRouteFinder graph = new NetworkRouteFinder();
  symbol:   class NetworkRouteFinder
...
22 errors
```

The only test-file diagnostic was an unused import in
`test/LabTestRunner.java`:

```java
import java.util.ArrayList;   // never used
```

**Fix applied.**

1. `C:\Users\xamgd\Downloads\CpE211-Labs\.vscode\settings.json`: removed the
   stale `"Lab09_NetworkRouteFinder/test"` source path. The Lab 7 paths and
   `"java.project.outputPath": "lab 7/out"` were left unchanged. Lab 9 now
   compiles through its own `.vscode/settings.json`, which points at `src`,
   `test`, and `out`; open `Lab09_NetworkRouteFinder` directly in VS Code for
   Lab 9 work.
2. `test/LabTestRunner.java`: removed the unused
   `import java.util.ArrayList;` line.

No test case, assertion, expected result, traversal, or source method was
changed. None of the 20 tests was deleted, commented out, or weakened.

**Final verification.** Run from `Lab09_NetworkRouteFinder`:

```text
> javac -version
javac 25.0.1

> javac -Xlint:all -Werror -d <temp> src/*.java test/*.java
(no output; exit 0)

> javac -d out src/*.java test/*.java
(no output; exit 0)

> java -ea -cp out LabTestRunner
PASS: testEmptyGraph
PASS: testSingleVertexAndStartEqualsTarget
PASS: testNullAndMissingInputs
PASS: testLabGraphRepresentation
PASS: testBfsShortestPath
PASS: testDfsPathAndNotShortest
PASS: testDfsBacktracks
PASS: testDisconnectedAndIsolated
PASS: testStudentAuthoredUndirectedTreeIsNotACycle
PASS: testUndirectedPathIsNotCycle
PASS: testUndirectedCycle
PASS: testSelfLoop
PASS: testDirectedAcyclicBlackEdge
PASS: testDirectedCycle
PASS: testCycleInLaterComponent
PASS: testDirectedEdgeNoReverse
PASS: testDuplicateRelationsIdempotent
PASS: testConflictingRelationsAtomic
PASS: testTraversalImmutability
PASS: testMixedGraphCoexistence

SUMMARY: 20 passed, 0 failed
(exit 0)
```

`run-tests.bat` was also run in place. It removed `out`, recompiled all four
sources, and produced the same 20/20 summary with exit code 0.

`java -cp out ComplexityExperiment` also exited 0.

`run-tests.sh` could not be executed on this Windows host because no POSIX
shell or WSL is installed. Its commands were inspected and are equivalent to
the verified manual commands:
`javac -d out src/*.java test/*.java` followed by
`java -ea -cp out LabTestRunner`.

No Git or GitHub commands were run.

Student name: ______________________  
Section: ______________________  
Date: ______________________  
Student review signature: ______________________

