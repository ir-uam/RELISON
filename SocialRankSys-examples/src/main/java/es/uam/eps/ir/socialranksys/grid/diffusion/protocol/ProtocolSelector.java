/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.protocol;



import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;
import es.uam.eps.socialranksys.diffusion.protocols.Protocol;

import java.io.Serializable;

import static es.uam.eps.ir.socialranksys.grid.diffusion.protocol.ProtocolIdentifiers.*;


/**
 * Class that selects an individual filter from a grid.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 * @param <I> Type of the information pieces
 * @param <P> Type of the parameters
 */
public class ProtocolSelector<U extends Serializable,I extends Serializable,P> 
{
    /**
     * Selects a protocol.
     * @param ppr Parameters for the protocol.
     * @return A pair containing the name and the selected protocol.
     */
    public Tuple2oo<String, Protocol<U,I,P>> select(ProtocolParamReader ppr)
    {
        String name = ppr.getName();
        ProtocolConfigurator<U,I,P> conf;
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
        
        Protocol<U,I,P> protocol = conf.configure(ppr);
        return new Tuple2oo<>(name, protocol);
    }
}
