/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.metrics.distance.graph;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.relison.sna.metrics.GraphMetric;

/**
 * Computes the reciprocal diameter of a network.
 *
 * <p>
 * <b>References: </b></p>
 *     <ol>
 *         <li>J. Sanz-Cruzado, P. Castells. Beyond accuracy in link prediction. 3rd Workshop on Social Media for Personalization and Search (SoMePEaS 2019).</li>
 *         <li>J. Sanz-Cruzado, S.M. Pepa, P. Castells. Structural novelty and diversity in link prediction. 0th International Workshop on Modeling Social Media (MSM 2018) at The Web Conference (WWW 2018)</li>
 *     </ol>
 *
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ReciprocalDiameter<U> implements GraphMetric<U>
{
    /**
     * Distance calculator.
     */
    private final Diameter<U> diameter;

    /**
     * Constructor.
     *
     * @param dc distance calculator.
     */
    public ReciprocalDiameter(DistanceCalculator<U> dc)
    {
        this.diameter = new Diameter<>(dc);
    }

    /**
     * Constructor.
     */
    public ReciprocalDiameter()
    {
        this.diameter = new Diameter<>();
    }

    @Override
    public double compute(Graph<U> graph)
    {
        double value = diameter.compute(graph);
        if (value == 0)
        {
            return 0.0;
        }
        return 1.0 / value;
    }
}
