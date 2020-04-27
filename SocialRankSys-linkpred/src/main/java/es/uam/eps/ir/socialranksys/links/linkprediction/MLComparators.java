/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.linkprediction;

import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.*;

/**
 * Comparators for ordering the link prediction algorithms.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public class MLComparators<U>
{   
    /**
     * Orders values in descending score ordering, and descending node position (top ranking is the greatest value)
     * @param <U> Type of the users
     * @param nodes List of nodes.
     * @return The comparator.
     */
    public static <U> Comparator<Tuple2od<Pair<U>>> descendingComparator(List<U> nodes)
    {
        List<U> n = new ArrayList<>(nodes);
        Collections.shuffle(n, new Random(0));
        
        return (Tuple2od<Pair<U>> t, Tuple2od<Pair<U>> t1) ->
        {
            double val = Double.compare(t1.v2, t.v2);
                        
            if(val == 0.0)
            {
                val = n.indexOf(t1.v1.v1()) - n.indexOf(t.v1.v1());
                if(val == 0.0)
                {
                    val = n.indexOf(t1.v1.v2()) - n.indexOf(t.v1.v2());
                }
            }

            return Double.compare(val, 0.0);
        };
    }
    
    /**
     * Orders values in descending score ordering, and descending node position (top ranking is the greatest value)
     * @param <U> Type of the users
     * @param nodes List of nodes.
     * @return The comparator.
     */
    public static <U> Comparator<Tuple2od<Pair<U>>> ascendingComparator(List<U> nodes)
    {
        List<U> n = new ArrayList<>(nodes);
        Collections.shuffle(n, new Random(0));
        
        return (Tuple2od<Pair<U>> t1, Tuple2od<Pair<U>> t) ->
        {
            double val = Double.compare(t1.v2, t.v2);
                        
            if(val == 0.0)
            {
                val = n.indexOf(t1.v1.v1()) - n.indexOf(t.v1.v1());
                if(val == 0.0)
                {
                    val = n.indexOf(t1.v1.v2()) - n.indexOf(t.v1.v2());
                }
            }

            return Double.compare(val, 0.0);
        };
    }
}
