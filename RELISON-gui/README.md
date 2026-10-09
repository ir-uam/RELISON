# RELISON-gui

A Gephi-like graphical interface for RELISON. It runs a small local web server (Javalin) that wraps RELISON's
graph IO and social-network-analysis metrics, and serves a [sigma.js](https://www.sigmajs.org/) frontend for
interactive visualization.

## Features
- **Load** a tab-separated edge list (`source⇥target[⇥weight]`) as a directed/undirected, weighted, multigraph
  and/or self-looping network.
- **Visualize** with sigma.js: pan/zoom, hover, ForceAtlas2 layout (start/stop), circular reset.
- **Layouts**: circular, seeded random, grid, shell, concentric, and saved positions
  computed by `RELISON-viz` through the layout selector. Shell groups use community
  partitions or node attributes; concentric scores use degree, a computed vertex
  metric, or a numeric node attribute. Animated layouts and circle packing remain
  available. Static layouts refresh both Sigma and Cosmograph.
- **Structural layouts**: radial/ego with root and traversal direction, automatic
  or grouped bipartite, grouped multipartite, and tidy tree/forest. Optional
  disconnected-component packing preserves each component's internal shape.
- **Appearance**: size and colour nodes by any computed vertex metric or by community membership.
- **Metrics** (computed by RELISON, not re-implemented):
  - *Vertex*: degree, inverse degree, reciprocity rate, coreness, eigenvector, length, free discovery, PageRank,
    Katz, HITS, local clustering coefficient, closeness, harmonic, eccentricity, betweenness.
  - *Graph-global*: number of edges, density, clustering coefficient, reciprocity, degree Gini / assortativity /
    Pearson, ASL, diameter, radius, infinite distances.
  - *Pair/edge*: weight, reciprocity, distance, geodesics, neighbour overlap (FOAF), preferential attachment,
    embeddedness, weakness, edge betweenness.
- **Community detection** (full RELISON algorithm set): connected components, Louvain, FastGreedy, Girvan-Newman,
  label propagation, Infomap, balanced/size-/Gini-weighted FastGreedy, ratio-/normalized-cut spectral; plus global
  community metrics (modularity, community-size Gini, weak ties, inter-community edge Gini, …).
- **Editing**: add/remove nodes and edges interactively; metric/community results are recomputed against the
  edited graph.

## Build
From the repository root (builds the whole reactor including this module):

```
mvn install
```

or only this module and its dependencies:

```
mvn -pl RELISON-gui -am package
```

This produces a runnable fat jar at `RELISON-gui/target/RELISON-gui.jar`.

Layout integration tests:

```sh
mvn -pl RELISON-gui -am -Dtest=BasicLayoutsTest,StructuralLayoutsTest,LayoutControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
node --test RELISON-gui/src/test/js/layouts.test.cjs
```

## Using the basic layouts

Choose an algorithm in **Layout**, adjust its visible parameters, and click
**Apply layout**. Use **Save positions** to capture the current arrangement;
choose **Saved positions** to restore it after trying other layouts. Saved
positions also lets you select numeric node attributes
independently for **X coordinate** and **Y coordinate**. Their values become the
coordinates directly, without normalization. Selecting features for both axes
does not require saving positions first; a saved axis can also be combined with
a feature axis. Each feature must have a finite numeric value for every node.
Axis selections are saved with the session. Saved
coordinates and layout controls are included in `.relison` sessions. Adding or
removing nodes requires saving a new arrangement before restoring it.

Shell groups are ordered lexicographically by their labels from inside to outside.
Concentric scores must be finite and available for every node. Neither option
represents hop distance. **Reset (circular)** also uses the RELISON-viz backend.

The `POST /api/layout` endpoint accepts `graphId`, `algorithm`, optional `params`,
`nodeOrder`, `positions`, `pinnedNodes`, `shells`, and `scores`. It returns a
node-ID-keyed coordinate map, bounds, algorithm ID, and termination reason.
Unknown graphs return 404; invalid layout inputs return 400. Coordinates stay
client-owned and do not change the stored graph's structure or attributes.

## Structural layouts

- **Radial / ego** accepts a root node ID (blank chooses the first ordered node),
  traversal direction, and spacing per hop. Unreachable nodes share an extra
  outer ring that does not represent a hop distance.
- **Bipartite** automatically detects two sides unless a grouping is selected.
  Explicit grouping must have exactly two groups. **Multipartite** requires a
  grouping with at least two groups. Groups become columns in label order.
  Edges within a group are rejected. Ordering passes use a crossing-reduction
  heuristic; zero passes keeps node order within each column.
- **Tidy tree / forest** requires acyclic topology. Directed edges must point
  from parents to children and every node has at most one distinct parent.
  The chosen directed root must have no parent. Undirected trees can be rerooted.
  Disconnected forests and isolates are supported.
- **Pack disconnected components** translates entire weak components onto
  shelves. It is off by default, so saved/attribute coordinates retain their
  original meaning. Packing can move global columns or axis origins between
  components; their internal shapes remain unchanged. The library/API preserves
  pinned components in place and rejects conflicting anchored boxes.

Structural endpoint options include `partitions` (ordered node arrays) and
parameters `root`, `direction` (`OUT`, `IN`, `UND`), `spacing`,
`columnSpacing`, `levelSpacing`, `sweeps`, `packComponents`, and `packingGap`.

## Run
```
java -jar RELISON-gui/target/RELISON-gui.jar [port]
```

The server listens on `http://localhost:7070` (override with the optional `port` argument) and opens your browser.
Load an edge list (e.g. the repository's `data/train.txt`, directed + weighted) to begin.

## Notes
- Node identifiers can be arbitrary tokens (numeric or textual): they are read as strings (`Parsers.sp`), so no
  node-type configuration is needed. The node-id fields in the UI are searchable selectors — start typing an id to
  filter the matches.
- Node and edge **attributes** can be imported from tab-separated sidecar files whose header declares the schema as
  `name:type` columns (types: `int`, `long`, `double`, `bool`, `string`, `categorical`), e.g.
  `id<TAB>age:int<TAB>country:categorical` for nodes and `source<TAB>target<TAB>since:long` for edges. Imported
  attributes appear as table columns, can be filtered, and can drive node size / colour (numeric → ramp,
  categorical → distinct colours) and edge thickness. For multigraphs, consecutive rows for the same
  `source<TAB>target` map to that pair's parallel edges in order (one row per parallel edge).
- Distance-based metrics (closeness, betweenness, eccentricity, ASL, diameter, …) trigger an all-pairs distance
  computation that is cached and shared per session; the first such metric on a large network may take a while.
- `Infomap` requires an external binary and the spectral algorithms need extra numeric libraries; they are listed
  in the UI but report a clear error if their prerequisites are missing.
- The frontend currently loads sigma.js / graphology from a CDN, so the first run needs internet access.

Additional layouts: `feature-grid` (`FeatureGridLayout`) places feature-value groups in columns and permits within-group edges; one group is valid. `ego-grid` (`EgoGridLayout`) places hop distances in columns, with a selectable root and `EdgeOrientation` traversal. Unreachable nodes occupy a final column. Both accept column spacing and row spacing; feature grids also support ordering passes.
RELISON animated force choices: fruchterman-reingold and relison-forceatlas2.
Controls include iterations, seed, Barnes–Hut theta (zero = exact), tolerance,
warm start, force settings, and optional overlap removal with coordinate-unit
radii and gaps. Responses include iteration count, displacement and termination.
The UI starts a server session and renders successive iteration snapshots in Sigma
and Cosmograph. Stop cancels it and preserves the last displayed frame. Optional
overlap removal and packing run only on natural completion. Batch computation
remains available through POST /api/layout.

Animation protocol: POST /api/layout/session accepts the same request as the batch endpoint and returns sessionId, finished, coordinates and diagnostics. POST /api/layout/session/{id}/step accepts integer iterations from 1 to 10. DELETE /api/layout/session/{id} cancels immediately. The browser requests one iteration per frame with no overlapping steps; start responses arriving after Stop are cleaned up without applying coordinates. Sessions are removed on completion, cancellation or failure, with idle entries reaped after two minutes on subsequent requests. At most 128 sessions can be retained.
### Equal Earth background

The network export toolbar provides **Include colour legend** (checked by
default). PNG and SVG exports respect this option for every layout in Sigma,
Cosmograph and both geographic projections. Geographic diffusion exports have
their own Include legend checkbox. Numeric legends show a gradient and range;
categorical legends show swatches. No legend is added when no colour mapping
is active. Map attribution remains present when the legend is disabled.

Both geographic projections draw transient recommended links with the selected
recommendation colour and dashed/solid setting. The overlay follows the map
camera and excludes endpoints hidden by network filters or timelines.

The Diffusion renderer selector includes **Geographic: Mercator** and
**Geographic: Equal Earth**. Choose numeric latitude/longitude attributes in
Network settings as needed. Its default **Follow network renderer** chooses
the main network renderer (including the applied geographic projection) when
entering Diffusion. Selecting a specific renderer overrides this default;
select Follow network renderer again to resume mirroring.
Choose the coordinate attributes in
Network → Advanced layout settings first. Each diffusion map owns its camera
and graph subscriptions independently of the network map. Iteration playback
updates informed/newly informed/propagating node colours and dashed propagation
edges; simulations run over recommendations also display those links.
GIF/WebM map recording is not yet available; use Sigma or Cosmograph to record.

In Geographic layout settings, select **Web Mercator (street map)** or
**Equal Earth (world map)**. Equal Earth accepts latitudes through ±90° and
uses the spherical equations of Savric, Patterson and Jenny (2018), as
implemented in [D3](https://github.com/d3/d3-geo/blob/main/src/projection/equalEarth.js)
and documented by [PROJ](https://proj.org/en/stable/operations/projections/eqearth.html).
The central meridian is chosen around the network, including date-line networks.
The backend returns metres using sphere radius 6378137 m; this is the spherical
form, rather than an ellipsoidal coordinate reference system.

Equal Earth projects Natural Earth land and country boundaries, graticules,
nodes and geodesic edges through one D3 projection and pan/zoom transform.
Its world background has no street detail. D3 7.9.0, TopoJSON client 3.1.0 and
world-atlas 2.0.2 (Natural Earth 1:110m data) load from jsDelivr on demand.
Mercator retains its OpenStreetMap tiles. Both views retain selection and
appearance settings. PNG/SVG exports capture the current map viewport, including
visible nodes, labels, edges, recommendation/propagation overlays and attribution.
Mercator SVG files embed loaded OpenStreetMap raster tiles with vector graph
elements; Equal Earth SVG files keep the background and graph as vectors.
Exports are standalone and do not fetch additional tiles. Mercator export waits
for visible tiles and reports missing tiles or CORS errors rather than omitting
the background. Diffusion map views have their own PNG/SVG export buttons.
