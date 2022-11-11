/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.links.recommendation.metrics;

/**
 * Identifiers for the different contact recommendation metrics
 * available in the library.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class RecommMetricIdentifiers
{
    // Accuracy metrics:
    /**
     * Identifier for the precision metric.
     */
    public final static String P = "Precision";
    /**
     * Identifier for the recall metric.
     */
    public final static String R = "Recall";
    /**
     * Identifier for the nDCG metric.
     */
    public final static String NDCG = "nDCG";
    /**
     * Identifier for the mean average precision metric (MAP)
     */
    public final static String MAP = "MAP";
    /**
     * Identifier for the intra-list distance metric (ILD)
     */
    public final static String ILD = "ILD";
    /**
     * Identifier for the long-tail novelty metric (LTN)
     */
    public final static String LTN = "LTN";
    /**
     * Identifier for the unexpectedness metric.
     */
    public final static String UNEXP = "Unexpectedness";
    /**
     * Identifier for the mean prediction distance.
     */
    public final static String MPD = "Mean prediction distance";
    /**
     * Identifier for the predicted Gini complement.
     */
    public final static String PGC = "Predicted Gini complement";
    /**
     * Identifier for the community recall.
     */
    public final static String CRECALL = "Community recall";
    /**
     * Identifier for the ERR-IA metric
     */
    public final static String ERRIA = "ERRIA";
}
