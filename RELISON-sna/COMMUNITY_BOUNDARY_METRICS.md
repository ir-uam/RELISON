# Individual community boundary metrics

These implement `IndividualCommunityMetric<U>` in
`sna.metrics.communities.indiv` and are available through
`IndividualCommunityMetricSelector` in RELISON-examples. In the GUI, use
**Metrics → Community metrics → Per-community metric**, or the individual
attribute-based metric controls. Computed values also appear in the Communities
table and inspector. Numeric attribute-bin communities use the same metrics.

| Class | Definition |
| --- | --- |
| `BoundaryNodes` | Distinct members incident to at least one edge crossing the community boundary. |
| `BoundaryNodeRatio` | Boundary-node count divided by the community size; zero for an empty community. |
| `NeighboringCommunities` | Number of distinct other community IDs reached across the boundary, excluding unassigned nodes. This differs from the existing community degree, which counts inter-community edges. |
| `InternalVolumeFraction` | Internal degree incidences divided by all degree incidences of the members; weighted mode uses strengths. |
| `Conductance` | Undirected boundary mass divided by the smaller of community and complement volumes. |
| `EdgeExpansion` | Boundary mass divided by this community's size. |
| `ModularityContribution` | Standard Newman modularity contribution, optionally weighted and with resolution gamma. |

## Direction and weights

Boundary metrics accept OUT, IN or UND. In directed graphs, these select outgoing
connections, incoming connections, or both. Boundary-node and neighboring-community
counts take unions for UND so a node or community is counted once. Undirected
graphs ignore the orientation. MUTUAL is not supported.

The last four classes accept a weighted flag, defaulting to false. Weighted mode
requires finite nonnegative weights and uses each parallel edge's own weight.
Unweighted mode counts each physical edge, including parallel edges, once.
Boundary-node and neighboring-community metrics are structural counts: zero-weight
edges still establish a boundary connection.

For internal volume, an undirected internal edge contributes two incidences,
including a loop. A directed internal edge contributes one in OUT/IN mode or two
in UND mode. This is a ratio of aggregated volumes, not an average of node ratios.

Conductance uses T = V minus C. Its directed variants are explicitly defined as:

- OUT: cut(C,T) / min(outVolume(C), inVolume(T)).
- IN: cut(T,C) / min(inVolume(C), outVolume(T)).
- UND: combined crossing mass / min(combined in/out volume(C), combined in/out volume(T)).

These directed variants are implementation conventions extending the symmetric
undirected definition. They are documented explicitly in the class Javadoc.
Expansion uses this community's size as its denominator, rather than the smaller
of the two sides' sizes.

Internal-volume fraction and conductance return NaN for zero-volume denominators;
expansion returns NaN for an empty community. In the GUI, undefined values become
JSON nulls, display as a dash, are excluded from finite-value averages, and export
as empty CSV cells. No undefined result is converted to zero.

## Modularity contribution

Let m be total graph edge mass, L_C internal edge mass, and gamma the resolution.
Undirected contribution is L_C/m - gamma*(volume(C)/(2m))^2.
Directed contribution is L_C/m - gamma*(outVolume(C)/m)*(inVolume(C)/m).
Each loop contributes once to L_C; undirected degree volume counts it twice.
A complete node partition is required. Zero graph mass yields zero.

These contributions sum to standard Newman modularity. They do **not** sum to
RELISON's existing differently normalized global `communities.graph.Modularity`
score. The GUI identifies this metric as **Modularity contribution (Newman)**.

## Sources

The relevant formulas and directed conventions are also recorded in the Java
comments, together with these citations:

- Conductance: [NetworkX reference definition](https://networkx.org/documentation/stable/reference/algorithms/generated/networkx.algorithms.cuts.conductance.html); Fan Chung, *Spectral Graph Theory*, AMS (1997), ISBN 0-8218-0315-8.
- Community-size-normalized expansion: Yang and Leskovec, *Defining and Evaluating Network Communities based on Ground-truth*, ICDM (2012), Section II: [arXiv:1205.6233](https://arxiv.org/abs/1205.6233).
- Modularity decomposition: Clauset, Newman and Moore, *Finding community structure in very large networks*, Phys. Rev. E 70, 066111 (2004): [DOI](https://doi.org/10.1103/PhysRevE.70.066111).
- Directed modularity: Leicht and Newman, *Community Structure in Directed Networks*, Phys. Rev. Lett. 100, 118703 (2008): [DOI](https://doi.org/10.1103/PhysRevLett.100.118703).
- Resolution: Reichardt and Bornholdt, *Statistical Mechanics of Community Detection*, Phys. Rev. E 74, 016110 (2006): [DOI](https://doi.org/10.1103/PhysRevE.74.016110).
