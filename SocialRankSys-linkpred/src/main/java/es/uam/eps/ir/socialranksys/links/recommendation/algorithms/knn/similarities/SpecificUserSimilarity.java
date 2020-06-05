package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.knn.similarities;

import es.uam.eps.ir.ranksys.fast.index.FastUserIndex;
import es.uam.eps.ir.ranksys.nn.sim.Similarity;
import es.uam.eps.ir.ranksys.nn.user.sim.UserSimilarity;

public class SpecificUserSimilarity<U> extends UserSimilarity<U> {
    public SpecificUserSimilarity(FastUserIndex<U> uIndex, Similarity sim)
    {
        super(uIndex, sim);
    }
}
