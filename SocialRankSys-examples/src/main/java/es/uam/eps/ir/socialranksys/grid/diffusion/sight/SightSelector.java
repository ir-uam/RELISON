/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.sight;


import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;
import es.uam.eps.socialranksys.diffusion.sight.SightMechanism;

import java.io.Serializable;

import static es.uam.eps.ir.socialranksys.grid.diffusion.sight.SightMechanismIdentifiers.*;


/**
 * Class that selects an individual sight mechanism.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class SightSelector<U extends Serializable,I extends Serializable,P> 
{
    /**
     * Selects and configures a sight mechanism.
     * @param ppr Parameters for the sight mechanism.
     * @return A pair containing the name and the selected sight mechanism.
     */
    public Tuple2oo<String, SightMechanism<U,I,P>> select(SightParamReader ppr)
    {
        String name = ppr.getName();
        SightConfigurator<U,I,P> conf;
        switch(name)
        {
            case ALLRECOMMENDED:
                conf = new AllRecommendedSightConfigurator<>();
                break;
            case ALLSIGHT:
                conf = new AllSightConfigurator<>();
                break;
            case ALLTRAIN:
                conf = new AllTrainSightConfigurator<>();
                break;
            case COUNT:
                conf = new CountSightConfigurator<>();
                break;
            case ALLNOTDISCARDED:
                conf = new AllNotDiscardedSightConfigurator<>();
                break;
            case RECOMMENDED:
                conf = new RecommendedSightConfigurator<>();
                break;
            default:
                return null;
        }
        
        SightMechanism<U,I,P> propagation = conf.configure(ppr);
        return new Tuple2oo<>(name, propagation);
    }
}
