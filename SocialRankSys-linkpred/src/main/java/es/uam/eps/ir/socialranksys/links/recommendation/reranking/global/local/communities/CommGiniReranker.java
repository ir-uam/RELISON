/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.local.communities;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.multigraph.DirectedMultiGraph;
import es.uam.eps.ir.socialranksys.utils.indexes.GiniIndex;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.Map;

/**
 * Reranks a recommendation by improving the Gini Index of the degree of the 
 * different communities in a community graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class CommGiniReranker<U> extends CommunityReranker<U>
{
    /**
     * The orientation which indicates which community degree to use
     */
    protected final EdgeOrientation orientation;
    /**
     * The community degrees
     */
    protected Map<Integer,Integer> degrees;
    /**
     * The total number of edges between communities.
     */
    protected double sum;
    /**
     * Constructor.
     * @param lambda Establishes the trait-off between the value of the Gini Index and the original score
     * @param cutoff The number of recommended items for each user.
     * @param norm true if the score and the Gini Index value have to be normalized.
     * @param graph The user graph.
     * @param communities A relation between communities and users in the graph.
     * @param orientation The orientation of the community degree to take.
     * @param rank Indicates if the normalization is by ranking (true) or by score (false)
     */
    public CommGiniReranker(double lambda, int cutoff, boolean norm, Graph<U> graph, Communities<U> communities, EdgeOrientation orientation, boolean rank)
    {
        super(lambda, cutoff, norm, rank, graph, communities);
        this.orientation = orientation;
        this.degrees = new Int2IntOpenHashMap();
        
        if(communityGraph.isDirected() && !orientation.equals(EdgeOrientation.UND))
        {
            DirectedMultiGraph<Integer> dirCommGraph = (DirectedMultiGraph<Integer>) communityGraph;
            if(orientation.equals(EdgeOrientation.IN))
            {
                communities.getCommunities().forEach((comm)-> 
                {
                    degrees.put(comm, dirCommGraph.inDegree(comm));
                    sum += dirCommGraph.inDegree(comm);        
                });

            }
            else // if(orientation.equals(EdgeOrientation.OUT))
            {
                communities.getCommunities().forEach((comm)-> 
                {
                    degrees.put(comm, dirCommGraph.outDegree(comm));
                    sum += dirCommGraph.outDegree(comm);
                });
            }
        }
        else //if the orientation is undirected or the graph is not directed, then, take the degree.
        {
            communities.getCommunities().forEach((comm)-> 
            {
                degrees.put(comm, communityGraph.degree(comm));
                sum += communityGraph.degree(comm);
            });
        }
    }
    
    @Override
    protected double nov(U u, Tuple2od<U> iv)
    {
        U recomm = iv.v1;
        int userComm = communities.getCommunity(u);
        int recommComm = communities.getCommunity(recomm);

        GiniIndex gini = new GiniIndex();
        // If no link is created between communities, the Gini metric remains
        // the same.
        if(userComm == recommComm)
        {
            return gini.compute(degrees.values().stream().map(integer -> integer + 0.0), true, communities.getNumCommunities(), this.sum);
        }


        if(communityGraph.isDirected() && !orientation.equals(EdgeOrientation.UND))
        {
            if(orientation.equals(EdgeOrientation.IN)) // Case of the in-degree. Add 1 to the indegree of the new item.
            {
                return gini.compute(degrees.entrySet().stream().map(entry -> {

                   if(entry.getKey().equals(recommComm))
                       return entry.getValue() + 1.0;
                   else
                       return entry.getValue() + 0.0;
                }), true, communities.getNumCommunities(), this.sum + 1.0);

            }
            else 
            {
                return gini.compute(degrees.entrySet().stream().map(entry -> {
                    if(entry.getKey().equals(userComm))
                        return entry.getValue() + 1.0;
                    else
                        return entry.getValue() + 0.0;
                }), true, communities.getNumCommunities(), this.sum + 1.0);
            }
        }
        else
        {
            return gini.compute(degrees.entrySet().stream().map(entry ->
            {
                if(entry.getKey().equals(userComm))
                    return entry.getValue() + 1.0;
                else if(entry.getKey().equals(recommComm))
                    return entry.getValue() + 1.0;
                else
                    return entry.getValue() + 0.0;
            }), true, communities.getNumCommunities(), this.sum + 2.0);
        }
    }

    @Override
    protected void update(U user, Tuple2od<U> iv)
    {
        U recomm = iv.v1;
        Integer userComm = this.communities.getCommunity(user);
        Integer recommComm = this.communities.getCommunity(recomm);
        if(communityGraph.isDirected() && !orientation.equals(EdgeOrientation.UND))
        {
            if(orientation.equals(EdgeOrientation.IN))
            {
                degrees.put(recommComm, degrees.get(recommComm)+1);
            }
            else
            {
                degrees.put(userComm, degrees.get(userComm) + 1);
            }
            ++sum;
        }
        else
        {
            degrees.put(recommComm, degrees.get(recommComm)+1);
            degrees.put(userComm, degrees.get(userComm) + 1);
        }
    }

    @Override
    protected void update(Recommendation<U, U> reranked)
    {
    }
}
