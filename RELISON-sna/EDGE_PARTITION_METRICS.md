# Edge partition metrics

`EdgePartitionMetric<U>` computes one value for a graph and an edge partition.
`IndividualEdgePartitionMetric<U>` mirrors the individual community metric API:
one group's value, a map of all group values, and their arithmetic mean.
Implementations are in `sna.metrics.edgepartition.graph` and
`sna.metrics.edgepartition.indiv`.

## Individual metrics

| Class | Definition |
| --- | --- |
| `Size` | Number of edges assigned to the group. |
| `EdgeShare` | Group edge count / total graph edge count. |
| `NodeCoverage` | Distinct endpoints covered by the group. Optional `EdgeOrientation.OUT` or `IN` counts distinct sources or targets for directed graphs; undirected graphs always count all endpoints. |
| `TotalWeight` | Sum of the group's signed, finite edge weights. Unweighted edges contribute one. |
| `SharedNodes` | Covered nodes incident to edges assigned to at least one other group. |
| `SharedNodeRatio` | Shared nodes / covered nodes. |
| `GroupDegree` | Number of distinct other groups sharing an endpoint with this group. |
| `Components` | Number of weakly connected components using the group's edges. |
| `LargestComponentShare` | Covered nodes in the largest weak component / covered nodes. |
| `IndividualEdgePartitionGraphMetric` | Applies a supplied graph metric to the subgraph containing only the group's edges and their endpoints. |

Parallel edges count separately in edge counts and weights. Endpoint counts count
each node once, including nodes incident to self-loops. Connectivity ignores
direction and multiplicity but retains all covered nodes. Empty groups have zero
for the basic individual metrics, including shares and connectivity.

## Global metrics

| Class | Definition |
| --- | --- |
| `NumberOfGroups` | Declared groups, including empty groups. Pass `true` to count only nonempty groups. |
| `AssignmentCoverage` | Assigned edges / all graph edges. |
| `GroupSizeGini` | Normalized Gini inequality across group edge counts, including empty groups. |
| `GroupWeightGini` | Normalized Gini inequality across group weight sums, including empty groups. Negative or non-finite assigned edge weights are rejected. |
| `GroupSizeEntropy` | Shannon entropy in bits, with probabilities given by group size / assigned edge count. |
| `NodeMixingRatio` | Nonisolated graph nodes incident to two or more groups / all nonisolated graph nodes. |
| `MeanNodeParticipation` | Average number of assigned groups per nonisolated graph node. |

The two Gini metrics use RELISON's normalized Gini coefficient: zero means equal
group mass and one means maximal inequality. They return zero for zero total mass
or fewer than two groups. Entropy and assignment coverage return zero when their
denominator is zero. Mixing and participation include nonisolated nodes whose
edges are all unassigned, with zero participation; isolates are excluded.

## Restricted graph metrics

`EdgePartitionSubgraphGenerator` preserves direction, weights, edge types,
parallel edges, loops, and available attributes. It excludes edges from other
groups even when both endpoints occur in the selected group. The graph metric
supplier is called for each group so graph-specific caches are never shared.
Existing graph metrics retain their usual interpretation and graph-type support.
Undefined empty-graph values are represented as NaN by the library.

Edge references must refer to edges in the supplied graph. Count-only metrics
assume the partition belongs to the current graph, just as community metrics do.
Metrics that inspect edges validate membership against the live graph and resolve
stable multigraph ids to their current parallel-edge indices.

## Computation cost

Size and edge share read stored group sizes and the graph's edge counter: O(1)
for one group and O(number of groups) for all groups. Declared group count is O(1).
Nonempty group count, assignment coverage and size entropy use only group counts;
size Gini additionally sorts these counts. None traverses the network's edges or
builds node memberships.

Other metrics request only the snapshot detail they use: edge lists for weights
and intra-partition graph metrics, endpoint sets for coverage and connectivity,
and node-to-group memberships for overlap metrics. Weight Gini reads each assigned
edge weight once. Intra-partition metrics retain the underlying graph metric's
computation cost.

## GUI

Detect partitions in **Edge partitions**, then compute their metrics in
**Metrics → Edge partition metrics**. Multiple partitions can be retained during
the loaded-graph session. Global values, finite per-group averages, and individual
values are shown in the Metrics tab and can be exported to CSV; computed group
values are also columns in the edge-group table and properties in its inspector.
Graph metrics are available with the prefix **Intra-partition** and retain their
parameter controls. Undefined results appear as a dash and are excluded from GUI
averages. Structural graph edits invalidate retained partitions and their results.
The inspector reuses group-edge and endpoint indexes. Metric computation updates
the metric chart once and refreshes the inspector when its tab is opened.

Nonnumeric edge attributes are available directly in the **Edge partitions** tab's
exploration selector under **Edge attributes**, and are grouped lazily when selected.
Network and Metrics edge-partition selectors include only explicitly detected
partitions, excluding these direct attribute groups. Use **By edge attribute** in
Network and **Edge attribute metrics** in Metrics for attributes; the explorer's
**View in Metrics** button opens attribute metrics when an attribute is selected.
The detection controls offer **Numeric edge attribute bins**
with a positive bin width. `EdgeAttributePartitionDetector(attribute, binWidth)`
provides the same binning in RELISON-sna: intervals are anchored at zero and are
lower-inclusive, upper-exclusive; missing values share an Unknown group. Attribute
edits invalidate cached groups of that attribute. Renaming a string/categorical
group updates its edge values and rebuilds its partition, merging equal categories.
