/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.data.filter;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;
import es.uam.eps.ir.socialranksys.index.Index;
import es.uam.eps.ir.socialranksys.index.Relation;
import es.uam.eps.ir.socialranksys.index.fast.FastIndex;
import es.uam.eps.ir.socialranksys.index.fast.FastWeightedPairwiseRelation;
import org.ranksys.core.util.tuples.Tuple2od;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * For each information piece without no features, it adds a new feature, with value 1.0
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public class EmptyFeatureFilter<U extends Serializable, I extends Serializable, F> extends AbstractDataFilter<U,I, F>
{
    /**
     * Value for the empty feature.
     */
    private final F emptyValue;
    
    /**
     * Constructor.
     * @param emptyValue the empty value.
     */
    public EmptyFeatureFilter(F emptyValue)
    {
        this.emptyValue = emptyValue;
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
        data.getAllInformationPieces().sorted().forEach(iIndex::addObject);
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
            List<F> par = data.getAllFeatureValues(name).collect(Collectors.toCollection(ArrayList::new));
            par.add(this.emptyValue);
            par.stream().sorted().forEach(pIndex::addObject);
            return pIndex;
        }
    }
    
    @Override
    protected Relation<Double> filterInfoParameterRelation(Data<U,I, F> fullData, String name, Index<I> iIndex, Index<F> pIndex)
    {
        Relation<Double> relation = new FastWeightedPairwiseRelation<>();

        IntStream.range(0, iIndex.numObjects()).forEach(relation::addFirstItem);
        IntStream.range(0, pIndex.numObjects()).forEach(relation::addSecondItem);

        iIndex.getAllObjects().forEach(i ->
        {
            int iidx = iIndex.object2idx(i);
            List<Tuple2od<F>> params = fullData.getInfoPiecesFeatures(i, name).collect(Collectors.toCollection(ArrayList::new));
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
