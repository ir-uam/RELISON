/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.stop;


import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;
import es.uam.eps.socialranksys.diffusion.stop.StopCondition;

import java.io.Serializable;

import static es.uam.eps.ir.socialranksys.grid.diffusion.stop.StopConditionIdentifiers.*;


/**
 * Class that selects an individual filter from a grid.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 * @param <I> Type of the information pieces
 * @param <P> Type of the parameters
 */
public class StopConditionSelector<U extends Serializable,I extends Serializable,P> 
{
    /**
     * Selects a filter.
     * @param fgr Grid containing the parameters of the filter.
     * @return A pair containing the name and the selected filter, null if something failed.
     */
    public Tuple2oo<String, StopCondition<U,I,P>> select(StopConditionParamReader fgr)
    {
        String name = fgr.getName();
        StopConditionConfigurator<U,I,P> fgs;
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
        
        StopCondition<U,I,P> stop = fgs.getStopCondition(fgr);
        return new Tuple2oo<>(name, stop);
    }
}
