package es.uam.eps.ir.relison.sna.metrics.communities.indiv;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;

/**
 * Fraction of the community's incident degree (or strength) contributed by internal edges.
 * This is a pooled ratio, not a mean of individual node ratios.
 * For undirected graphs: 2*internalMass/volume(C), with loops contributing two incidences.
 * For directed OUT/IN: internalMass/outVolume(C) or internalMass/inVolume(C).
 * Directed UND uses 2*internalMass/(outVolume(C)+inVolume(C)).
 * Zero-volume communities yield NaN. Weighted mode requires nonnegative finite weights;
 * the unweighted mode ignores weights. This is not generally 1 minus symmetric conductance.
 */
public class InternalVolumeFraction<U> extends AbstractCommunityBoundaryMetric<U>
{
    public InternalVolumeFraction() { this(EdgeOrientation.UND, false); }
    public InternalVolumeFraction(EdgeOrientation orientation, boolean weighted) { super(orientation, weighted); }

    @Override
    protected double evaluate(CommunityBoundaryStatistics<U> s, int group)
    {
        double volume = s.volume(group, orientation);
        double factor = !s.directed || orientation == EdgeOrientation.UND ? 2.0 : 1.0;
        return volume == 0.0 ? Double.NaN : factor * s.internal[group] / volume;
    }
}
