/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.data.filter;

import es.uam.eps.ir.relison.diffusion.data.Data;
import es.uam.eps.ir.relison.index.Index;
import es.uam.eps.ir.relison.index.fast.FastIndex;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Filter that keeps only those information pieces which contain an information feature
 * corresponding to a certain field (i.e. if the information piece cannot be described in
 * terms of that type of feature, it is removed from the data).
 * 
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public class ContainsInformationFeatureFilter<U extends Serializable, I extends Serializable, F> extends AbstractDataFilter<U,I, F>
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
    protected Index<U> filterUsers(Data<U, I, F> data)
    {
        Index<U> uIndex = new FastIndex<>();
        data.getAllUsers().sorted().forEach(uIndex::addObject);
        return uIndex;
    }

    @Override
    protected Index<I> filterInfoPieces(Data<U, I, F> data)
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
    protected Index<F> filterParameters(Data<U, I, F> data, String name, Index<I> iIndex)
    {
        if(name == null ? this.featureName != null : !name.equals(this.featureName))
        {
            Index<F> pIndex = new FastIndex<>();
            data.getAllFeatureValues(name).sorted().forEach(pIndex::addObject);
            return pIndex;
        }
        else
        {
            Index<F> pIndex = new FastIndex<>();
            Set<F> parameters = new HashSet<>();
            data.getAllInformationPieces().forEach(i -> data.getInfoPiecesFeatures(i, name).forEach(p -> parameters.add(p.v1)));
            parameters.stream().sorted().forEach(pIndex::addObject);
            return pIndex;
        }
    }
    
}
