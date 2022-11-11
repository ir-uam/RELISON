/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.links.prediction.metrics;

/**
 * Identifiers for the different contact recommendation metrics
 * available in the library.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class LinkPredictionMetricIdentifiers
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
     * Identifier for the area under the ROC curve metric.
     */
    public final static String AUC = "AUC";
    /**
     * Identifier for the accuracy metric.
     */
    public final static String ACCURACY = "Accuracy";
    /**
     * Identifier for the F1-score metric.
     */
    public final static String F1SCORE = "F1-score";
}
