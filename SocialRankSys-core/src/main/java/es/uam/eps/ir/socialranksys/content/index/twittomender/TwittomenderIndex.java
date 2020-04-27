/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.content.index.twittomender;

import es.uam.eps.ir.socialranksys.content.ContentVector;
import es.uam.eps.ir.socialranksys.content.index.ContentIndexMode;
import es.uam.eps.ir.socialranksys.content.index.exceptions.WrongModeException;
import es.uam.eps.ir.socialranksys.content.index.weighting.WeightingScheme;
import es.uam.eps.ir.socialranksys.content.parsing.TextParser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Twittomender index. In this index, each document represents an item, and 
 * its content is comprised of the concatenation of all information pieces that
 * represent the user.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public abstract class TwittomenderIndex<U>
{
    /**
     * Route to the index
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
     * Constructor.
     * @param route Route to the index.
     * @param created Indicates if the index already exits (true) or not (false)
     */
    public TwittomenderIndex(String route, boolean created)
    {
        this.route = route;
        this.mode = ContentIndexMode.NONE;
        this.created = created;
    }
    
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
     * Obtains the contents of a single user from the index.
     * @param user The user identifier
     * @param model Scoring for the different terms for the user.
     * @return A content vector containing the terms if OK, null if not.
     * @throws WrongModeException If the index is not configured in read mode.
     */
    public abstract ContentVector<U> readUser(U user, WeightingScheme model) throws WrongModeException;
    
    /**
     * Computes the average length of the user vectors.
     * @return the average length of the user vectors.
     */
    public abstract double averageUserLength();
    
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
     * @param stopwords List of stopwords.
     * @return true if everything went OK, false if not.
     */
    public abstract boolean setWriteMode(List<String> stopwords);
    
    /**
     * Configures the index for writing.
     * @param stopwordFile File containing stopwords.
     * @return true if everything went OK, false if not.
     */
    public boolean setWriteMode(String stopwordFile)
    {
        List<String> stopwords = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(stopwordFile))))
        {
            br.lines().forEach(stopwords::add);
            return setWriteMode(stopwords);
        }
        catch(IOException ioe)
        {
            return false;
        }
    }
    
    /**
     * Configures the index for writing. The set of stopwords is the default set
     * of the index.
     * @return true if everything went OK, false if not.
     */
    public boolean setWriteMode()
    {
        return setWriteMode(new ArrayList<>());
    }
    
    /**
     * Writes an individual content in the index.
     * @param userId The user identifier.
     * @param content The content to store.
     * @param parser Parser to treat the text of the content.
     * @return true if everything went OK, false if not
     * @throws WrongModeException if the index is not configured in Write Mode
     */
    public abstract boolean writeContent(U userId, String content, TextParser parser) throws WrongModeException;
    
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
     * Frees all the available resources.
     */
    public abstract void close();
}
