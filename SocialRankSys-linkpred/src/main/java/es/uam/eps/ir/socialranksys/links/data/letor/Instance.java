/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.letor;

import java.util.List;

/**
 * Machine learning individual pattern.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 *
 * @param <U> Type of the users of the graph.
 */
public class Instance<U>
{
    /**
     * Origin node.
     */
    private final U u;
    /**
     * Destination node.
     */
    private final U v;
    /**
     * Category.
     */
    private int category;
    /**
     * List of values.
     */
    private final List<Double> values;
    
    /**
     * Value to apply when the class is unknown or undefined.
     */
    public final static int ERROR_CLASS = -1;
    
    /**
     * Constructor.
     * @param u origin user.
     * @param v destination user.
     * @param values List that contains the values of the attributes.
     * @param category Class which the pattern belongs to..
     */
    public Instance(U u, U v, List<Double> values, int category)
    {
        this.u = u;
        this.v = v;
        this.values = values;
        this.category = category;
    }

    /**
     * Constructor.This is useful if you do not want to assign a class to the pattern.
     * @param u origin user.
     * @param v destination user.
     * @param values List that contains the values of the attributes.
     */
    public Instance(U u, U v, List<Double> values)
    {
        this(u,v,values, ERROR_CLASS);
    }

    
    /**
     * Gets the list of values for the different attributes in the pattern.
     * @return the list of attribute values.
     */
    public List<Double> getValues() {
        return values;
    }
    
    /**
     * Gets the value for a certain attribute.
     * @param attrId Index of the attribute
     * @return the value of the attribute.
     */
    public double getValue(int attrId)
    {
        if(attrId >= 0 && attrId < this.values.size())
        {
            return this.values.get(attrId);
        }
        else
        {
            return Double.NaN;
        }
    }

    /**
     * Gets the class of the pattern.
     * @return the class of the pattern.
     */
    public int getCategory() 
    {
        return category;
    }
    
    /**
     * Get the origin node.
     * @return the origin node.
     */
    public U getOrigin()
    {
        return u;
    }
    
    /**
     * Get the destination node.
     * @return the destination node.
     */
    public U getDest()
    {
        return v;
    }
}
