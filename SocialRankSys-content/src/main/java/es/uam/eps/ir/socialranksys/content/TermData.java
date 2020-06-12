/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.content;

/**
 * Posting information for a term in a content.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the content identifiers.
 */
public class TermData<U,I> 
{
    /**
     * Content creator.
     */
    private final U creator;
    /**
     * Content identifier.
     */
    private final I contentId;
    /**
     * Creation timestamp.
     */
    private final long timestamp;
    /**
     * Score value
     */
    private final double score;

    /**
     * Constructor
     * @param creator content creator.
     * @param contentId content identifier.
     * @param timestamp creation timestamp.
     * @param score value of the term in the document.
     */
    public TermData(U creator, I contentId, long timestamp, double score) 
    {
        this.creator = creator;
        this.contentId = contentId;
        this.timestamp = timestamp;
        this.score = score;
    }

    /**
     * Gets the creator.
     * @return the creator.
     */
    public U getCreator() 
    {
        return creator;
    }

    /**
     * Gets the content identifier.
     * @return the content identifier.
     */
    public I getContentId() 
    {
        return contentId;
    }

    /**
     * Gets the creation timestamp of the content.
     * @return the creation timestamp.
     */
    public long getTimestamp() 
    {
        return timestamp;
    }
    
    /**
     * Gets the score of the represented term in the document.
     * @return the score of the represented term in the document.
     */
    public double getScore()
    {
        return score;
    }
    
    
}
