/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.sight;


import es.uam.eps.ir.socialranksys.diffusion.sight.SightMechanism;
import es.uam.eps.ir.socialranksys.grid.Parameters;
import org.jooq.lambda.tuple.Tuple2;

import java.io.Serializable;

import static es.uam.eps.ir.socialranksys.grid.diffusion.sight.SightMechanismIdentifiers.*;


/**
 * Class that selects a sight mechanism from its parameter selection.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public class SightSelector<U extends Serializable,I extends Serializable,F>
{
    /**
     * Selects and configures a selection mechanism.
     * @param name      the name of the sight mechanism.
     * @param params    the parameters of the sight mechanism.
     * @return A pair containing the name and the selected sight mechanism.
     */
    public Tuple2<String, SightMechanism<U,I, F>> select(String name, Parameters params)
    {
        SightConfigurator<U,I,F> conf;
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
            case ALLNOTPROPAGATED:
                conf = new AllNotPropagatedSightConfigurator<>();
                break;
            case ALLNOTDISCARDEDNOTPROPAGATED:
                conf = new AllNotDiscardedNorPropagatedSightConfigurator<>();
                break;
            case RECOMMENDED:
                conf = new RecommendedSightConfigurator<>();
                break;
            default:
                return null;
        }
        
        SightMechanism<U,I,F> propagation = conf.configure(params);
        return new Tuple2<>(name, propagation);
    }
}
