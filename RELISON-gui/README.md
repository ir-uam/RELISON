# RELISON-gui

A Gephi-like graphical interface for RELISON. It runs a small local web server (Javalin) that wraps RELISON's
graph IO and social-network-analysis metrics, and serves a [sigma.js](https://www.sigmajs.org/) frontend for
interactive visualization.

## Features
- **Load** a tab-separated edge list (`source⇥target[⇥weight]`) as a directed/undirected, weighted, multigraph
  and/or self-looping network.
- **Visualize** with sigma.js: pan/zoom, hover, ForceAtlas2 layout (start/stop), circular reset.
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
- Distance-based metrics (closeness, betweenness, eccentricity, ASL, diameter, …) trigger an all-pairs distance
  computation that is cached and shared per session; the first such metric on a large network may take a while.
- `Infomap` requires an external binary and the spectral algorithms need extra numeric libraries; they are listed
  in the UI but report a clear error if their prerequisites are missing.
- The frontend currently loads sigma.js / graphology from a CDN, so the first run needs internet access.
