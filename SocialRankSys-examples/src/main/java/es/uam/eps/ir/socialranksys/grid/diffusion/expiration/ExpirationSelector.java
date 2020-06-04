/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.expiration;

import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;
import es.uam.eps.socialranksys.diffusion.expiration.ExpirationMechanism;

import java.io.Serializable;

import static es.uam.eps.ir.socialranksys.grid.diffusion.expiration.ExpirationMechanismIdentifiers.*;

/**
 * Class that selects an individual expiration mechanism.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class ExpirationSelector<U extends Serializable,I extends Serializable,P> 
{
    /**
     * Selects and configures a expiration mechanism.
     * @param epr Parameters for the expiration mechanism.
     * @return A pair containing the name and the selected expiration mechanism.
     */
    public Tuple2oo<String, ExpirationMechanism<U,I,P>> select(ExpirationParamReader epr)
    {
        String name = epr.getName();
        ExpirationConfigurator<U,I,P> conf;
        switch(name)
        {
            case INFINITETIME:
                conf = new InfiniteTimeExpirationConfigurator<>();
                break;
            case ALLNOTPROP:
                conf = new AllNotPropagatedExpirationConfigurator<>();
                break;
            case TIMED:
                conf = new TimedExpirationConfigurator<>();
                break;
            case EXPDECAY:
                conf = new ExponentialDecayExpirationConfigurator<>();
                break;
            case ALLNOTREALPROP:
                conf = new AllNotRealPropagatedExpirationConfigurator<>();
                break;
            case ALLNOTREALPROPTIMESTAMP:
                conf = new AllNotRealPropagatedTimestampExpirationConfigurator<>();
                break;
            default:
                return null;
        }
        
        ExpirationMechanism<U,I,P> expiration = conf.configure(epr);
        return new Tuple2oo<>(name, expiration);
    }
}
