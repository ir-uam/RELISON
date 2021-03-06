/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.global.globalranking;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.relison.links.recommendation.reranking.global.GlobalReranker;
import es.uam.eps.ir.relison.utils.datatypes.Tuple2oo;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.ranksys.core.util.tuples.Tuple2id;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Implementation of a greedy reranking strategy for optimizing global properties of the system.
 * This reranker chooses the items which have a more significant impact on the global property to optimize.
 *
 * Given a set of recommendations for different users, these rerankers iteratively choose the user-item
 * pair that maximizes a given objective function. A user can only be selected as long as the ranking
 * does not have enough items.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 * @param <I> type of the items
 */
public abstract class GlobalRankingGreedyReranker<U,I> implements GlobalReranker<U,I>
{
    /**
     * The number of recommended items that are reranked for each user.
     */
    protected final int cutOff;
    /**
     * Maximum score of an item.
     */
    private double maxScore;
    /**
     * User that achieves the maximum value.
     */
    private U maxUser;
    /**
     * Item that achieves the maximum score and its value.
     */
    private Tuple2id maxItemScore;
    
    /**
     * Constructor.
     * @param cutOff the cutoff.
     */
    public GlobalRankingGreedyReranker(int cutOff)
    {
        this.cutOff = cutOff;
    }
    
    @Override
    public Stream<Recommendation<U, I>> rerankRecommendations(Stream<Recommendation<U, I>> recommendation, int maxLength)
    {
        int numItems = Math.min(maxLength, cutOff);
        List<Recommendation<U,I>> recommendations = recommendation.collect(Collectors.toCollection(ArrayList::new));
        
        Map<U, List<Tuple2od<I>>> remaining = new HashMap<>();
        Map<U, Integer> max = new Object2IntOpenHashMap<>();
        Map<U, List<Tuple2od<I>>> output = new HashMap<>();
        
        recommendations.forEach((rec) -> 
        {
            output.put(rec.getUser(), new ArrayList<>());
            remaining.put(rec.getUser(), new ArrayList<>(rec.getItems()));
            max.put(rec.getUser(), Math.min(numItems, rec.getItems().size()));
        });
        
        // Do the reranking
        while(!remaining.isEmpty())
        {
            U maxScoreUser;
            Tuple2od<I> maxItemScore;

            // We select the next user-item pair to "add" to the system.
            Tuple2oo<U, Tuple2id> newRec = selectRecommendation(remaining);
            maxScoreUser = newRec.v1();
            Tuple2id idx = newRec.v2();

            maxItemScore = new Tuple2od<>(remaining.get(maxScoreUser).get(idx.v1).v1, idx.v2);
            Tuple2od<I> maxOutputScore = new Tuple2od<>(remaining.get(maxScoreUser).get(idx.v1).v1, cutOff - output.get(maxScoreUser).size() + 0.0);

            // We update the lists:
            // We remove the item from the items to explore for user maxScoreUser
            remaining.get(maxScoreUser).remove(idx.v1);
            // We reduce the number of remaining items in the user ranking.
            max.put(maxScoreUser, max.get(maxScoreUser)-1);
            // We add the item to the definitive ranking.
            output.get(maxScoreUser).add(maxOutputScore);

            // If we have completed the ranking for the user, we remove the remaining items to prevent them from
            // being selected.
            if(max.get(maxScoreUser).equals(0))
            {
                remaining.remove(maxScoreUser);
                max.remove(maxScoreUser);
            }
            
            this.update(maxScoreUser, maxItemScore);
        }       
        
        return output.entrySet().stream().map((entry) -> new Recommendation<>(entry.getKey(), entry.getValue()));
    }
    
    /**
     * Selects the next recommendation to add to the reranked one.
     * @param remainingItems the remaining items.
     * @return a tuple containing the recommendation to add.
     */
    protected Tuple2oo<U, Tuple2id> selectRecommendation(Map<U, List<Tuple2od<I>>> remainingItems)
    {
        maxUser = null;
        maxItemScore = null;
        maxScore = Double.NEGATIVE_INFINITY;       
        ReentrantLock lock = new ReentrantLock();
        
        remainingItems.entrySet().parallelStream().forEach(entry ->
            IntStream.range(0, entry.getValue().size()).forEach(i ->
            {
                Tuple2od<I> rec = entry.getValue().get(i);
                double score = this.score(entry.getKey(), rec);
                lock.lock();
                try
                {
                    if(this.maxScore < score)
                    {
                        this.maxScore = score;
                        this.maxItemScore = new Tuple2id(i, score);
                        this.maxUser = entry.getKey();
                    }
                }
                finally
                {
                    lock.unlock();
                }
            }));
        
        return new Tuple2oo<>(this.maxUser, this.maxItemScore);
    }
    
    /**
     * Computes the score of a recommendation item
     * @param user the user
     * @param item the original value.
     * @return the new score.
     */
    protected abstract double score(U user, Tuple2od<I> item);
    
    /**
     * Updates the value of the objective function after a selection.
     * @param user          the selected user.
     * @param selectedItem  the selected item and its score.
     */
    protected abstract void update(U user, Tuple2od<I> selectedItem);
}
