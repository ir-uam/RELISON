/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.sna.graph;

import es.uam.eps.ir.relison.metrics.GraphMetric;
import es.uam.eps.ir.relison.metrics.distance.DistanceCalculator;

/**
 * Functional interface for retrieving global graph metrics.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 */
@FunctionalInterface
public interface GraphMetricFunction<U> 
{
    GraphMetric<U> apply(DistanceCalculator<U> dc);
}
