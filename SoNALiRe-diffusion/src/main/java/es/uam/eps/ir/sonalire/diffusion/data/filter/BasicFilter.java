/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.data.filter;

import es.uam.eps.ir.sonalire.diffusion.data.Data;
import es.uam.eps.ir.sonalire.index.Index;
import es.uam.eps.ir.sonalire.index.fast.FastIndex;

import java.io.Serializable;

/**
 * Basic implementation of a filter. It returns the original data.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information.
 * @param <F> type of the parameters.
 */
public class BasicFilter<U extends Serializable, I extends Serializable, F> extends AbstractDataFilter<U,I, F> {

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
        data.getAllInformationPieces().sorted().forEach(iIndex::addObject);
        return iIndex;
    }

    @Override
    protected Index<F> filterParameters(Data<U, I, F> data, String name, Index<I> iIndex) {
        Index<F> pIndex = new FastIndex<>();
        data.getAllFeatureValues(name).sorted().forEach(pIndex::addObject);
        return pIndex;    
    }
    
    
}
