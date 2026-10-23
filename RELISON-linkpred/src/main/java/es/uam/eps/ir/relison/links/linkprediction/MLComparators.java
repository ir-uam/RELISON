/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.linkprediction;

import es.uam.eps.ir.relison.utils.datatypes.Pair;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.*;

/**
 * Comparators for ordering the link prediction algorithms.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 */
public class MLComparators<U>
{   
    /**
     * Sorts values in descending score order (top ranking is the greatest value)
     * @param <U> type of the users
     * @param nodes a list of nodes.
     * @return the comparator.
     */
    public static <U> Comparator<Tuple2od<Pair<U>>> descendingComparator(List<U> nodes)
    {
        Map<U, Integer> positions = shuffledPositions(nodes);
        
        return (Tuple2od<Pair<U>> t, Tuple2od<Pair<U>> t1) ->
        {
            double val = Double.compare(t1.v2, t.v2);
                        
            if(val == 0.0)
            {
                val = positions.getOrDefault(t1.v1.v1(), -1) - positions.getOrDefault(t.v1.v1(), -1);
                if(val == 0.0)
                {
                    val = positions.getOrDefault(t1.v1.v2(), -1) - positions.getOrDefault(t.v1.v2(), -1);
                }
            }

            return Double.compare(val, 0.0);
        };
    }

    /**
     * Sorts values in ascending score order (top ranking is the lowest value)
     * @param <U> type of the users
     * @param nodes a list of nodes.
     * @return the comparator.
     */
    public static <U> Comparator<Tuple2od<Pair<U>>> ascendingComparator(List<U> nodes)
    {
        Map<U, Integer> positions = shuffledPositions(nodes);
        
        return (Tuple2od<Pair<U>> t1, Tuple2od<Pair<U>> t) ->
        {
            double val = Double.compare(t1.v2, t.v2);
                        
            if(val == 0.0)
            {
                val = positions.getOrDefault(t1.v1.v1(), -1) - positions.getOrDefault(t.v1.v1(), -1);
                if(val == 0.0)
                {
                    val = positions.getOrDefault(t1.v1.v2(), -1) - positions.getOrDefault(t.v1.v2(), -1);
                }
            }

            return Double.compare(val, 0.0);
        };
    }

    /** Indexes the original seeded tie order once instead of scanning it per comparison. */
    private static <U> Map<U, Integer> shuffledPositions(List<U> nodes)
    {
        List<U> shuffled = new ArrayList<>(nodes);
        Collections.shuffle(shuffled, new Random(0));
        Map<U, Integer> positions = new HashMap<>();
        for (int i = 0; i < shuffled.size(); i++) positions.putIfAbsent(shuffled.get(i), i);
        return positions;
    }
}
