package es.uam.eps.ir.socialranksys.links.data.letor.normalization;

import org.ranksys.core.util.tuples.Tuple2id;

public interface FastIndividualNormalizer
{
    void add(Tuple2id tuple);
    double getScore(int iidx);

}
