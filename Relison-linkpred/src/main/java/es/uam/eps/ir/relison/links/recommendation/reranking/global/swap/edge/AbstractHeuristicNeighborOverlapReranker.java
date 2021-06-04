/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.global.swap.edge;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.links.recommendation.reranking.global.swap.GraphSwapReranker;
import es.uam.eps.ir.relison.links.recommendation.reranking.normalizer.Normalizer;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Swap reranker that modifies the rankings according to the average embeddedness of the network.
 * It uses heuristics to improve the execution times.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 */
public abstract class AbstractHeuristicNeighborOverlapReranker<U> extends GraphSwapReranker<U>
{
    /**
     * Map containing the intersections between two nodes in the network (CN)
     */
    private Map<U, Map<U, Double>> intersection;
    /**
     * Map containing the unions of two nodes in the network (TN)
     */
    private Map<U, Map<U, Double>> union;
    /**
     * Execution mode: 1) Embededness is corrected any time a swap is done. 2) Embededness is corrected every time a user has finished its reranking.
     * 3) Embededness is never corrected (only use the heuristic)
     */
    private final int mode;
    
    /**
     * True if we want edges with greater embeddedness, false if we want edges with smaller embeddedness (more weakness)
     */
    private final boolean promote;
    
    /**
     * Constructor
     * @param cutOff    the definitive length of the recommendation rankings.
     * @param lambda    trade-off between the average embeddedness and the original score
     * @param norm      the normalization scheme.
     * @param graph     the original graph
     * @param mode      the execution mode:
     *                  1) Embededness is corrected any time a swap is done.
     *                  2) Embededness is corrected every time a user has finished its reranking.
     *                  3) Embededness is never corrected (only use the heuristic)
     * @param promote   true if we want edges with greater embeddedness, false if we want edges with smaller embeddedness (more weakness)
     */
    public AbstractHeuristicNeighborOverlapReranker(double lambda, int cutOff, Supplier<Normalizer<U>> norm, Graph<U> graph, int mode, boolean promote)
    {
        super(lambda, cutOff, norm, graph);
        this.mode = mode;
        this.promote = promote;
    }

    @Override
    protected void computeGlobalValue() 
    {
        intersection = new HashMap<>();
        union = new HashMap<>();
        
        this.globalvalue = this.graph.getAllNodes().mapToDouble(u -> 
        {
            Set<U> uNodes = this.graph.getAdjacentNodes(u).collect(Collectors.toCollection(HashSet::new));
            intersection.put(u, new HashMap<>());
            union.put(u, new HashMap<>());
            
            // Store the intersection and the union, and compute the embeddedness of the graph.
            return this.graph.getAdjacentNodes(u).mapToDouble(v -> 
            {
                Set<U> vNodes = this.graph.getIncidentNodes(v).collect(Collectors.toCollection(HashSet::new));
                Set<U> inter = new HashSet<>(vNodes);
                Set<U> uni = new HashSet<>(vNodes);
                inter.retainAll(uNodes);
                uni.addAll(uNodes);
                
                double interSc = inter.size() + 0.0;
                double unionSc = uni.size();
                this.intersection.get(u).put(v, interSc);
                this.union.get(u).put(v, unionSc);
                
                if(unionSc > 2.0)
                {
                    return interSc/(unionSc + 2.0);
                }
                return 0.0;
            }).sum();
        }).sum();
               
        if(this.graph.isDirected())
        {
            this.globalvalue /= this.graph.getEdgeCount() + 0.0;
        }
        else
        {
            this.globalvalue /= (2.0*this.graph.getEdgeCount() + 0.0);
        }
        
        if(!promote)
        {
            this.globalvalue = 1.0 - this.globalvalue;
        }
    }
    
    @Override
    protected double novAddDelete(U u, Tuple2od<U> itemValue, Tuple2od<U> compared)
    {
        if(this.graph.isDirected())
            return this.novAddDeleteDirected(u, itemValue, compared);
        else
            return this.novAddDeleteUndirected(u, itemValue, compared);
    }
    
    /**
     * Computes the embededness in a directed graph (adding and deleting an edge)
     * @param u the target user.
     * @param itemValue The new candidate user
     * @param compared The old candidate user
     * @return the new novelty score.
     */
    private double novAddDeleteDirected(U u, Tuple2od<U> itemValue, Tuple2od<U> compared)
    {
        U v = compared.v1();
        U w = itemValue.v1();
        
        
        if(!intersection.get(u).containsKey(w) || !union.get(u).containsKey(w))
        {
            Set<U> uSet = graph.getAdjacentNodes(u).collect(Collectors.toCollection(HashSet::new));
            Set<U> wSet = graph.getAdjacentNodes(w).collect(Collectors.toCollection(HashSet::new));
            Set<U> inter = new HashSet<>(uSet);
            inter.retainAll(wSet);
            
            Set<U> uni = new HashSet<>(uSet);
            uni.addAll(wSet);
            
            this.intersection.get(u).put(w, inter.size() + 0.0);
            this.union.get(u).put(w, uni.size() + 0.0);
        }
        
        double value = this.graph.getEdgeCount()*(1.0 - this.globalvalue);
        
        value -= this.intersection.get(u).get(v)/this.union.get(u).get(v);
        
        boolean contains = this.graph.containsEdge(v,w);
        double interScUW = this.intersection.get(u).get(w) - (contains ? 1.0 : 0.0);
        double unionScUW = this.union.get(u).get(w) - (contains ? 0.0 : 1.0) + 2.0;
        
        value += interScUW / unionScUW;
        
        if(promote)
        {
            return value/this.graph.getEdgeCount();
        }
        else
        {
            return 1.0 - value/this.graph.getEdgeCount();
        }
    }
    
    
    
    /**
     * Computes the embededness in a directed graph (adding and deleting an edge)
     * @param u the target user.
     * @param itemValue The new candidate user
     * @param compared The old candidate user
     * @return the new novelty score.
     */
    private double novAddDeleteUndirected(U u, Tuple2od<U> itemValue, Tuple2od<U> compared)
    {
        U v = compared.v1();
        U w = itemValue.v1();
        
        
        if(!intersection.get(u).containsKey(w) || !union.get(u).containsKey(w))
        {
            Set<U> uSet = graph.getAdjacentNodes(u).collect(Collectors.toCollection(HashSet::new));
            Set<U> wSet = graph.getAdjacentNodes(w).collect(Collectors.toCollection(HashSet::new));
            Set<U> inter = new HashSet<>(uSet);
            inter.retainAll(wSet);
            
            Set<U> uni = new HashSet<>(uSet);
            uni.addAll(wSet);
            
            this.intersection.get(u).put(w, inter.size() + 0.0);
            this.union.get(u).put(w, uni.size() + 0.0);
        }
        
        double value = 2.0*this.graph.getEdgeCount()*(1.0 - this.globalvalue);
        
        value -= 2.0*this.intersection.get(u).get(v)/this.union.get(u).get(v);
        
        boolean contains = this.graph.containsEdge(v,w);
        double interScUW = this.intersection.get(u).get(w) - (contains ? 1.0 : 0.0);
        double unionScUW = this.union.get(u).get(w) - (contains ? 0.0 : 1.0) + 2.0;
        
        value += 2.0*interScUW / unionScUW;

        if(promote)
        {
            return value/(2.0*this.graph.getEdgeCount());
        }
        else
        {
            return 1.0 - value/(2.0*this.graph.getEdgeCount());
        }

    }
    
    @Override
    protected double novAdd(U u, Tuple2od<U> itemValue, Tuple2od<U> compared)
    {
        U v = compared.v1();
        U w = itemValue.v1();
        
        
        if(!intersection.get(u).containsKey(w) || !union.get(u).containsKey(w))
        {
            Set<U> uSet = graph.getAdjacentNodes(u).collect(Collectors.toCollection(HashSet::new));
            Set<U> wSet = graph.getAdjacentNodes(w).collect(Collectors.toCollection(HashSet::new));
            Set<U> inter = new HashSet<>(uSet);
            inter.retainAll(wSet);
            
            Set<U> uni = new HashSet<>(uSet);
            uni.addAll(wSet);
            
            this.intersection.get(u).put(w, inter.size() + 0.0);
            this.union.get(u).put(w, uni.size() + 0.0);
        }
        
        double value = 2.0*this.graph.getEdgeCount()*(1.0 - this.globalvalue);
        
        value -= 2.0*this.intersection.get(u).get(v)/this.union.get(u).get(v);
        
        boolean contains = this.graph.containsEdge(w,v);
        double interScUV = this.intersection.get(u).get(v) + (contains ? 1.0 : 0.0);
        double unionScUV = this.union.get(u).get(v) + (contains ? 0.0 : 1.0) + 2.0;
        value += 2.0*interScUV/unionScUV;
        
        double interScUW = this.intersection.get(u).get(w) - (contains ? 1.0 : 0.0);
        double unionScUW = this.union.get(u).get(w) - (contains ? 0.0 : 1.0) + 2.0;
        value += 2.0*interScUW / unionScUW;

        if(promote)
        {
            return value/(2.0*this.graph.getEdgeCount()+2.0);
        }
        else
        {
            return 1.0 - value/(2.0*this.graph.getEdgeCount()+2.0);
        }
    }

    @Override
    protected double novDelete(U u, Tuple2od<U> itemValue, Tuple2od<U> compared)
    {
        U v = compared.v1();
        U w = itemValue.v1();
        
        
        if(!intersection.get(u).containsKey(w) || !union.get(u).containsKey(w))
        {
            Set<U> uSet = graph.getAdjacentNodes(u).collect(Collectors.toCollection(HashSet::new));
            Set<U> wSet = graph.getAdjacentNodes(w).collect(Collectors.toCollection(HashSet::new));
            Set<U> inter = new HashSet<>(uSet);
            inter.retainAll(wSet);
            
            Set<U> uni = new HashSet<>(uSet);
            uni.addAll(wSet);
            
            this.intersection.get(u).put(w, inter.size() + 0.0);
            this.union.get(u).put(w, uni.size() + 0.0);
        }
        
        double value = 2.0*this.graph.getEdgeCount()*(1.0 - this.globalvalue);
        
        value -= 2.0*this.intersection.get(u).get(v)/this.union.get(u).get(v);
        value -= 2.0*this.intersection.get(u).get(w)/this.union.get(u).get(w);
        
        boolean contains = this.graph.containsEdge(v,w);
        double interScUW = this.intersection.get(u).get(w) - (contains ? 1.0 : 0.0);
        double unionScUW = this.union.get(u).get(w) - (contains ? 0.0 : 1.0) + 2.0;
        
        value += 2.0*interScUW / unionScUW;

        if(promote)
        {
            return value/(2.0*this.graph.getEdgeCount()-2.0);
        }
        else
        {
            return 1.0 - value/(2.0*this.graph.getEdgeCount()-2.0);
        }
    }

    @Override
    protected void innerUpdate(U user, Tuple2od<U> updated, Tuple2od<U> old)
    {         
        if(this.graph.isDirected())
        {
            updateAddDeleteDirected(user, updated, old);
        }
        else if(this.recs.get(old.v1).contains(user))
        {
            updateAdd(user, updated, old);
        }
        else if(this.recs.get(updated.v1).contains(user))
        {
            updateDelete(user, updated, old);
        }
        else
        {
            updateAddDeleteUndirected(user, updated, old);
        }
        

    }
    
    /**
     * Updates the statistics for a directed graph (adding the reranked edge and deleting the previous one).
     * @param u target user
     * @param updated new recommended user
     * @param old the old recommended user
     */
    private void updateAddDeleteDirected(U u, Tuple2od<U> updated, Tuple2od<U> old)
    {
        if(this.mode % 3 == 1)
        {
            super.update(u, updated, old);
            this.computeGlobalValue();
        }
        else
        {
            U v = old.v1();
            U w = updated.v1();
        
            double value = this.graph.getEdgeCount()*(1.0 - this.globalvalue);

            value -= this.intersection.get(u).get(v)/this.union.get(u).get(v);

            boolean contains = this.graph.containsEdge(v,w);
            if(contains)
            {
                this.intersection.get(u).put(w, this.intersection.get(u).get(w)-1.0);
            }
            else
            {
                this.union.get(u).put(w, this.union.get(u).get(w)-1.0);
            }
            
            double interScUW = this.intersection.get(u).get(w) - (contains ? 1.0 : 0.0);
            double unionScUW = this.union.get(u).get(w) - (contains ? 0.0 : 1.0) + 2.0;

            value += interScUW / unionScUW;
            
            contains = this.graph.containsEdge(w, v);
            if(contains)
            {
                this.intersection.get(u).put(v, this.intersection.get(u).get(w)+1.0);
            }
            else
            {
                this.union.get(u).put(w, this.union.get(u).get(w)+1.0);
            }
            
            if(promote)
            {
                this.globalvalue = value/(this.graph.getEdgeCount());
            }
            else
            {
                this.globalvalue = 1.0 - value/(this.graph.getEdgeCount());
            }
            
            
        }
    }
    
    /**
     * Updates the statistics for an undirected graph (adding the reranked edge and deleting the previous one).
     * @param u target user
     * @param updated new recommended user
     * @param old the old recommended user
     */
    private void updateAddDeleteUndirected(U u, Tuple2od<U> updated, Tuple2od<U> old)
    {
        if(this.mode % 3 == 1)
        {
            //super.update(u, updated, old);
            this.computeGlobalValue();
        }
        else
        {
            U v = old.v1();
            U w = updated.v1();
        
            double value = this.graph.getEdgeCount()*(1.0 - this.globalvalue);

            value -= 2.0*this.intersection.get(u).get(v)/this.union.get(u).get(v);

            boolean contains = this.graph.containsEdge(v,w);
            if(contains)
            {
                this.intersection.get(u).put(w, this.intersection.get(u).get(w)-1.0);
                this.intersection.get(w).put(u, this.intersection.get(u).get(w));
                this.intersection.get(u).put(v, this.intersection.get(u).get(w)+1.0);
                this.intersection.get(v).put(u, this.intersection.get(u).get(v));
            }
            else
            {
                this.union.get(u).put(w, this.union.get(u).get(w)-1.0);
                this.union.get(w).put(u, this.union.get(u).get(w));
                this.union.get(u).put(v, this.union.get(u).get(v)+1.0);
                this.union.get(v).put(u, this.union.get(u).get(v));
            }
            
            double interScUW = this.intersection.get(u).get(w) - (contains ? 1.0 : 0.0);
            double unionScUW = this.union.get(u).get(w) - (contains ? 0.0 : 1.0) + 2.0;

            value += 2.0 * interScUW / unionScUW;
            if(promote)
            {
                this.globalvalue = value/(2.0*this.graph.getEdgeCount());
            }
            else
            {
                this.globalvalue = 1.0 - value/(2.0*this.graph.getEdgeCount());
            }
        }
    }
    
    /**
     * Updates the statistics for an undirected graph (adding the reranked edge and not deleting anyone).
     * @param u target user
     * @param updated new recommended user
     * @param old old recommended user
     */
    private void updateAdd(U u, Tuple2od<U> updated, Tuple2od<U> old)
    {
        if(this.mode % 3 == 1)
        {
            super.update(u, updated, old);
            this.computeGlobalValue();
        }
        else
        {
            U v = old.v1();
            U w = updated.v1();
        
            double value = 2.0*this.graph.getEdgeCount()*(1.0 - this.globalvalue);

            value -= 2.0*this.intersection.get(u).get(v)/this.union.get(u).get(v);

            boolean contains = this.graph.containsEdge(w,v);
            double interScUV = this.intersection.get(u).get(v) + (contains ? 1.0 : 0.0);
            double unionScUV = this.union.get(u).get(v) + (contains ? 0.0 : 1.0) + 2.0;
            value += 2.0*interScUV/unionScUV;

            double interScUW = this.intersection.get(u).get(w) - (contains ? 1.0 : 0.0);
            double unionScUW = this.union.get(u).get(w) - (contains ? 0.0 : 1.0) + 2.0;
            value += 2.0*interScUW / unionScUW;
            
            if(contains)
            {
                this.intersection.get(u).put(v, this.intersection.get(u).get(v) + 1.0);
                this.intersection.get(v).put(u, this.intersection.get(u).get(v));
                this.intersection.get(u).put(w, this.intersection.get(u).get(w) - 1.0);
                this.intersection.get(w).put(u, this.intersection.get(u).get(w));
            }
            else
            {
                this.intersection.get(u).put(v, this.intersection.get(u).get(v) + 1.0);
                this.intersection.get(v).put(u, this.intersection.get(u).get(v));
                this.intersection.get(u).put(w, this.intersection.get(u).get(w) - 1.0);
                this.intersection.get(w).put(u, this.intersection.get(u).get(w));
            }

            if(promote)
            {
                this.globalvalue = value/(2.0*this.graph.getEdgeCount()+2.0);
            }
            else
            {
                this.globalvalue = 1.0 - value/(2.0*this.graph.getEdgeCount()+2.0);
            }
        }
    }
    
    
    /**
     * Updates the statistics for an undirected graph (deleting the old edge and not adding anyone).
     * @param u target user
     * @param updated new recommended user
     * @param old the old recommended user
     */
    private void updateDelete(U u, Tuple2od<U> updated, Tuple2od<U> old)
    {
        if(this.mode % 3 == 1)
        {
            super.update(u, updated, old);
            this.computeGlobalValue();
        }
        else
        {
            U v = old.v1();
            U w = updated.v1();


            if(!intersection.get(u).containsKey(w) || !union.get(u).containsKey(w))
            {
                Set<U> uSet = graph.getAdjacentNodes(u).collect(Collectors.toCollection(HashSet::new));
                Set<U> wSet = graph.getAdjacentNodes(w).collect(Collectors.toCollection(HashSet::new));
                Set<U> inter = new HashSet<>(uSet);
                inter.retainAll(wSet);

                Set<U> uni = new HashSet<>(uSet);
                uni.addAll(wSet);

                this.intersection.get(u).put(w, inter.size() + 0.0);
                this.union.get(u).put(w, uni.size() + 0.0);
            }

            double value = 2.0*this.graph.getEdgeCount()*(1.0 - this.globalvalue);

            value -= 2.0*this.intersection.get(u).get(v)/this.union.get(u).get(v);
            value -= 2.0*this.intersection.get(u).get(w)/this.union.get(u).get(w);

            boolean contains = this.graph.containsEdge(v,w);
            double interScUW = this.intersection.get(u).get(w) - (contains ? 1.0 : 0.0);
            double unionScUW = this.union.get(u).get(w) - (contains ? 0.0 : 1.0) + 2.0;

            value += 2.0*interScUW / unionScUW;

            if(contains)
            {
                this.intersection.get(u).put(w, this.intersection.get(u).get(w) - 1.0);
                this.intersection.get(w).put(u, this.intersection.get(u).get(w));
            }
            else
            {
                this.union.get(u).put(w, this.union.get(u).get(w) - 1.0);
                this.union.get(w).put(u, this.union.get(u).get(w));
            }
            if(promote)
            {
                this.globalvalue = value/(2.0*this.graph.getEdgeCount()-2.0);
            }
            else
            {
                this.globalvalue = 1.0 - value/(2.0*this.graph.getEdgeCount()-2.0);
            }
        }
    }

    @Override
    protected void update(Recommendation<U, U> reranked)
    {
        if(this.mode % 3 == 2)
        {
            this.computeGlobalValue();
        }
        
    }

 
}
