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
import org.ranksys.core.util.tuples.Tuple2od;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Filters a list of information piece features, given by its identifier. Removes all the 
 * information pieces that do not contain any of those features, and the rest of features.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class InformationFeatureSelectionFilter<U extends Serializable, I extends Serializable,P> extends AbstractDataFilter<U,I,P> 
{
    /**
     * Set of allowed information parametrs.
     */
    private final Set<P> allowedHashtags;
    /**
     * Name of the information parameter to check.
     */
    private final String tagName;
    
    /**
     * Constructor.
     * @param allowedHashtagsFile File containing the set of allowed information parametrs.
     * @param tagName Name of the information parameter to check.
     */
    public InformationFeatureSelectionFilter(Set<P> allowedHashtagsFile, String tagName)
    {
        this.allowedHashtags = allowedHashtagsFile;
        this.tagName = tagName;
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
            List<Tuple2od<P>> params = data.getInfoPiecesFeatures(i, tagName).collect(Collectors.toCollection(ArrayList::new));
            for(Tuple2od<P> param : params)
            {
                boolean include = this.allowedHashtags.contains(param.v1());
                if(include) 
                {
                    informationPieces.add(i);
                    break;
                }
            }
        });
        informationPieces.stream().sorted().forEach(iIndex::addObject);
        return iIndex;    
    }

    @Override
    protected Index<P> filterParameters(Data<U, I, P> data, String name, Index<I> iIndex) 
    {
        if(this.tagName.equals(name))
        {
            Index<P> pIndex = new FastIndex<>();
            this.allowedHashtags.stream().sorted().forEach(pIndex::addObject);
            return pIndex;
        }
        else
        {
            Index<P> pIndex = new FastIndex<>();
            data.getAllFeatureValues(name).sorted().forEach(pIndex::addObject);
            return pIndex;
        }
    }
}
