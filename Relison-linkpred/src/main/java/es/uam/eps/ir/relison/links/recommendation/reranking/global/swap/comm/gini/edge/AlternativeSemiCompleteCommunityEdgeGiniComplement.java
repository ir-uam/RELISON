/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.global.swap.comm.gini.edge;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.links.recommendation.reranking.global.swap.comm.CommunityReranker;
import es.uam.eps.ir.relison.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import es.uam.eps.ir.relison.utils.indexes.GiniIndex;
import org.jooq.lambda.tuple.Tuple3;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.*;
import java.util.function.Supplier;

/**
 * Swap reranker that optimizes the Gini index of the distribution of edges between communities.
 * Both edges between different communities and links inside of communities are considered,
 * but links between communities are all stored in a single group when computing the Gini
 * coefficient.
 *
 * Alternative reranker to the one implemented in es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap.comm.gini.edge.SemiCompleteCommunityEdgeGiniReranker.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 */
public class AlternativeSemiCompleteCommunityEdgeGiniComplement<U> extends CommunityReranker<U>
{
    /**
     * Contains freq for each pair of communities. (-1,-1) contains autolinks
     */
    private final Map<Integer, Map<Integer, Double>> values;
    /**
     * Ranking order
     */
    private final List<Pair<Integer>> list;
    /**
     * Positions in the ranking.
     */
    private final Map<Integer, Map<Integer, Integer>> rankpos;
    /**
     * Number of edges between communities.
     */
    private double sum;
    /**
     * The number of communities.
     */
    private final double numComms;

    /**
     * Constructor.
     * @param lambda        establishes the trade-off between the value of the Gini Index and the original score
     * @param cutoff        the definitive number of recommended items for each user.
     * @param norm          the normalization scheme.
     * @param graph         the original graph.
     * @param communities   a relation between communities and users in the graph.
     * @param selfloops     if we allow self-loops between users.
     */
    public AlternativeSemiCompleteCommunityEdgeGiniComplement(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities, boolean selfloops)
    {
        super(lambda, cutoff, norm, graph, communities, selfloops);
        
        this.values = new HashMap<>();
        this.list = new ArrayList<>();
        this.rankpos = new HashMap<>();
        this.numComms = communities.getNumCommunities()*(communities.getNumCommunities() - 1.0) + 1.0;
        sum = 0.0;
    }

    @Override
    protected void update(Recommendation<U, U> reranked)
    {
      
    }

    @Override
    protected double novAddDelete(U u, Tuple2od<U> recommUser, Tuple2od<U> deleteUser)
    {
        U recomm = recommUser.v1;
        U del = deleteUser.v1;
        
        int userComm = this.communities.getCommunity(u);
        int recommComm = this.communities.getCommunity(recomm);
        int delComm = this.communities.getCommunity(del);
        
        if(recommComm == delComm)
        {
            return this.globalvalue;
        }
        
        int recommCurrentPos;
        double recommCurrentVal;
        if(userComm == recommComm)
        {
           recommCurrentPos = this.rankpos.get(-1).get(-1);
           recommCurrentVal = this.values.get(-1).get(-1);
        }
        else if(this.graph.isDirected() || userComm < recommComm)
        {
            recommCurrentPos = this.rankpos.get(userComm).get(recommComm);
            recommCurrentVal = this.values.get(userComm).get(recommComm);
        }
        else
        {
           recommCurrentPos = this.rankpos.get(recommComm).get(userComm);
           recommCurrentVal = this.values.get(recommComm).get(userComm);
        }
        
        int delCurrentPos;
        double delCurrentVal;
        if(userComm == delComm)
        {
           delCurrentPos = this.rankpos.get(-1).get(-1);
           delCurrentVal = this.values.get(-1).get(-1);
        }
        else if(this.graph.isDirected() || userComm < delComm)
        {
            delCurrentPos = this.rankpos.get(userComm).get(delComm);
            delCurrentVal = this.values.get(userComm).get(delComm);
        }
        else
        {
           delCurrentPos = this.rankpos.get(delComm).get(userComm);
           delCurrentVal = this.values.get(delComm).get(userComm);
        }
        
        
        // CASE 1: recommCurrentVal = delCurrentVal - 1. A swap is produced between both objects. Therefore, Gini is not modified.
        if(recommCurrentVal == delCurrentVal - 1)
        {
            return this.globalvalue;
        }
        else if(recommCurrentVal == delCurrentVal) // CASE 2: both are equal. To prevent conflicts, we swap them.
        {
            int min = Math.min(delCurrentPos, recommCurrentPos);
            int max = Math.max(delCurrentPos, recommCurrentPos);
            delCurrentPos = min;
            recommCurrentPos = max;
        }
        
        // Once we prevent conflicts between the recommended and deleted communities, we proceed to the calculus of the new Gini coefficient.
        
        // Step 1: We move the pair (userComm, recommComm) to the right.
        boolean exit = false;
        int numPos = 0;
        double toAdd = 0.0;

        while(!exit)
        {
            int cursor = recommCurrentPos + numPos + 1;
            if(cursor >= this.list.size())
            {
                exit = true;
            }
            else
            {
                Pair<Integer> pos = this.list.get(cursor);
                double value = this.values.get(pos.v1()).get(pos.v2());
                if(value >= recommCurrentVal + 1)
                {
                    exit = true;
                }
                else
                {
                    toAdd -= 2*value;
                    ++numPos;
                }
            }
        }
        
        toAdd += 2*numPos * recommCurrentVal + 2*(numPos + (recommCurrentPos + 1)) - this.numComms - 1;
        
        exit = false;
        numPos = 0;

        while(!exit)
        {
            int cursor = delCurrentPos - numPos - 1;
            if(cursor < 0)
            {
                exit = true;
            }
            else
            {
                Pair<Integer> pos = this.list.get(cursor);
                double value = this.values.get(pos.v1()).get(pos.v2());
                if(value <= delCurrentVal - 1)
                {
                    exit = true;
                }
                else
                {
                    toAdd += 2*value;
                    ++numPos;
                }
            }
        }
                
        toAdd += -2*numPos * delCurrentVal - 2*((delCurrentPos + 1)- numPos) + this.numComms + 1;
        
        return this.globalvalue - (toAdd/(this.sum*(this.numComms - 1)));
        
    }
    
    @Override
    protected double novAdd(U u, Tuple2od<U> recommUser, Tuple2od<U> deleteUser)
    {
        U recomm = recommUser.v1;
        
        int userComm = this.communities.getCommunity(u);
        int recommComm = this.communities.getCommunity(recomm);
        
        int recommCurrentPos;
        double recommCurrentVal;
        if(userComm == recommComm)
        {
           recommCurrentPos = this.rankpos.get(-1).get(-1);
           recommCurrentVal = this.values.get(-1).get(-1);
        }
        else if(this.graph.isDirected() || userComm < recommComm)
        {
            recommCurrentPos = this.rankpos.get(userComm).get(recommComm);
            recommCurrentVal = this.values.get(userComm).get(recommComm);
        }
        else
        {
           recommCurrentPos = this.rankpos.get(recommComm).get(userComm);
           recommCurrentVal = this.values.get(recommComm).get(userComm);
        }
        
               
        // Step 1: We move the pair (userComm, recommComm) to the right.
        boolean exit = false;
        int numPos = 0;
        double toAdd = 0.0;

        while(!exit)
        {
            int cursor = recommCurrentPos + numPos + 1;
            if(cursor >= this.list.size())
            {
                exit = true;
            }
            else
            {
                Pair<Integer> pos = this.list.get(cursor);
                double value = this.values.get(pos.v1()).get(pos.v2());
                if(value >= recommCurrentVal + 1)
                {
                    exit = true;
                }
                else
                {
                    toAdd -= 2*value;
                    ++numPos;
                }
            }
        }
        
        toAdd += 2*numPos * recommCurrentVal + 2*(numPos + (recommCurrentPos+1)) - this.numComms - 1;
        
        return this.globalvalue - toAdd/((this.sum+1)*(this.numComms-1));
    }
    
    @Override
    protected double novDelete(U u, Tuple2od<U> itemValue, Tuple2od<U> compared)
    {
        U del = compared.v1;
        
        int userComm = this.communities.getCommunity(u);
        int delComm = this.communities.getCommunity(del);
                
        int delCurrentPos;
        double delCurrentVal;
        if(userComm == delComm)
        {
           delCurrentPos = this.rankpos.get(-1).get(-1);
           delCurrentVal = this.values.get(-1).get(-1);
        }
        else if(this.graph.isDirected() || userComm < delComm)
        {
            delCurrentPos = this.rankpos.get(userComm).get(delComm);
            delCurrentVal = this.values.get(userComm).get(delComm);
        }
        else
        {
           delCurrentPos = this.rankpos.get(delComm).get(userComm);
           delCurrentVal = this.values.get(delComm).get(userComm);
        }
                
        boolean exit = false;
        int numPos = 0;
        double toAdd = 0.0;

        while(!exit)
        {
            int cursor = delCurrentPos - numPos - 1;
            if(cursor < 0)
            {
                exit = true;
            }
            else
            {
                Pair<Integer> pos = this.list.get(cursor);
                double value = this.values.get(pos.v1()).get(pos.v2());
                if(value <= delCurrentVal - 1)
                {
                    exit = true;
                }
                else
                {
                    toAdd += 2*value;
                    ++numPos;
                }
            }
        }
                
        toAdd += -2*numPos * delCurrentVal - 2*((delCurrentPos+1) - numPos) + this.numComms + 1;
        
        return this.globalvalue - toAdd/((this.numComms-1)*(this.sum - 1));
    }

    @Override
    protected void innerUpdate(U user, Tuple2od<U> updated, Tuple2od<U> old)
    {
        U recomm = updated.v1();
        U del = old.v1();
        
        if(this.graph.isDirected())
        {
            this.innerUpdateAddDelete(user, updated, old);
        }
        else if(!recs.get(del).contains(user) && !recs.get(recomm).contains(user))
        {
            this.innerUpdateAddDelete(user, updated, old);
        }
        else if(!recs.get(recomm).contains(user) && recs.get(del).contains(user))
        {
            this.innerUpdateAdd(user, updated, old);
        }
        else if(recs.get(recomm).contains(user) && !recs.get(del).contains(user))
        {
            this.innerUpdateDelete(user, updated, old);
        }
    }
    
    /**
     * Updates the different variables and values necessary for the reranking of the
     * recommendation. Particularly, when a new edge is added and another one is deleted.
     * @param u the target user of the recommendation.
     * @param updated the user the new edge is created to
     * @param old the user the old edge was directed to
     */
    private void innerUpdateAddDelete(U u, Tuple2od<U> updated, Tuple2od<U> old)
    {
        
        U recomm = updated.v1;
        U del = old.v1;
        
        int userComm = this.communities.getCommunity(u);
        int recommComm = this.communities.getCommunity(recomm);
        int delComm = this.communities.getCommunity(del);
        
        // If the communities of both edges (new and deleted) are the same, no
        // further action is needed.
        if(recommComm == delComm)
        {
            return;
        }
        
        // Obtain the recommendation positions and the values for the destination
        // of the new edge.
        int recommCurrentPos;
        double recommCurrentVal;
        Pair<Integer> recommPair;
        if(userComm == recommComm)
        {
            // Case in which the target and candidate communities are the same
           recommCurrentPos = this.rankpos.get(-1).get(-1);
           recommCurrentVal = this.values.get(-1).get(-1);
           recommPair = new Pair<>(-1,-1);
        }
        else if(this.graph.isDirected() || userComm < recommComm)
        {
            recommCurrentPos = this.rankpos.get(userComm).get(recommComm);
            recommCurrentVal = this.values.get(userComm).get(recommComm);
            recommPair = new Pair<>(userComm,recommComm);

        }
        else
        {
           recommCurrentPos = this.rankpos.get(recommComm).get(userComm);
           recommCurrentVal = this.values.get(recommComm).get(userComm);
           recommPair = new Pair<>(recommComm, userComm);
        }
        
        this.values.get(recommPair.v1()).put(recommPair.v2(), recommCurrentVal + 1.0);
        int delCurrentPos;
        double delCurrentVal;
        Pair<Integer> delPair;

        if(userComm == delComm)
        {
           delCurrentPos = this.rankpos.get(-1).get(-1);
           delCurrentVal = this.values.get(-1).get(-1);
           delPair = new Pair<>(-1,-1);
        }
        else if(this.graph.isDirected() || userComm < delComm)
        {
            delCurrentPos = this.rankpos.get(userComm).get(delComm);
            delCurrentVal = this.values.get(userComm).get(delComm);
            delPair = new Pair<>(userComm, delComm);
        }
        else
        {
           delCurrentPos = this.rankpos.get(delComm).get(userComm);
           delCurrentVal = this.values.get(delComm).get(userComm);
           delPair = new Pair<>(delComm, userComm);
        }
        
        this.values.get(delPair.v1()).put(delPair.v2(), delCurrentVal - 1.0);
        
        
        // CASE 1: recommCurrentVal = delCurrentVal - 1. A swap is produced between both objects. Therefore, Gini is not modified.
        if(recommCurrentVal == delCurrentVal - 1)
        {
            this.list.set(recommCurrentPos, delPair);
            this.list.set(delCurrentPos, recommPair);
            this.rankpos.get(delPair.v1()).put(delPair.v2(), recommCurrentPos);
            this.rankpos.get(recommPair.v1()).put(recommPair.v2(), delCurrentPos);
            return; // Nothing else is changed.
        }
        else if(recommCurrentVal == delCurrentVal) // CASE 2: both are equal. To prevent conflicts, we swap them.
        {
            int min = Math.min(delCurrentPos, recommCurrentPos);
            int max = Math.max(delCurrentPos, recommCurrentPos);
            
            // We change the objects in the list to do the same with all objects
            this.list.set(min, delPair);
            this.list.set(max, recommPair);
            this.rankpos.get(delPair.v1()).put(delPair.v2(), min);
            this.rankpos.get(recommPair.v1()).put(recommPair.v2(), max);
            
            delCurrentPos = min;
            recommCurrentPos = max;
        }
        
        // Once we prevent conflicts between the recommended and deleted communities, we proceed to the calculus of the new Gini coefficient.
        
        // Step 1: We move the pair (userComm, recommComm) to the right.
        boolean exit = false;
        int numPos = 0;
        double toAdd = 0.0;

        while(!exit)
        {
            int cursor = recommCurrentPos + numPos + 1;
            if(cursor >= this.list.size())
            {
                exit = true;
            }
            else
            {
                Pair<Integer> pos = this.list.get(cursor);
                double value = this.values.get(pos.v1()).get(pos.v2());
                if(value >= recommCurrentVal + 1)
                {
                    exit = true;
                }
                else
                {
                    this.rankpos.get(pos.v1()).put(pos.v2(), cursor - 1);
                    toAdd -= 2*value;                   
                    ++numPos;
                }
            }
        }
        
        toAdd += 2*numPos * recommCurrentVal + (2*(numPos + (recommCurrentPos+1)) - this.numComms - 1);
        
        if(numPos > 0)
        {
            this.list.add(recommCurrentPos + numPos + 1, recommPair);
            this.list.remove(recommCurrentPos);
            this.rankpos.get(recommPair.v1()).put(recommPair.v2(), recommCurrentPos + numPos);
        }
        
        exit = false;
        numPos = 0;

        while(!exit)
        {
            int cursor = delCurrentPos - numPos - 1;
            if(cursor < 0)
            {
                exit = true;
            }
            else
            {
                Pair<Integer> pos = this.list.get(cursor);
                double value = this.values.get(pos.v1()).get(pos.v2());
                if(value <= delCurrentVal - 1)
                {
                    exit = true;
                }
                else
                {
                    this.rankpos.get(pos.v1()).put(pos.v2(), cursor + 1);
                    toAdd += 2*value;
                    ++numPos;
                }
            }
        }
        
        toAdd += -2*numPos * delCurrentVal - 2*((delCurrentPos+1) - numPos) + this.numComms + 1;
        
        if(numPos > 0)
        {
            this.list.add(delCurrentPos - numPos, delPair);
            this.list.remove(delCurrentPos+1);
            this.rankpos.get(delPair.v1()).put(delPair.v2(), delCurrentPos - numPos);
        }
                
        this.globalvalue -= (toAdd/(this.sum*(this.numComms - 1)));
    }
    
    /**
     * Updates the different variables and values necessary for the reranking of the
     * recommendation. Particularly, when only a new edge is added (none is deleted)
     * @param u the target user of the recommendation.
     * @param updated the user the new edge is created to
     * @param old the user the old edge was directed to
     */
    private void innerUpdateAdd(U u, Tuple2od<U> updated, Tuple2od<U> old)
    {
        U recomm = updated.v1;
       
        int userComm = this.communities.getCommunity(u);
        int recommComm = this.communities.getCommunity(recomm);
        
        this.sum += 1;
        
        // Obtain the recommendation positions and the values for the destination
        // of the new edge.
        int recommCurrentPos;
        double recommCurrentVal;
        Pair<Integer> recommPair;
        if(userComm == recommComm)
        {
            // Case in which the target and candidate communities are the same
           recommCurrentPos = this.rankpos.get(-1).get(-1);
           recommCurrentVal = this.values.get(-1).get(-1);
           recommPair = new Pair<>(-1,-1);
        }
        else if(this.graph.isDirected() || userComm < recommComm)
        {
            recommCurrentPos = this.rankpos.get(userComm).get(recommComm);
            recommCurrentVal = this.values.get(userComm).get(recommComm);
            recommPair = new Pair<>(userComm,recommComm);

        }
        else
        {
           recommCurrentPos = this.rankpos.get(recommComm).get(userComm);
           recommCurrentVal = this.values.get(recommComm).get(userComm);
           recommPair = new Pair<>(recommComm, userComm);
        }
        
        this.values.get(recommPair.v1()).put(recommPair.v2(), recommCurrentVal + 1.0);

        
        // Once we prevent conflicts between the recommended and deleted communities, we proceed to the calculus of the new Gini coefficient.
        
        // Step 1: We move the pair (userComm, recommComm) to the right.
        boolean exit = false;
        int numPos = 0;
        double toAdd = 0.0;

        while(!exit)
        {
            int cursor = recommCurrentPos + numPos + 1;
            if(cursor >= this.list.size())
            {
                exit = true;
            }
            else
            {
                Pair<Integer> pos = this.list.get(cursor);
                double value = this.values.get(pos.v1()).get(pos.v2());
                if(value >= recommCurrentVal + 1)
                {
                    exit = true;
                }
                else
                {
                    this.rankpos.get(pos.v1()).put(pos.v2(), cursor - 1);
                    toAdd -= 2*value;                   
                    ++numPos;
                }
            }
        }
        
        toAdd += 2*numPos * recommCurrentVal + (2*(numPos + (recommCurrentPos+1)) - this.communities.getNumCommunities() + 1);
        
        if(numPos > 0)
        {
            this.list.add(recommCurrentPos + numPos, recommPair);
            this.list.remove(recommCurrentPos);
            this.rankpos.get(recommPair.v1()).put(recommPair.v2(), recommCurrentPos + numPos);
        }
        
        this.globalvalue = this.globalvalue*(this.sum - 1)/this.sum - (toAdd/(this.sum*(this.numComms - 1)));    
    }
    
    /**
     * Updates the different variables and values necessary for the reranking of the
     * recommendation. Particularly, when only the old edge is deleted (none is added)
     * @param u the target user of the recommendation.
     * @param updated the user the new edge is created to
     * @param old the user the old edge was directed to
     */
    private void innerUpdateDelete(U u, Tuple2od<U> updated, Tuple2od<U> old)
    {
            
        U recomm = updated.v1;
        U del = old.v1;
        
        this.sum -= 1;
        int userComm = this.communities.getCommunity(u);
        int recommComm = this.communities.getCommunity(recomm);
        int delComm = this.communities.getCommunity(del);
        
        // If the communities of both edges (new and deleted) are the same, no
        // further action is needed.
        if(recommComm == delComm)
        {
            return;
        }
                
        int delCurrentPos;
        double delCurrentVal;
        Pair<Integer> delPair;

        if(userComm == delComm)
        {
           delCurrentPos = this.rankpos.get(-1).get(-1);
           delCurrentVal = this.values.get(-1).get(-1);
           delPair = new Pair<>(-1,-1);
        }
        else if(this.graph.isDirected() || userComm < delComm)
        {
            delCurrentPos = this.rankpos.get(userComm).get(delComm);
            delCurrentVal = this.values.get(userComm).get(delComm);
            delPair = new Pair<>(userComm, delComm);
        }
        else
        {
           delCurrentPos = this.rankpos.get(delComm).get(userComm);
           delCurrentVal = this.values.get(delComm).get(userComm);
           delPair = new Pair<>(delComm, userComm);
        }
                
        this.values.get(delPair.v1()).put(delPair.v2(), delCurrentVal - 1.0);

        
        // Step 1: We move the pair (userComm, recommComm) to the right.
        boolean exit = false;
        int numPos = 0;
        double toAdd = 0.0;

        while(!exit)
        {
            int cursor = delCurrentPos - numPos - 1;
            if(cursor < 0)
            {
                exit = true;
            }
            else
            {
                Pair<Integer> pos = this.list.get(cursor);
                double value = this.values.get(pos.v1()).get(pos.v2());
                if(value <= delCurrentVal - 1)
                {
                    exit = true;
                }
                else
                {
                    this.rankpos.get(pos.v1()).put(pos.v2(), cursor + 1);
                    toAdd += 2*value;
                    ++numPos;
                }
            }
        }
        
        toAdd += -2*numPos * delCurrentVal - 2*((delCurrentPos+1) - numPos) + this.numComms + 1;
        
        if(numPos > 0)
        {
            this.list.add(delCurrentPos - numPos, delPair);
            this.list.remove(delCurrentPos+1);
            this.rankpos.get(delPair.v1()).put(delPair.v2(), delCurrentPos - numPos);
        }
                
        this.globalvalue = this.globalvalue*(this.sum + 1)/this.sum - (toAdd/(this.sum*(this.numComms - 1)));    
    }
    

    @Override
    protected void computeGlobalValue() 
    {
        // Gini index for computing the initial value.
        GiniIndex gini = new GiniIndex();

        super.computeGlobalValue();
        long vertexcount = communityGraph.getVertexCount();
        
        // Comparator for the ranking heap
        Comparator<Tuple3<Integer,Integer,Double>> comp = Comparator.comparing((Tuple3<Integer, Integer, Double> x) -> x.v3);
        
        // Ranking heap. It will order the different community pairs from less frequency to most.
        PriorityQueue<Tuple3<Integer,Integer,Double>> heap = new PriorityQueue<>(1000, comp);
        
        // CASE 1: The graph is directed.
        if(communityGraph.isDirected())
        {
            
            double autolinks = 0.0;
            // Obtain the values. Initialize the maps values and add elements to the heap
            for(int i = 0; i < vertexcount; ++i)
            {
                this.values.put(i, new HashMap<>());
                this.rankpos.put(i, new HashMap<>());
                for(int j = 0; j < vertexcount; ++j)
                {
                    if(i != j)
                    {
                        double links = communityGraph.getNumEdges(i, j);
                        this.values.get(i).put(j, links);
                        this.sum += links;
                        heap.add(new Tuple3<>(i,j,links));
                    }
                    else
                    {
                        autolinks += communityGraph.getNumEdges(i, i);
                    }
                }
            }
            
            // Add the value to the heap.
            heap.add(new Tuple3<>(-1,-1,autolinks));
            this.values.put(-1, new HashMap<>());
            this.values.get(-1).put(-1, autolinks);
            this.rankpos.put(-1, new HashMap<>());
            this.sum += autolinks;
            
            // Temporal values.
            List<Double> tempValues = new ArrayList<>();
            int i = 0;
            
            // Extract one by one the elements in the heap, and complete the values in the ranking heap
            // and the list.
            while(!heap.isEmpty())
            {
                Tuple3<Integer, Integer, Double> triplet = heap.poll();
                tempValues.add(triplet.v3);
                this.list.add(new Pair<>(triplet.v1, triplet.v2));
                this.rankpos.get(triplet.v1).put(triplet.v2, i);
                ++i;
            }
            
            this.globalvalue = 1.0 - gini.compute(tempValues, false, communities.getNumCommunities()*(communities.getNumCommunities()-1)+1, this.sum);
        }
        else
        {
            double autolinks = 0.0;
            for(int i = 0; i < vertexcount; ++i)
            {
                this.values.put(i, new HashMap<>());
                this.rankpos.put(i, new HashMap<>());
                for(int j = 0; j <= i; ++j)
                {
                    if(i != j)
                    {
                        double links = communityGraph.getNumEdges(i, j);
                        this.values.get(i).put(j, links);
                        this.sum += links;
                        heap.add(new Tuple3<>(i,j,links));
                    }
                    else
                    {
                        autolinks += communityGraph.getNumEdges(i, i);
                    }
                }
            }
            heap.add(new Tuple3<>(-1,-1,autolinks));
            this.values.put(-1, new HashMap<>());
            this.values.get(-1).put(-1, autolinks);
            this.sum += autolinks;
            
            List<Double> tempValues = new ArrayList<>();

            int i = 0;
            while(!heap.isEmpty())
            {
                Tuple3<Integer, Integer, Double> triplet = heap.poll();
                tempValues.add(triplet.v3);
                this.list.add(new Pair<>(triplet.v1, triplet.v2));
                this.rankpos.get(triplet.v1).put(triplet.v2, i);
                ++i;
            }
            
            this.globalvalue = 1.0 - gini.compute(tempValues, true, communities.getNumCommunities()*(communities.getNumCommunities()-1)/2 + 1, this.sum);
        }
    }

    

 
    
}
