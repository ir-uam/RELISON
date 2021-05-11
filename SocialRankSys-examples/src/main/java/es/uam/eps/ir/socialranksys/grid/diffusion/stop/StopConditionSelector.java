/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.stop;


import es.uam.eps.ir.socialranksys.diffusion.stop.StopCondition;
import es.uam.eps.ir.socialranksys.grid.Parameters;
import org.jooq.lambda.tuple.Tuple2;

import java.io.Serializable;

import static es.uam.eps.ir.socialranksys.grid.diffusion.stop.StopConditionIdentifiers.*;


/**
 * Class for selecting an individual stop condition from a grid.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public class StopConditionSelector<U extends Serializable,I extends Serializable, F>
{
    /**
     * Selects an stop condition.
     * @param name      the name for the stop condition.
     * @param params    the parameters of the stop condition.
     * @return a pair containing the name and the stop condition if everything went OK, null otherwise.
     */
    public Tuple2<String, StopCondition<U,I, F>> select(String name, Parameters params)
    {
        StopConditionConfigurator<U,I, F> fgs;
        switch(name)
        {
            case NOMORENEW:
                fgs = new NoMoreNewStopConditionConfigurator<>();
                break;
            case NOMOREPROP:
                fgs = new NoMorePropagatedStopConditionConfigurator<>();
                break;
            case NUMITER:
                fgs = new NumIterStopConditionConfigurator<>();
                break;
            case TOTALPROP:
                fgs = new TotalPropagatedStopConditionConfigurator<>();
                break;
            case NOMORETIME:
                fgs = new NoMoreTimestampsStopConditionConfigurator<>();
                break;
            case MAXTIME:
                fgs = new MaxTimestampStopConditionConfigurator<>();
                break;
            case NOMORETIMENORINFO:
                fgs = new NoMoreTimestampsNorPropInfoStopConditionConfigurator<>();
                break;
            default:
                return null;
        }
        
        StopCondition<U,I, F> stop = fgs.getStopCondition(params);
        return new Tuple2<>(name, stop);
    }
}
