package es.uam.eps.ir.relison.sna.metrics.communities.indiv;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;

/**
 * Standard Newman modularity contribution with resolution gamma.
 * Undirected: L_C/m - gamma*(volume(C)/(2m))^2.
 * Directed: L_C/m - gamma*(outVolume(C)/m)*(inVolume(C)/m).
 * Here m is physical edge count or total edge weight, and L_C is internal edge
 * mass, counting each parallel edge and loop once. Weighted mode rejects negative
 * or non-finite weights. Requires all graph nodes to be assigned. Edgeless or
 * zero-total-weight graphs yield zero, including for empty communities.
 *
 * <p>References:
 * Clauset, Newman and Moore, Phys. Rev. E 70, 066111 (2004),
 * <a href="https://doi.org/10.1103/PhysRevE.70.066111">doi:10.1103/PhysRevE.70.066111</a>;
 * Leicht and Newman, Phys. Rev. Lett. 100, 118703 (2008), directed null model,
 * <a href="https://doi.org/10.1103/PhysRevLett.100.118703">doi:10.1103/PhysRevLett.100.118703</a>;
 * Reichardt and Bornholdt, Phys. Rev. E 74, 016110 (2006), resolution parameter,
 * <a href="https://doi.org/10.1103/PhysRevE.74.016110">doi:10.1103/PhysRevE.74.016110</a>.
 * The community-sum formulas are also documented in
 * <a href="https://networkx.org/documentation/stable/reference/algorithms/generated/networkx.algorithms.community.quality.modularity.html">NetworkX modularity</a>.</p>
 *
 * <p>The sum is standard modularity; it does not decompose RELISON's existing
 * differently normalized global communities.graph.Modularity score.</p>
 */
public class ModularityContribution<U> extends AbstractCommunityBoundaryMetric<U>
{
    private final double resolution;

    public ModularityContribution() { this(false, 1.0); }

    public ModularityContribution(boolean weighted, double resolution)
    {
        super(EdgeOrientation.UND, weighted);
        if (!Double.isFinite(resolution) || resolution < 0.0)
            throw new IllegalArgumentException("Modularity resolution must be finite and nonnegative.");
        this.resolution = resolution;
    }

    @Override
    protected double evaluate(CommunityBoundaryStatistics<U> s, int group)
    {
        if (!s.complete) throw new IllegalArgumentException("Modularity contribution requires a complete node partition.");
        if (s.totalMass == 0.0) return 0.0;
        double expected;
        if (s.directed) expected = (s.outVolume[group] / s.totalMass) * (s.inVolume[group] / s.totalMass);
        else
        {
            double fraction = s.outVolume[group] / (2.0 * s.totalMass);
            expected = fraction * fraction;
        }
        return s.internal[group] / s.totalMass - resolution * expected;
    }
}
