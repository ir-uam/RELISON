package es.uam.eps.ir.socialranksys.links.recommendation.updateable.nn.sim;

import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.knn.similarities.GraphSimilarity;

public abstract class UpdateableGraphSimilarity extends GraphSimilarity implements UpdateableSimilarity
{
    /**
     * Constructor.
     *
     * @param graph the social network graph.
     */
    public UpdateableGraphSimilarity(FastGraph<?> graph)
    {
        super(graph);
    }
}
