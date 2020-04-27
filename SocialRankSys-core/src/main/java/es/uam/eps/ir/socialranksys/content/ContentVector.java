/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.content;

import java.util.Map;

/**
 * Content information.
 * @author Javier Sanz-Cruzado Puig
 * @param <I> Type of the content identifier.
 */
public class ContentVector<I> 
{
    /**
     * Content identifier
     */
    private final I id;
    /**
     * Content vector.
     */
    private final TextVector vector;
    
    /**
     * Constructor
     * @param id content identifier
     * @param vector content vector
     */
    public ContentVector(I id, TextVector vector)
    {
        this.id = id;
        this.vector = vector;
    }
    
    /**
     * Constructor.
     * @param id content identifier.
     * @param vector map representation of a vector.
     */
    public ContentVector(I id, Map<String, Double> vector)
    {
        this.id = id;
        this.vector = new TextVector(vector);
    }
    
    /**
     * Obtains the content identifier
     * @return the content identifier
     */
    public I getId()
    {
        return id;
    }
    
    /**
     * Obtains the vector representation of the content
     * @return the vector representation of the content
     */
    public TextVector getVector()
    {
        return this.vector;
    }
}
