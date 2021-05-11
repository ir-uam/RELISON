/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.protocol;


import es.uam.eps.ir.socialranksys.diffusion.protocols.Protocol;
import org.jooq.lambda.tuple.Tuple2;

import java.io.Serializable;

import static es.uam.eps.ir.socialranksys.grid.diffusion.protocol.ProtocolIdentifiers.*;


/**
 * Class for selecting a suitable information diffusion protocol from its configuration.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public class ProtocolSelector<U extends Serializable,I extends Serializable, F>
{
    /**
     * Selects a protocol.
     * @param ppr the configuration parameters of the protocol.
     * @return a pair containing the name and the selected protocol.
     */
    public Tuple2<String, Protocol<U,I, F>> select(YAMLProtocolParameterReader ppr)
    {
        String name = ppr.getName();
        ProtocolConfigurator<U,I, F> conf;
        if(!ppr.isPreconfigured()) // Custom protocol
        {
            conf = new CustomProtocolConfigurator<>();
        }
        else // Preconfigured one
        {
            switch(name)
            {
                case INDEPCASCADE:
                    conf = new IndependentCascadeModelConfigurator<>();
                    break;
                case SIMPLE:
                    conf = new SimpleConfigurator<>();
                    break;
                case PUSH:
                    conf = new PushModelConfigurator<>();
                    break;
                case PULL:
                    conf = new PullModelConfigurator<>();
                    break;
                case RUMORSPREADING:
                    conf = new RumorSpreadingModelConfigurator<>();
                    break;
                case BIDIRRUMORSPREADING:
                    conf = new BidirectionalRumorSpreadingModelConfigurator<>();
                    break;
                case THRESHOLD:
                    conf = new ThresholdModelConfigurator<>();
                    break;
                case COUNTTHRESHOLD:
                    conf = new CountThresholdModelConfigurator<>();
                    break;
                case TEMPORAL:
                    conf = new TemporalConfigurator<>();
                    break;
                default:
                    return null;
            }
        }
        
        Protocol<U,I, F> protocol = conf.configure(ppr);
        return new Tuple2<>(name, protocol);
    }
}
