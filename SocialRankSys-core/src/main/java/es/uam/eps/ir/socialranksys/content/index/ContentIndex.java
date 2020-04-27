/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.content.index;

import es.uam.eps.ir.socialranksys.content.Content;
import es.uam.eps.ir.socialranksys.content.ContentVector;
import es.uam.eps.ir.socialranksys.content.TermData;
import es.uam.eps.ir.socialranksys.content.index.exceptions.WrongModeException;
import es.uam.eps.ir.socialranksys.content.index.weighting.WeightingScheme;
import es.uam.eps.ir.socialranksys.content.parsing.TextParser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract IR index for storing text user-created contents.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the user identifiers.
 * @param <I> Type of the content identifiers.
 */
public abstract class ContentIndex<U,I> 
{
    /**
     * Route to the index.
     */
    private final String route;
    
    /**
     * Configuration mode for the index.
     */
    private ContentIndexMode mode;
    
    /**
     * Indicates if the index has already been created or not.
     */
    private boolean created;
   
    /**
     * List of stopwords
     */
    private final List<String> stopwords;
    /**
     * Constructor.
     * @param route Route to the index. 
     * @param created Indicates if the index already exists (true) or not (false)
     */
    public ContentIndex(String route, boolean created)
    {
        this.route = route;
        this.mode = ContentIndexMode.NONE;
        this.created = created;
        this.stopwords = new ArrayList<>();
    }
    
    /**
     * Constructor.
     * @param route Route to the index. 
     * @param created Indicates if the index already exists (true) or not (false)
     * @param stopwords List of stopwords
     */
    public ContentIndex(String route, boolean created, List<String> stopwords)
    {
        this.route = route;
        this.mode = ContentIndexMode.NONE;
        this.created = created;
        this.stopwords = stopwords;
    }
    
    /**
     * Constructor.
     * @param route Route to the index. 
     * @param created Indicates if the index already exists (true) or not (false)
     * @param stopwordsFile Route to a file containing the stopwords.
     */
    public ContentIndex(String route, boolean created, String stopwordsFile)
    {
        this.stopwords = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(stopwordsFile))))
        {
            br.lines().forEach(stopwords::add);
        }
        catch(IOException ioe)
        {
            this.stopwords.clear();
        }
        
        this.route = route;
        this.mode = ContentIndexMode.NONE;
        this.created = created;
    }
    
    // READ FUNCTIONS **********************************************************/
    /**
     * Configures the index for reading.
     * @return true if everything went OK, false if not.
     */
    public abstract boolean setReadMode();
    
    /**
     * Indicates if the current mode is Read Mode
     * @return true if the index is configured in Read Mode
     */
    public boolean isReadModeSet()
    {
        return mode.equals(ContentIndexMode.READ);
    }
    
    /**
     * Obtain a single content from the index.
     * @param contentId Content identifier
     * @param model Scoring for the different terms in the content
     * @return A content vector containing the terms if OK, false if not
     * @throws WrongModeException if the index is not configured in Read Mode
     */
    public abstract ContentVector<I> readContent(I contentId, WeightingScheme model) throws WrongModeException;
    
    /**
     * Obtains the vectors of a single user.
     * @param userId user identifier
     * @param model retrieval model
     * @return A map containing the term vectors for each content published by the user before the given date.
     * @throws WrongModeException if the index is not configured in Read Mode
     */
    public abstract Map<I, ContentVector<I>> readUser(U userId, WeightingScheme model) throws WrongModeException;
    
    /**
     * Gets all the possible terms in the content
     * @return a stream containing all the possible terms.
     * @throws WrongModeException if the index is not properly configured
     */
    public abstract Stream<String> getAllTerms() throws WrongModeException;
    
    
    
    /**
     * Obtains the vectors of a single user, created in a certain time interval.
     * @param userId user identifier.
     * @param minTimestamp the minimum date.
     * @param maxTimestamp the maximum date.
     * @param model retrieval model.
     * @return A map containing the term vectors for each content published by the user before the given date.
     * @throws WrongModeException if the index is not configured in Read Mode
     */
    public abstract Map<I, ContentVector<I>> readUserDateRange(U userId, long minTimestamp, long maxTimestamp, WeightingScheme model) throws WrongModeException;
    
    /**
     * Obtains the vectors of a single user, created before a certain date.
     * @param userId user identifier.
     * @param maxTimestamp the maximum date.
     * @param model retrieval model.
     * @return A map containing the term vectors for each content published by the user before the given date.
     * @throws WrongModeException if the index is not configured in Read Mode
     */
    public Map<I, ContentVector<I>> readUserMaxDate(U userId, long maxTimestamp, WeightingScheme model) throws WrongModeException
    {
        return this.readUserDateRange(userId, 0, maxTimestamp, model);
    }
    
    /**
     * Obtains the vectors of a single user, created after a certain date.
     * @param userId user identifier.
     * @param minTimestamp the minimum date.
     * @param model retrieval model.
     * @return A map containing the term vectors for each content published by the user before the given date.
     * @throws WrongModeException if the index is not configured in Read Mode
     */
    public Map<I, ContentVector<I>> readUserMinDate(U userId, long minTimestamp, WeightingScheme model) throws WrongModeException
    {
        return this.readUserDateRange(userId, minTimestamp, Long.MAX_VALUE, model);
    }
    
    /**
     * Obtains the vectors created in a certain time interval.
     * @param minTimestamp the minimum date.
     * @param maxTimestamp the maximum date.
     * @param model retrieval model.
     * @return A map containing the term vectors for each content published by the user before the given date.
     * @throws WrongModeException if the index is not configured in Read Mode
     */
    public abstract Map<U,Map<I,ContentVector<I>>> readDateRange(long minTimestamp, long maxTimestamp, WeightingScheme model) throws WrongModeException;
    
    /**
     * Obtains the vectors created before a certain date.
     * @param maxTimestamp the maximum date.
     * @param model retrieval model.
     * @return A map containing the term vectors for each content published by the user before the given date.
     * @throws WrongModeException if the index is not configured in Read Mode
     */
    public Map<U,Map<I,ContentVector<I>>> readPreviousDateRange(long maxTimestamp, WeightingScheme model) throws WrongModeException
    {
        return this.readDateRange(0, maxTimestamp, model);
    }
    
    /**
     * Obtains the vectors created after a certain date.
     * @param minTimestamp the minimum date.
     * @param model retrieval model.
     * @return A map containing the term vectors for each content published by the user before the given date.
     * @throws WrongModeException if the index is not configured in Read Mode
     */
    public Map<U,Map<I,ContentVector<I>>> readPosteriorDateRange(long minTimestamp, WeightingScheme model) throws WrongModeException
    {
        return this.readDateRange(minTimestamp, Long.MAX_VALUE, model);
    }
    
    /**
     * Obtains all the contents that contain a certain term.
     * @param term The term to search
     * @param model Weighting scheme.
     * @return An stream containing data (empty if the data does not exist or a problem occurs)
     * @throws WrongModeException if the index is not configured in Write mode.
     */
    public abstract Stream<TermData<U,I>> getContents(String term, WeightingScheme model) throws WrongModeException;
    
    /**
     * Computes the average length of the contents
     * @return the average length of the contents.
     */
    public abstract double averageDocLength();
    
    // WRITE FUNCTIONS *********************************************************/
    
    /**
     * Indicates if the current mode is Write Mode
     * @return true if the index is configured in Write Mode
     */
    public boolean isWriteModeSet()
    {
        return mode.equals(ContentIndexMode.WRITE);
    }
    
    /**
     * Configures the index for writing.
     * @return true if everything went OK, false if not.
     */
    public abstract boolean setWriteMode();
    
    /**
     * Writes an individual content in the index.
     * @param user User identifier.
     * @param content The content to store.
     * @param parser Parser to treat the text of the content.
     * @return true if everything went OK, false if not
     * @throws WrongModeException if the index is not configured in Write Mode
     */
    public abstract boolean writeContent(U user, Content<I, String> content, TextParser parser) throws WrongModeException;
    
    /**
     * Writes all the contents published by an individual user in the index
     * @param user User identifier
     * @param content The contents to store
     * @param parser Parser to treat the text of each content.
     * @return true if everything went OK, false if not.
     * @throws WrongModeException if the index is not configured in Write Mode
     */
    public boolean writeUser(U user, Stream<Content<I, String>> content, TextParser parser) throws WrongModeException
    {
        if(!this.isWriteModeSet())
            throw new WrongModeException("Index is configured in " + this.mode + " mode, instead of " + ContentIndexMode.WRITE + " mode.");
        
        List<Content<I, String>> contents = content.collect(Collectors.toList());
        
        for(Content<I, String> c : contents)
        {
            if(!this.writeContent(user, c, parser))
                return false; 
        }
        return true;
    }

    /**
     * Obtains the index route.
     * @return the index route.
     */
    public String getRoute() 
    {
        return route;
    }

    /**
     * Obtains the current index mode.
     * @return the current index mode. 
     */
    public ContentIndexMode getMode() 
    {
        return mode;
    }

    /**
     * Indicates if the index has been previously created or not.
     * @return true if it has been created, false if not.
     */
    public boolean isCreated() 
    {
        return created;
    }

    /**
     * Changes the configuration mode.
     * @param mode the new mode
     */
    protected void setMode(ContentIndexMode mode) 
    {
        this.mode = mode;
    }

    /**
     * Changes the value that indicates if the index has been previously created.
     * @param created the new value.
     */
    protected void setCreated(boolean created) 
    {
        this.created = created;
    }
    
    /**
     * Closes all the resources related to the index.
     */
    public abstract void close();
    
    /**
     * Gets the stopwords used in this index.
     * @return the list of stopwords.
     */
    protected List<String> getStopwords()
    {
        return this.stopwords;
    }
    
    
}
