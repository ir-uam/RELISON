/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.metrics;

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
    public final static String P = "Precision";
    public final static String R = "Recall";
    public final static String NDCG = "nDCG";
    public final static String MAP = "MAP";

    public final static String ILD = "ILD";
    public final static String LTN = "LTN";
    public final static String UNEXP = "Unexpectedness";
    public final static String MPD = "Mean prediction distance";
    public final static String PGC = "Predicted Gini complement";

    public final static String CRECALL = "Community recall";
    public final static String ERRIA = "ERRIA";
}
