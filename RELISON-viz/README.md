# RELISON-viz

Headless 2D layouts for RELISON graphs. The module depends on `RELISON-core`
and exposes generic node coordinates without requiring a renderer, browser,
native executable, or `RELISON-sna` dependency. The source targets Java 14,
matching core; use the JDK required by your current parent build.

## Stage A layouts

| Class | Configuration | Behaviour |
|---|---|---|
| `PresetLayout<U>` | Positions in the request | Requires coordinates for every node |
| `RandomLayout<U>` | Width, height; seed in request | Uniform rectangle centred at the origin |
| `GridLayout<U>` | Columns (0 = automatic), spacing | Row-major grid; incomplete last rows retain column alignment |
| `CircularLayout<U>` | Radius, start angle in radians | Equally spaced circle; singleton at origin |
| `ShellLayout<U>` | Explicit partition, radial spacing | Inner-to-outer rings; singleton inner shell at origin |
| `ConcentricLayout<U>` | Score function, radial spacing | Equal scores share rings; largest scores innermost |

All dimensions and spacings must be finite and positive. Coordinate transforms
also support zero and negative scales. Coordinates use mathematical x/y axes;
renderers choose screen orientation. Basic layouts ignore topology for placement.
Force sessions, cancellation, node radii and overlap removal are available in Stage C.
Edge routing and parameter/capability metadata beyond identity remain future stages.

## Stage B layouts

| Class | Configuration | Behaviour |
|---|---|---|
| `RadialLayout<U>` | Root, `EdgeOrientation`, hop spacing | BFS hop rings; unreachable nodes share an extra outer ring |
| `BipartiteLayout<U>` | Automatic colouring or two explicit partitions; spacing, ordering passes | Two columns; odd cycles/self-loops rejected |
| `MultipartiteLayout<U>` | Ordered partitions; column/row spacing, ordering passes | One column per partition; within-partition edges rejected |
| `TreeLayout<U>` | Optional preferred root; sibling/level spacing | Tidy outward tree or forest; cycles and multiple parents rejected |
| `ComponentPacking<U>` | Gap between weak components | Graph-aware postprocessor applying rigid component translations |

These structural layouts use distinct neighbours, ignoring weights and parallel
multiplicity. Self-loops are accepted by radial traversal but rejected by tree
and partition layouts. Explicit partitions must cover every node exactly once;
empty columns are allowed. Barycentre ordering is a heuristic, with no global
crossing-minimum guarantee; zero passes preserves request order within columns.

Radial traversal defaults to ignoring direction and chooses the first node in
request order when no root is supplied. Outgoing/incoming modes follow the chosen
arc direction. Unreachable nodes' outer ring is explicitly not a hop distance.

The tidy tree uses the Buchheim-Juenger-Leipert refinement of Walker's algorithm
([Graph Drawing 2002](https://doi.org/10.1007/3-540-36151-0_32)), with iterative
walks to support deep trees. Directed edges must point outward from roots and
each node may have at most one distinct parent. Undirected trees can be rerooted.
Disconnected forests are supported; no spanning tree is silently extracted from
a cyclic input. Node separation refers to centres, not rendered glyph sizes.

Packing is optional and fits padded bounding boxes on shelves. A component
containing a pinned node stays entirely fixed, with movable components placed
to the right of all anchors. Conflicting anchored boxes raise an error rather
than moving pins. Empty and single-component results are unchanged. Packing
preserves internal displacement vectors but may move global columns, root
origins, or attribute coordinate axes between components.

Using the graph and request from the example below:

```java
Layout<Long> radial = new LayoutPipeline<>(
    new RadialLayout<>(10L, EdgeOrientation.OUT, 50.0),
    Collections.singletonList(new es.uam.eps.ir.relison.viz.transforms.ComponentPacking<>(30.0)));
LayoutResult<Long> result = radial.compute(graph, request);
```

`LayoutPostProcessor` has an additional graph-aware overload. Its default
implementation delegates to the original coordinate-only method, preserving
existing processors and lambdas. `LayoutPipeline` calls the graph-aware overload.
Standalone component packing requires `process(graph, result, request)`.

## Computing a layout

```java
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.FastDirectedUnweightedGraph;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.layouts.*;
import es.uam.eps.ir.relison.viz.transforms.CoordinateTransform;
import java.util.*;

Graph<Long> graph = new FastDirectedUnweightedGraph<>();
graph.addEdge(10L, 20L);
graph.addNode(30L);

LayoutRequest<Long> request = LayoutRequest.<Long>builder()
    .nodeOrder(Arrays.asList(10L, 20L, 30L))
    .seed(42L)
    .build();

Layout<Long> layout = new LayoutPipeline<>(
    new CircularLayout<>(100.0, 0.0),
    Collections.singletonList(new CoordinateTransform<>(1, -1, 200, 200)));

LayoutResult<Long> result = layout.compute(graph, request);
result.getPositions().forEach((node, point) ->
    System.out.println(node + "\t" + point.v1() + "\t" + point.v2()));
```

For score-based rings, pass precomputed analysis values, for example
`new ConcentricLayout<Long>(node -> scores.get(node), 50.0)`.
Scores are evaluated once per node. Signed zero scores share a ring; otherwise
groups use exact numeric equality. Concentric rings represent scores, not hops.

## Ordering, snapshots, and pins

- By default, node order follows `Graph.getAllNodes()` encounter order. For
  reproducibility across graph instances, supply an explicit exact permutation.
  A seed alone does not make different encounter orders equivalent.
- Requests, positions, and results are defensively copied and immutable.
  Node identities themselves are not copied; their equality and hash codes
  must remain stable.
- Each computation captures topology in an indexed snapshot. Directed arcs,
  self-loops, and parallel weights are retained. Undirected edges are represented
  from both endpoints. The snapshot does not copy edge types or stable edge IDs.
  Capture costs O(V + E) storage plus neighbour sorting; no graph mutation is
  permitted during capture. Snapshots remain independent of later graph changes.
- Pinned nodes require initial positions and stay at those exact coordinates.
  Extra initial-position keys outside the graph are rejected. Unpinned initial
  positions are used by `PresetLayout`; other Stage A layouts replace them.
- Pins override generated coordinates without reserving space or preventing
  overlaps. `CoordinateTransform` scales then translates only unpinned nodes;
  this is not a global affine transformation when pins exist.
- `LayoutPipeline` applies processors in order and rejects changes to node
  coverage or pinned coordinates. Bounds are derived again for each result.
- Empty graphs produce empty coordinate maps with bounds at the origin.
  Invalid parameters and non-finite generated coordinates raise an exception.
- Layout instances have no per-run mutable state. A supplied scoring callback
  must itself be thread-safe if used concurrently.

## Build and tests

From the repository root:

```sh
mvn -pl RELISON-viz -am test
```

JUnit tests cover reference geometries, ordering and seeded reproducibility,
empty/singleton/disconnected graphs, pin preservation, defensive copies,
weighted and multigraph snapshots, pipeline constraints, and invalid inputs.
Stage B tests additionally cover directed hop distances, odd cycles, partition
coverage, tree centring/mirroring, random branching, a 10,000-node chain,
component separation, rigid shape preservation, and conflicting anchors.

The module inherits the parent version and MPL-2.0 license. It is registered
in the Maven reactor; publishing remains a separate repository release step.

Coordinates use RELISON-core’s `Pair<Double>`: `v1()` is x and `v2()` is y. Coordinates must be non-null and finite. Traversal uses `EdgeOrientation.OUT`, `IN`, `UND`, or `MUTUAL`; mutual traversal follows only reciprocal arcs.

Additional layouts: `feature-grid` (`FeatureGridLayout`) places feature-value groups in columns and permits within-group edges; one group is valid. `ego-grid` (`EgoGridLayout`) places hop distances in columns, with a selectable root and `EdgeOrientation` traversal. Unreachable nodes occupy a final column. Both accept column spacing and row spacing; feature grids also support ordering passes.
## Stage C: force sessions

| Class | Configuration | Behaviour |
|---|---|---|
| FruchtermanReingoldLayout | FruchtermanReingoldConfig | Inverse-distance repulsion, quadratic attraction, geometric temperature cooling |
| ForceAtlas2Layout | ForceAtlas2Config | Degree masses, inverse-distance repulsion, linear/LinLog attraction, gravity, swinging/traction speed adaptation |
| OverlapRemoval | Glyph gap, maximum passes, request node radii | Circular glyph separation preserving exact pins |
| IterativeLayout / LayoutSession | Request limits, seed, positions, pins, cancellation signal | Independent caller-scheduled sessions with batch compatibility |

Force models follow [Fruchterman and Reingold (1991)](https://reingold.co/force-directed.pdf)
and [Jacomy et al. (2014)](https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0098679).
FR defaults to the original bounded frame model: k = sqrt(width * height / V),
unit-mass repulsion k²/d, and edge attraction d²/k. The centred frame defaults
to 600 by 600, with initial temperature 60. Each simultaneous displacement is
limited by the current temperature, then clamped to the frame. Temperature
decreases linearly over the requested iteration budget; the paper leaves the
cooling function configurable. Movable warm starts are clamped at initialization;
pins outside the frame are rejected. Diagnostics report actual movement after
clamping. The six-argument config constructor controls frame settings.
The existing five-argument constructor selects the unbounded extension, with
explicit ideal edge length, initial temperature equal to that length, and
geometric cooling. The UI exposes both modes. Weighted attraction and
Barnes-Hut repulsion are optional extensions; theta zero and unit weights select
the original force model.
ForceAtlas2 supports normal/strong gravity and linear/LinLog attraction.
Its speed uses global swinging/traction feedback, bounded growth and local swinging damping.
The controller includes graph-size-dependent jitter and persistent speed efficiency,
following the [Gephi reference equations](https://github.com/gephi/gephi/tree/master/modules/LayoutPlugin/src/main/java/org/gephi/layout/plugin/forceAtlas2).
Standard movement has no fixed displacement cap. Optional outbound attraction
distribution divides attraction by source mass and compensates by mean mass.
Weights can be inverted and normalized before exponentiation. Equal normalized
weights become one; inverted zero weights remain zero.
Optional in-force size adjustment uses request radii, boundary-distance repulsion,
and suppresses attraction between overlapping glyphs. This reference collision
mode alone uses slower movement and a 10-unit limiter. It selects exact pairs
even when theta is positive; the separate OverlapRemoval postprocessor is unchanged.
It is single-threaded and does not reproduce Gephi coordinates exactly.

Both project direction structurally. Undirected edges count once; directed
reciprocal arcs contribute independently. Parallel edges accumulate. Self-loops
exert no force. ForceAtlas2 mass is one plus incident edge multiplicity,
with each self-loop contributing two incidences. FR ignores weights unless weighted is enabled.
ForceAtlas2 raises weights to weightInfluence; zero ignores weights.
Used weights must be finite and non-negative. Numerical overflow raises an error.

Theta zero (Java default) selects exact O(V² + E) repulsion.
Positive theta selects a Barnes–Hut mass quadtree, typically O(V log V + E).
Pathological distributions can degrade. Cells containing the target are always
opened to exclude self-force. FR regularizes exactly coincident pairs with deterministic
directions. ForceAtlas2 uses unregularized forces: coincident points exert zero
repulsion, so provide distinct warm-start positions or use seeded initialization.
Tree depth is bounded. The UI defaults to theta 0.8.

The request defaults to maxIterations 500 and timeLimitMillis zero (unlimited).
A zero iteration budget returns initialization only. Time limits count active
stepping; initialization and idle time between calls are excluded.
Five consecutive movements below tolerance report CONVERGED.
Exhausted budgets report LIMIT_REACHED; unfinished snapshots report RUNNING.
Diagnostics include iteration count and maximum last-step movement.
Cancellation and timeout discard incomplete force evaluation.
Checks run inside force loops and tree construction.

Warm starts accept partial positions; missing positions use the seeded initializer.
Pins stay exact and still influence movable nodes. Empty and fully pinned sessions
are immediately converged. Sessions capture topology and never mutate the input graph.
Results reproduce with matching order, seed, configuration and iteration budget;
wall-clock limits may terminate at different iterations.
Step and snapshot use one caller-owned thread; cancel may use another.
Node sizes are non-negative radii in coordinate units; missing radii are zero.

Initialize a session with layout.initialize(graph, request), call session.step(10),
and consume session.snapshot() until session.isFinished(). Another thread may call
session.cancel(). The ordinary layout.compute(graph, request) runs to termination.

Overlap removal costs O(V²) per pass. Conflicting pins and unresolved pass-limit
collisions raise errors. The postprocessor checks caller cancellation but does
not use the force time budget. Packing can follow it; packing currently considers
node centres, so large glyphs can extend outside component boxes.

The UI animates RELISON sessions alongside browser force methods, with force controls,
warm starts and optional glyph separation. Sessions are limited to 5000 iterations
with no active computation time limit. Start displays initialization; each successive
frame advances one iteration. Stop cancels the server session and keeps displayed
coordinates. Overlap removal and packing run only on natural completion.
Direct Java sessions also support cancellation and progress snapshots.
