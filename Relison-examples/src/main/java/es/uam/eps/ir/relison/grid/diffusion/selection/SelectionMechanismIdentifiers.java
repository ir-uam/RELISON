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
    public final static String ALLREALPROP = "All real propagated";
    public final static String PURERECBATCH = "Batch recommender";
    public final static String COUNTREALPROP = "Count real propagated";
    public final static String COUNT = "Count";
    public final static String COUNTTHRESHOLD = "Count threshold";
    public final static String ICM = "Independent cascade model";
    public final static String LIMITEDCOUNTTHRESHOLD = "Limited count threshold";
    public final static String LIMITEDPROPTHRESHOLD = "Limited proportion threshold";
    public final static String LOOSETIMESTAMP = "Loose timestamp-based";
    public final static String ONLYOWN = "Only own";
    public final static String PROPORTIONTHRESHOLD = "Proportion threshold";
    public final static String PUSHPULL = "Push-pull";
    public final static String PUREREC = "Pure recommender";
    public final static String PURETIMESTAMP = "Pure timestamp-based";
    public final static String REC = "Recommender";
    public final static String TIMESTAMPORDERED = "Timestamp-ordered";
}
