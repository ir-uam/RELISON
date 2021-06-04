/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.diffusion.propagation;

import es.uam.eps.ir.relison.diffusion.propagation.PropagationMechanism;
import es.uam.eps.ir.relison.grid.Parameters;
import org.jooq.lambda.tuple.Tuple2;

import java.io.Serializable;

import static es.uam.eps.ir.relison.grid.diffusion.propagation.PropagationMechanismIdentifiers.*;

/**
 * Class for selecting a propagation mechanism from its configuration.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */

public class PropagationSelector<U extends Serializable,I extends Serializable, F>
{
    /**
     * Selects and configures a propagation mechanism.
     * @param name      the name of the propagation mechanism.
     * @param params    the parameters of the propagation mechanism.
     * @return a pair containing the name and the selected propagation mechanism.
     */
    public Tuple2<String, PropagationMechanism<U,I, F>> select(String name, Parameters params)
    {
        PropagationConfigurator<U,I, F> conf;
        switch(name)
        {
            case ALLNEIGHS:
                conf = new AllNeighborsPropagationConfigurator<>();
                break;
            case ALLRECNEIGHS:
                conf = new AllRecommendedNeighborsPropagationConfigurator<>();
                break;
            case PUSHPULL:
                conf = new PullPushPropagationConfigurator<>();
                break;
            case PUSH:
                conf = new PushPropagationConfigurator<>();
                break;
            case PULL:
                conf = new PullPropagationConfigurator<>();
                break;
            case PUSHPULLPUREREC:
                conf = new PullPushPureRecommenderPropagationConfigurator<>();
                break;
            case PUSHPULLREC:
                conf = new PullPushRecommenderPropagationConfigurator<>();
                break;
            default:
                return null;
        }
        
        PropagationMechanism<U,I, F> propagation = conf.configure(params);
        return new Tuple2<>(name, propagation);
    }
}
