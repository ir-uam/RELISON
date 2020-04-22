/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.utils.indexes;

import it.unimi.dsi.fastutil.doubles.Double2DoubleMap;
import it.unimi.dsi.fastutil.doubles.Double2DoubleOpenHashMap;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Computes the value of the Gini Index of a list of values.
 * @author Javier Sanz-Cruzado Puig
 */
public class GiniIndex 
{
    /**
     * Given a list of values, it computes the value of the Gini index
     * @param values The list of values.
     * @param sort Indicates if the list of values has to be sorted.
     * @return The value of the Gini index.
     */
    public double compute(List<Double> values, boolean sort)
    {
        List<Double> newValues = new ArrayList<>(values);
        if(newValues.size() <= 1)
            return 0.0;
        if(sort)
        {
            return this.computeUnsorted(newValues);
        }
        else
        {
            return this.computeSorted(newValues);
        }
    }
    
    /**
     * Given a stream of values, it computes the value of the Gini index.
     * @param values The stream containing all the values.
     * @param sort Indicates if the list of values has to be sorted or not.
     * @return The value of the Gini index
     */
    public double compute(Stream<Double> values, boolean sort)
    {
        return this.compute(values.collect(Collectors.toCollection((Supplier<ArrayList<Double>>) ArrayList::new)), sort);
    }
    
    /**
     * Computes the Gini coefficient for an unsorted list of values
     * @param values the unsorted list of values
     * @return the value of the Gini coefficient
     */
    private double computeUnsorted(List<Double> values)
    {
        Double2DoubleMap freqs = new Double2DoubleOpenHashMap();
        int numValues = values.size();
        for(double d : values)
        {
            freqs.put(d, freqs.getOrDefault(d, 0.0) + 1.0);
        }
        
        Set<Entry<Double,Double>> entries = new TreeSet<>(Entry.comparingByKey());
        
        entries.addAll(freqs.double2DoubleEntrySet());
        double min;
        double max = 0;
        double value = 0.0;
        double sum = 0.0;
        for(Entry<Double, Double> entry : entries)
        {
            double key = entry.getKey();
            double v = entry.getValue();
            min = max+1;
            max = min+v - 1.0;
            value += ((max + min)*(max - min + 1) - v*(numValues + 1.0))*key;
            sum += v*key;
        }
        
        value /= (sum*(numValues - 1.0));
        return value;
    }
    
    /**
     * Computes the Gini coefficient for a sorted list of values
     * @param values the sorted list of values
     * @return the value of the Gini coefficient.
     */
    private double computeSorted(List<Double> values)
    {
        int numItems = values.size();
        double value = 0.0;
        double sum = 0.0;
        for(int i = 0; i < numItems; ++i)
        {
            value += (2*(i+1) - numItems - 1.0)*values.get(i);
            sum += values.get(i);
        }
        
        return value/(sum*(numItems - 1.0));
    }
    
    /**
     * Computes the value of the Gini Index
     * @param values The list of values
     * @param sort Indicates if the list of values has to be sorted (true) or it
     * is already sorted (false)
     * @param numItems Total number of items
     * @param sumValues Sum of the list of values
     * @return The value of the Gini Index
     */
    public double compute(List<Double> values, boolean sort, long numItems, double sumValues)
    {
        if(numItems <= 1 || sumValues == 0.0)
            return 0.0;
        
        List<Double> newValues = new ArrayList<>(values);
        if(sort)
        {
            newValues.sort(Comparator.naturalOrder());
        }
        double value = 0.0;
        for(int i = 0; i < numItems; ++i)
        {
            value += (2*(i+1) - numItems - 1.0)*newValues.get(i)/(sumValues + 0.0);
        }
        
        return value/(numItems - 1.0);
    }
    
    /**
     * Computes the value of the Gini Index
     * @param values A stream of values
     * @param sort Indicates if the list of values has to be sorted (true) or it
     * is already sorted (false)
     * @param numItems Total number of items
     * @param sumValues Sum of the list of values
     * @return The value of the Gini Index
     */
    public double compute(Stream<Double> values, boolean sort, long numItems, double sumValues)
    {
        if(numItems <= 1 || sumValues == 0.0)
            return 0.0;
        
        List<Double> listValues;
        if(sort)
        {
            listValues = values.sorted(Comparator.naturalOrder()).collect(Collectors.toCollection(ArrayList::new));
        }
        else
        {
            listValues = values.collect(Collectors.toCollection(ArrayList::new));
        }
        
        return this.compute(listValues, false, numItems, sumValues);
    }
}
