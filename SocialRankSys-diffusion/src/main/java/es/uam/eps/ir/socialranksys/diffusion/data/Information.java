/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.data;

import java.io.Serializable;
import java.util.Objects;

/**
 * Piece of information.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <I> type of the information id
 */
public class Information<I> implements Serializable, Cloneable
{
    /**
     * Unique identifier of a piece of information.
     */
    private final I infoId;
    /**
     * Time of creation of the information / Time of arrival.
     */
    private long timestamp;
    
    /**
     * Creates a basic piece of information.
     * @param infoId Information id.
     * @param timestamp Time of creation of the information / Time of arrival.
     */
    public Information(I infoId, long timestamp)
    {
        this.infoId = infoId;
        this.timestamp = timestamp;
    }

    /**
     * Obtains the information identifier.
     * @return The identifier of the information.
     */
    public I getInfoId()
    {
        return infoId;
    }

    /**
     * Obtains the timestamp of the piece.
     * @return The timestamp.
     */
    public long getTimestamp()
    {
        return timestamp;
    }
    
    /**
     * Modifies the timestamp of the piece.
     * @param timestamp the new timestamp.
     */
    public void setTimestamp(long timestamp)
    {
        this.timestamp = timestamp;
    }

    /**
     * Checks if two elements are equal. It only compares the information piece
     * identifier.
     * @param object the object to check equality with.
     * @return true if the object is an information piece and the ids are equal, false if not.
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object object)
    {
        if(object.getClass() != this.getClass())
        {
            return false;
        }
        Information<I> info = (Information<I>) object;
        return this.infoId.equals(info.getInfoId());
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 17 * hash + Objects.hashCode(this.infoId);
        return hash;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Information<I> clone() throws CloneNotSupportedException
    {
        return (Information<I>) super.clone();
    }
}
