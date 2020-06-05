/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.local;

import es.uam.eps.ir.ranksys.core.Recommendation;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static java.lang.Double.isNaN;
import static java.lang.Double.min;

/**
 * Generalization of the greedy local rerankers for processing several of them in a row.
 * @author Javier Sanz-Cruzado Puig.
 * @param <U> Type of the users.
 * @param <I> Type of the items.
 */
public abstract class LocalGreedyReranker<U,I> extends LocalReranker<U,I> 
{

    /**
     * Maximum lenght of the rankings
     */
    protected final int cutOff;
    
    /**
     * Constructor.
     * @param cutOff the maximum length of the definitive rankings
     */
    public LocalGreedyReranker(int cutOff)
    {
        super();
        this.cutOff = cutOff;
    }  
    
    /**
     * Constructor
     * @param cutOff the maximum length of the definitive rankings
     * @param seed random seed.
     */
    public LocalGreedyReranker(int cutOff, int seed)
    {
        super(seed);
        this.cutOff = cutOff;
    }
    
    
    @Override
    protected Recommendation<U, I> rerankRecommendation(Recommendation<U, I> rec, int maxLength)
    {
        int[] perm = rerankPermutation(rec, maxLength);
        return this.permuteRecommendation(rec, perm);
    }

    /**
     * Given a recommendation, permutates it according to the given criteria
     * @param rec Original recommendation
     * @param maxLength the maximum length of the definitive rankings
     * @return The permutation
     */
    protected int[] rerankPermutation(Recommendation<U, I> rec, int maxLength) 
    {
        List<Tuple2od<I>> list = rec.getItems();
        
        IntList perm = new IntArrayList();
        IntLinkedOpenHashSet remainingI = new IntLinkedOpenHashSet();
        IntStream.range(0, list.size()).forEach(remainingI::add);
        
        while(!remainingI.isEmpty() && perm.size() < min(maxLength, cutOff))
        {
            int bestI = selectItem(rec.getUser(),remainingI, list);
            
            perm.add(bestI);
            remainingI.remove(bestI);
            
            update(rec.getUser(),list.get(bestI));
        }

        while(perm.size() < min(maxLength, list.size()))
            perm.add(remainingI.removeFirstInt());
        
        return perm.toIntArray();
    }

    /**
     * Given a recommendation and a permutation, builds a new recommendation with the changed order.
     * @param rec The original recommendation.
     * @param perm The permutation.
     * @return The permuted recommendation.
     */
    private Recommendation<U, I> permuteRecommendation(Recommendation<U, I> rec, int[] perm) {
        List<Tuple2od<I>> from = rec.getItems();
        List<Tuple2od<I>> to = new ArrayList<>();
        
        for(int i = 0; i < perm.length; i++)
        {
            Tuple2od<I> t = new Tuple2od<>(from.get(perm[i]).v1, perm.length - i);
            to.add(t);
        }
        
        return new Recommendation<>(rec.getUser(), to);
    }
    
    /**
     * Gets the base permutation (the i-th element remains at the i-th position)
     * @param n the size of the base permutation
     * @return the permutation.
     */
    protected static int[] getBasePerm(int n) 
    {
        int[] perm = new int[n];
        for(int i = 0; i < n; i++)
            perm[i] = i;
        
        return perm;
    }

    /**
     * Selects the next item to add in the permutation
     * @param u The user
     * @param remainingI Remaining items for user u
     * @param list The list of scored items.
     * @return the next item.
     */
    protected int selectItem(U u, IntSortedSet remainingI, List<Tuple2od<I>> list) {
        double[] max = new double[]{Double.NEGATIVE_INFINITY};
        int[] bestI = new int[]{remainingI.firstInt()};
        remainingI.stream().mapToInt(i->i).forEach(i ->
        {
            double value = this.value(list.get(i));
            if(isNaN(value)) return;
            
            if(value > max[0] || (value == max[0] && i < bestI[0]))
            {
                max[0] = value;
                bestI[0] = i;
            }
        });
        
        return bestI[0];
    }
    
    /**
     * Given a user, and the next value to add, updates the reranker parameters.
     * @param user The user
     * @param bestItemValue The next value to add at the ranking 
     */
    protected abstract void update(U user, Tuple2od<I> bestItemValue);

    /**
     * Computes the value for an item
     * @param get The item, along its original value.
     * @return the value.
     */
    protected abstract double value(Tuple2od<I> get);
    
}
