/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap;

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

/**
 * Greedy Reranker for optimizing a global metric of the recommendation algorithm.
 * @author Javier Sanz-Cruzado Puig.
 * @param <U> Type of the users.
 * @param <I> Type of the items.
 */
public abstract class SwapGreedyReranker<U,I> extends SwapReranker<U,I> 
{

    /**
     * Cut-off of the ranking
     */
    protected final int cutOff;
    
    /**
     * Constructor.
     * @param cutOff maximum cut-off the ranking.
     */
    public SwapGreedyReranker(int cutOff)
    {
        super();
        this.cutOff = cutOff;
    }
    
    @Override
    protected Recommendation<U, I> rerankRecommendation(Recommendation<U, I> rec, int maxLength)
    {
        int[] perm = rerankPermutation(rec, maxLength);
        return this.permuteRecommendation(rec, perm);
    }

    /**
     * Obtains a permutation for a ranking that greedily optimizes the global metric.
     * @param rec the recommendation ranking.
     * @param maxLength maximum length of the permutation.
     * @return a permutation of the indexes.
     */
    protected int[] rerankPermutation(Recommendation<U, I> rec, int maxLength)
    {
        List<Tuple2od<I>> list = rec.getItems();

        //Top k recommendation
        IntList perm = new IntArrayList();
        IntLinkedOpenHashSet remainingI = new IntLinkedOpenHashSet();
    
        
        int permSize = Math.min(maxLength, cutOff);
        // Generate the top k and the remaining list recommendation
        IntStream.range(permSize, list.size()).forEach(remainingI::add);
        IntStream.range(0, permSize).forEach(perm::add);
        
        for(int i = 0; i < permSize; ++i)
        {
            Tuple2od<I> compared = list.get(perm.getInt(i));
            int bestI = selectItem(rec.getUser(), remainingI, compared, list);
            if(bestI != -1)
            {
                //Swap the elements in the ranking
                remainingI.add(perm.getInt(i));
                remainingI.remove(bestI);
                perm.set(i, bestI);
                
                //Update values
                update(rec.getUser(), list.get(bestI), compared);
            }
        }
        
        return perm.toIntArray();
    }

    /**
     * Permutes a recommendation.
     * @param rec The original recommendation.
     * @param perm The permutation of the data.
     * @return the new recommendation.
     */
    private Recommendation<U, I> permuteRecommendation(Recommendation<U, I> rec, int[] perm) 
    {
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
     * Obtains the base permutation of size n.
     * @param n size of the permutation.
     * @return an array containing the indexes.
     */
    protected static int[] getBasePerm(int n) 
    {
        int[] perm = new int[n];
        for(int i = 0; i < n; i++)
            perm[i] = i;
        
        return perm;
    }

    /**
     * Select the next item to add.
     * @param user the target user of the recommendation.
     * @param remainingI elements outside the top k
     * @param compared the element we want to compare with the elements out of the top N.
     * @param list List of values for all the elements
     * @return the selected item if we have to swap elements, -1 if not.
     */
    protected int selectItem(U user, IntSortedSet remainingI, Tuple2od<I> compared, List<Tuple2od<I>> list)
    {
        double[] max = new double[]{this.valuetop(compared)};
        int[] bestI = new int[]{-1};
        
        remainingI.stream().mapToInt(i -> i).forEach(i ->
        {
            double value = this.value(list.get(i));
            if(isNaN(value)) value = Double.NEGATIVE_INFINITY;
            
            if(value > max[0] || (value == max[0] && i < bestI[0]))
            {
                max[0] = value;
                bestI[0] = i;
            }
        });
        
        return bestI[0];
    }
    
    /**
     * Updates the value of the global metric.
     * @param user The user whose recommendation we are reranking.
     * @param updated The new item.
     * @param old The old item.
     */
    protected abstract void update(U user, Tuple2od<I> updated, Tuple2od<I> old);
    
    /**
     * Computes the value for a item.
     * @param get the item whose value we want to compute
     * @return the value 
     */
    protected abstract double value(Tuple2od<I> get);
    
    /**
     * Computes the value for the top users.
     * @param get item between the top.
     * @return the value for the top users.
     */
    protected abstract double valuetop(Tuple2od<I> get);
    
}
