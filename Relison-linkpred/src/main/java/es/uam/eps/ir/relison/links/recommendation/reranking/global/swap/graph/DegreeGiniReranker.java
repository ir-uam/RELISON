/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.global.swap.graph;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.relison.graph.DirectedGraph;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.links.recommendation.reranking.global.swap.GraphSwapReranker;
import es.uam.eps.ir.relison.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.relison.utils.indexes.GiniIndex;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;


/**
 * Optimizes the degree Gini of a graph.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class DegreeGiniReranker<U> extends GraphSwapReranker<U>
{
    /**
     * Sorted list that contains the degrees of the nodes.
     */
    private final List<Tuple2od<U>> degrees = new ArrayList<>();
    /**
     * Map that contains the positions of the nodes in the degrees list.
     */
    private final Map<U, Integer> map = new HashMap<>();
    /**
     * Selected orientation for the Degree Gini
     */
    private final EdgeOrientation orient;
    /**
     * The total sum of the degrees. Equal to the number of edges in the network
     * if both the graph and the selected orientation are directed, equal to the
     * double in other case.
     */
    private double sum;
    /**
     * Number of nodes.
     */
    private long count;
    
    /**
     * Constructor
     * @param lambda    trade-off between the original and novelty score (clustering coefficient).
     * @param cutOff    maximum length of the recommendation ranking.
     * @param norm      the normalization strategy.
     * @param graph     the original graph.
     * @param orient    orientation for selecting the degree.
     */
    public DegreeGiniReranker(double lambda, int cutOff, Supplier<Normalizer<U>> norm, Graph<U> graph, EdgeOrientation orient)
    {
        super(lambda, cutOff, norm, graph);
        
        this.orient = orient;
    }

    @Override
    protected double novAddDelete(U u, Tuple2od<U> newUser, Tuple2od<U> oldUser)
    {
        // The out-degree of the node is not modified, so reranking the out-degree will 
        // give the same ranking as a result. It is equivalent to adding 0 to a sum.
        if(!this.graph.isDirected() || this.orient != EdgeOrientation.OUT)
        {
            int userToAdd = this.map.get(newUser.v1);
            int userToDel = this.map.get(oldUser.v1);
         
            // we obtain \sum_{i=1}^|U| (2i - |U| - 1) |\Gamma(u_i)|
            double value = (1.0-this.globalvalue)*(this.count - 1)*this.sum;
            
            // Obtain the updated degree.
            double degreeToAdd = this.degrees.get(userToAdd).v2 + 1.0;
            double degreeToDelete = this.degrees.get(userToDel).v2 - 1.0;

            if(degreeToAdd == degreeToDelete + 1.0)
            {
                return this.globalvalue;
            }
            // Select the position of the new destination node
            int i;
            for(i = userToAdd + 1; i < this.degrees.size(); ++i)
            {
                if(this.degrees.get(i).v2 >= degreeToAdd)
                    break;
            }
            
            int posAdd = i-1;
            
            // Select the position of the old destination node
            for(i = userToDel - 1; i >= 0; --i)
            {
                if(this.degrees.get(i).v2 <= degreeToDelete)
                    break;
            }
            
            int posDel = i+1;
            // We add 1 to posAdd and posDel, since sum starts with 1, not 0
            return 1.0 - (value + 2.0*((posAdd+1 + this.count - 1)-(posDel+1 + this.count - 1)))/(this.sum*(this.count - 1));          
        }
        else // if(this.graph.isDirected() && this.orient == EdgeOrientation.OUT
        {
            return this.globalvalue;
        }
    }
    
    
    @Override
    protected double novAdd(U u, Tuple2od<U> newUser, Tuple2od<U> oldUser)
    {
        int candidate = this.map.get(newUser.v1);
        int target = this.map.get(u);

        // we obtain \sum_{i=1}^|U| (2i - |U| - 1) |\Gamma(u_i)|
        double value = (1.0-this.globalvalue)*(this.graph.getVertexCount() - 1)*this.sum;

        // Obtain the updated degree.
        double candidateDegree = this.degrees.get(candidate).v2 + 1.0;
        double targetDegree = this.degrees.get(target).v2 + 1.0;

        int i;
        for(i = candidate + 1; i < this.degrees.size(); ++i)
        {
            if(this.degrees.get(i).v2 >= candidateDegree)
            {
                break;
            }
        }

        int candidateAdd = i - 1;
        int targetAdd;
        if(candidateDegree == targetDegree)
        {
            targetAdd = i - 2;
        }
        else
        {
            for(i = target + 1; i < this.degrees.size(); ++i)
            {
                if(this.degrees.get(i).v2 >= targetDegree)
                    break;
            }

            targetAdd = i - 1;
        }

        // We add 1 to posAdd and posDel, since sum starts with 1, not 0
        return 1.0 - (value + 2.0*((targetAdd+1)+(candidateAdd+1)))/((this.sum+2.0)*(this.graph.getVertexCount() - 1));
    }

    @Override
    protected double novDelete(U u, Tuple2od<U> newUser, Tuple2od<U> oldUser)
    {
        int candidate = this.map.get(newUser.v1);
        int target = this.map.get(u);

        // we obtain \sum_{i=1}^|U| (2i - |U| - 1) |\Gamma(u_i)|
        double value = (1.0-this.globalvalue)*(this.graph.getVertexCount() - 1)*this.sum;

        // Obtain the updated degree.
        double candidateDegree = this.degrees.get(candidate).v2 - 1.0;
        double targetDegree = this.degrees.get(target).v2 - 1.0;

        int i;
        for(i = candidate - 1; i >= 0; --i)
        {
            if(this.degrees.get(i).v2 <= candidateDegree)
            {
                break;
            }
        }

        int candidateAdd = i + 1;
        int targetAdd;
        if(candidateDegree == targetDegree)
        {
            targetAdd = i + 2;
        }
        else
        {
            for(i = target - 1; i >= 0; --i)
            {
                if(this.degrees.get(i).v2 <= targetDegree)
                    break;
            }

            targetAdd = i + 1;
        }

        // We add 1 to posAdd and posDel, since sum starts with 1, not 0
        return 1.0 - (value - 2.0*((targetAdd+1)+(candidateAdd+1)))/((this.sum-2.0)*(this.graph.getVertexCount() - 1));
    }

    @Override
    protected void computeGlobalValue() 
    {
        if(graph.isDirected())
        {
            DirectedGraph<U> dgraph = (DirectedGraph<U>) graph;
            switch (this.orient)
            {
                case IN -> graph.getAllNodes().forEach(u -> degrees.add(new Tuple2od<>(u, dgraph.inDegree(u))));
                case OUT -> graph.getAllNodes().forEach(u -> degrees.add(new Tuple2od<>(u, dgraph.outDegree(u))));
                default -> graph.getAllNodes().forEach(u -> degrees.add(new Tuple2od<>(u, dgraph.inDegree(u) + dgraph.outDegree(u))));
            }
        }
        else
        {
            graph.getAllNodes().forEach(u -> degrees.add(new Tuple2od<>(u, graph.degree(u))));
        }
        
        
        /*
         * We sort the list containing the degrees.
         */
        degrees.sort((x,y) -> 
        {
            Double x1 = x.v2;
            Double y1 = y.v2;
            return x1.compareTo(y1);
        });
        
        /*
         * The number of elements is equal to the number of nodes in the network.
         */
        this.count = graph.getVertexCount();
        

        /*
         * store the positions of each user in the list.
         */
        for(int i = 0; i < degrees.size(); ++i)
        {
            map.put(degrees.get(i).v1, i);
        }
        
        // If the graph is undirected, or the selected orientation is undirected, 
        // then, each edge in the network is computed twice.
        if(!this.graph.isDirected() || this.orient.equals(EdgeOrientation.UND))
        {
            this.sum = 2.0*graph.getEdgeCount();
        }
        else
        {
            this.sum = graph.getEdgeCount();
        }

        GiniIndex gi = new GiniIndex();
        this.globalvalue =  1.0 - gi.compute(degrees.stream().map(Tuple2od::v2), false, this.count, this.sum);
    }

    @Override
    protected void update(Recommendation<U, U> reranked)
    {
        
    }
    
    @Override
    protected void innerUpdate(U user, Tuple2od<U> updated, Tuple2od<U> old)
    {
        if(this.graph.isDirected())
        {
            this.globalvalue = newcoef(user, updated, old);
        }
        else if(this.recs.get(old.v1).contains(user) && this.recs.get(updated.v1).contains(user))
        {
            // do nothing
        }
        else if(this.recs.get(old.v1).contains(user))
        {
            this.globalvalue = newcoefadd(user, updated);
        }
        else if(this.recs   .get(updated.v1).contains(user))
        {
            this.globalvalue = newcoefdel(user, old);
        }
        else
        {
            this.globalvalue = newcoef(user, updated, old);
        }
    }

    /**
     * Updates the parameters, considering that a only new edge is added. Every
     * edge that enters this function is considered to be undirected.
     * @param user      the target user.
     * @param updated   the new candidate user.
     * @return the new global value.
     */
    private double newcoefadd(U user, Tuple2od<U> updated)
    {
        int candidate = this.map.get(updated.v1);
        int target = this.map.get(user);

        
        // we obtain \sum_{i=1}^|U| (2i - |U| - 1) |\Gamma(u_i)|
        double value = (1.0-this.globalvalue)*(this.graph.getVertexCount() - 1)*this.sum;

        // Obtain the updated degree.
        double candidateDegree = this.degrees.get(candidate).v2 + 1.0;
        double targetDegree = this.degrees.get(target).v2 + 1.0;

        int i;
        for(i = candidate + 1; i < this.degrees.size(); ++i)
        {
            if(this.degrees.get(i).v2 >= candidateDegree)
            {
                break;
            }
        }

        int candidateAdd = i - 1;
        int targetAdd;
        if(candidateDegree == targetDegree)
        {
            targetAdd = i - 2;
        }
        else
        {
            for(i = target + 1; i < this.degrees.size(); ++i)
            {
                if(this.degrees.get(i).v2 >= targetDegree)
                    break;
            }

            targetAdd = i - 1;
        }
        
        Tuple2od<U> candidateTuple = new Tuple2od<>(updated.v1, candidateDegree);
        Tuple2od<U> targetTuple = new Tuple2od<>(user, targetDegree);
        
        // Change the position of the old destination node
        Tuple2od<U> aux = this.degrees.get(candidateAdd);
        this.degrees.set(candidateAdd, candidateTuple);
        this.degrees.set(candidate, aux);
        this.map.put(aux.v1, candidate);
        this.map.put(updated.v1, candidateAdd);
        
        // Change the position of the target user
        target = this.map.get(user);
        aux = this.degrees.get(targetAdd);
        this.degrees.set(targetAdd, targetTuple);
        this.degrees.set(target, aux);
        
        this.map.put(aux.v1, target);
        this.map.put(user, targetAdd);

        this.sum += 2.0;
        // We add 1 to posAdd and posDel, since sum starts with 1, not 0
        return 1.0 - (value + 2.0*((targetAdd+1)+(candidateAdd+1)))/((this.sum)*(this.graph.getVertexCount() - 1));   
    }

    /**
     * Updates the parameters, considering that a new edge is added, and the old edge is removed.
     * @param user      the target user.
     * @param updated   the new candidate user.
     * @param old       the old candidate user.
     * @return the new global value.
     */
    private double newcoef(U user, Tuple2od<U> updated, Tuple2od<U> old)
    {
        // The out-degree of the node is not modified, so reranking the out-degree will 
        // give the same ranking as a result. It is equivalent to adding 0 to a sum.
        if(!this.graph.isDirected() || this.orient != EdgeOrientation.OUT)
        {
            int userToAdd = this.map.get(updated.v1);
            int userToDel = this.map.get(old.v1);
         
            // we obtain \sum_{i=1}^|U| (2i - |U| - 1) |\Gamma(u_i)|
            double value = (1.0-this.globalvalue)*(this.count - 1);
            
            // Obtain the updated degree.
            double degreeToAdd = this.degrees.get(userToAdd).v2 + 1.0;
            double degreeToDelete = this.degrees.get(userToDel).v2 - 1.0;
            
            // Swap the new and old, in case they both occupy the same position.
            if(degreeToAdd == degreeToDelete + 1)
            {
                Tuple2od<U> tupleToAdd = new Tuple2od<>(updated.v1, degreeToAdd);
                Tuple2od<U> tupleToDel = new Tuple2od<>(old.v1, degreeToDelete);
                
                this.degrees.set(userToAdd, tupleToDel);
                this.degrees.set(userToDel, tupleToAdd);
                
                this.map.put(updated.v1, userToDel);
                this.map.put(old.v1, userToAdd);
                
                return this.globalvalue;
            }
            else
            {
                int i;
                // Select the position of the new destination node
                for(i = userToAdd + 1; i < this.degrees.size(); ++i)
                {
                    if(this.degrees.get(i).v2 >= degreeToAdd)
                        break;
                }
                      
                int posAdd = i-1;
            
                // Select the position of the old destination node
                for(i = userToDel - 1; i >= 0; --i)
                {
                    if(this.degrees.get(i).v2 <= degreeToDelete)
                        break;
                }

                int posDel = i+1;
                

                Tuple2od<U> toAddTuple = new Tuple2od<>(updated.v1, degreeToAdd);
                Tuple2od<U> toDeleteTuple = new Tuple2od<>(old.v1, degreeToDelete);
                
                
                // Change the position of the new destination node
                
                if(userToAdd == posAdd)
                {
                    this.degrees.set(userToAdd, toAddTuple);
                }
                else
                {
                    Tuple2od<U> aux = this.degrees.get(posAdd);
                    this.degrees.set(userToAdd, aux);
                    this.map.put(aux.v1, userToAdd);
                    this.degrees.set(posAdd, toAddTuple);
                    this.map.put(updated.v1, posAdd);
                }
                
                if(userToDel == posDel)
                {
                    this.degrees.set(userToDel, toDeleteTuple);
                }
                else
                {
                    userToDel = this.map.get(old.v1);
                    Tuple2od<U> aux = this.degrees.get(posDel);
                    this.degrees.set(userToDel, aux);
                    this.map.put(aux.v1, userToDel);
                    this.degrees.set(posDel, toDeleteTuple);
                    this.map.put(old.v1, posDel);
                }
                                
                // We add 1 to posAdd and posDel, since sum starts with 1, not 0
                value = 1.0 - (value + 2.0*((posAdd+1)-(posDel+1))/this.sum)/(this.count - 1);
                return value;
            }
        }
        else // if(this.graph.isDirected() && this.orient == EdgeOrientation.OUT
        {
            return this.globalvalue;
        }  
    }
    
    /**
     * Updates the parameters, considering that the new edge is not added (only the old is removed.)
     * @param user  the target user.
     * @param old   the old candidate user.
     * @return the new global value.
     */
    private double newcoefdel(U user, Tuple2od<U> old)
    {
        int candidate = this.map.get(old.v1);
        int target = this.map.get(user);

        // we obtain \sum_{i=1}^|U| (2i - |U| - 1) |\Gamma(u_i)|
        double value = (1.0-this.globalvalue)*(this.graph.getVertexCount() - 1)*this.sum;

        // Obtain the updated degree.
        double candidateDegree = this.degrees.get(candidate).v2 - 1.0;
        double targetDegree = this.degrees.get(target).v2 - 1.0;

        int i;
        for(i = candidate + 1; i >= 0; --i)
        {
            if(this.degrees.get(i).v2 <= candidateDegree)
            {
                break;
            }
        }

        int candidateAdd = i + 1;
        int targetAdd;
        if(candidateDegree == targetDegree)
        {
            targetAdd = i + 2;
        }
        else
        {
            for(i = target + 1; i >= 0; --i)
            {
                if(this.degrees.get(i).v2 <= targetDegree)
                    break;
            }

            targetAdd = i + 1;
        }
        
        Tuple2od<U> candidateTuple = new Tuple2od<>(old.v1, candidateDegree);
        Tuple2od<U> targetTuple = new Tuple2od<>(user, targetDegree);
        
        // Change the position of the old destination node
        Tuple2od<U> aux = this.degrees.get(candidateAdd);
        this.degrees.set(candidateAdd, candidateTuple);
        this.degrees.set(candidate, aux);
        this.map.put(aux.v1, candidate);
        this.map.put(old.v1, candidateAdd);
        
        // Change the position of the target user
        target = this.map.get(user);
        aux = this.degrees.get(targetAdd);
        this.degrees.set(targetAdd, targetTuple);
        this.degrees.set(target, aux);
        
        this.map.put(aux.v1, target);
        this.map.put(user, targetAdd);

        this.sum -= 2.0;
        // We add 1 to posAdd and posDel, since sum starts with 1, not 0
        return 1.0 - (value + 2.0*((targetAdd+1)+(candidateAdd+1)))/((this.sum)*(this.graph.getVertexCount() - 1));    
    }
    
}
