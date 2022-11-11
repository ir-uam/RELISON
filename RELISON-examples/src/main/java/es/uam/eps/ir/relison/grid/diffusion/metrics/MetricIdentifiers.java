/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.metrics;

/**
 * The list of identifiers for the information diffusion metrics available.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class MetricIdentifiers 
{
    // Parameter metrics
    /**
     * Identifier for the feature recall metric.
     */
    public final static String RECALL = "Feature recall";
    /**
     * Identifier for the individual feature Gini complement metric.
     */
    public final static String GINI = "Individual feature Gini complement";
    /**
     * Identifier for the global feature Gini complement metric.
     */
    public final static String GLOBALGINI = "Global feature Gini complement";
    /**
     * Identifier for the global feature user Gini complement.
     */
    public final static String GLOBALUSERGINI = "Global feature user Gini complement";

    // External features
    /**
     * Identifier for the individual external feature rate metric.
     */
    public final static String EXTRATE = "Individual external feature rate";
    /**
     * Identifier for the global external feature rate metric.
     */
    public final static String GLOBALEXTRATE = "Global external feature rate";
    /**
     * Identifier for the external feature recall metric.
     */
    public final static String EXTRECALL = "External feature recall";
    /**
     * Identifier for the individual external feature Gini complement metric.
     */
    public final static String EXTGINI = "Individual external feature Gini complement";
    /**
     * Identifier for the global external feature Gini complement metric.
     */
    public final static String GLOBALEXTGINI = "Global external feature Gini complement";
    /**
     * Identifier for the feature Entropy metric.
     */
    public final static String ENTROPY = "Feature entropy";
    /**
     * Identifier for the global feature Entropy metric.
     */
    public final static String GLOBALENTROPY = "Global feature entropy";
    /**
     * Identifier for the global feature user Entropy metric.
     */
    public final static String GLOBALUSERENTROPY = "Global feature user entropy";
    /**
     * Identifier for the individual feature KL divergence metric.
     */
    public final static String KLD = "Individual feature KLD";
    /**
     * Identifier for the global feature KL divergence metric.
     */
    public final static String GLOBALKLD = "Global feature KLD";
    /**
     * Identifier for the user-feature count metric.
     */
    public final static String USERFEATURECOUNT = "User-feature count";
    /**
     * Identifier for the user-feature Gini complement metric.
     */
    public final static String USERFEATUREGINI = "User-feature Gini complement";

    // Information pieces metrics
    /**
     * Identifier for the user speed (information piece count) metric.
     */
    public final static String USERSPEED = "Information count";
    /**
     * Identifier for the speed metric.
     */
    public final static String SPEED = "Speed";
    /**
     * Identifier for the information Gini complement.
     */
    public final static String INFOGINI = "Information Gini complement";
    /**
     * Identifier for the individual real propagated recall metric.
     */
    public final static String REALPROPRECALL = "Individual real propagated recall";
    /**
     * Identifier for the global real propagated recall metric.
     */
    public final static String GLOBALREALPROPRECALL = "Global real propagated recall";
    // User metrics

    /**
     * Identifier for the global creator Gini complement metric.
     */
    public final static String USERGLOBALGINI = "Global creator Gini complement";
    /**
     * Identifier for the individual creator Gini complement metric.
     */
    public final static String USERINDIVGINI = "Individual creator Gini complement";
    /**
     * Identifier for the creator recall metric.
     */
    public final static String USERRECALL = "Creator recall";
    /**
     * Identifier for the global creator entropy metric.
     */
    public final static String USERGLOBALENTROPY = "Global creator entropy";
    /**
     * Identifier for the individual creator entropy metric.
     */
    public final static String USERINDIVENTROPY = "Individual creator entropy";

    /**
     * Prints the list of available metrics
     */
    public static void printMetricList()
    {
        System.out.println("Metrics:");
        System.out.println("\tUser metrics:");
        System.out.println("\t\t" + USERGLOBALGINI);
        System.out.println("\t\t" + USERINDIVGINI);
        System.out.println("\t\t" + USERRECALL);
        System.out.println("\t\t" + USERGLOBALENTROPY);
        System.out.println("\t\t" + USERINDIVENTROPY);
        System.out.println("\tInformation pieces metrics:");
        System.out.println("\t\t" + USERSPEED);
        System.out.println("\t\t" + SPEED);
        System.out.println("\t\t" + INFOGINI);
        System.out.println("\t\t" + REALPROPRECALL);
        System.out.println("\t\t" + GLOBALREALPROPRECALL);
        System.out.println("\t\tParameter metrics:");
        System.out.println("\t\t" + RECALL);
        System.out.println("\t\t" + GINI);
        System.out.println("\t\t" + GLOBALGINI);
        System.out.println("\t\t" + GLOBALUSERGINI);
        System.out.println("\t\t" + ENTROPY);
        System.out.println("\t\t" + GLOBALENTROPY);
        System.out.println("\t\t" + GLOBALUSERENTROPY);
        System.out.println("\t\t" + EXTRATE);
        System.out.println("\t\t" + GLOBALEXTRATE);
        System.out.println("\t\t" + KLD);
        System.out.println("\t\t" + GLOBALKLD);
        System.out.println();
    }
}
