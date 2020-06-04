/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.metrics;

/**
 * Identifiers for the different information diffusion metrics available in 
 * the library
 * @author Javier Sanz-Cruzado Puig

 */
public class MetricIdentifiers 
{
    // Parameter metrics
    public final static String RECALL = "Feature Recall";
    public final static String NRECALL = "Normalized Feature Recall";    
    
    public final static String GINI = "Feature Gini";
    public final static String GLOBALGINI = "Global Feature Gini";
    public final static String GLOBALUSERGINI = "Global Feature User Gini";
    
    public final static String EXTRATE = "External Feature Rate";
    public final static String GLOBALEXTRATE = "Global External Feature Rate";

    public final static String EXTGINI = "External Feature Gini";
    public final static String EXTRECALL = "External Feature Recall";
    public final static String GLOBALEXTGINI = "Global External Feature Gini";
    
    public final static String ENTROPY = "Feature Entropy";
    public final static String GLOBALENTROPY = "Global Feature Entropy";
    public final static String GLOBALUSERENTROPY = "Global Feature User Entropy";
    
    public final static String KLD = "KLD";
    public final static String INVKLD = "Inverse KLD";
    public final static String GLOBALKLD = "Global KLD";
    public final static String GLOBALKLDINVERSE = "Global Inverse KLD";
    
    public final static String USERFEATURECOUNT = "User Feature Count";
    public final static String USERFEATUREGINI = "User Feature Gini";
    
    // MonteCarlo Metrics
    public final static String MONTECARLOGLOBALGINI = "MonteCarlo Global Feature Gini";
    public final static String MONTECARLOGLOBALUSERGINI = "MonteCarlo Global Feature User Gini";
    public final static String MONTECARLOUSERFEATUREGINI = "MonteCarlo User Feature Gini";
    
    // Information pieces metrics
    public final static String USERSPEED = "Average Speed";
    public final static String SPEED = "Speed";
    public final static String INFOGINI = "Information Gini";
    
    public final static String REALPROPRECALL = "Real Propagated Recall";
    public final static String GLOBALREALPROPRECALL = "Global Real Propagated Recall";
    // User metrics
    public final static String USERGLOBALGINI = "Global User Gini";
    public final static String USERRECALL = "User Recall";
    public final static String USERGLOBALENTROPY = "Global User Entropy";
    
    /**
     * Prints the list of available metrics
     */
    public static void printMetricList()
    {
        System.out.println("Metrics:");
        System.out.println("\tUser metrics:");
        System.out.println("\t\t" + USERGLOBALGINI);
        System.out.println("\t\t" + USERRECALL);
        System.out.println("\t\t" + USERGLOBALENTROPY);
        System.out.println("\tInformation pieces metrics:");
        System.out.println("\t\t" + USERSPEED);
        System.out.println("\t\t" + SPEED);
        System.out.println("\t\t" + INFOGINI);
        System.out.println("\t\t" + REALPROPRECALL);
        System.out.println("\t\t" + GLOBALREALPROPRECALL);
        System.out.println("\t\tParameter metrics:");
        System.out.println("\t\t" + RECALL);
        System.out.println("\t\t" + NRECALL);
        System.out.println("\t\t" + GINI);
        System.out.println("\t\t" + GLOBALGINI);
        System.out.println("\t\t" + GLOBALUSERGINI);
        System.out.println("\t\t" + ENTROPY);
        System.out.println("\t\t" + GLOBALENTROPY);
        System.out.println("\t\t" + GLOBALUSERENTROPY);
        System.out.println("\t\t" + EXTRATE);
        System.out.println("\t\t" + GLOBALEXTRATE);
        System.out.println("\t\t" + KLD);
        System.out.println("\t\t" + INVKLD);
        System.out.println("\t\t" + GLOBALKLD);
        System.out.println("\t\t" + GLOBALKLDINVERSE);
        System.out.println("\t\t" + MONTECARLOGLOBALGINI);
        System.out.println("\t\t" + MONTECARLOGLOBALUSERGINI);
        System.out.println("\t\t" + MONTECARLOUSERFEATUREGINI);
        System.out.println();
    }
}
