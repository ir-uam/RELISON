/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.communities;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.graph.CommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.community.graph.InterCommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.multigraph.MultiGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.GlobalLambdaReranker;

/**
 * Reranker that uses community metrics of the user graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public abstract class GlobalCommunityReranker<U> extends GlobalLambdaReranker<U,U>
{

    /**
     * Communities of the graph.
     */
    protected final Communities<U> communities;
    /**
     * The community graph.
     */
    protected final MultiGraph<Integer> communityGraph;
    /**
     * The original graph.
     */
    protected final Graph<U> graph;
    
    /**
     * Constructor
     * @param lambda A trait-off between the original score and the metric value
     * @param cutoff The number of items to rerank
     * @param norm true if the original score and the metric value require optimization
     * @param graph The user graph
     * @param communities A relation between communities and users.
     */
    public GlobalCommunityReranker(double lambda, int cutoff, boolean norm, Graph<U> graph, Communities<U> communities) 
    {
        super(lambda, cutoff, norm);
        this.communities = communities;
        this.graph = graph;
        CommunityGraphGenerator<U> cgg = new InterCommunityGraphGenerator<>();
        
        communityGraph = cgg.generate(graph, communities);
        
    }
    
    

    
}
