package es.uam.eps.ir.relison.sna.metrics.communities.indiv;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;

/**
 * One-sided expansion: boundary edge mass divided by |C|; NaN for an empty community.
 * OUT/IN select outgoing/incoming boundary mass in directed graphs; UND sums both.
 * An undirected crossing edge counts once per incident community. Weighted mode
 * uses nonnegative finite weights and unweighted mode counts parallel edges separately.
 *
 * <p>Citation: J. Yang and J. Leskovec, "Defining and Evaluating Network Communities
 * based on Ground-truth", ICDM (2012), Section II, expansion c_S/n_S:
 * <a href="https://arxiv.org/abs/1205.6233">arXiv:1205.6233</a>.
 * This uses community size, not min(|C|,|V minus C|); the weighted and directed
 * variants extend that stated undirected, unweighted definition.</p>
 */
public class EdgeExpansion<U> extends AbstractCommunityBoundaryMetric<U>
{
    public EdgeExpansion() { this(EdgeOrientation.UND, false); }
    public EdgeExpansion(EdgeOrientation orientation, boolean weighted) { super(orientation, weighted); }

    @Override
    protected double evaluate(CommunityBoundaryStatistics<U> s, int group)
    {
        return s.members[group] == 0 ? Double.NaN : s.cut(group, orientation) / s.members[group];
    }
}
