/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.utils.indexes;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Computes the entropy of a series.
 * @author Javier Sanz-Cruzado Puig
 */
public class Entropy 
{
    
    /**
     * Computes the entropy of a series of values
     * @param values A list of values
     * @param sumValues The sum of the values
     * @return the entropy
     */
    public double compute(List<Double> values, double sumValues)
    {
        return this.compute(values.stream(), sumValues);
    }
    
    /**
     * Computes the entropy of a series of values
     * @param values A stream of values
     * @param sumValues The sum of the values
     * @return the entropy
     */
    public double compute(Stream<Double> values, double sumValues)
    {
        return -values.mapToDouble(value -> 
        {
            if(value == 0.0)
                return 0.0;
            else
                return value/sumValues * Math.log(value/sumValues)/Math.log(2.0);
        }).sum();
    }
    
    /**
     * Optimized version of the entropy, that allows repeated values.
     * @param map a map containing values as keys, and number of times a value is retrieved as values.
     * @return the entropy
     */
    public double compute(Map<Double, Integer> map)
    {
        double entropy = 0.0;
        double sum = 0.0;
        for(Double key : map.keySet())
        {
            sum += key*map.get(key);
            entropy += map.get(key)*(key*Math.log(key)/Math.log(2));
        }
        if(sum > 0.0)
            return Math.log(sum)/Math.log(2) - entropy/(sum + 0.0);
        return 0.0;
    }

}
