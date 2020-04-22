/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.utils.indexes;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

/**
 * Gini coefficient of a random distribution
 * @author Javier Sanz-Cruzado Puig
 */
public class MonteCarloGini 
{
    /**
     * Size of the population
     */
    private final int populationSize;
    /**
     * Total frequency (total income of the population)
     */
    private int totalFreq;
    /**
     * Main sum for the Gini coefficient
     */
    private double mainsum = 0.0;
    /**
     * Maximum frequency for each individual (maximum income of an individual)
     */
    private final int maxPerIndiv;
    /***
     * Auxiliar map for storing the frequency of each element in the population.
     */
    private final Map<Integer, Double> frequencies;
    /**
     * Auxiliar map for computing Gini coefficient (contains the minimum position for each frequency)
     */
    private final Map<Double, Integer> minimumPos;
    /**
     * Auxiliar map for computing Gini coefficient (contains the maximum position for each frequency)
     */
    private final Map<Double, Integer> maximumPos;
    /***
     * Auxiliar set containing the possible values
     */
    private final TreeSet<Double> values;
    
    /**
     * Constructor. 
     * @param populationSize the total size of the population
     * @param totalFreq the total frequency of the elements.
     * @param maxPerIndiv maximum frequency by individual (smaller or equal than 0 in case of no limits)
     */
    public MonteCarloGini(int populationSize, int totalFreq, int maxPerIndiv)
    {
        this.populationSize = populationSize;
        this.totalFreq = totalFreq;
        this.maxPerIndiv = maxPerIndiv;
        this.maximumPos = new HashMap<>();
        this.minimumPos = new HashMap<>();
        this.values = new TreeSet<>();
        this.frequencies = new HashMap<>();
        
        for(int i = 0; i < this.populationSize; ++i)
        {
            this.frequencies.put(i, 0.0);
        }
        this.values.add(0.0);
        this.minimumPos.put(0.0, 1);
        this.maximumPos.put(0.0, this.populationSize);
    }
    
    /**
     * Constructor. It does not set any constraint to the income of an individual.
     * @param populationSize the total size of the population.
     * @param totalFreq total frequency of the elements.
     */
    public MonteCarloGini(int populationSize, int totalFreq)
    {
        this(populationSize, totalFreq, 0);
        
        
    }
    
    /**
     * Obtains the Gini value.
     * @return the Gini value.
     */
    public double getValue()
    {
        if(this.totalFreq <= 0.0 || this.populationSize <= 1)
        {
            return Double.NaN;
        }
        else
        {
            return 1.0 - this.mainsum/(this.totalFreq * (this.populationSize - 1));
        }
    }
    
    /**
     * Updates the value of the Gini metric
     * @param numElems number of elements to add (increase in the total income).
     */
    public void update(int numElems)
    {
        Random rng = new Random();
        int limit = numElems;
        if(this.maxPerIndiv > 0 && this.totalFreq < this.populationSize*this.maxPerIndiv)
        {
            limit = Math.min(limit, this.populationSize*this.maxPerIndiv - this.totalFreq);
        }
        else if(this.maxPerIndiv > 0)
        {
            limit = 0;
        }
        

        this.totalFreq += limit;
        for(int i = 0; i < limit; ++i)
        {
            // Add one element to the comparison
            int select = rng.nextInt(this.populationSize);
            
            double oldfreq = frequencies.get(select);
            double newfreq = oldfreq + 1;
            double increase = this.increaseOld(oldfreq);
            
            Double cursor = oldfreq;
            do
            {
                cursor = this.values.higher(cursor);
                if(cursor != null && cursor < newfreq)
                {
                    increase += this.increaseIntermediate(cursor);
                }
            }
            while(cursor != null && cursor < newfreq);
            
            increase += this.increaseNew(newfreq);
            
            this.frequencies.put(select, newfreq);
            this.mainsum += increase;
        }
    }
    
    /**
     * Computes the increment for old frequencies.
     * @param oldfreq old frequency.
     * @return the variation for the old frequency.
     */
    private double increaseOld(double oldfreq) 
    {
        int oldMax = maximumPos.get(oldfreq);
        int oldMin = minimumPos.get(oldfreq);    
        

        if(oldMax <= oldMin) //It should only be greater or equal, but we add < for safety
        {
            maximumPos.remove(oldfreq);
            minimumPos.remove(oldfreq);
            this.values.remove(oldfreq);
        }
        else
        {
            maximumPos.put(oldfreq, oldMax-1);
        }
        
        return oldfreq*(this.populationSize + 1 - 2*oldMax);

        
    }
    
    /**
     * Computes the increment for new frequencies.
     * @param newfreq new frequency.
     * @return the variation for the new frequency.
     */
    private double increaseNew(double newfreq)
    {
        double increase;
        // Modify the corresponding values for the new frequency
        if(maximumPos.containsKey(newfreq))
        {
            int newMin = minimumPos.get(newfreq);

            increase = newfreq*(2*newMin - this.populationSize - 3);

            minimumPos.put(newfreq, newMin - 1);
        }
        else
        {
            double prev = this.values.lower(newfreq);

            int newMax = this.maximumPos.get(prev) + 1;
            int newMin = newMax;

            increase = newfreq*(2*newMax - this.populationSize - 1);

            minimumPos.put(newfreq, newMin);
            maximumPos.put(newfreq, newMax);
            this.values.add(newfreq);
        }
        
        return increase;
    }
    
    /**
     * Computes the increment for frequencies between old and new.
     * @param freq new frequency.
     * @return the variation for the new frequency.
     */
    private double increaseIntermediate(double freq)
    {
        int intMax = maximumPos.get(freq);
        int intMin = minimumPos.get(freq);

        maximumPos.put(freq, intMax-1);
        minimumPos.put(freq, intMin-1);
        
        return freq * (2*intMin - 2*intMax - 2);
    }
}
