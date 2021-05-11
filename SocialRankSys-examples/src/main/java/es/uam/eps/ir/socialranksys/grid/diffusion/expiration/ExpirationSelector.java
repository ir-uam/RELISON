/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.expiration;

import es.uam.eps.ir.socialranksys.diffusion.expiration.AllNotRealPropagatedExpirationMechanism;
import es.uam.eps.ir.socialranksys.diffusion.expiration.ExpirationMechanism;
import es.uam.eps.ir.socialranksys.grid.Parameters;
import org.jooq.lambda.tuple.Tuple2;

import java.io.Serializable;

import static es.uam.eps.ir.socialranksys.grid.diffusion.expiration.ExpirationMechanismIdentifiers.*;

/**
 * Class for selecting an expiration mechanism from its configuration.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 *
 * @see AllNotRealPropagatedExpirationMechanism
 */
public class ExpirationSelector<U extends Serializable,I extends Serializable, F>
{
    /**
     * Selects and configures a expiration mechanism.
     * @param name      the name of the expiration mechanism.
     * @param params    the parameters of the expiration mechanism.
     * @return a pair containing the name and the selected expiration mechanism.
     */
    public Tuple2<String, ExpirationMechanism<U,I, F>> select(String name, Parameters params)
    {
        ExpirationConfigurator<U,I, F> conf;
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
        
        ExpirationMechanism<U,I,F> expiration = conf.configure(params);
        return new Tuple2<>(name, expiration);
    }
}
