/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna.comm.indiv;

import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.sna.metrics.IndividualCommunityMetric;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Class for performing the grid search for a given individual community metric.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 */
public interface IndividualCommunityMetricGridSearch<U>
{
    /**
     * Obtains the different individual community metrics to compute in a grid.
     * @param grid the grid for the metric
     * @return the grid parameters.
     */
    Map<String, Supplier<IndividualCommunityMetric<U>>> grid(Grid grid);
}
