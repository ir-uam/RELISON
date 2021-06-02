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
import java.util.HashSet;
import java.util.Set;

/**
 * Filter that limits the maximum number of information pieces that a single user
 * can have. In case a user has more than the given number, the filter selects the
 * desired number of information pieces for the user accordingly to the order
 * established for the information type.
 * 
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public class NumInformationPiecesFilter<U extends Serializable, I extends Serializable, F> extends AbstractDataFilter<U,I, F>
{
    /**
     * The maximum number of information pieces to retrieve from each user.
     */
    private final int numTweets;
    
    /**
     * Constructor.
     * @param numTweets maximum number of information pieces to retrieve from each user.
     */
    public NumInformationPiecesFilter(int numTweets)
    {
        this.numTweets = numTweets;
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
        Set<I> info = new HashSet<>();
        data.getAllUsers().forEach(u -> data.getPieces(u).sorted().limit(numTweets).forEach(info::add));
        
        info.stream().sorted().forEach(iIndex::addObject);
        return iIndex;
    }

    @Override
    protected Index<F> filterParameters(Data<U, I, F> data, String name, Index<I> iIndex) {
        Index<F> pIndex = new FastIndex<>();
        
        if(data.isUserFeature(name))
        {
            data.getAllFeatureValues(name).sorted().forEach(pIndex::addObject);
            return pIndex;    
        }
        else // Item feature
        {
            Set<F> par = new HashSet<>();
            iIndex.getAllObjects().forEach(i -> data.getInfoPiecesFeatures(i, name).forEach(p -> par.add(p.v1)));
            par.stream().sorted().forEach(pIndex::addObject);
            return pIndex;
        }
    }
}
