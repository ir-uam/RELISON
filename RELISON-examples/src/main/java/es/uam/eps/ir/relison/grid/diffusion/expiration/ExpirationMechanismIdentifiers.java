/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.expiration;

/**
 * Identifiers for the different expiration mechanisms for information propagation protocols available in 
 * the framework.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ExpirationMechanismIdentifiers 
{
    /**
     * Identifier of the non-expiration mechanism for information pieces.
     */
    public final static String INFINITETIME = "Infinite time";
    /**
     * Identifier of the expiration mechanism for information pieces that expires all the not propagated pieces.
     */
    public final static String ALLNOTPROP = "All not propagated";
    /**
     * Identifier of the expiration mechanism for information pieces that expires them after a while (fixed)
     */
    public final static String TIMED = "Timed";
    /**
     * Identifier of the expiration mechanism for information pieces where they have an exponential probability
     * of decaying.
     */
    public final static String EXPDECAY = "Exponential decay";
    /**
     * Identifier of the expiration mechanism for information pieces for all pieces which have not propagated in
     * real life scenarios.
     */
    public final static String ALLNOTREALPROP = "All not real propagated";
    /**
     * Identifier of the expiration mechanism for information pieces for all pieces which have not propagated in
     * real life scenarios, and expires those propagated after the corresponding timestamp.
     */
    public final static String ALLNOTREALPROPTIMESTAMP = "All not real propagated timestamp";
   
    
    /**
     * Prints the list of available algorithms
     */
    public static void printExpirationMechanismList()
    {
        System.out.println("Expiration Mechanisms:");
        System.out.println("\t" + INFINITETIME);
        System.out.println("\t" + ALLNOTPROP);
        System.out.println("\t" + TIMED);
        System.out.println("\t" + EXPDECAY);
        System.out.println("\t" + ALLNOTREALPROP);
        System.out.println("\t" + ALLNOTREALPROPTIMESTAMP);
    }
}
