/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.data.filter;

import es.uam.eps.ir.socialranksys.index.Index;
import es.uam.eps.ir.socialranksys.index.fast.FastIndex;
import es.uam.eps.socialranksys.diffusion.data.Data;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Filter that mantains only those information pieces which contain an information feature
 * corresponding to a certain field (i.e. if the information piece cannot be described in
 * terms of that type of feature, it is removed from the data).
 * 
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 */
public class ContainsInformationFeatureFilter<U extends Serializable, I extends Serializable, P> extends AbstractDataFilter<U,I,P> 
{
    /**
     * Name of the field.
     */
    private final String featureName;
    
    /**
     * Constructor.
     * @param featureName the feature name.
     */
    public ContainsInformationFeatureFilter(String featureName)
    {
        this.featureName = featureName;
    }
    
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
        Set<I> informationPieces = new HashSet<>();
        data.getAllInformationPieces().forEach(i -> 
        {
            if(data.getInfoPiecesFeatures(i, featureName).count() > 0)
            {
                informationPieces.add(i);
            }
        });
        informationPieces.stream().sorted().forEach(iIndex::addObject);
        return iIndex;
    }

    @Override
    protected Index<P> filterParameters(Data<U, I, P> data, String name, Index<I> iIndex) 
    {
        if(name == null ? this.featureName != null : !name.equals(this.featureName))
        {
            Index<P> pIndex = new FastIndex<>();
            data.getAllFeatureValues(name).sorted().forEach(pIndex::addObject);
            return pIndex;
        }
        else
        {
            Index<P> pIndex = new FastIndex<>();
            Set<P> parameters = new HashSet<>();
            data.getAllInformationPieces().forEach(i -> data.getInfoPiecesFeatures(i, name).forEach(p -> parameters.add(p.v1)));
            parameters.stream().sorted().forEach(pIndex::addObject);
            return pIndex;
        }
    }
    
}
