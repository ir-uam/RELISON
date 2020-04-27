/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.letor;

import es.uam.eps.ir.ranksys.core.util.Stats;
import it.unimi.dsi.fastutil.doubles.Double2IntMap;
import it.unimi.dsi.fastutil.doubles.Double2IntOpenHashMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Statistics values for nominal features.
 * @author Javier Sanz-Cruzado Puig
 */
public class NominalStats extends Stats
{
    /**
     * Counts the number of values for each nominal value.
     */
    private final List<Long> counter;
    /**
     * Maps doubles to integers.
     */
    private final Double2IntMap index;
    /**
     * Maps integers to doubles.
     */
    private final List<Double> list;
    /**
     * Number of different values for the attribute.
     */
    private int numValues;
    
    /**
     * Constructor.
     */
    public NominalStats()
    {
        super();
        this.counter = new ArrayList<>();
        this.index = new Double2IntOpenHashMap();
        this.index.defaultReturnValue(-1);
        this.list = new ArrayList<>();
        this.numValues = 0;
    }
    
    @Override
    public void accept(double value)
    {
        // Update all statistics
        super.accept(value);
        
        // Add the nominal value stats.
        int next = index.getOrDefault(value, index.defaultReturnValue());
        if(next == index.defaultReturnValue())
        {
            next = index.size();
            list.add(value);
            index.put(value, next);
            numValues++;
            counter.add(1L);
        }
        else
        {
            counter.set(next, counter.get(next) + 1L);
        }
    }
    
    @Override
    public void combine(Stats stats)
    {
        if(stats.getClass() == this.getClass())
        {
            super.combine(stats);
            NominalStats nomStats = (NominalStats) stats;
            
            nomStats.list.forEach(val -> 
            {
                int idx = this.index.getOrDefault(val.intValue(), index.defaultReturnValue());
                if(idx == index.defaultReturnValue())
                {
                    idx = this.list.size();
                    list.add(val);
                    index.put(val.doubleValue(), idx);
                    numValues++;
                    counter.add(nomStats.getCount(val));
                }
                else
                {
                    counter.set(idx, counter.get(idx) + nomStats.getCount(val));
                }
            });
            
        }
    }
    
    @Override
    public void reset()
    {
        super.reset();
        this.counter.clear();
        this.index.clear();
        this.list.clear();
        this.numValues=0;
    }
 
    /**
     * Obtains the number of different values for the feature.
     * @return the number of different values for the feature.
     */
    public int getNumValues()
    {
        return this.numValues;
    }
        
    /**
     * Obtains the different possible values for the features.
     * @return the values for the features.
     */
    public List<Double> getValues()
    {
        return this.list;
    }
    
    /**
     * Obtains the value of the i-th nominal value.
     * @param idx index for the value.
     * @return the value if it exists, NaN otherwise
     */
    public double getValue(int idx)
    {
        if(idx >= 0 && idx < numValues)
            return this.list.get(idx);
        else
            return Double.NaN;
    }
    
    /**
     * Obtains the index for some value.
     * @param value the value.
     * @return the index if it exists, -1 otherwise.
     */
    public int indexOfValue(double value)
    {
        return this.index.getOrDefault(value, index.defaultReturnValue());
    }
    
    /**
     * Obtains the number of times that the i-th nominal value has appeared
     * in the collection.
     * @param idx the index of the nominal value.
     * @return the index of the nominal value.
     */
    public long getCount(int idx)
    {
        if(idx < 0 || idx >= numValues) return 0L;
        return counter.get(idx);
    }
    
    /**
     * Obtains the number of elements for some nominal value.
     * @param value the nominal value.
     * @return the numbe of elements for that value.
     */
    public long getCount(double value)
    {
        int idx = index.getOrDefault(value, index.defaultReturnValue());
        if(idx == index.defaultReturnValue()) return 0L;
        return counter.get(idx);
    }
    
    /**
     * Obtains the proportion of the examples which have the i-th nominal
     * value for the corresponding parameter.
     * @param idx the index of the nominal value.
     * @return the corresponding proportion.
     */
    public double getProportion(int idx)
    {
        if(idx < 0 || idx >= numValues || this.getN() == 0) return 0.0;
        return (this.getCount(idx)+0.0)/(this.getN() + 0.0);
    }
    
    /**
     * Obtains the proportion of the examples which have the given nominal
     * value for the corresponding parameter.
     * @param value the nominal value.
     * @return the corresponding proportion, NaN if something failed.
     */
    public double getProportion(double value)
    {
        return (this.getCount(value)+0.0)/(this.getN() + 0.0);
    }
}
