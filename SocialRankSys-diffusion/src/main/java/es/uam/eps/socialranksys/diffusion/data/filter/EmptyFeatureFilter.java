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
import es.uam.eps.ir.socialranksys.index.Relation;
import es.uam.eps.ir.socialranksys.index.fast.FastIndex;
import es.uam.eps.ir.socialranksys.index.fast.FastWeightedPairwiseRelation;
import es.uam.eps.socialranksys.diffusion.data.Data;
import org.ranksys.core.util.tuples.Tuple2od;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * For each information piece without no features, it adds a new feature, with value 1.0
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 * @param <I> type of the information.
 * @param <P> type of the parameters.
 */
public class EmptyFeatureFilter<U extends Serializable, I extends Serializable, P> extends AbstractDataFilter<U,I,P> 
{
    /**
     * Value for the empty feature.
     */
    private final P emptyValue;
    
    /**
     * Constructor.
     * @param emptyValue the empty value.
     */
    public EmptyFeatureFilter(P emptyValue)
    {
        this.emptyValue = emptyValue;
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
        data.getAllInformationPieces().sorted().forEach(iIndex::addObject);
        return iIndex;
    }

    @Override
    protected Index<P> filterParameters(Data<U, I, P> data, String name, Index<I> iIndex) {
        Index<P> pIndex = new FastIndex<>();
        
        if(data.isUserFeature(name))
        {
            data.getAllFeatureValues(name).sorted().forEach(pIndex::addObject);
            return pIndex;    
        }
        else // Item feature
        {
            List<P> par = data.getAllFeatureValues(name).collect(Collectors.toCollection(ArrayList::new));
            par.add(this.emptyValue);
            par.stream().sorted().forEach(pIndex::addObject);
            return pIndex;
        }
    }
    
    @Override
    protected Relation<Double> filterInfoParameterRelation(Data<U,I,P> fullData, String name, Index<I> iIndex, Index<P> pIndex)
    {
        Relation<Double> relation = new FastWeightedPairwiseRelation<>();

        IntStream.range(0, iIndex.numObjects()).forEach(relation::addFirstItem);
        IntStream.range(0, pIndex.numObjects()).forEach(relation::addSecondItem);

        iIndex.getAllObjects().forEach(i ->
        {
            int iidx = iIndex.object2idx(i);
            List<Tuple2od<P>> params = fullData.getInfoPiecesFeatures(i, name).collect(Collectors.toCollection(ArrayList::new));
            if(params.isEmpty())
            {
                int pidx = pIndex.object2idx(this.emptyValue);
                relation.addRelation(iidx, pidx, 1.0);
            }
            else
            {
                params.forEach(p ->
                {
                   int pidx = pIndex.object2idx(p.v1);
                   relation.addRelation(iidx, pidx, p.v2);
                });
            }
        });

        return relation;
    }
}
