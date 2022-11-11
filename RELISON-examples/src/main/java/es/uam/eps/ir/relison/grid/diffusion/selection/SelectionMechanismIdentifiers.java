/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.selection;

/**
 * List of identifiers of the selection mechanisms which are available in the framework.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class SelectionMechanismIdentifiers 
{
    // Non stop simulators (until there is no more propagation)
    /**
     * Identifier for selecting all those pieces of information which were truly propagated
     * for their diffusion.
     */
    public final static String ALLREALPROP = "All real propagated";
    /**
     * Identifier for selecting pieces of information to propagate either from the recommended links
     * or from the original links (chosen with a probability)
     */
    public final static String PURERECBATCH = "Batch recommender";
    /**
     * Identifier for selecting a maximum number of those pieces of information which were truly propagated
     * for their diffusion.
     */
    public final static String COUNTREALPROP = "Count real propagated";
    /**
     * Identifier for selecting a fixed number of pieces for propagation.
     */
    public final static String COUNT = "Count";
    /**
     * Identifier for selecting to propagate pieces which have been received from more than a number of users.
     * Propagates all those pieces.
     */
    public final static String COUNTTHRESHOLD = "Count threshold";
    /**
     * Identifier for selecting to propagate pieces from other users with a certain probability depending
     * (only) on the sender and receiver.
     */
    public final static String ICM = "Independent cascade model";
    /**
     * Identifier for selecting to propagate pieces which have been received from more than a number of users.
     * Propagates a fix number of pieces from them.
     */
    public final static String LIMITEDCOUNTTHRESHOLD = "Limited count threshold";
    /**
     * Identifier for selecting to propagate pieces which have been received from more than a proportion of users
     * who send him content.
     * Propagates a fix number of pieces from them.
     */
    public final static String LIMITEDPROPTHRESHOLD = "Limited proportion threshold";
    /**
     * Identifier for selecting pieces to propagate according to timestamp, which can re-propagate received pieces
     * at a later time than originally.
     */
    public final static String LOOSETIMESTAMP = "Loose timestamp-based";
    /**
     * Identifier for selecting only pieces of information they have created.
     */
    public final static String ONLYOWN = "Only own";
    /**
     * Identifier for selecting to propagate pieces which have been received from more than a proportion of users
     * who send him content. Propagates all of those.
     */
    public final static String PROPORTIONTHRESHOLD = "Proportion threshold";
    /**
     * Identifier for selecting all the information the users know to propagate and a fixed number of their own pieces.
     */
    public final static String PUSHPULL = "Push-pull";
    /**
     * Identifier for selecting pieces of information to propagate which come from the recommended links. No pieces from other
     * users are propagated.
     */
    public final static String PUREREC = "Pure recommender";
    /**
     * Identifier for selecting pieces to propagate according to timestamp, which cannot re-propagate received pieces
     * at a later time than originally.
     */
    public final static String PURETIMESTAMP = "Pure timestamp-based";
    /**
     * Identifier for selecting pieces to propagate according to whether they come from recommended links or not.
     * A coin is tossed to determine from which list each of them is selected.
     */
    public final static String REC = "Recommender";
    /**
     * Identifier for selecting a fixed number of pieces for propagation, and selecting them according to their
     * timestamps.
     */
    public final static String TIMESTAMPORDERED = "Timestamp-ordered";
}
