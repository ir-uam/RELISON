package es.uam.eps.ir.relison.sna.metrics.communities.indiv;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;

/**
 * Symmetric undirected conductance: cut(C,T)/min(volume(C),volume(T)), T=V minus C.
 * Weighted mode replaces edge counts with nonnegative finite weights; loops contribute
 * twice to undirected volume and never to the cut. Zero denominator yields NaN.
 *
 * <p>Reference definition:
 * <a href="https://networkx.org/documentation/stable/reference/algorithms/generated/networkx.algorithms.cuts.conductance.html">NetworkX conductance documentation</a>.
 * See also Fan Chung, Spectral Graph Theory, AMS (1997), ISBN 0-8218-0315-8.</p>
 *
 * <p>Explicit directed extensions used here (not an assertion that the undirected
 * reference defines these variants): OUT=cut(C,T)/min(outVolume(C),inVolume(T));
 * IN=cut(T,C)/min(inVolume(C),outVolume(T)). UND uses the combined cut and combined
 * in/out volumes on each side, equivalent to symmetrizing directed incidences.
 * These conventions keep nonnegative-weight results in [0,1].</p>
 */
public class Conductance<U> extends AbstractCommunityBoundaryMetric<U>
{
    public Conductance() { this(EdgeOrientation.UND, false); }
    public Conductance(EdgeOrientation orientation, boolean weighted) { super(orientation, weighted); }

    @Override
    protected double evaluate(CommunityBoundaryStatistics<U> s, int group)
    {
        double complement;
        if (!s.directed) complement = 2.0 * s.totalMass - s.outVolume[group];
        else if (orientation == EdgeOrientation.OUT) complement = s.totalMass - s.inVolume[group];
        else if (orientation == EdgeOrientation.IN) complement = s.totalMass - s.outVolume[group];
        else complement = 2.0 * s.totalMass - s.outVolume[group] - s.inVolume[group];
        double denominator = Math.min(s.volume(group, orientation), Math.max(0.0, complement));
        return denominator == 0.0 ? Double.NaN : s.cut(group, orientation) / denominator;
    }
}
