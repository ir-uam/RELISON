# RELISON-gui

A Gephi-like graphical interface for RELISON. It runs a small local web server (Javalin) that wraps RELISON's
graph IO and social-network-analysis metrics, and serves a [sigma.js](https://www.sigmajs.org/) frontend for
interactive visualization.

## Features

- **Sphere packing 3D** mirrors circle packing: one filled ball without
  grouping, or one compact ball per community/attribute arranged around a
  sphere. Cluster envelopes stay separated even with unequal group sizes.

- **Spectral 2D / 3D**: classical Laplacian or Koren degree-normalized
  eigenprojection, with optional similarity weights. Disconnected components
  are solved separately. Scale, residual tolerance and eigensolver sweep budget
  are available in Advanced layout settings. Nonconvergence is reported.

- **Paper-defined 3D models** (select Cosmograph 3D): spherical MDS
  (Miller–Huroyan–Kobourov, animated), upward DAG grid
  (Dujmovic–Wood, Theorem 3.1), and H3 hyperbolic hierarchy (Munzner).
  H3 provides root/traversal selection, non-tree-link visibility, and selected-node
  hyperbolic focus/reset controls. Spherical MDS requires a connected graph;
  upward grid rejects directed cycles. See the
  [algorithm contracts and references](../RELISON-viz/README.md#paper-defined-non-euclidean-and-upward-models)
  for precision limits, supported paper variants and viewer differences.
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
- **Chart exports**: metric and diffusion chart downloads offer PNG and vector SVG,
  with either the current view or the complete chart at the current zoom. Both
  retain chart styling and legend selections while excluding zoom controls.
  SVG supports complete charts beyond the PNG pixel-size limit without allocating
  a full-size raster canvas.

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

### Filters and selection

The Nodes & Edges tables share a working network. Each table offers **Match all
(AND)** (default) or **Match any (OR)** for combining its active column filters.
For example, edge filters `source = A` and `target = A` with OR match links
incident to A. The two table groups still apply together: eligible edges must
satisfy their edge filter group and have two eligible endpoints satisfying the
node filter group. With no active filters, either mode includes every row.
Clearing filters preserves the combination preference; loading a new network
resets both tables to AND. Changing the combination retains selected targets,
refreshes tables, renderers and exports, and invalidates filtered path results.
Choose **Build filters…** to open a large overlay above the table and use multiple conditions on the same field, individual
AND/OR connectors, and nested groups. AND binds before OR; groups provide explicit
parentheses, and the displayed expression makes the grouping visible. For example,
`(country = Spain OR country = Portugal) AND age > 30` can be built with a country
group and an age condition. Operators follow the selected field's type.

The builder copies the currently applied filters into a draft. Edits show a node
and edge match preview without changing the view until **Apply filters** is pressed.
Incomplete conditions and invalid values block Apply. **Close** discards unapplied
edits. Escape, the × button and a backdrop click also close the overlay without
applying pending edits. The editor scrolls independently while Apply and Close
remain visible, and closing returns keyboard focus to Build filters. Applying
replaces that table's column filters with the expression; new
column filters added afterward combine with it using AND. Opening the builder
again includes those additional filters so they can be regrouped. **Remove custom filter**
beside an applied expression clears only that expression, preserving column filters.
**Clear filters**
removes both the expression and column filters. Removing a field prunes its applied
conditions; renaming an attribute preserves conditions using that attribute.
With edge filters active, the nodes table contains the matching edges' endpoints.
Without edge filters, matching isolated nodes remain. Filter chips in both tables
show all active constraints, including filters on hidden columns.

Selection is independent of filters. **None** removes selection emphasis while
retaining filter emphasis. **Highlight** keeps the working rows and marks the
selection; **Show only** restricts tables and graph to the same node and edge
sets. Clearing selection retains filters, entity type and view-mode preference.
Clearing either table's filters retains valid selected targets. An excluded
target remains in the inspector with an explanation; empty isolation never
silently expands the working set. Each selection type remembers its view mode.

Path searches default to **Filtered graph**, traversing eligible nodes and links
on the server. **Whole graph** searches the complete network, while its selected
paths remain subject to view filters; clipped paths are explicitly identified.
Changing endpoints or search scope invalidates results immediately. Filtered
search results also expire when their filters or relevant data change; late
responses cannot restore them. All returned paths are initially selected, and
individual paths can be deselected without losing the results. None preserves
selected paths. Directed paths exclude reverse links; eligible parallel links
between consecutive path nodes participate in the same node-sequence path.

CSV exports contain every row of the effective table result, across pages, in
the current sort order. IDs and string/categorical equality are exact;
substring search is case-insensitive. Numeric and boolean comparisons follow
the declared column types; invalid numeric bounds are identified beside filters.

Regression checks:

```sh
node --test RELISON-gui/src/test/js/network-selection.test.cjs
mvn -pl RELISON-gui -am -Dtest=PathControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

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
### Geographic maps and Globe.gl

For a rotatable globe, choose **Geographic map / globe** in the network renderer
selector, then **Globe (Globe.gl)** under Advanced layout settings → Projection.
Select numeric latitude/longitude node attributes (decimal degrees), then Apply
layout. Nodes are anchored to the surface; no altitude attribute is needed.
Globe view does not modify the network's XY/Z layout coordinates. Drag rotates,
scroll zooms, and the toolbar zoom/fit controls operate on the globe camera.
Node/link selection, shared appearance reducers, labels, filters, path highlights,
recommendation overlays and legends use the same graph as other renderers.
Links follow raised great-circle curves, with arrows for directed links and
separate curves for parallel edges. Coincident endpoints use small loops.

Diffusion supports **Geographic: Globe** and automatically uses it with **Follow
network renderer** when the network is a globe. State colours and propagation
overlays update during playback. Each globe has an independent camera.
Graph editing, node dragging and force layouts are unavailable in geographic views.

Globe.gl 2.46.2 and Three.js 0.180.0 are loaded on demand from esm.sh; TopoJSON
3.1.0 converts world-atlas 2.0.2 country boundaries (Natural Earth) from jsDelivr.
An internet connection and WebGL are required. No imagery service/API key is used.
The globe export captures the current camera at twice the viewport resolution.
Its PNG includes labels, optional legend and attribution. Its SVG embeds the
WebGL globe/network as a high-resolution raster image, with vector labels and
legend; it is **not a fully vector globe export**.

GIF and WebM animation exports provide HD (1280px wide), Full HD (1920px,
the default), QHD (2560px), 4K (3840px), and native-resolution choices.
These settings apply to network rotation cycles, timeline exports and
Sigma/Cosmograph diffusion playback. The viewport's aspect ratio is preserved,
so a 16:9 viewport produces 3840×2160 at 4K. Vector frames are rasterized at
the selected size; Globe and diffusion Cosmograph captures render at the
corresponding pixel density and then restore the live renderer settings.
WebM uses a bitrate scaled to the output dimensions. GIF frames are encoded
sequentially in a worker to avoid retaining all uncompressed frames. WebM is
recommended for detailed or shaded networks because GIF has a limited colour
palette and produces larger files.

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
