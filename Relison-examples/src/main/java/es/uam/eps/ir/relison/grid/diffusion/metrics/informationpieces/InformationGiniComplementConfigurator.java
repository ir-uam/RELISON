/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.metrics.informationpieces;

import es.uam.eps.ir.relison.diffusion.metrics.SimulationMetric;
import es.uam.eps.ir.relison.diffusion.metrics.informationpieces.global.InformationPieceGiniComplement;
import es.uam.eps.ir.relison.grid.Parameters;
import es.uam.eps.ir.relison.grid.diffusion.metrics.MetricConfigurator;

import java.io.Serializable;

/**
 * Configures a metric that measures how balanced the distribution of times each information piece has been received is.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user / information features.
 *
 * @see InformationPieceGiniComplement
 */
public class InformationGiniComplementConfigurator<U extends Serializable,I extends Serializable, F> implements MetricConfigurator<U,I, F>
{
    @Override
    public SimulationMetric<U, I, F> configure(Parameters params)
    {
        return new InformationPieceGiniComplement<>();
    }
    
}
