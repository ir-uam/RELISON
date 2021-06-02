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
 * Filter that removes any information feature that appears less than a fixed 
 * number of times (i.e. there are less than X information pieces represented by
 * that feature).
 * 
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public class MinimumFrequencyInformationFeatureFilter<U extends Serializable,I extends Serializable, F> extends AbstractDataFilter<U,I, F>
{
    /**
     * The definitive of tags to filter.
     */
    private final Set<F> tags;
    /**
     * The minimum number of tags.
     */
    private final long minimumPieces;
    /**
     * The name of the information parameter.
     */
    private final String featureName;
    
    
    /**
     * Constructor.
     * @param minimumPieces minimum number of information pieces that must contain the feature.
     * @param featureName the name of the feature.
     */
    public MinimumFrequencyInformationFeatureFilter(long minimumPieces, String featureName)
    {
        this.minimumPieces = minimumPieces;
        this.tags = new HashSet<>();
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
        // First, we determine which tags are valid. Then, we select the corresponding tweets.
        this.selectTags(data);
        
        Index<I> iIndex = new FastIndex<>();
        Set<I> informationPieces = new HashSet<>();
        data.getAllInformationPieces().forEach(i -> 
        {
            Set<F> features = new HashSet<>();
            data.getInfoPiecesFeatures(i, featureName).forEach(p -> 
            {
                if(this.tags.contains(p.v1))
                {
                    features.add(p.v1);
                }
            });
            
            if(!features.isEmpty())
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
        Index<F> pIndex = new FastIndex<>();
        
        if(data.isUserFeature(name))
        {
            data.getAllFeatureValues(name).sorted().forEach(pIndex::addObject);
            return pIndex;    
        }
        else if(name != null && !this.featureName.equals(name))// Item feature
        {
            Set<F> par = new HashSet<>();
            iIndex.getAllObjects().forEach(i -> data.getInfoPiecesFeatures(i, name).forEach(p -> par.add(p.v1)));
            par.stream().sorted().forEach(pIndex::addObject);
            return pIndex;
        }
        else
        {
            this.tags.stream().sorted().forEach(pIndex::addObject);
            return pIndex;
        }
    }

    /**
     * Selects the subset of the current tags that appear in (at least) minimumFeatures 
     * information pieces.
     * @param data the original data.
     */
    private void selectTags(Data<U,I, F> data)
    {
        this.tags.clear();
        data.getAllFeatureValues(featureName).forEach(f ->
        {
            long count = data.getInformationPiecesWithFeature(featureName, f).count();
            if(count >= this.minimumPieces)
            {
                this.tags.add(f);
            }
        });
    }
    
}
