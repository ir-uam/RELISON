package es.uam.eps.ir.socialranksys.links.data.letor.normalization;

import org.ranksys.core.util.tuples.Tuple2od;

public interface IndividualNormalizer<I>
{
    void add(Tuple2od<I> tuple);
    double getScore(I item);

}
