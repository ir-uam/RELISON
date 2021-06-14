/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna.comm.global;

import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.sna.metrics.CommunityMetric;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Class for performing the grid search for a given global community metric.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public interface GlobalCommunityMetricGridSearch<U>
{
    /**
     * Obtains the different global community metrics metrics to compute in a grid.
     * @param grid The grid for the metric
     * @return the grid parameters.
     */
    Map<String, Supplier<CommunityMetric<U>>> grid(Grid grid);
}
