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
Edge routing is available in Stage D; parameter/capability metadata beyond identity remains future work.

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

## Stage D: directed layered layouts

`SugiyamaLayout<U>` accepts directed graphs, including disconnected components,
cycles, parallel arcs and self-loops. It never mutates the input graph.

| Stage | Implementation |
|---|---|
| Cycle handling | Eades–Lin–Smyth greedy feedback-arc ordering: sinks, sources, then maximum outdegree minus indegree; backwards arcs are temporarily reversed |
| Layer assignment | Longest-path ranks from sources in the acyclic orientation |
| Proper layering | One dummy vertex at every intermediate layer of a long edge |
| Crossing reduction | Alternating stable barycentre sorts, keeping the best crossing count seen |
| Coordinates | Sugiyama's degree-priority barycentre sweeps; lower-priority neighbours can be pushed while preserving minimum separation |
| Routing | Polylines through dummy vertices, restored to original source-to-target direction; self-loops receive a side loop |

The ordering and coordinate methods follow [Sugiyama, Tagawa and Toda (1981)](https://doi.org/10.1109/TSMC.1981.4308636).
Cycle handling follows [Eades, Lin and Smyth (1993)](https://doi.org/10.1016/0020-0190(93)90079-O).
These are heuristic methods, not exact crossing minimization or a minimum feedback
arc set. Stable ties use request node order. Longest-path ranks minimize height
for the chosen acyclic orientation, without prescribing a maximum layer width.
Edge weights and glyph radii are ignored. Parallel arcs count separately in
degrees, barycentres and crossings; adjacent-layer parallel routes may overlap.

```java
LayoutResult<String> result = new SugiyamaLayout<String>(50, 75, 8)
    .compute(graph, LayoutRequest.defaults());
List<EdgeRoute<String>> routes = result.getEdgeRoutes();
```

Constructor arguments are horizontal centre/bend separation, vertical layer
spacing, and the number of downward/upward sweep pairs. Zero sweeps preserves
the initial within-layer order and placement. Pins retain their exact requested
coordinates and may break layer alignment or spacing. This static layout does
not use force iteration/time budgets.

Each `EdgeRoute` includes both endpoint centres and a zero-based parallel-edge
occurrence per original source/target pair. `LayoutResult` bounds include route
bends. Coordinate transforms transform bends, packing translates each complete
component including its routes, and overlap removal reconnects moved endpoints
without recomputing bends or crossing reduction.

The GUI selector exposes Sugiyama with spacing, level spacing and ordering passes.
`/api/layout` returns `edgeRoutes` alongside positions. Sigma and Cosmograph
currently apply the node positions while retaining their usual edge rendering;
polyline rendering is not yet connected to these viewers.

With V real nodes, E arcs and D dummy vertices, the greedy ordering here costs
O(V² + E); ranking and expansion cost O(V + E + D). Each ordering sweep costs
O(E + D + sum of layer sorting costs), with inversion counting logarithmic in
layer width. Coordinate sweeps may push O((V + D)²) vertices in the worst case.
Dummy storage can grow to O(VE) for graphs with many long edges.

## Stage E: advanced layouts

| Class | Publication / model | One animation step |
|---|---|---|
| `StressMajorizationLayout<U>` | Gansner, Koren and North, GD 2004 (published 2005) | Solve both coordinate systems for one majorization update |
| `KamadaKawaiLayout<U>` | Kamada and Kawai (1989) | One Newton update of the currently selected vertex |
| `MultilevelForceLayout<U>` | Hu (2005), edge-collapsing (EC) variant | One sequential force sweep at the current hierarchy level |
| `CommunityLayout<U>` | Composition of published child layouts | Batch composition; returns the final result |

The first three implement `IterativeLayout`, with seeded initialization, partial
warm starts, exact pins, active-time/caller cancellation and atomic snapshots.
The GUI exposes their parameters and tooltips, animates through the existing
position-only update path, and retains final-only overlap removal and packing.
No default wall-clock limit is added. The common iteration budget is explicit;
exhausting it yields `LIMIT_REACHED`, retaining the latest complete coordinates.

### Full stress majorization

The implementation follows [Gansner, Koren and North, Graph Drawing by Stress
Majorization](https://doi.org/10.1007/978-3-540-31843-9_25), equations (1) and (8–9).
Its objective is `sum(i<j) (||xi-xj||/dij - 1)^2`, with target distances
`dij = edgeLength * shortestPath(i,j)` and weights `1/dij^2`. Each iteration solves
`Lw * Xnext = L(Xcurrent) * Xcurrent`. Reduced Laplacians use reusable Cholesky
factors. Each unpinned component fixes its first vertex to remove translational
singularity; this does not constrain the component's relative coordinates.
Pins use Dirichlet constraints, an explicit extension to the unconstrained model.
Termination uses relative stress reduction, default `1e-4`; stress zero also stops.
The paper's `inv(0)=0` convention is retained: completely coincident warm starts
can remain coincident. Disable warm starting to obtain a seeded nondegenerate start.

### Kamada–Kawai

The implementation follows [Kamada and Kawai, An Algorithm for Drawing General
Undirected Graphs](https://doi.org/10.1016/0020-0190(89)90102-6). Within each component,
`lij = drawingSize * shortestPath(i,j) / graphDiameter` and
`kij = springConstant / shortestPath(i,j)^2`. It minimizes
`0.5 * sum(i<j) kij * (||xi-xj|| - lij)^2` using the analytic 2D gradient and Hessian.
The original nested selection is retained: select the largest-gradient movable
vertex and continue optimizing it before selecting another. Convergence means
all movable gradient magnitudes are at most tolerance, default `1e-4`.

Numerical extensions are explicit: backtracking accepts only energy-decreasing
Newton updates; singular/non-descent Hessians use a gradient step; coincident
vertices use a deterministic direction to define the otherwise undefined
derivative. No arbitrary displacement cap is applied. If no decreasing step is
representable after backtracking, the result is `LIMIT_REACHED`, not convergence.
Pins constrain vertex movement without removing their springs.

Both distance models ignore direction and self-loops. Default shortest paths
use hop counts; optional weights must be positive finite **path lengths**.
Reciprocal/parallel edges use the minimum length. Unreachable pairs contribute
no spring/stress; weak components are solved independently and can be packed
afterwards. Full-distance storage is O(V²). Shortest-path preparation here uses
O(V(V+E) log V) Dijkstra searches; stress factorization is O(sum(componentSize³)),
with O(V²) updates. Kamada–Kawai's Newton update is O(V); selecting the maximum
gradient can cost O(V²). These implementations target moderate-sized networks.

### Multilevel spring-electrical layout

The implementation follows [Hu, Efficient, High-Quality Force-Directed Graph
Drawing (2005)](https://yifanhu.net/PUB/graph_draw_small.pdf), Algorithm 1 and the EC
multilevel construction. It uses heavy-edge maximal matching, quotient adjacency
`P^T G P` with summed crossing strengths, and stops coarsening at two vertices or
when the coarse/fine vertex-count ratio exceeds `.75`. This is the EC variant;
MIVS and hybrid coarsening are not claimed. Stars and graphs constrained by many
pins may therefore stop coarsening early.

Attraction magnitude is `weight * r²/K`, repulsion is `C*K²/r`, default `C=.2`.
Vertices move sequentially by `step * force/||force||`. The adaptive coarsest
cooling uses the sum of squared force norms, growing the step after five decreases
and shrinking it otherwise; finer levels use geometric cooling, default `.9`.
The coarsest drawing is seeded random when no warm start is supplied; warm starts
are aggregated into coarse centroids. Coarsest K uses average initial edge length. Refinement scales K by the ratio of
fine/coarse pseudo-diameters, estimated with two BFS sweeps per component.
Children inherit their parent coordinates with small seeded perturbations.
Coincident force pairs also use a deterministic separation direction.
No gravity, velocity damping, global distance cutoff or fixed movement cap is added.

Theta zero evaluates exact pair forces; positive theta uses a Barnes–Hut tree
whose counts/centers are updated after **each** vertex movement, preserving
sequential update semantics. Typical sparse-graph sweep cost is O(V log V + E),
with worst-case quadratic force evaluation. Hierarchy storage uses sparse edges;
all-pairs graph distances are not computed.

Weighted input uses positive finite **attraction strengths**, not path lengths.
Unit original edges are the default; parallel and reciprocal directed arcs sum.
Directions and loops are ignored. Coarse edges always sum crossing strengths.
Pinned vertices are singleton aggregates and never match other nodes. Hierarchy
frames project the current coarse coordinates back to all original vertices;
members of an aggregate may coincide until refinement separates them.

The movement stopping rule is `||Xnext-X|| < tolerance*K`, default `.001`.
`levelIterations` (default 100) limits each level; `request.maxIterations` limits
all sweeps across the hierarchy. Per-level truncation advances refinement and
is reported in the final diagnostics. If the total budget expires before the
finest level, the last coarse projection is returned; increase the total budget
to allow more refinement. These scheduling limits are extensions to the paper.

### Community composition

Supply a disjoint nonempty partition covering every graph node; an empty graph
accepts an empty partition. Layout each induced community with `inner`, then its
undirected quotient graph with `outer`, then separate circular envelopes and
translate the community drawings. Induced subgraphs retain original directions,
weights and parallel edges. Quotient edge weights count original crossing edges,
independently of attribute values. Defaults use Fruchterman–Reingold inside and
Hu with crossing counts as attraction strengths outside.

This is a configurable composition of published models, **not** a claimed
reproduction of another named community-layout algorithm. The UI exposes both
child choices: FR, stress majorization, Kamada–Kawai, or Hu, with default model
hyperparameters. Group by selects an existing community assignment or node
attribute; community detection is external.

Every child receives its own iteration and active-time budget, seed and filtered
constraints. Diagnostics sum child iterations and propagate cancellation/limits.
A community with pins remains at its original center and preserves its pins;
conflicting pinned envelopes raise an error. Within-community edge routes are
translated with their nodes. Inter-community edges retain ordinary straight-edge
rendering. Community composition is batch-only; individual iterative children
remain available separately for animation.

```java
LayoutSession<String> stress = new StressMajorizationLayout<String>().initialize(
    graph, LayoutRequest.<String>builder().seed(42).maxIterations(1000).build());
stress.step(1);
LayoutResult<String> frame = stress.snapshot();

LayoutResult<String> grouped = new CommunityLayout<String>(communities, 50)
    .compute(graph, LayoutRequest.<String>builder().seed(42).maxIterations(1000).build());
```

## Geographic layout and background map

The default projection is Web Mercator. The four-argument constructor also
accepts `GeographicLayout.Projection.EQUAL_EARTH`, implementing the spherical
[Equal Earth equations](https://proj.org/en/stable/operations/projections/eqearth.html)
with radius 6378137 m. Equal Earth accepts latitudes through ±90° and uses
the supplied central meridian as x=0. In the UI, select **Equal Earth (world map)**
to show a projected Natural Earth vector background with the same camera as
the network. The API accepts `params.projection: "equal-earth"` or `"mercator"`
(default). Equal Earth edges are geodesic paths clipped at the projection seam;
its world background provides country boundaries rather than street detail.

`GeographicLayout<U>` projects decimal-degree latitude/longitude functions to
spherical Web Mercator metres (EPSG:3857), using WGS84's 6378137 m semi-major axis.
The equations follow the [PROJ Web Mercator documentation](https://proj.org/en/stable/operations/projections/webmerc.html).
Edges, weights and radii do not alter geographic placement. Longitudes must be
within -180 to 180 degrees; the map's square tile world supports latitudes within
approximately +/-85.05112878 degrees. Out-of-range or missing coordinates are
rejected rather than silently replaced. A central longitude controls date-line
wrapping. Pins must exactly match their projected locations.

```java
LayoutResult<String> geographic = new GeographicLayout<String>(
    node -> latitudes.get(node), node -> longitudes.get(node), 180)
    .compute(graph, LayoutRequest.defaults());
```

In the UI, choose **Geographic (background map)** and select numeric latitude and
longitude node attributes under Advanced layout settings, then Apply layout.
Common attribute names such as `lat`, `latitude`, `lon`, `lng` and `longitude` are
automatically suggested. Computed metrics are not offered as coordinate inputs.
The Geographic map renderer also opens these settings. The UI automatically
chooses the smallest longitudinal span, keeping nearby nodes across the date line
together. `/api/layout` accepts `algorithm: "geographic"`, complete `latitudes`
and `longitudes` node maps, and optional `params.centralLongitude`.

The dedicated geographic renderer uses [Leaflet 1.9.4](https://leafletjs.com/)
and OpenStreetMap raster tiles. Nodes and straight edges are native map overlays,
so tiles and graph geometry share one projection and camera during zoom/pan.
Node/edge selection, appearance colours/sizes, timeline visibility and selection
filters use the shared graph and reducers. Hover labels identify edges, including
their direction; arrowheads and curved/parallel-edge separation are not rendered
in this initial map view. Map labels can be shown persistently using the shared
node-label toggle. Forces, node dragging, graph editing, overlap removal and
component packing are disabled while viewing geography. Choosing another layout
returns to Sigma/Cosmograph. Projected coordinates are available to normal graph
exports. PNG/SVG map exports capture the visible geographic viewport and retain
attribution. Mercator embeds loaded raster tiles and draws the graph as vectors;
Equal Earth retains a vector background and graph. Diffusion maps also provide
PNG/SVG exports including the current iteration and its overlays.

The library is loaded on demand from the pinned CDN, and background tiles require
an internet connection. Visible attribution links to OpenStreetMap contributors.
Tiles use ordinary browser caching and are fetched for the viewport only; there is
no bulk downloading or offline prefetching. Tile behaviour follows the
[OpenStreetMap tile usage policy](https://operations.osmfoundation.org/policies/tiles/).
If tile requests fail after library loading, graph overlays remain usable and
the UI reports the failure.
