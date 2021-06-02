/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.content;

/**
 * User-created content.
 *
 * @param <T> Type of the content identifier
 * @param <C> Type of the content
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Content<T, C>
{
    /**
     * Content identifier
     */
    private final T contentId;
    /**
     * Content text.
     */
    private final C content;
    /**
     * Content timestamp
     */
    private final long timestamp;

    /**
     * Constructor.
     *
     * @param contentId the identifier
     * @param content   the text representation of the content
     * @param timestamp the creation timestamp.
     */
    public Content(T contentId, C content, long timestamp)
    {
        this.contentId = contentId;
        this.content = content;
        this.timestamp = timestamp;
    }

    /**
     * Gets the identifier of the content
     *
     * @return the content identifier.
     */
    public T getContentId()
    {
        return contentId;
    }

    /**
     * Gets the text representation of the content.
     *
     * @return the text representation of the content.
     */
    public C getContent()
    {
        return content;
    }

    /**
     * Gets the timestamp of the content.
     *
     * @return the timestamp of the content.
     */
    public long getTimestamp()
    {
        return timestamp;
    }


}
