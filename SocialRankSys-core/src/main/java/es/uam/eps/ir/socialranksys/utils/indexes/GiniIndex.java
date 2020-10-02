/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.utils.indexes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Computes the value of the Gini Index of a list of values.
 *
 * <p>
 * <b>Reference:</b> R. Dorfman. A formula for the Gini coefficient. The Review of Economics and Statistics 61(1), pp. 146-149 (1979)
 * </p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class GiniIndex
{
    /**
     * Given a list of values, it computes the value of the Gini index
     *
     * @param values The list of values.
     * @param sort   Indicates if the list of values has to be sorted.
     *
     * @return The value of the Gini index.
     */
    public double compute(List<Double> values, boolean sort)
    {
        List<Double> newValues = new ArrayList<>(values);
        if (newValues.size() <= 1)
        {
            return 0.0;
        }
        if (sort)
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
     *
     * @param values The stream containing all the values.
     * @param sort   Indicates if the list of values has to be sorted or not.
     *
     * @return The value of the Gini index
     */
    public double compute(Stream<Double> values, boolean sort)
    {
        return this.compute(values.collect(Collectors.toCollection((Supplier<ArrayList<Double>>) ArrayList::new)), sort);
    }

    /**
     * Computes the Gini coefficient for an unsorted list of values
     *
     * @param values the unsorted list of values
     *
     * @return the value of the Gini coefficient
     */
    private double computeUnsorted(List<Double> values)
    {
        List<Double> list = new ArrayList<>(values);
        Collections.sort(list);
        int numItems = values.size();
        double value = 0.0;
        double sum = 0.0;
        for (int i = 0; i < numItems; ++i)
        {
            value += (2 * (i + 1) - numItems - 1.0) * values.get(i);
            sum += values.get(i);
        }

        return value / (sum * (numItems - 1.0));

        /*

        Double2DoubleMap freqs = new Double2DoubleOpenHashMap();
        int numValues = values.size();

        double sum = 0.0;
        for(double d : values)
        {
            freqs.put(d, freqs.getOrDefault(d, 0.0) + 1.0);
            sum += d;
        }

        Set<Double> entries = new TreeSet<>();
        entries.addAll(freqs.keySet());

        double min;
        double max = 0;
        double value = 0.0;
        for(double key : entries)
        {
            min = max + 1.0;
            max = max + freqs.get(key);
            value += (max - min + 1)*(max+min - numValues - 1)*key;
        }

        value /= (sum*(numValues-1.0));
        return value;

       /* Double2DoubleMap freqs = new Double2DoubleOpenHashMap();
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
        return value;*/
    }

    /**
     * Computes the Gini coefficient for a sorted list of values
     *
     * @param values the sorted list of values
     *
     * @return the value of the Gini coefficient.
     */
    private double computeSorted(List<Double> values)
    {
        int numItems = values.size();
        double value = 0.0;
        double sum = 0.0;
        for (int i = 0; i < numItems; ++i)
        {
            value += (2 * (i + 1) - numItems - 1.0) * values.get(i);
            sum += values.get(i);
        }

        return value / (sum * (numItems - 1.0));
    }

    /**
     * Computes the value of the Gini Index
     *
     * @param values    The list of values
     * @param sort      Indicates if the list of values has to be sorted (true) or it
     *                  is already sorted (false)
     * @param numItems  Total number of items
     * @param sumValues Sum of the list of values
     *
     * @return The value of the Gini Index
     */
    public double compute(List<Double> values, boolean sort, long numItems, double sumValues)
    {
        if (numItems <= 1 || sumValues == 0.0)
        {
            return 0.0;
        }

        List<Double> newValues = new ArrayList<>(values);
        if (sort)
        {
            newValues.sort(Comparator.naturalOrder());
        }
        double value = 0.0;
        for (int i = 0; i < numItems; ++i)
        {
            value += (2 * (i + 1) - numItems - 1.0) * newValues.get(i) / (sumValues + 0.0);
        }

        return value / (numItems - 1.0);
    }

    /**
     * Computes the value of the Gini Index
     *
     * @param values    A stream of values
     * @param sort      Indicates if the list of values has to be sorted (true) or it
     *                  is already sorted (false)
     * @param numItems  Total number of items
     * @param sumValues Sum of the list of values
     *
     * @return The value of the Gini Index
     */
    public double compute(Stream<Double> values, boolean sort, long numItems, double sumValues)
    {
        if (numItems <= 1 || sumValues == 0.0)
        {
            return 0.0;
        }

        List<Double> listValues;
        if (sort)
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
