package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.knn.similarities;

import es.uam.eps.ir.ranksys.fast.index.FastItemIndex;
import es.uam.eps.ir.ranksys.nn.item.sim.ItemSimilarity;
import es.uam.eps.ir.ranksys.nn.sim.Similarity;

public class SpecificItemSimilarity<I> extends ItemSimilarity<I>
{
    public SpecificItemSimilarity(FastItemIndex<I> iIndex, Similarity sim)
    {
        super(iIndex, sim);
    }
}
