/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna.comm.global;

/**
 * Identifiers for edge metrics.
 * @author Javier Sanz-Cruzado Puig
 */
public class GlobalCommunityMetricIdentifiers 
{
    // Degree Gini

    /**
     * Identifier for the inter-community degree Gini metric.
     */
    public static final String INTERCOMMUNITYDEGREEGINI = "Inter-community degree Gini";
    /**
     * Identifier for the complete community degree Gini metric.
     */
    public static final String COMPLETECOMMUNITYDEGREEGINI = "Complete community degree Gini";
    // Size Normalized Degree Gini
    /**
     * Identifier for the size-normalized inter-community degree Gini metric.
     */
    public static final String SIZENORMINTERCOMMUNITYDEGREEGINI = "Size-normalized Inter-Community Degree Gini";
    /**
     * Identifier for the size-normalized complete community degree Gini metric.
     */
    public static final String SIZENORMCOMPLETECOMMUNITYDEGREEGINI = "Size-normalized Complete Community Degree Gini";
    // Edge Gini
    /**
     * Identifier for the inter-community edge Gini metric.
     */
    public static final String INTERCOMMUNITYEDGEGINI = "Inter-community edge Gini complement";
    /**
     * Identifier for the complete community edge Gini metric.
     */
    public static final String COMPLETECOMMUNITYEDGEGINI = "Complete community edge Gini complement";
    /**
     * Identifier for the semi-complete community edge Gini metric.
     */
    public static final String SEMICOMPLETECOMMUNITYEDGEGINI = "Semi-complete community edge Gini complement";
    // Size Normalized Gini
    /**
     * Identifier for the size-normalized inter-community degree Gini metric.
     */
    public static final String SIZENORMINTERCOMMUNITYEDGEGINI = "Size-normalized Inter-Community Edge Gini";
    /**
     * Identifier for the size-normalized complete community degree Gini metric.
     */
    public static final String SIZENORMCOMPLETECOMMUNITYEDGEGINI = "Size-normalized Complete Community Edge Gini";
    /**
     * Identifier for the size-normalized semi-complete community degree Gini metric.
     */
    public static final String SIZENORMSEMICOMPLETECOMMUNITYEDGEGINI = "Size-normalized Semi-Complete Community Edge Gini";
    // Dice Normalized Gini
    /**
     * Identifier for the Dice inter-community edge Gini metric.
     */
    public static final String DICEINTERCOMMUNITYEDGEGINI = "Dice Inter-Community Edge Gini";
    /**
     * Identifier for the Dice complete community edge Gini metric.
     */
    public static final String DICECOMPLETECOMMUNITYEDGEGINI = "Dice Complete Community Edge Gini";
    /**
     * Identifier for the Dice semi-complete community edge Gini metric.
     */
    public static final String DICESEMICOMPLETECOMMUNITYEDGEGINI = "Dice Semi-Complete Community Edge Gini";
    // Other metrics

    /**
     * Identifier for the number of communities.
     */
    public static final String NUMCOMMS = "Num. communities";
    /**
     * Identifier for the modularity metric.
     */
    public static final String MODULARITY = "Modularity";
    /**
     * Identifier for the modularity complement metric.
     */
    public static final String MODULARITYCOMPL = "Modularity complement";
    /**
     * Identifier for the number of weak ties metric.
     */
    public static final String WEAKTIES = "Weak ties";
    /**
     * Identifier for the destiny community size metric.
     */
    public static final String COMMDESTSIZE = "Destiny community size";
    /**
     * Identifier for the community size Gini metric.
     */
    public static final String COMMSIZEGINI = "Community Size Gini";
    
}
