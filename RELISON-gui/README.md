# RELISON-gui

A Gephi-like graphical interface for RELISON. It runs a small local web server (Javalin) that wraps RELISON's
graph IO and social-network-analysis metrics, and serves a [sigma.js](https://www.sigmajs.org/) frontend for
interactive visualization.

## Features

- **Report notebook (cell configuration)**: the report button opens **New report**
  and a disabled **Load report** option. The notebook has an outline and editable
  options for text, graph snapshots, top-k node tables and metric charts. Cells
  can be reordered with drag handles (drop into the document or its outline) or
  the arrow buttons, duplicated, deleted and previewed.
  The full-height contents and cell-options panels can be collapsed independently
  and resized by dragging their inner edges or using arrow keys on the dividers.
  The selection screen lists every open report, so creating a new one preserves
  the others. Drafts remain in memory while the page is open, independently of
  `.relison` sessions. Report loading,
  saving and metric cell execution are not connected yet.
  **Export PDF** downloads the current report directly as a PDF, with A3, A4,
  A5, Letter and Legal page sizes and portrait or landscape orientation. Text,
  caption references, snapshots and captions are included; unfinished cells are
  identified as such. PDF text uses the bundled Roboto family, including bold and
  italic variants. Images that cannot be loaded for export are noted in the PDF.
  Graph snapshot cells offer **Capture from network**, which opens Network with
  **Capture for report** and **Cancel and return to report** buttons. Adjust the
  current renderer, camera, layout, filters and appearance there, then capture
  a still image into the original report cell. Sigma, Cosmograph (2D/3D) and
  geographic maps/globe use their existing image capture paths; the network's
  **Include visible legends** export option also applies. **Replace snapshot**
  updates only that image, retaining the caption. Captures stay in the report's
  in-memory draft when the network changes, independently of `.relison` sessions.
  Text cells support Markdown headings, lists, links, code, quotations and tables.
  A shared toolbar at the top of the editor formats the active text cell with
  bold, italic, underline, strikethrough, colour, font family/size, headings,
  lists, quotations, code and links. Click a text cell or **Edit text** to edit
  its rendered content normally. **Visual / Markdown** switches between visual
  editing and source; no formatting toolbar is placed inside cells or side panels.
  Direct edits are retained in the draft; **Done** or Ctrl/Cmd+Enter returns to
  reading. Switching cells/reports or previewing retains edits. Cells have no
  titles; numbered type labels identify them in the editor. Tables, graph snapshots
  and charts support optional captions beneath their output, including in preview.
  Captioned tables and figures are numbered in separate sequences in report order;
  graph snapshots and charts share the figure sequence. Uncaptioned cells are not
  numbered. In a text cell, use **Insert reference** in the top toolbar to link
  to a captioned cell. References follow that cell as numbering changes; clicking
  one in reading or preview mode navigates to it. Removing its caption or deleting
  its cell displays “Missing reference”; restoring the caption restores the link.
  Markdown stores these as `[reference](#relison-report-cell-ID)` links, with the
  current label resolved when rendered.
  Local Marked, DOMPurify and
  Turndown bundles render, sanitize and convert the two representations,
  preserving underline, colour/font spans, tables, lists, code and links.

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
  Import controls follow the explicit format or extension auto-detection: edge
  lists expose delimiter/header and graph flags; Pajek exposes graph flags;
  GEXF exposes label handling and automatically preserves weights, parallel
  edges and loops; sessions use their saved settings. GEXF imports check actual
  edge multiplicity and use a simple graph when no parallel edges exist, keeping
  Recommendation available. Weights, attributes and loops are retained.
- **Visualize** with sigma.js: pan/zoom, hover, ForceAtlas2 layout (start/stop), circular reset.
- **2D view transforms**: hold ↻ beside zoom/fit to rotate node coordinates around
  the centroid of all nodes; ↔ and ↕ mirror horizontally and vertically around
  that centroid. These controls work in Sigma and Cosmograph 2D. Release the
  rotation button (or Space/Enter) to stop. Transforms stop running layouts and
  preserve node styling. The ⟳ button rebuilds the active renderer while retaining
  the graph and positions, including a fresh Cosmograph instance.
- **Layouts**: circular, seeded random, grid, shell, concentric, and saved positions
  computed by `RELISON-viz` through the layout selector. Shell groups use community
  partitions or node attributes; concentric scores use degree, a computed vertex
  metric, or a numeric node attribute. Animated layouts and circle packing remain
  available. Static layouts refresh both Sigma and Cosmograph.
- **Grid ordering**: optionally choose Degree or a computed node metric and
  reverse its order. Applies to Grid, Grid 3D, feature grids and ego grids,
  including flat 3D variants. Feature/hop group membership stays unchanged;
  ties retain ID order. Explicit metric order disables feature-grid ordering
  passes. Default order preserves the previous behaviour.
  The **Ego group** contains Grid / ego (2D) and Grid / ego (3D); the latter
  arranges each hop on a grid layer and also supports metric order and reverse.
- **Random selection**: the right Network selection panel can pick a random
  node, edge, or distinct node pair. Nodes and edges use the current filters;
  pairs use the path search scope and immediately run the usual shortest-path
  search. Parallel edges are sampled individually.
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
  community metrics (modularity, community-size Gini, weak ties, inter-community edge Gini, …). Individual metrics
  also include each graph-global metric computed on the induced subgraph of every detected community or node-attribute
  value (for example, `Intra-community Density` and `Intra-Attribute Clustering coefficient`).
- **Editing**: add/remove nodes and edges interactively; metric/community results are recomputed against the
  edited graph.
- **Chart exports**: metric and diffusion chart downloads offer PNG and vector SVG,
  with either the current view or the complete chart at the current zoom. Both
  retain chart styling and legend selections while excluding zoom controls.
  SVG supports complete charts beyond the PNG pixel-size limit without allocating
  a full-size raster canvas.
- **Recommendation metric batches**: **Compute for recommendations** reruns the
  saved metric configurations for any checked recommendations, with progress,
  cancellation and per-result failure reporting. The visible network overlay
  stays unchanged. New metric requests retain their exact submitted parameters;
  older results recover configurations from their catalog labels where possible,
  and the dialog identifies any originals that must be computed once again.
  Saved sessions retain these configurations and recommendation metric results.
  Distribution statistics and rankings each have an Original/recommendation
  result selector. Distributions and metric-versus-metric comparisons offer
  independent result checkboxes, including Original. Comparisons also support
  Original versus multiple recommendations, or a reference recommendation
  versus other checked recommendations. Every comparison matches entity IDs;
  node/edge groups keep their existing partition. Recommendation rankings and
  distributions include newly recommended links when their metric values exist.
- **Metric analysis workspace**: choose a metric type tab, then use the Summary,
  Rankings, Distributions and Compare rail. **Compute metrics** opens or hides
  the existing runner panel. Rankings retain top/bottom-k, sorting and display
  options. Compact icon exports sit at the top right of each panel. Summaries for
  communities, edge partitions and node/edge attributes include their related
  global results. Distributions include histograms configured by bin size with
  shared boundaries for recommendation overlays, exact value frequency charts,
  cumulative curves and box plots (whiskers show min/max). Frequency and cumulative
  charts support points, lines, or both. Stored all-pair bins can be merged using
  whole multiples of their original width. Comparisons
  match finite values by entity ID within the same partition or attribute, using
  independent scales for different metrics and equal scales for original versus
  recommendation results. Recommendation changes also have a distribution view.
  Chart data and distribution statistics export to CSV. All-pairs results retain
  every node pair exactly. Distinct-value counts support a selectable minimum
  histogram bin size, exact value-frequency charts, exact cumulative curves and
  distribution statistics without storing one record per pair. Individual
  rankings/comparisons still require raw pair values.

## Tracking background processes

Open **Processes** in the top bar from any tab to see active computations and the
100 most recent finished processes. Metrics, community detection, edge-partition
detection, recommendations/evaluation and diffusion share this registry. Each run
has its own identifier, configuration, graph, phase, running time and queue time.
The list is restored from the running server after a browser reload; it is not
saved in `.relison` files or retained across server restarts.

Progress counts completed node values, existing-link endpoint pairs, individual
communities/edge groups and diffusion turns. Shortest-path preprocessing reports
completed source nodes. All-pairs metrics report each evaluated pair while
collecting statistics and distinct-value counts in one pass. Native matrix
operations and algorithms without a known total show their phase and elapsed
time. Percentages describe work units, not remaining time. Diffusion starts at
iteration zero, so an upper iteration index of 50 produces 51 completed turns
under the existing stop-condition semantics.

**Cancel** in this panel and the existing **Stop** buttons target the same run.
An accepted request remains **Cancelling** until the worker and its cleanup have
finished. Blocking numerical operations may have to return before they can stop.
Cancelled runs do not publish partial results. Computations on one graph queue
behind each other to protect shared caches; different graphs can run concurrently.
Graph edits return a conflict while that graph is in use, including cancellation
cleanup. Infomap runs in an isolated temporary directory and its child process
is terminated on cancellation.

The API exposes `GET /api/jobs` and `POST /api/jobs/cancel` with
`{graphId, jobId}`. Computation requests accept an optional `X-Relison-Job-ID`
UUID header; the browser generates a fresh ID for each request. The legacy
`{graphId, kind}` cancellation form remains available.

Library monitoring is opt-in through a scoped `ComputationMonitor` in
`RELISON-core`; existing method signatures and bulk computations remain intact.
Without a scope, result maps are returned directly without tracking wrappers.
Parallel code must capture the monitor before dispatch and pass that reference
to its worker callbacks. Progress notifications are batched in output maps.

Regression checks:

```sh
mvn -o -pl RELISON-gui -am -Dtest=JobManagerTest,ProgressMonitoringTest,ProcessAdapterTest,ProcessControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
node --test RELISON-gui/src/test/js/process-panel.test.cjs
```

`ProcessControllerTest` uses localhost and is skipped when the Windows sandbox
denies HttpClient's loopback connection. The other process tests require no sockets.

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

## Exploring communities

The **Communities** tab appears before **Metrics**. Its left panel contains the
blue **Detect** button and partition controls, including the summary, partition
metrics and **View in Metrics**. Choose a partition and switch between **Graph**
and **Table**. Detect selects an existing result when the method and typed parameter
values match a stored configuration, preserving its labels, metrics and selection.
New configurations run detection. Saved configuration metadata supports reuse
after restoring a session; older results without that metadata are detected again.
These are separate views with a shared community selection and inspector.
Detection and exploration use the whole loaded network, independently of Network
table filters. All metric computation remains in **Metrics**; **View in Metrics**
selects the current partition there. Existing saved sessions remain supported.

The graph offers independent **Cosmograph 2D**, **Cosmograph 3D**, and **Sigma**
renderers. Cosmograph loads on demand from the same local bundle/version as the Network
renderer. Community graph controls share Network's renderer/dimensions pickers,
camera buttons, Export menu and canvas styling. Renderer and dimension choices
remain independent of Network; choosing Sigma hides the dimensions picker while
retaining the last Cosmograph dimension preference. Cosmograph
requires WebGL. Layouts include circular/spherical,
grid, seeded random, spectral, Fruchterman–Reingold, stress majorization,
Kamada–Kawai, multilevel force (Hu), ForceAtlas2, and concentric grouping by
community-graph degree. RELISON-viz computes their coordinates. Cosmograph 3D uses
native spatial variants where available; Kamada–Kawai, multilevel force and
ForceAtlas2 explicitly use planar layouts in 3D. Switching views preserves community selection; renderer cameras are
independent of Network and Diffusion and are retained in saved sessions.

Switching partitions, renderers or views cancels pending community renderer
initialization and removes its canvas without waiting for a stalled upload.
Cosmograph initialization has a 30-second timeout, with **Apply layout** to retry
or **Sigma** as an alternative. Obsolete loads cannot overwrite the active graph
or its status.

Graph nodes represent communities, sized by membership and coloured consistently
with Network's partition colouring (including legend overrides). **Aggregated
weights** uses RELISON-sna's `SimpleCommunityGraphGenerator`, including weighted
parallel edges. The **Complete** and **Inter-community** modes use the corresponding
RELISON-sna generators; their displayed link values are generator multiplicities
over adjacent original node pairs, not original parallel-edge counts. Directed
links retain direction. Isolated communities remain visible. **Show internal
connections** only controls loop visibility and does not change metric values.

In **Appearance → Edges → Shape**, choose **Curved** (the default) or **Straight**
for Sigma and Cosmograph 2D/3D. Edge labels and SVG/PNG exports follow the chosen
shape; internal connections remain loops. The shape is retained in saved sessions,
and changing it automatically fits the graph.

Expand **Appearance** in the Graph view to set minimum/maximum community node
radii, proportional or percentile size scaling (matching Network), and optional
node borders with a chosen colour and width. Node sizes scale by member count within
the chosen bounds; equal bounds give uniform sizes. Node and edge labels have
independent visibility, size, font and proportional-size controls. Default labels
in Cosmograph follow Network's viewport-dependent dynamic selection (up to 200),
instead of displaying every node label. Hovering always shows the community label,
even when **Show node labels** is off, in both renderers. Label colours
are white in Cosmograph and black in Sigma; single colours and node/edge colours
are also available. A shared SVG foreground keeps all labels above nodes, edges
and internal loops, including in exported images. Edge thickness can be uniform or scaled by the
absolute connection value, with configurable minimum/maximum widths. Edge colours
can use the default, a single colour, the average of the two community colours,
or the source/target community colour.
For undirected graphs, source/target colours follow the displayed endpoint order.
Internal loops use the same edge settings. Appearance updates preserve layout
coordinates and automatically Fit the graph. Cosmograph updates are serialized;
Fit runs after the latest upload, so a late update cannot restore an obsolete
camera. Selection-only refreshes retain the camera. Appearance applies to exports
and is saved in sessions. A width of zero hides links.

The searchable, sortable community table shows IDs, editable display labels,
membership counts, network shares and metrics already computed for that partition.
Selecting a community opens its inspector in either view. The inspector includes
a searchable, paginated member list with selectable attributes/node metrics, and
its incoming/outgoing or undirected community connections. Member and community
CSV exports include all matching rows across pages. Network actions highlight or
isolate the members while retaining existing Network filters. Clicking a member
opens its node details in Network and preserves the community selection.

Graph exports provide PNG and SVG of the current projected view, including labels
and a size/link legend. Cosmograph 3D exports flatten the current camera projection
to circles and curves; sphere shading is not included. Session files retain labels,
partition metadata, selected community, renderer, positions and cameras. Rerunning
the same detection configuration replaces its partition and clears that partition's
old community metric results and labels. Structural edits use the existing result
invalidation flow.

Regression checks:

```sh
mvn -pl RELISON-gui -am -Dtest=CommunityGraphTest,LayoutControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
node --test RELISON-gui/src/test/js/community-explorer.test.cjs RELISON-gui/src/test/js/community-cosmograph.test.cjs
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
- Frontend libraries, icons, fonts, map boundaries and Cosmograph's DuckDB workers/WASM are packaged locally;
  the first run works without internet access or a populated browser cache.

### Offline use and frontend dependencies

The runnable JAR and portable Windows distribution include the browser assets under
`public/vendor`. Normal Maven builds copy these resources automatically and do not
require Node.js, npm, or a frontend download step. The portable runtime includes Java.
External algorithm prerequisites (such as the Infomap executable) remain separate.

**Settings → Use online street maps** is off by default and is remembered on this
browser/device. While off, the **Web Mercator (street / offline Earth)** choice uses the bundled
Natural Earth land and country boundaries in Web Mercator projection, retaining network
nodes, links, selection, pan/zoom and PNG/SVG exports. Equal Earth and Globe work
offline directly. Enabling online street maps requests OpenStreetMap tiles;
disconnecting or a tile-load failure falls back to the local Mercator Earth map. The preference
applies independently of saved graph sessions and is shared by Network and Diffusion.
The offline background shows countries and coastlines, without street detail.

The page's content security policy restricts scripts, fonts and data connections to
local assets. Optional OpenStreetMap tile images are the only automatic external
resource allowed. Help and attribution links still open external websites when clicked.

To update the pinned dependencies (maintainers only; this step needs internet access):

```sh
cd RELISON-gui
npm ci
npm run build:vendor
node --test src/test/js/offline-assets.test.cjs src/test/js/globe-map.test.cjs src/test/js/animation-export.test.cjs
```

Check in `package.json`, `package-lock.json` and the generated `public/vendor` files.
The vendor build retains package license notices and produces a SHA-256 manifest.
It also adapts the pinned Cosmograph DuckDB loader to use local workers and WASM;
upstream loader changes cause the build to fail for review. Node/npm are needed only
when regenerating these checked-in assets. The browser still connects to the local
server using HTTP; bundling assets does not configure HTTPS.

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

Globe.gl 2.46.2 and Three.js 0.180.0 are loaded on demand from local bundles; TopoJSON
3.1.0 converts the packaged world-atlas 2.0.2 country boundaries (Natural Earth).
WebGL is required. No internet connection, imagery service or API key is needed.
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
world-atlas 2.0.2 (Natural Earth 1:110m data) load locally on demand.
The street-map choice uses the offline Earth background unless online tiles are enabled in Settings.
Both views retain selection and
appearance settings. PNG/SVG exports capture the current map viewport, including
visible nodes, labels, edges, recommendation/propagation overlays and attribution.
Mercator SVG files embed loaded OpenStreetMap raster tiles with vector graph
elements; Equal Earth SVG files keep the background and graph as vectors.
Exports are standalone and do not fetch additional tiles. Mercator export waits
for visible tiles and reports missing tiles or CORS errors rather than omitting
the background. Diffusion map views have their own PNG/SVG export buttons.

### Diffusion analysis workspace

Diffusion charts share an Analysis workspace with Overview, Metrics, Nodes, Pieces, Features, Distributions and Compare tabs. Select or hide runs independently of deletion; run colours remain stable. The latest run drives graph playback. Older runs retain their disk-backed simulation files until deleted, cleared, input changes or the graph session is removed. Saved `.relison` sessions retain metric curves and configuration, but not simulation files; imported runs are labelled Metrics only.

Line charts offer points, lines, or both, independent numeric log axes and CSV/image exports. Analysis titles are arranged across the top; each tab except Overview has a left rail for chart types, using the same panel styling as metrics. Overview lists every run in a nodes/edges-style table with selection checkboxes, editable chart colours, preset/custom mechanism settings, stop condition, iteration count, metrics, network and deletion. Deleting the playback run activates the most recent remaining simulation. Log scales omit nonpositive points only in the display. Metrics can be compared at matching iterations, and runs can be compared against a chosen baseline over their shared iteration range, using paired values, absolute differences or percentage changes (undefined for a zero baseline).

Node and piece rankings count unique memberships over the whole simulation. Numeric feature distributions support weighted histograms with an explicit bin width, cumulative shares and exact numeric value counts; categorical features use counts/shares and mixed features use complete heatmaps. Detailed analysis is queued in Processes, cancellable, and cached by graph/run/configuration in the current browser session. Display changes reuse cached data.

Node and information-piece timelines show all four categories for a single base run. Single-run feature views select a category and label curves by feature value. Multiple-run views select one category, and feature comparisons also select one exact feature value shared across runs; legends contain run/protocol names only. Node and piece rankings offer Show all, Top k and Bottom k without repeating the computation.
