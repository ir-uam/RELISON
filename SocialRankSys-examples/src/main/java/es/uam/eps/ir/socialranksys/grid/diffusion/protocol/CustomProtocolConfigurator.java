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
import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;

import java.io.Serializable;

/**
 * Configures a custom protocol.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class CustomProtocolConfigurator<U extends Serializable,I extends Serializable,P> implements ProtocolConfigurator<U,I,P> 
{

    @Override
    public Protocol<U, I, P> configure(ProtocolParamReader params)
    {
        if(params == null)
            return null;
        
        // Configure the selection mechanism
        SelectionSelector<U,I,P> selSel = new SelectionSelector<>();
        Tuple2oo<String, SelectionMechanism<U,I,P>> selPair = selSel.select(params.getSelection());
        if(selPair == null)
            return null;
        SelectionMechanism<U,I,P> selection = selPair.v2();
        
        // Configure the expiration mechanism
        ExpirationSelector<U,I,P> expSel = new ExpirationSelector<>();
        Tuple2oo<String, ExpirationMechanism<U,I,P>> expPair = expSel.select(params.getExpiration());
        if(expPair == null)
            return null;
        ExpirationMechanism<U,I,P> expiration = expPair.v2();
        
        // Configure the propagation mechanism
        PropagationSelector<U,I,P> propSel = new PropagationSelector<>();
        Tuple2oo<String, PropagationMechanism<U,I,P>> propPair = propSel.select(params.getPropagation());
        if(propPair == null)
            return null;
        PropagationMechanism<U,I,P> propagation = propPair.v2();
        
        // Configure the update mechanism
        UpdateSelector updSel = new UpdateSelector();
        Tuple2oo<String, UpdateMechanism> updPair = updSel.select(params.getUpdate());
        if(updPair == null)
            return null;
        UpdateMechanism update = updPair.v2();
        
        // Configure the sight mechanism
        SightSelector<U,I,P> sightSel = new SightSelector<>();
        Tuple2oo<String, SightMechanism<U,I,P>> sightPair = sightSel.select(params.getSight());
        if(sightPair == null)
            return null;
        SightMechanism<U,I,P> sight = sightPair.v2();
        
        return new CustomProtocol<>(selection, expiration, update, propagation, sight);
    }
    
}
