/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.sna.vertex;

import es.uam.eps.ir.socialranksys.metrics.VertexMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;

/**
 *
 * @author Javier
 */
@FunctionalInterface
public interface VertexMetricFunction<U> 
{
    VertexMetric<U> apply(DistanceCalculator<U> dc);
}
