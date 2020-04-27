/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.content;

import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Vector whose dimensions are determined by a set of words.
 * @author Javier Sanz-Cruzado Puig
 */
public class TextVector 
{
    /**
     * The weights for the different terms.
     */
    private final Object2DoubleMap<String> terms;
    /**
     * The module of the vector.
     */
    private final double module;
    
    /**
     * Constructor.
     * @param terms The weights for the different terms.
     * @param module The module of the vector.
     */
    public TextVector(Map<String, Double> terms, double module)
    {
        this.terms = new Object2DoubleOpenHashMap<>(terms);
        this.terms.defaultReturnValue(0.0);
        this.module = module;
    }
    
    /**
     * Constructor.
     * @param terms The weights for the different terms in the vector.
     */
    public TextVector(Map<String, Double> terms)
    {
        this.terms = new Object2DoubleOpenHashMap<>(terms);
        this.terms.defaultReturnValue(0.0);
        this.module = Math.sqrt(terms.values().stream().mapToDouble(v -> v*v).sum());
    }
    
    /**
     * Constructor. Treats this vector as a centroid.
     * @param textVectors A stream containing several vectors to combine.
     * @param num The number of vectors to combine.
     */
    public TextVector(Stream<TextVector> textVectors, int num)
    {
        this.terms = new Object2DoubleOpenHashMap<>();
        this.terms.defaultReturnValue(0.0);
        textVectors.forEach(vector -> vector.terms.object2DoubleEntrySet().forEach(entry ->
        {
            String term = entry.getKey();
            double weight = entry.getDoubleValue() / (num + 0.0);

            this.terms.put(term, this.terms.getOrDefault(term, this.terms.defaultReturnValue()) + weight);
        }));
        
        this.module = Math.sqrt(terms.values().stream().mapToDouble(v -> v*v).sum());
    }
    
    /**
     * Obtains the weight for an individual term if the vector contains it, 0.0 if not.
     * @param term The term.
     * @return the weight for the term
     */
    public double getWeight(String term)
    {
        return this.terms.getOrDefault(term, this.terms.defaultReturnValue());
    }
    
    /**
     * Obtains the module of the vector.
     * @return the module of the vector.
     */
    public double getModule()
    {
        return this.module;
    }
    
    /**
     * Gets all the terms with a value.
     * @return a stream containing all terms with a value
     */
    public Stream<String> getTerms()
    {
        return this.terms.keySet().stream();
    }
    
    /**
     * Gets the vector.
     * @return a stream containing all possible term/value pairs.
     */
    public Stream<Tuple2oo<String, Double>> getVector()
    {
        return this.terms.object2DoubleEntrySet().stream().map(entry -> new Tuple2oo<>(entry.getKey(), entry.getDoubleValue()));
    }
    
    /**
     * Computes the scalar product of two vectors.
     * @param vector the vectors.
     * @return the scalar product.
     */
    public double scalarProduct(TextVector vector)
    {
        TextVector smaller;
        TextVector bigger;
        
        // If a vector does not contain any term, then, the dot product is 0
        if(this.getModule() == 0 || vector.getModule() == 0)
        {
            return 0.0;
        }
        
        // Select the smaller vector (to iterate over it)
        if(this.terms.size() < vector.terms.size())
        {
            smaller = this;
            bigger = vector;
        }
        else
        {
            smaller = vector;
            bigger = this;
        }
        
        return smaller.terms.object2DoubleEntrySet().stream().mapToDouble(entry ->
        {
            String term = entry.getKey();
            double weight = entry.getDoubleValue();
            
            return weight*bigger.getWeight(term);
        }).sum();
    }
    
    /**
     * Sums this vector with another one.
     * @param vector the vector.
     * @return the sum of two vectors.
     */
    public TextVector sum(TextVector vector)
    {
        Object2DoubleMap<String> map = new Object2DoubleOpenHashMap<>();
        Set<String> set = new HashSet<>(this.terms.keySet());
        set.addAll(vector.terms.keySet());
        
        for(String term : set)
        {
            map.put(term, this.getWeight(term) + vector.getWeight(term));
        }
        
        return new TextVector(map);
    }
    
    /**
     * Multiplies the vector by a number.
     * @param num the scalar value.
     * @return the resulting vector.
     */
    public TextVector multiply(double num)
    {
        Object2DoubleMap<String> map = new Object2DoubleOpenHashMap<>();
        for(Entry<String, Double> entry : this.terms.object2DoubleEntrySet())
        {
            map.put(entry.getKey(), entry.getValue()*num);
        }
        
        return new TextVector(map);
    }
}
