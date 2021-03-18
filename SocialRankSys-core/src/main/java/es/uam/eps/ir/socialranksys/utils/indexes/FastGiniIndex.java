/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0.
 *
 */
package es.uam.eps.ir.socialranksys.utils.indexes;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntSortedMap;

import java.util.Map;

/**
 * Class for computing and updating the Gini index.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class FastGiniIndex
{
    /**
     * For each item in the collection, stores the number of times it has been recommended.
     */
    private final Int2LongMap frequencies;
    /**
     * The minimum indexes for the different possible frequency values.
     */
    private final Long2IntMap mins;
    /**
     * The maximum indexes for the different possible frequencies values.
     */
    private final Long2IntMap maxs;
    /**
     * The total number of items.
     */
    private final int numElements;
    /**
     * The sum of the frequencies of all items.
     */
    private double freqSum;
    /**
     * The main term of the Gini index.
     */
    private double numSum;

    /**
     * Constructor. Assumes that the initial values for frequencies are equal to 0.
     *
     * @param numElements the number of elements to consider.
     */
    public FastGiniIndex(int numElements)
    {
        // initialize:
        this.numElements = numElements;

        // Initialize the sums to zero:
        // The sum of all the frequencies.
        this.freqSum = 0.0;
        // The current value for the numerator of the Gini coefficient.
        this.numSum = 0.0;

        // Initialize the minimums and the maximums. As we do not have any additional
        // information, we only add the 0:
        this.mins = new Long2IntOpenHashMap();
        this.maxs = new Long2IntOpenHashMap();

        this.mins.put(0L, 1);
        this.maxs.put(0L, numElements);

        // Initialize the frequency values:
        this.frequencies = new Int2LongOpenHashMap();
        this.frequencies.defaultReturnValue(0L);
    }

    /**
     * Constructor. Uses an initial setting for the different values.
     *
     * @param numElements total number of items.
     * @param frequencies frequencies for the different item values.
     */
    public FastGiniIndex(int numElements, Map<Integer, Long> frequencies)
    {
        // Initialize the different variables.
        this.numElements = numElements;
        this.freqSum = 0.0;
        this.numSum = 0.0;

        // Initialize the maps for minimums and maximums
        this.mins = new Long2IntOpenHashMap();
        this.maxs = new Long2IntOpenHashMap();

        // Initialize the frequencies of the different items.
        this.frequencies = new Int2LongOpenHashMap();
        this.frequencies.defaultReturnValue(0L);

        // Fill the previously created structures with data.
        this.fillValues(frequencies);
    }

    /**
     * Obtains the current value of the Gini index.
     *
     * @return the current value: a number between 0 and 1 representing the proper value of the index,
     * NaN if the frequencies are all equal to zero, or there is less than one element in the collection.
     */
    public double getValue()
    {
        if (this.numElements <= 1)
        {
            return Double.NaN;
        }
        else if (this.freqSum == 0.0)
        {
            return Double.NaN;
        }
        else
        {
            return this.numSum / ((this.numElements - 1.0) * this.freqSum);
        }
    }

    /**
     * Updates the different variables for the Gini index, considering
     * a unit increment on the value.
     * @param idx the index of the element to increase in a unit.
     * @return true if everything went OK, false otherwise.
     */
    public boolean increaseFrequency(int idx)
    {
        if (idx < 0 || idx >= this.numElements)
        {
            return false;
        }

        long oldFreq = this.frequencies.getOrDefault(idx, this.frequencies.defaultReturnValue());
        long newFreq = oldFreq + 1;

        // First, we obtain the indexes:
        int minOldIndex = this.mins.get(oldFreq);
        int maxOldIndex = this.maxs.get(oldFreq);

        boolean delete = (minOldIndex == maxOldIndex);
        boolean add = (!this.mins.containsKey(newFreq));

        if(delete)
        {
            this.mins.remove(oldFreq);
            this.maxs.remove(oldFreq);
        }
        else
        {
            this.maxs.put(oldFreq, maxOldIndex-1);
        }

        this.mins.put(newFreq, maxOldIndex);
        if(add) this.maxs.put(newFreq, maxOldIndex);

        this.numSum += (2.0*maxOldIndex - numElements - 1.0);
        this.freqSum += 1.0;

        this.frequencies.put(idx, newFreq);

        return true;
    }

    /**
     * Updates the different variables for the Gini index, considering
     * a unit increment on the value. The value cannot descend 0.
     * @param idx the index of the element to increase in a unit.
     * @return true if everything went OK, false otherwise.
     */
    public boolean decreaseFrequency(int idx)
    {
        if (idx < 0 || idx >= this.numElements)
        {
            return false;
        }

        long oldFreq = this.frequencies.getOrDefault(idx, this.frequencies.defaultReturnValue());
        long newFreq = oldFreq - 1;
        if(newFreq < 0) return false;

        // First, we obtain the indexes:
        int minOldIndex = this.mins.get(oldFreq);
        int maxOldIndex = this.maxs.get(oldFreq);

        boolean delete = (minOldIndex == maxOldIndex);
        boolean add = (!this.mins.containsKey(newFreq));

        if(delete)
        {
            this.mins.remove(oldFreq);
            this.maxs.remove(oldFreq);
        }
        else
        {
            this.mins.put(oldFreq, minOldIndex+1);
        }

        this.maxs.put(newFreq, minOldIndex);
        if(add) this.mins.put(newFreq, minOldIndex);

        this.numSum += (-2.0*minOldIndex + numElements + 1.0);
        this.freqSum -= 1.0;

        this.frequencies.put(idx, newFreq);

        return true;
    }

    /**
     * Given a relation of items and frequencies, updates the values of the
     * items indicated in the relation. This method supposes that the CumulativeGini
     * object is empty.
     *
     * @param frequencies the relation between elements and its frequencies.
     */
    private void fillValues(Map<Integer, Long> frequencies)
    {
        Long2IntSortedMap counter = new Long2IntAVLTreeMap();
        counter.defaultReturnValue(0);
        frequencies.forEach((key, value1) ->
        {
            int item = key;
            long value = value1;

            if (item >= 0 && value > 0 && item < numElements)
            {
                this.frequencies.put(item, value);
                freqSum += value + 0.0;
                int count = counter.getOrDefault(value, counter.defaultReturnValue()) + 1;
                counter.put(value, count);
            }
        });

        int numElemsWithoutFreq = numElements - frequencies.size();
        if (numElemsWithoutFreq > 0)
        {
            this.mins.put(0L, 1);
            this.maxs.put(0L, numElemsWithoutFreq);
        }

        int currentIndex = numElemsWithoutFreq;
        for (long freq : counter.keySet())
        {
            int count = counter.get(freq);
            int min = currentIndex + 1;
            int max = currentIndex + count;

            this.mins.put(freq, currentIndex + 1);
            currentIndex += counter.get(freq);
            this.maxs.put(freq, currentIndex);
            numSum += freq * ((max - min + 1) * (max + min - numElements - 1));
        }
    }

    /**
     * Resets the metric to the state with no initial information.
     */
    public void reset()
    {
        this.maxs.clear();
        this.mins.clear();

        this.numSum = 0.0;
        this.freqSum = 0.0;
        this.frequencies.clear();

        this.maxs.put(0L, this.numElements);
        this.mins.put(0L, 1);
    }

    /**
     * Resets the metric to an initial state.
     *
     * @param frequencies the initial frequencies.
     */
    public void reset(Map<Integer, Long> frequencies)
    {
        this.maxs.clear();
        this.mins.clear();
        this.numSum = 0.0;
        this.freqSum = 0.0;
        this.frequencies.clear();

        this.fillValues(frequencies);
    }
}
