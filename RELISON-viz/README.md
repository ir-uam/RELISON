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
Node sizes, overlap prevention, iterative sessions, cancellation, edge routing,
and parameter/capability metadata beyond algorithm identity are future stages.

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
