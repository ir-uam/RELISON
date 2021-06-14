/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.global.local.communities;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.multigraph.DirectedMultiGraph;
import es.uam.eps.ir.relison.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.relison.utils.indexes.GiniIndex;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Implementation of a reranking strategy for balancing the distribution of the degrees of the communities
 * in a community graph. It only considers links between communities.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 */
public class InterCommunityDegreeGiniComplement<U> extends InterCommunityReranker<U>
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
     * @param lambda        trade-off between the recommendation score and the novelty/diversity value.
     * @param cutoff        number of elements to take.
     * @param norm          the normalization strategy.
     * @param graph         the original graph.
     * @param communities   the relation between users and communities.
     * @param orientation   the neighborhood selection for computing the degree.
     */
    public InterCommunityDegreeGiniComplement(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities, EdgeOrientation orientation)
    {
        super(lambda, cutoff, norm, graph, communities);
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
        else if(graph.isDirected() || !graph.containsEdge(u, recomm))
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
        else // If the edge already exists, nothing is modified.
        {
            return gini.compute(degrees.values().stream().map(integer -> integer + 0.0), true, communities.getNumCommunities(), this.sum);
        }
    }

    @Override
    protected void innerUpdate(U user, Tuple2od<U> iv)
    {
        U recomm = iv.v1;
        Integer userComm = this.communities.getCommunity(user);
        Integer recommComm = this.communities.getCommunity(recomm);

        // We do only update when the communities are different (otherwise, the link is not counted).
        if(userComm.equals(recommComm)) return;

        if(communityGraph.isDirected() && !orientation.equals(EdgeOrientation.UND))
        {
            if(orientation.equals(EdgeOrientation.IN))
            {
                degrees.put(recommComm, degrees.get(recommComm) + 1);
            }
            else
            {
                degrees.put(userComm, degrees.get(userComm) + 1);
            }
            ++sum;
        }
        else if(communityGraph.isDirected() || !this.graph.containsEdge(user, recomm))
        {
            degrees.put(recommComm, degrees.get(recommComm)+1);
            degrees.put(userComm, degrees.get(userComm) + 1);
            sum += 2.0;
        }
    }

    @Override
    protected void update(Recommendation<U, U> reranked)
    {
    }
}
