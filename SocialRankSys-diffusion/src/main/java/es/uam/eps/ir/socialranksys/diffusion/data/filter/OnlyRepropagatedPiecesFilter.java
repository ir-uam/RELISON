/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.data.filter;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;
import es.uam.eps.ir.socialranksys.index.Index;
import es.uam.eps.ir.socialranksys.index.fast.FastIndex;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Filter that removes information pieces that are not propagated by other users.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the features.
 */
public class OnlyRepropagatedPiecesFilter<U extends Serializable, I extends Serializable, P> extends AbstractDataFilter<U,I,P> 
{

    @Override
    protected Index<U> filterUsers(Data<U, I, P> data)
    {
        Index<U> uIndex = new FastIndex<>();
        data.getAllUsers().sorted().forEach(uIndex::addObject);
        return uIndex;
    }

    @Override
    protected Index<I> filterInfoPieces(Data<U, I, P> data) 
    {
        Index<I> iIndex = new FastIndex<>();
        data.getAllInformationPieces().sorted().forEach(i -> 
        {
            if(data.getRealPropagatedUsers(i).count() > 0)
            {
                iIndex.addObject(i);
            }
        });
        return iIndex;
    }

    @Override
    protected Index<P> filterParameters(Data<U, I, P> data, String name, Index<I> iIndex) 
    {
        if(data.isUserFeature(name))
        {
            Index<P> pIndex = new FastIndex<>();
            data.getAllFeatureValues(name).sorted().forEach(pIndex::addObject);
            return pIndex;
        }
        else
        {
            Index<P> pIndex = new FastIndex<>();
            Set<P> parameters = new HashSet<>();
            iIndex.getAllObjects().forEach(i -> data.getInfoPiecesFeatures(i, name).forEach(p -> parameters.add(p.v1)));
            parameters.stream().sorted().forEach(pIndex::addObject);
            return pIndex;
        }    
    }
    
}
