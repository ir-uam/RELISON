/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.reranking.global.swap.edge;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.global.swap.GraphSwapReranker;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.normalizer.Normalizer;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Class that tries to maximize the average embededness of the graph.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public abstract class AbstractNeighborOverlapReranker<U> extends GraphSwapReranker<U>
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
     * True if we want edges with greater embeddedness, false if we want edges with smaller embeddedness (more weakness)
     */
    private final boolean promote;
    
    /**
     * Constructor
     * @param cutOff    the maximum length of the definitive recommendation rankings.
     * @param lambda    trade-off between the average embeddedness and the original score.
     * @param norm      the normalization scheme.
     * @param graph     the original graph
     * @param promote   true if we want edges with greater embeddedness, false if we want edges with smaller embeddedness (more weakness)
     */
    public AbstractNeighborOverlapReranker(double lambda, int cutOff, Supplier<Normalizer<U>> norm, Graph<U> graph, boolean promote)
    {
        super(lambda, cutOff, norm, graph);
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
    protected double novAddDelete(U u, Tuple2od<U> itemValue, Tuple2od<U> compared) {
        if(this.graph.isDirected())
            return this.novAddDeleteDirected(u, itemValue, compared);
        else
            return this.novAddDeleteUndirected(u, itemValue, compared);
    }
    
    /**
     * Computes the embededness in a directed graph (adding and deleting an edge)
     * @param u         the target user.
     * @param itemValue the new candidate user
     * @param compared  the old candidate user
     * @return the new novelty score.
     */
    private double novAddDeleteDirected(U u, Tuple2od<U> itemValue, Tuple2od<U> compared)
    {
        U v = compared.v1();
        U w = itemValue.v1();

        if(!intersection.get(u).containsKey(w) || !union.get(u).containsKey(w))
        {
            Set<U> uSet = graph.getAdjacentNodes(u).collect(Collectors.toCollection(HashSet::new));
            Set<U> wSet = graph.getIncidentNodes(w).collect(Collectors.toCollection(HashSet::new));
            Set<U> inter = new HashSet<>(uSet);
            inter.retainAll(wSet);
            
            Set<U> uni = new HashSet<>(uSet);
            uni.addAll(wSet);
            
            this.intersection.get(u).put(w, inter.size() + 0.0);
            this.union.get(u).put(w, uni.size() + 0.0);
        }
        
        boolean contains = this.graph.containsEdge(v, w);
        double interScUW = this.intersection.get(u).get(w) - (contains ? 1.0 : 0.0);
        double unionScUW = this.union.get(u).get(w) + 2 - (contains ? 0.0 : 1.0);

        double novelty = 0.0;
        if(unionScUW > 2.0)
        {
            novelty = interScUW / (unionScUW - 2.0);
        }
        
        novelty += this.graph.getAllNodes().mapToDouble(x -> 
        {
           if(u.equals(x))
           {
                return this.graph.getAdjacentNodes(x).mapToDouble(y ->
                {
                    if(y.equals(v))
                    {
                        return 0.0;
                    }
                    else
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        boolean condV = this.graph.containsEdge(y, v);
                        boolean condW = this.graph.containsEdge(y, w);
                        
                        interSc += (condW ? 1.0 : 0.0) - (condV ? 1.0 : 0.0);
                        unionSc += (condV ? 1.0 : 0.0) - (condW ? 1.0 : 0.0);
                        if(unionSc > 2.0)
                            return interSc/(unionSc-2.0);
                        return 0.0;
                    }
                    
                }).sum();
           }
           else
           {
               return this.graph.getAdjacentNodes(x).mapToDouble(y -> 
               {
                    double interSc = this.intersection.get(x).get(y);
                    double unionSc = this.union.get(x).get(y);
                    boolean cond = this.graph.containsEdge(x, u);

                    if(y.equals(v))
                    {
                        interSc -= cond ? 1.0 : 0.0;
                        unionSc -= (cond ? 0.0 : 1.0);
                    }
                    else
                    {
                        interSc += cond ? 1.0 : 0.0;
                        unionSc += (cond ? 0.0 : 1.0);
                    }
                    
                    if(unionSc > 2.0)
                        return interSc/(unionSc-2.0);
                    return 0.0;
               }).sum();
           }
           
        }).sum();
        
        if(promote)
        {
            return novelty/(this.graph.getEdgeCount() + 0.0);
        }
        else
        {
            return 1.0 - novelty/(this.graph.getEdgeCount() + 0.0);
        }
    }
    
    
    
    /**
     * Computes the embededness in an undirected graph (adding and deleting an edge)
     * @param u         the target user.
     * @param itemValue the new candidate user
     * @param compared  the old candidate user
     * @return the new novelty score.
     */
    private double novAddDeleteUndirected(U u, Tuple2od<U> itemValue, Tuple2od<U> compared)
    {
        U v = compared.v1();
        U w = itemValue.v1();

        if(!intersection.get(u).containsKey(w) || !union.get(u).containsKey(w) || !intersection.get(w).containsKey(u) || !union.get(w).containsKey(u))
        {
            Set<U> uSet = graph.getAdjacentNodes(u).collect(Collectors.toCollection(HashSet::new));
            Set<U> wSet = graph.getIncidentNodes(w).collect(Collectors.toCollection(HashSet::new));
            Set<U> inter = new HashSet<>(uSet);
            inter.retainAll(wSet);
            
            Set<U> uni = new HashSet<>(uSet);
            uni.addAll(wSet);
            
            this.intersection.get(u).put(w, inter.size() + 0.0);
            this.intersection.get(w).put(u, inter.size() + 0.0);
            this.union.get(u).put(w, uni.size() + 0.0);
            this.union.get(w).put(u, uni.size() + 0.0);
        }
        
        
        boolean contains = this.graph.containsEdge(v, w);
        
        double interScUW = this.intersection.get(u).get(w) - (contains ? 1.0 : 0.0);
        double unionScUW = this.union.get(u).get(w) + (contains ? 1.0 : 0.0) - 1.0;
        
        double novelty = 0;
        if(unionScUW > 0)
        {
            novelty = 2.0 * interScUW / unionScUW;
        }
         
        novelty += this.graph.getAllNodes().mapToDouble(x -> 
        {
            if(u.equals(x))
            {
                return this.graph.getAdjacentNodes(x).mapToDouble(y ->
                {
                    if(y.equals(v))
                    {
                        return 0.0;
                    }
                    else
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        boolean condV = this.graph.containsEdge(y, v);
                        boolean condW = this.graph.containsEdge(y, w);

                        interSc += (condW ? 1.0 : 0.0) - (condV ? 1.0 : 0.0);
                        unionSc += (condV ? 1.0 : 0.0) - (condW ? 1.0 : 0.0);
                        if(unionSc > 2.0)
                            return interSc/(unionSc-2.0);
                        return 0.0;
                    }
                }).sum();
            }
            else if(v.equals(x))
            {
                return this.graph.getAdjacentNodes(x).mapToDouble(y -> 
                {
                    if(y.equals(u))
                    {
                        return 0.0;
                    }
                    else if(y.equals(w))
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        if(unionSc > 2.0)
                            return interSc/(unionSc-2.0);
                        return 0.0;
                    }
                    else
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        boolean cond = this.graph.containsEdge(y, u);
                        interSc -= cond ? 1.0 : 0.0;
                        unionSc -= (cond ? 0.0 : 1.0);
                        if(unionSc > 2.0)
                            return interSc/(unionSc-2.0);
                        return 0.0;
                    }
                }).sum();
            }
            else if(w.equals(x))
            {
                return this.graph.getAdjacentNodes(x).mapToDouble(y -> 
                {
                    if(y.equals(v))
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        if(unionSc > 2.0)
                            return interSc/(unionSc-2.0);
                        return 0.0;
                    }
                    else
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        boolean cond = this.graph.containsEdge(y, u);
                        interSc += cond ? 1.0 : 0.0;
                        unionSc += cond ? 0.0 : 1.0;
                        if(unionSc > 2.0)
                            return interSc/(unionSc-2.0);
                        return 0.0;                        
                    }
                }).sum();
            }
            else
            {
               return this.graph.getAdjacentNodes(x).mapToDouble(y -> 
               {
                    double interSc = this.intersection.get(x).get(y);
                    double unionSc = this.union.get(x).get(y);
                    boolean cond = this.graph.containsEdge(x, u);

                    if(y.equals(v))
                    {
                        interSc -= cond ? 1.0 : 0.0;
                        unionSc -= (cond ? 0.0 : 1.0);
                    }
                    else
                    {
                        interSc += cond ? 1.0 : 0.0;
                        unionSc += (cond ? 0.0 : 1.0);
                    }
                    
                    if(unionSc > 2.0)
                        return interSc/(unionSc-2.0);
                    return 0.0;
               }).sum();
           }
           
        }).sum();
        
        if(promote)
        {
            return novelty/(2.0*this.graph.getEdgeCount() + 0.0);
        }
        else
        {
            return 1.0 - novelty/(2.0*this.graph.getEdgeCount() + 0.0);
        }
    }
    
    @Override
    protected double novAdd(U u, Tuple2od<U> itemValue, Tuple2od<U> compared)
    {
        //We already assume that the graph is undirected.
        U w = itemValue.v1;
        
        if(!intersection.get(u).containsKey(w) || !union.get(u).containsKey(w) || !intersection.get(w).containsKey(u) || !union.get(w).containsKey(u))
        {
            Set<U> uSet = graph.getAdjacentNodes(u).collect(Collectors.toCollection(HashSet::new));
            Set<U> wSet = graph.getIncidentNodes(w).collect(Collectors.toCollection(HashSet::new));
            Set<U> inter = new HashSet<>(uSet);
            inter.retainAll(wSet);
            
            Set<U> uni = new HashSet<>(uSet);
            uni.addAll(wSet);
            
            this.intersection.get(u).put(w, inter.size() + 0.0);
            this.intersection.get(w).put(u, inter.size() + 0.0);
            this.union.get(u).put(w, uni.size() + 0.0);
            this.union.get(w).put(u, uni.size() + 0.0);
        }
        
        // Compute the embededness of both links (u,w) and (w,u). 
        double interScUW = this.intersection.get(u).get(w);
        double unionScUW = this.union.get(u).get(w);
        
        double novelty = 0.0;
        if(unionScUW > 0.0)
        {
            novelty = 2.0*(interScUW/unionScUW);
        }
        
        // Compute the novelty
        novelty += this.graph.getAllNodes().mapToDouble(x -> 
        {
            if(u.equals(x)) // If the first node is the target user
            {
                return this.graph.getAdjacentNodes(x).mapToDouble(y -> 
                {
                    // The edge (u,w) does not exist, so it won't be considered as an option
                    double interSc = this.intersection.get(x).get(y);
                    double unionSc = this.union.get(x).get(y);
                    boolean cond = this.graph.containsEdge(y, w);
                    interSc += cond ? 1.0 : 0.0;
                    unionSc += cond ? 0.0 : 1.0;
                    
                    if(unionSc > 2.0)
                    {
                        return (interSc)/(unionSc - 2.0);
                    }
                    return 0.0;
                    
                }).sum();
            }
            else if(w.equals(x)) // If the first node is the newly recommended user
            {
                return this.graph.getAdjacentNodes(x).mapToDouble(y -> 
                {
                    // The edge (w,u) does not exist, so it won't be considered as an option

                    double interSc = this.intersection.get(x).get(y);
                    double unionSc = this.union.get(x).get(y);
                    boolean cond = this.graph.containsEdge(y, u);
                    interSc += cond ? 1.0 : 0.0;
                    unionSc += cond ? 0.0 : 1.0;
                    
                    if(unionSc > 2.0)
                    {
                        return (interSc)/(unionSc - 2.0);
                    }
                    return 0.0;
                }).sum();
            }
            else // Otherwise
            {
                return this.graph.getAdjacentNodes(x).mapToDouble(y -> 
                {
                    if(y.equals(u)) // If the second node is equal to the target user
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        boolean cond = this.graph.containsEdge(x, w);
                        interSc += cond ? 1.0 : 0.0;
                        unionSc += cond ? 0.0 : 1.0;

                        if(unionSc > 2.0)
                        {
                            return (interSc)/(unionSc - 2.0);
                        }
                        return 0.0;
                    }
                    else if(y.equals(w)) // If the second node is equal to the newly recommended user
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        boolean cond = this.graph.containsEdge(x, u);
                        interSc += cond ? 1.0 : 0.0;
                        unionSc += cond ? 0.0 : 1.0;

                        if(unionSc > 2.0)
                        {
                            return (interSc)/(unionSc - 2.0);
                        }
                        return 0.0;
                    }
                    else // otherwise
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        if(unionSc > 2.0)
                        {
                            return (interSc)/(unionSc - 2.0);
                        }
                        return 0.0;                        
                    }
                }).sum();
            }
        }).sum();
        
        
        if(promote)
        {
            return novelty/(2.0*this.graph.getEdgeCount() + 2.0);
        }
        else
        {
            return 1.0 - novelty/(2.0*this.graph.getEdgeCount() + 2.0);
        }
    }

    @Override
    protected double novDelete(U u, Tuple2od<U> itemValue, Tuple2od<U> compared)
    {
       //We already assume that the graph is undirected.
        U v = compared.v1;
                
        double novelty = 0.0;

        // Compute the novelty
        novelty += this.graph.getAllNodes().mapToDouble(x -> 
        {
            if(u.equals(x)) // If the first node is the target user
            {
                return this.graph.getAdjacentNodes(x).mapToDouble(y -> 
                {
                    if(y.equals(v)) // The edge (u,v) does not exist now
                    {
                        return 0.0;
                    }
                    else // Other case
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        boolean cond = this.graph.containsEdge(y, v);
                        interSc -= cond ? 1.0 : 0.0;
                        unionSc -= cond ? 0.0 : 1.0;

                        if(unionSc > 2.0)
                        {
                            return (interSc)/(unionSc - 2.0);
                        }
                        return 0.0;
                    }
                    
                    
                }).sum();
            }
            else if(v.equals(x)) // If the first node is the newly recommended user
            {
                return this.graph.getAdjacentNodes(x).mapToDouble(y -> 
                {
                    if(y.equals(u)) // The edge (v,u) does not exist now
                    {
                        return 0.0;
                    }
                    else // Other case
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        boolean cond = this.graph.containsEdge(y, u);
                        interSc -= cond ? 1.0 : 0.0;
                        unionSc -= cond ? 0.0 : 1.0;

                        if(unionSc > 2.0)
                        {
                            return (interSc)/(unionSc - 2.0);
                        }
                        return 0.0;
                    }
                }).sum();
            }
            else // Otherwise
            {
                return this.graph.getAdjacentNodes(x).mapToDouble(y -> 
                {
                    if(y.equals(u)) // If the second node is equal to the target user
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        boolean cond = this.graph.containsEdge(x, v);
                        interSc -= cond ? 1.0 : 0.0;
                        unionSc -= cond ? 0.0 : 1.0;

                        if(unionSc > 2.0)
                        {
                            return (interSc)/(unionSc - 2.0);
                        }
                        return 0.0;
                    }
                    else if(y.equals(v)) // If the second node is equal to the newly recommended user
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        boolean cond = this.graph.containsEdge(x, u);
                        interSc -= cond ? 1.0 : 0.0;
                        unionSc -= cond ? 0.0 : 1.0;

                        if(unionSc > 2.0)
                        {
                            return (interSc)/(unionSc - 2.0);
                        }
                        return 0.0;
                    }
                    else // otherwise
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        if(unionSc > 2.0)
                        {
                            return (interSc)/(unionSc - 2.0);
                        }
                        return 0.0;                        
                    }
                }).sum();
            }
        }).sum();
        
        if(promote)
        {
            return novelty/(2.0*this.graph.getEdgeCount() - 2.0);
        }
        else
        {
            return 1.0 - novelty/(2.0*this.graph.getEdgeCount() - 2.0);
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
            updateAdd(user, updated);
        }
        else if(this.recs.get(updated.v1).contains(user))
        {
            updateDelete(user, old);
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
        U v = old.v1();
        U w = updated.v1();

        if(!intersection.get(u).containsKey(w) || !union.get(u).containsKey(w))
        {
            Set<U> uSet = graph.getAdjacentNodes(u).collect(Collectors.toCollection(HashSet::new));
            Set<U> wSet = graph.getIncidentNodes(w).collect(Collectors.toCollection(HashSet::new));
            Set<U> inter = new HashSet<>(uSet);
            inter.retainAll(wSet);
            
            Set<U> uni = new HashSet<>(uSet);
            uni.addAll(wSet);
            
            this.intersection.get(u).put(w, inter.size() + 0.0);
            this.union.get(u).put(w, uni.size() + 0.0);
        }
        
        boolean contains = this.graph.containsEdge(v, w);
        
        double interScUW = this.intersection.get(u).get(w) - (contains ? 1.0 : 0.0);
        double unionScUW = this.union.get(u).get(w) + 2 - (contains ? 0.0 : 1.0);

        double novelty = 0.0;
        if(unionScUW > 2.0)
        {
            novelty = interScUW / (unionScUW - 2.0);
        }
        
        this.intersection.get(u).put(w, interScUW);
        this.union.get(u).put(w, unionScUW);
        
        
        
        novelty += this.graph.getAllNodes().mapToDouble(x -> 
        {
           if(u.equals(x))
           {
                return this.graph.getAdjacentNodes(x).mapToDouble(y ->
                {
                    if(y.equals(v))
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        boolean cond = this.graph.containsEdge(w, v);
                        interSc += cond ? 1.0 : 0.0;
                        unionSc += cond ? 1.0 : 0.0 - 2.0;
                        
                        this.intersection.get(x).put(y, interSc);
                        this.union.get(x).put(y, unionSc);
                        return 0.0;
                    }
                    else
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        boolean condV = this.graph.containsEdge(y, v);
                        boolean condW = this.graph.containsEdge(y, w);
                        
                        interSc += (condW ? 1.0 : 0.0) - (condV ? 1.0 : 0.0);
                        unionSc += (condV ? 1.0 : 0.0) - (condW ? 1.0 : 0.0);
                        
                        this.intersection.get(x).put(y, interSc);
                        this.union.get(x).put(y, unionSc);
                        
                        if(unionSc > 2.0)
                            return interSc/(unionSc-2.0);
                        return 0.0;
                    }
                    
                }).sum();
           }
           else
           {
               return this.graph.getAdjacentNodes(x).mapToDouble(y -> 
               {
                    double interSc = this.intersection.get(x).get(y);
                    double unionSc = this.union.get(x).get(y);
                    boolean cond = this.graph.containsEdge(x, u);

                    if(y.equals(v))
                    {
                        interSc -= cond ? 1.0 : 0.0;
                        unionSc -= (cond ? 0.0 : 1.0);
                    }
                    else
                    {
                        interSc += cond ? 1.0 : 0.0;
                        unionSc += (cond ? 0.0 : 1.0);
                    }
                    
                    this.intersection.get(x).put(y, interSc);
                    this.union.get(x).put(y, unionSc);
                    
                    if(unionSc > 2.0)
                        return interSc/(unionSc-2.0);
                    return 0.0;
               }).sum();
           }
           
        }).sum();
        
        if(promote)
        {
            this.globalvalue = novelty/(this.graph.getEdgeCount() + 0.0);
        }
        else
        {
            this.globalvalue =  1.0 - novelty/(this.graph.getEdgeCount() + 0.0);
        }
    }
    
    /**
     * Updates the statistics for an undirected graph (adding the reranked edge and deleting the previous one).
     * @param u         target user
     * @param updated   new recommended user
     * @param old       the old recommended user
     */
    private void updateAddDeleteUndirected(U u, Tuple2od<U> updated, Tuple2od<U> old)
    {
        U v = old.v1();
        U w = updated.v1();

        if(!intersection.get(u).containsKey(w) || !union.get(u).containsKey(w) || !intersection.get(w).containsKey(u) || !union.get(w).containsKey(u))
        {
            Set<U> uSet = graph.getAdjacentNodes(u).collect(Collectors.toCollection(HashSet::new));
            Set<U> wSet = graph.getIncidentNodes(w).collect(Collectors.toCollection(HashSet::new));
            Set<U> inter = new HashSet<>(uSet);
            inter.retainAll(wSet);
            
            Set<U> uni = new HashSet<>(uSet);
            uni.addAll(wSet);
            
            this.intersection.get(u).put(w, inter.size() + 0.0);
            this.intersection.get(w).put(u, inter.size() + 0.0);
            this.union.get(u).put(w, uni.size() + 0.0);
            this.union.get(w).put(u, uni.size() + 0.0);
        }
        
        boolean contains = this.graph.containsEdge(v, w);
        
        double interScUW = this.intersection.get(u).get(w) - (contains ? 1.0 : 0.0);
        double unionScUW = this.union.get(u).get(w) + 2.0 - (contains ? 0.0 : 1.0);
        
        this.intersection.get(u).put(w,interScUW);
        this.union.get(u).put(w, unionScUW);
        this.intersection.get(w).put(u,interScUW);
        this.union.get(w).put(u, unionScUW);
        
        double novelty = 0;
        if(unionScUW > 2.0)
        {
            novelty = 2.0 * interScUW / (unionScUW-2.0);
        }
         
        novelty += this.graph.getAllNodes().mapToDouble(x -> 
        {
            if(u.equals(x))
            {
                return this.graph.getAdjacentNodes(x).mapToDouble(y ->
                {
                    if(y.equals(v))
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        boolean cond = this.graph.containsEdge(v, w);
                        
                        interSc += cond ? 1.0 : 0.0;
                        unionSc += (cond ? 0.0 : 1.0) - 2.0;
                        this.intersection.get(x).put(y,interSc);
                        this.union.get(x).put(y, unionSc);
                        return 0.0;
                    }
                    else
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        boolean condV = this.graph.containsEdge(y, v);
                        boolean condW = this.graph.containsEdge(y, w);

                        interSc += (condW ? 1.0 : 0.0) - (condV ? 1.0 : 0.0);
                        unionSc += (condV ? 1.0 : 0.0) - (condW ? 1.0 : 0.0);
                        
                        this.intersection.get(x).put(y,interSc);
                        this.union.get(x).put(y, unionSc);
                        
                        if(unionSc > 2.0)
                            return interSc/(unionSc-2.0);
                        return 0.0;
                    }
                }).sum();
            }
            else if(v.equals(x))
            {
                return this.graph.getAdjacentNodes(x).mapToDouble(y -> 
                {
                    if(y.equals(u))
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        boolean cond = this.graph.containsEdge(v, w);
                        
                        interSc += cond ? 1.0 : 0.0;
                        unionSc += (cond ? 0.0 : 1.0) - 2.0;
                        this.intersection.get(x).put(y,interSc);
                        this.union.get(x).put(y, unionSc);
                        return 0.0;
                    }
                    else if(y.equals(w))
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        if(unionSc > 2.0)
                            return interSc/(unionSc-2.0);
                        
                        this.intersection.get(x).put(y,interSc);
                        this.union.get(x).put(y, unionSc);
                        return 0.0;
                    }
                    else
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        boolean cond = this.graph.containsEdge(y, u);
                        interSc -= cond ? 1.0 : 0.0;
                        unionSc -= (cond ? 0.0 : 1.0);
                        
                        this.intersection.get(x).put(y,interSc);
                        this.union.get(x).put(y, unionSc);
                        
                        if(unionSc > 2.0)
                            return interSc/(unionSc-2.0);
                        return 0.0;
                    }
                }).sum();
            }
            else if(w.equals(x))
            {
                return this.graph.getAdjacentNodes(x).mapToDouble(y -> 
                {
                    if(y.equals(v))
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        
                        this.intersection.get(x).put(y,interSc);
                        this.union.get(x).put(y, unionSc);
                        
                        if(unionSc > 2.0)
                            return interSc/(unionSc-2.0);
                        return 0.0;
                    }
                    else
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        boolean cond = this.graph.containsEdge(y, u);
                        interSc += cond ? 1.0 : 0.0;
                        unionSc += cond ? 0.0 : 1.0;
                        
                        this.intersection.get(x).put(y,interSc);
                        this.union.get(x).put(y, unionSc);                        
                        
                        if(unionSc > 2.0)
                            return interSc/(unionSc-2.0);
                        return 0.0;                        
                    }
                }).sum();
            }
            else
            {
               return this.graph.getAdjacentNodes(x).mapToDouble(y -> 
               {
                    double interSc = this.intersection.get(x).get(y);
                    double unionSc = this.union.get(x).get(y);
                    boolean cond = this.graph.containsEdge(x, u);

                    if(y.equals(v))
                    {
                        interSc -= cond ? 1.0 : 0.0;
                        unionSc -= (cond ? 0.0 : 1.0);
                    }
                    else
                    {
                        interSc += cond ? 1.0 : 0.0;
                        unionSc += (cond ? 0.0 : 1.0);
                    }
                    
                    this.intersection.get(x).put(y,interSc);
                    this.union.get(x).put(y, unionSc);                    
                    
                    if(unionSc > 2.0)
                        return interSc/(unionSc-2.0);
                    return 0.0;
               }).sum();
           }
           
        }).sum();
        
        if(promote)
        {
            this.globalvalue = novelty/(2.0*this.graph.getEdgeCount() + 0.0);
        }
        else
        {
            this.globalvalue =  1.0 - novelty/(2.0*this.graph.getEdgeCount() + 0.0);
        }
    }
    
    /**
     * Updates the statistics for an undirected graph (adding the reranked edge and not deleting anyone).
     * @param u       target user
     * @param updated new recommended user
     */
    private void updateAdd(U u, Tuple2od<U> updated)
    {
        // We already assume that the graph is undirected.
        U w = updated.v1;
        
        if(!intersection.get(u).containsKey(w) || !union.get(u).containsKey(w) || !intersection.get(w).containsKey(u) || !union.get(w).containsKey(u))
        {
            Set<U> uSet = graph.getAdjacentNodes(u).collect(Collectors.toCollection(HashSet::new));
            Set<U> wSet = graph.getIncidentNodes(w).collect(Collectors.toCollection(HashSet::new));
            Set<U> inter = new HashSet<>(uSet);
            inter.retainAll(wSet);
            
            Set<U> uni = new HashSet<>(uSet);
            uni.addAll(wSet);
            
            this.intersection.get(u).put(w, inter.size() + 0.0);
            this.intersection.get(w).put(u, inter.size() + 0.0);
            this.union.get(u).put(w, uni.size() + 0.0);
            this.union.get(w).put(u, uni.size() + 0.0);
        }
        
        // Compute the embededness of both links (u,w) and (w,u). 
        double interScUW = this.intersection.get(u).get(w);
        double unionScUW = this.union.get(u).get(w) + 2.0;
        
        this.intersection.get(u).put(w,interScUW);
        this.union.get(u).put(w, unionScUW);
        this.intersection.get(w).put(u,interScUW);
        this.union.get(w).put(u, unionScUW);
        
        double novelty = 0.0;
        if(unionScUW > 0.0)
        {
            novelty = 2.0*(interScUW/unionScUW);
        }
        
        // Compute the novelty
        novelty += this.graph.getAllNodes().mapToDouble(x -> 
        {
            if(u.equals(x)) // If the first node is the target user
            {
                return this.graph.getAdjacentNodes(x).mapToDouble(y -> 
                {
                    // The edge (u,w) does not exist, so it won't be considered as an option
                    double interSc = this.intersection.get(x).get(y);
                    double unionSc = this.union.get(x).get(y);
                    boolean cond = this.graph.containsEdge(y, w);
                    interSc += cond ? 1.0 : 0.0;
                    unionSc += cond ? 0.0 : 1.0;
                    
                    this.intersection.get(x).put(y,interSc);
                    this.union.get(x).put(y, unionSc);                        
                    if(unionSc > 2.0)
                    {
                        return (interSc)/(unionSc - 2.0);
                    }
                    return 0.0;
                    
                }).sum();
            }
            else if(w.equals(x)) // If the first node is the newly recommended user
            {
                return this.graph.getAdjacentNodes(x).mapToDouble(y -> 
                {
                    // The edge (w,u) does not exist, so it won't be considered as an option

                    double interSc = this.intersection.get(x).get(y);
                    double unionSc = this.union.get(x).get(y);
                    boolean cond = this.graph.containsEdge(y, u);
                    interSc += cond ? 1.0 : 0.0;
                    unionSc += cond ? 0.0 : 1.0;
                    
                    this.intersection.get(x).put(y,interSc);
                    this.union.get(x).put(y, unionSc);                         
                    
                    if(unionSc > 2.0)
                    {
                        return (interSc)/(unionSc - 2.0);
                    }
                    return 0.0;
                }).sum();
            }
            else // Otherwise
            {
                return this.graph.getAdjacentNodes(x).mapToDouble(y -> 
                {
                    if(y.equals(u)) // If the second node is equal to the target user
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        boolean cond = this.graph.containsEdge(x, w);
                        interSc += cond ? 1.0 : 0.0;
                        unionSc += cond ? 0.0 : 1.0;

                        this.intersection.get(x).put(y,interSc);
                        this.union.get(x).put(y, unionSc);                             

                        if(unionSc > 2.0)
                        {
                            return (interSc)/(unionSc - 2.0);
                        }
                        return 0.0;
                    }
                    else if(y.equals(w)) // If the second node is equal to the newly recommended user
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        boolean cond = this.graph.containsEdge(x, u);
                        interSc += cond ? 1.0 : 0.0;
                        unionSc += cond ? 0.0 : 1.0;

                        this.intersection.get(x).put(y,interSc);
                        this.union.get(x).put(y, unionSc);
                        
                        if(unionSc > 2.0)
                        {
                            return (interSc)/(unionSc - 2.0);
                        }
                        return 0.0;
                    }
                    else // otherwise
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        
                        if(unionSc > 2.0)
                        {
                            return (interSc)/(unionSc - 2.0);
                        }
                        return 0.0;                        
                    }
                }).sum();
            }
        }).sum();
        
        if(promote)
        {
            this.globalvalue = novelty/(2.0*this.graph.getEdgeCount() + 2.0);
        }
        else
        {
            this.globalvalue =  1.0 - novelty/(2.0*this.graph.getEdgeCount() + 2.0);
        }
    }
    
    
    /**
     * Updates the statistics for an undirected graph (deleting the old edge and not adding anyone).
     * @param u     target user
     * @param old   the old recommended user
     */
    private void updateDelete(U u, Tuple2od<U> old)
    {
       //We already assume that the graph is undirected.
        U v = old.v1;
                
        double novelty = 0.0;

        // Compute the novelty
        novelty += this.graph.getAllNodes().mapToDouble(x -> 
        {
            if(u.equals(x)) // If the first node is the target user
            {
                return this.graph.getAdjacentNodes(x).mapToDouble(y -> 
                {
                    if(y.equals(v)) // The edge (u,v) does not exist now
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y) - 2.0;
                        
                        this.intersection.get(x).put(y, interSc);
                        this.union.get(x).put(y, unionSc);
                                
                        return 0.0;
                    }
                    else // Other case
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        boolean cond = this.graph.containsEdge(y, v);
                        interSc -= cond ? 1.0 : 0.0;
                        unionSc -= cond ? 0.0 : 1.0;

                        this.intersection.get(x).put(y, interSc);
                        this.union.get(x).put(y, unionSc);                        
                        
                        if(unionSc > 2.0)
                        {
                            return (interSc)/(unionSc - 2.0);
                        }
                        return 0.0;
                    }
                    
                    
                }).sum();
            }
            else if(v.equals(x)) // If the first node is the newly recommended user
            {
                return this.graph.getAdjacentNodes(x).mapToDouble(y -> 
                {
                    if(y.equals(u)) // The edge (v,u) does not exist now
                    {
                        return 0.0;
                    }
                    else // Other case
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        boolean cond = this.graph.containsEdge(y, u);
                        interSc -= cond ? 1.0 : 0.0;
                        unionSc -= cond ? 0.0 : 1.0;

                        this.intersection.get(x).put(y, interSc);
                        this.union.get(x).put(y, unionSc);                        
                        
                        if(unionSc > 2.0)
                        {
                            return (interSc)/(unionSc - 2.0);
                        }
                        return 0.0;
                    }
                }).sum();
            }
            else // Otherwise
            {
                return this.graph.getAdjacentNodes(x).mapToDouble(y -> 
                {
                    if(y.equals(u)) // If the second node is equal to the target user
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        boolean cond = this.graph.containsEdge(x, v);
                        interSc -= cond ? 1.0 : 0.0;
                        unionSc -= cond ? 0.0 : 1.0;

                        this.intersection.get(x).put(y, interSc);
                        this.union.get(x).put(y, unionSc);                        
                        
                        if(unionSc > 2.0)
                        {
                            return (interSc)/(unionSc - 2.0);
                        }
                        return 0.0;
                    }
                    else if(y.equals(v)) // If the second node is equal to the newly recommended user
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        boolean cond = this.graph.containsEdge(x, u);
                        interSc -= cond ? 1.0 : 0.0;
                        unionSc -= cond ? 0.0 : 1.0;

                        this.intersection.get(x).put(y, interSc);
                        this.union.get(x).put(y, unionSc);                        
                        
                        if(unionSc > 2.0)
                        {
                            return (interSc)/(unionSc - 2.0);
                        }
                        return 0.0;
                    }
                    else // otherwise
                    {
                        double interSc = this.intersection.get(x).get(y);
                        double unionSc = this.union.get(x).get(y);
                        if(unionSc > 2.0)
                        {
                            return (interSc)/(unionSc - 2.0);
                        }
                        return 0.0;                        
                    }
                }).sum();
            }
        }).sum();
        
        if(promote)
        {
            this.globalvalue = novelty/(2.0*this.graph.getEdgeCount() - 2.0);
        }
        else
        {
            this.globalvalue =  1.0 - novelty/(2.0*this.graph.getEdgeCount() - 2.0);
        }
    }

    @Override
    protected void update(Recommendation<U, U> reranked)
    {
        
    }

 
}
