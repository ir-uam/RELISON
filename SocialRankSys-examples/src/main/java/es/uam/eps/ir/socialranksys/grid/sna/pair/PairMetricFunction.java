/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.sna.pair;

import es.uam.eps.ir.socialranksys.metrics.PairMetric;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;

/**
 * Functional interface for obtaining pair metrics.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
@FunctionalInterface
public interface PairMetricFunction<U> 
{
    /**
     * Obtains a pair metric.
     * @param calc Distance calculator.
     * @return the pair metric.
     */
    PairMetric<U> apply(DistanceCalculator<U> calc);
}
