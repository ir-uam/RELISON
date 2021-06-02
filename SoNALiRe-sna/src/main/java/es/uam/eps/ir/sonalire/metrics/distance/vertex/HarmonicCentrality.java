/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.metrics.distance.vertex;

import es.uam.eps.ir.sonalire.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.sonalire.metrics.distance.FastDistanceCalculator;
import es.uam.eps.ir.sonalire.metrics.distance.modes.ClosenessMode;

/**
 * Metric that computes the harmonic centrality of the nodes (a version of closeness that
 * uses the harmonic mean of the distances from the target node to the rest of nodes in the
 * network.
 *
 * <p>
 * <b>References:</b><br>
 * M.E.J. Newman. Networks: an introduction (2010)<br>
 * L.C. Freeman. Centrality in Networks: I. Conceptual clarification, Social Networks 1, 1979, pp.215-239
 * </p>
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class HarmonicCentrality<U> extends Closeness<U>
{
    /**
     * Basic constructor. Uses the harmonic mean computing algorithm.
     */
    public HarmonicCentrality()
    {
        super(new FastDistanceCalculator<>(), ClosenessMode.HARMONICMEAN);
    }

    /**
     * Constructor. Uses the harmonic mean computing algorithm.
     *
     * @param dc distance calculator.
     */
    public HarmonicCentrality(DistanceCalculator<U> dc)
    {
        super(dc, ClosenessMode.HARMONICMEAN);
    }
}
