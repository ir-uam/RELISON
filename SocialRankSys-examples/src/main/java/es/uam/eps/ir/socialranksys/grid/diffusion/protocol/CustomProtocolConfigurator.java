/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.protocol;

import es.uam.eps.ir.socialranksys.diffusion.expiration.ExpirationMechanism;
import es.uam.eps.ir.socialranksys.diffusion.propagation.PropagationMechanism;
import es.uam.eps.ir.socialranksys.diffusion.protocols.CustomProtocol;
import es.uam.eps.ir.socialranksys.diffusion.protocols.Protocol;
import es.uam.eps.ir.socialranksys.diffusion.selections.SelectionMechanism;
import es.uam.eps.ir.socialranksys.diffusion.sight.SightMechanism;
import es.uam.eps.ir.socialranksys.diffusion.update.UpdateMechanism;
import es.uam.eps.ir.socialranksys.grid.diffusion.expiration.ExpirationSelector;
import es.uam.eps.ir.socialranksys.grid.diffusion.propagation.PropagationSelector;
import es.uam.eps.ir.socialranksys.grid.diffusion.selection.SelectionSelector;
import es.uam.eps.ir.socialranksys.grid.diffusion.sight.SightSelector;
import es.uam.eps.ir.socialranksys.grid.diffusion.update.UpdateSelector;
import org.jooq.lambda.tuple.Tuple2;

import java.io.Serializable;

/**
 * Configures a custom protocol.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 *
 * @see CustomProtocol
 */
public class CustomProtocolConfigurator<U extends Serializable,I extends Serializable, F> implements ProtocolConfigurator<U,I, F>
{

    @Override
    public Protocol<U, I, F> configure(YAMLProtocolParameterReader params)
    {
        if(params == null)
            return null;
        
        // Configure the selection mechanism
        SelectionSelector<U,I, F> selSel = new SelectionSelector<>();
        Tuple2<String, SelectionMechanism<U,I, F>> selPair = selSel.select(params.getSelection().getName(), params.getSelection().getParams());
        if(selPair == null)
            return null;
        SelectionMechanism<U,I, F> selection = selPair.v2();
        
        // Configure the expiration mechanism
        ExpirationSelector<U,I, F> expSel = new ExpirationSelector<>();
        Tuple2<String, ExpirationMechanism<U,I, F>> expPair = expSel.select(params.getExpiration().getName(), params.getExpiration().getParams());
        if(expPair == null)
            return null;
        ExpirationMechanism<U,I, F> expiration = expPair.v2();
        
        // Configure the propagation mechanism
        PropagationSelector<U,I, F> propSel = new PropagationSelector<>();
        Tuple2<String, PropagationMechanism<U,I, F>> propPair = propSel.select(params.getPropagation().getName(), params.getPropagation().getParams());
        if(propPair == null)
            return null;
        PropagationMechanism<U,I, F> propagation = propPair.v2();
        
        // Configure the update mechanism
        UpdateSelector updSel = new UpdateSelector();
        Tuple2<String, UpdateMechanism> updPair = updSel.select(params.getUpdate().getName(), params.getUpdate().getParams());
        if(updPair == null)
            return null;
        UpdateMechanism update = updPair.v2();
        
        // Configure the sight mechanism
        SightSelector<U,I, F> sightSel = new SightSelector<>();
        Tuple2<String, SightMechanism<U,I, F>> sightPair = sightSel.select(params.getSight().getName(), params.getSight().getParams());
        if(sightPair == null)
            return null;
        SightMechanism<U,I, F> sight = sightPair.v2();
        
        return new CustomProtocol<>(selection, expiration, update, propagation, sight);
    }
    
}
