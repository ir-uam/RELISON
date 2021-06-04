/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.global.swap.comm.gini.degree;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.relison.community.Communities;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.multigraph.DirectedMultiGraph;
import es.uam.eps.ir.relison.links.recommendation.reranking.global.swap.comm.CommunityReranker;
import es.uam.eps.ir.relison.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import es.uam.eps.ir.relison.utils.indexes.GiniIndex;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Swap reranker for promoting the balance in the degree distribution for the different
 * communities.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 */
public abstract class AbstractCommunityDegreeGiniComplement<U> extends CommunityReranker<U>
{
    /**
     * Indicates if we use the complete community graph, or only inter-community links.
     */
    private final boolean complete;
    /**
     * Indicates if the reranker promotes the links.
     */
    private final boolean outer;
    /**
     * Neighborhood selection
     */
    protected final EdgeOrientation orientation;
    /**
     * Total sum for the Gini coefficient.
     */
    private double sum;
    /**
     * Current values for each community
     */
    protected final Map<Integer, Double> degrees;
    
    /**
     * Constructor.
     * @param lambda        establishes the trade-off between the value of the Gini Index and the original score
     * @param cutoff        the maximum number of contacts in the definitive rankings.
     * @param norm          the normalization scheme.
     * @param graph         the original graph.
     * @param communities   the relation between communities and users in the graph.
     * @param orientation   the orientation of the community degree to take.
     * @param complete      true if we use intra-community links, false if not.
     * @param selfloops     true if selfloops are allowed, false if they are not.
     * @param outer         true if we want to force links to go outside communities.
     */
    public AbstractCommunityDegreeGiniComplement(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities, boolean selfloops, boolean complete, boolean outer, EdgeOrientation orientation)
    {
        super(lambda, cutoff, norm, graph, communities, selfloops);
        this.complete = complete;
        this.outer = outer;
        this.orientation = orientation;
        this.degrees = new Int2DoubleOpenHashMap();
    }
    
    @Override
    protected void update(Recommendation<U, U> reranked)
    {
    }

    @Override
    protected double novAddDelete(U u, Tuple2od<U> newItem, Tuple2od<U> oldItem)
    {
        U recomm = newItem.v1;
        U del = oldItem.v1;
        Integer userComm = communities.getCommunity(u);
        Integer recommComm = communities.getCommunity(recomm);
        Integer delComm = communities.getCommunity(del);
        GiniIndex gini = new GiniIndex();
        
        if(this.outer && userComm.equals(recommComm))
        {
            return -1.0;
        }
        
        Pair<Double> addIncr = this.computeAddValue(userComm, recommComm);
        Pair<Double> delIncr = this.computeDelValue(userComm, delComm);
        
        double userCommIncr = addIncr.v1() - delIncr.v1();
        double recommCommIncr = addIncr.v2();
        double delCommIncr = delIncr.v2();
        
        double sumAux = userCommIncr + recommCommIncr - delCommIncr;
        return 1.0 - gini.compute(degrees.entrySet().stream().map(entry -> 
        {
            double value = entry.getValue();
            
            if(entry.getKey().equals(userComm))
            {
                value += userCommIncr;
            }
            
            if(entry.getKey().equals(recommComm))
            {
                value += recommCommIncr;
            }
            
            if(entry.getKey().equals(delComm))
            {
                value -= delCommIncr;
            }
            return value;
        }), true, communities.getNumCommunities(), this.sum + sumAux);
    }
    
    @Override
    protected double novAdd(U u, Tuple2od<U> newItem, Tuple2od<U> oldItem)
    {
        U recomm = newItem.v1;
        Integer userComm = communities.getCommunity(u);
        Integer recommComm = communities.getCommunity(recomm);
        GiniIndex gini = new GiniIndex();
        
        if(this.outer && userComm.equals(recommComm))
        {
            return -1.0;
        }
        
        Pair<Double> increment = this.computeAddValue(userComm, recommComm);
        double userCommIncr = increment.v1();
        double recommCommIncr = increment.v2();
        
        return 1.0 - gini.compute(degrees.entrySet().stream().map(entry -> 
        {
            double value = entry.getValue();
            if(entry.getKey().equals(userComm))
            {
                value += userCommIncr;
            }
            else if(entry.getKey().equals(recommComm))
            {
                value += recommCommIncr;
            }
            return value;
        }), true, communities.getNumCommunities(), this.sum + userCommIncr + recommCommIncr);
    }

    @Override
    protected double novDelete(U u, Tuple2od<U> newItem, Tuple2od<U> oldItem)
    {
        U del = oldItem.v1;
        Integer userComm = communities.getCommunity(u);
        Integer delComm = communities.getCommunity(del);
        GiniIndex gini = new GiniIndex();
        
        
        Pair<Double> increment = this.computeDelValue(userComm, delComm);
        double userCommIncr = increment.v1();
        double delCommIncr = increment.v2();
        
        return 1.0 - gini.compute(degrees.entrySet().stream().map(entry -> 
        {
            double value = entry.getValue();
            if(entry.getKey().equals(userComm))
            {
                value -= userCommIncr;
            }
            else if(entry.getKey().equals(delComm))
            {
                value -= delCommIncr;
            }
            return value;
        }), true, communities.getNumCommunities(), this.sum - userCommIncr - delCommIncr);
    }
    
    @Override
    protected void innerUpdate(U user, Tuple2od<U> updated, Tuple2od<U> old)
    {
        U recomm = updated.v1;
        U del = old.v1;
        
        if(this.graph.isDirected())
        {
            this.innerUpdateAddDelete(user, updated, old);
        }
        else if(recs.get(del).contains(user) && recs.get(recomm).contains(user))
        {
            // It is already updated.
        }
        else if(recs.get(del).contains(user))
        {
            this.innerUpdateAdd(user, updated);
        }
        else if(recs.get(recomm).contains(user))
        {
            this.innerUpdateDel(user, old);
        }
        else
        {
            this.innerUpdateAddDelete(user, updated, old);
        }      
    }
    
    /**
     * Updates the reranker after adding and deleting an edge in the graph.
     * @param user      the user affected by the reranking.
     * @param updated   the new user in the recommendation.
     * @param old       the old user in the recommendation.
     */
    private void innerUpdateAddDelete(U user, Tuple2od<U> updated, Tuple2od<U> old)
    {
        U recomm = updated.v1;
        U del = old.v1;
        Integer userComm = this.communities.getCommunity(user);
        Integer recommComm = this.communities.getCommunity(recomm);
        Integer delComm = this.communities.getCommunity(del);
        
        Pair<Double> addVal = this.computeAddValue(userComm, recommComm);
        Pair<Double> delVal = this.computeDelValue(userComm, delComm);
        double userCommIncr = addVal.v1() - delVal.v1();
        double recommCommIncr = addVal.v2();
        double delCommIncr = delVal.v2();

        degrees.put(userComm, degrees.get(userComm) + userCommIncr);
        degrees.put(recommComm, degrees.get(recommComm) + recommCommIncr);
        degrees.put(delComm, degrees.get(delComm) - delCommIncr);
        this.sum += (userCommIncr + recommCommIncr - delCommIncr);
        
        GiniIndex gini = new GiniIndex();
        this.globalvalue = 1.0 - gini.compute(degrees.values().stream().map(aDouble -> aDouble + 0.0), true, communities.getNumCommunities(), this.sum);
    }
    
    /**
     * Updates the reranker after adding a new edge in the graph (undirected case).
     * @param user the user affected by the reranking.
     * @param updated the new edge in the graph.
     */
    private void innerUpdateAdd(U user, Tuple2od<U> updated)
    {
        U recomm = updated.v1;
        Integer userComm = this.communities.getCommunity(user);
        Integer recommComm = this.communities.getCommunity(recomm);
        
        Pair<Double> addVal = this.computeAddValue(userComm, recommComm);
        double userCommIncr = addVal.v1();
        double recommCommIncr = addVal.v2();
        if(!userComm.equals(recommComm))
        {
            degrees.put(userComm, degrees.get(userComm) + userCommIncr);
            degrees.put(recommComm, degrees.get(recommComm) + recommCommIncr);
            this.sum += userCommIncr + recommCommIncr;
        }
        
        GiniIndex gini = new GiniIndex();
        this.globalvalue = 1.0 - gini.compute(degrees.values().stream().map(aDouble -> aDouble + 0.0), true, communities.getNumCommunities(), this.sum);
    }
    
    /**
     * Updates the reranker after removing a new edge in the graph (undirected case).
     * @param user the user affected by the reranking.
     * @param old the edge to delete in the graph.
     */
    private void innerUpdateDel(U user, Tuple2od<U> old)
    {
        U del = old.v1;
        Integer userComm = this.communities.getCommunity(user);
        Integer delComm = this.communities.getCommunity(del);
        
        Pair<Double> delVal = this.computeDelValue(userComm, delComm);
        double userCommIncr = delVal.v1();
        double delCommIncr = delVal.v2();
        if(!userComm.equals(delComm))
        {
            degrees.put(userComm, degrees.get(userComm) - userCommIncr);
            degrees.put(delComm, degrees.get(delComm) - delCommIncr);
            this.sum -= (userCommIncr + delCommIncr);
        }
        
        GiniIndex gini = new GiniIndex();
        this.globalvalue = 1.0 - gini.compute(degrees.values().stream().map(aDouble -> aDouble + 0.0), true, communities.getNumCommunities(), this.sum);
    }
    
    
    @Override
    protected void computeGlobalValue()
    {
        super.computeGlobalValue();
        this.degrees.clear();
        if(communityGraph.isDirected())
        {
            DirectedMultiGraph<Integer> dirCommGraph = (DirectedMultiGraph<Integer>) communityGraph;
            if(orientation.equals(EdgeOrientation.IN))
            {
                communities.getCommunities().forEach((comm)-> 
                {
                    double value;
                    if(this.complete)
                    {
                        value = this.communityValue(comm,dirCommGraph.inDegree(comm)+0.0);
      
                    }
                    else
                    {
                        value = this.communityValue(comm, dirCommGraph.inDegree(comm) - dirCommGraph.getNumEdges(comm, comm) + 0.0);                        
                    }
                    degrees.put(comm, value);
                    sum += value;  
                });

            }
            else if(orientation.equals(EdgeOrientation.OUT))
            {
                communities.getCommunities().forEach((comm)-> 
                {
                    double value;
                    if(this.complete)
                    {
                        value = this.communityValue(comm,dirCommGraph.outDegree(comm)+0.0);
                    }
                    else
                    {
                        value = this.communityValue(comm, dirCommGraph.outDegree(comm) - dirCommGraph.getNumEdges(comm, comm) + 0.0);
                    }
                    degrees.put(comm, value);
                    sum += value;                      
                });
            }
            else 
            {
                communities.getCommunities().forEach((comm)-> 
                {
                    double value;
                    if(this.complete)
                    {
                        value = this.communityValue(comm,dirCommGraph.inDegree(comm) + dirCommGraph.outDegree(comm)+0.0);
                    }
                    else
                    {
                        value = this.communityValue(comm,dirCommGraph.inDegree(comm) + dirCommGraph.outDegree(comm) - 2*dirCommGraph.getNumEdges(comm, comm) +0.0);
                    }
                    degrees.put(comm, value);
                    sum += value;                      
                });
            }
        }
        else //if the orientation is undirected or the graph is not directed, then, take the degree.
        {
            communities.getCommunities().forEach((comm)-> 
            {
                double value;
                if(this.complete)
                {
                    value = this.communityValue(comm,communityGraph.degree(comm));
                }
                else
                {
                    value = this.communityValue(comm,communityGraph.degree(comm)-communityGraph.getNumEdges(comm, comm));
                }
                degrees.put(comm, value);
                sum += value;                      
            });
        }
        
        GiniIndex gini = new GiniIndex();
        this.globalvalue = 1.0 - gini.compute(degrees.values().stream().map(aDouble -> aDouble + 0.0), true, communities.getNumCommunities(), this.sum);
    }

    /**
     * Computes the increment when we add a link between two communities.
     * @param userComm      community of the target user.
     * @param recommComm    community of the added candidate user.
     * @return the increment
     */
    protected abstract Pair<Double> computeAddValue(Integer userComm, Integer recommComm);
    /**
     * Computes the decrement when we add a link between two communities.
     * @param userComm  community of the target user.
     * @param delComm   community of the removed candidate user.
     * @return the decrement
     */
    protected abstract Pair<Double> computeDelValue(Integer userComm, Integer delComm);

    /**
     * Given the effective degree for the reranker, and the community, this function
     * computes the value of this community, to store it.
     * @param comm      the community.
     * @param degree    the effective degree.
     * @return the real value for that community.
     */
    protected abstract double communityValue(int comm, double degree);
}
