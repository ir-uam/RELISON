/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.graph.index.fast;

import es.uam.eps.ir.socialnetwork.graph.index.IdxValue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Fast implementation for a weighted relation. Represented items have indexes between 0 and N-1, where N
 * is the number of items.
 *
 * @param <W> Type of the weight.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class FastWeightedPairwiseRelation<W> extends FastWeightedRelation<W>
{
    /**
     * Constructor. Builds an empty autorelation.
     */
    public FastWeightedPairwiseRelation()
    {
        super();
    }

    /**
     * Constructor. Builds an autorelation from previous information.
     *
     * @param weightsList The list of weights.
     */
    public FastWeightedPairwiseRelation(List<List<IdxValue<W>>> weightsList)
    {
        super(new ArrayList<>(), weightsList);

        for (int i = 0; i < weightsList.size(); ++i)
        {
            this.firstIdxList.add(new ArrayList<>());
        }

        for (int i = 0; i < weightsList.size(); ++i)
        {
            List<IdxValue<W>> list = weightsList.get(i);
            for (IdxValue<W> wIdxValue : list)
            {
                this.firstIdxList.get(wIdxValue.getIdx()).add(new IdxValue<>(i, wIdxValue.getValue()));
            }
        }

        // Sorts the lists.
        firstIdxList.parallelStream()
                .filter(Objects::nonNull)
                .forEach(l -> l.sort(Comparator.naturalOrder()));
        secondIdxList.parallelStream()
                .filter(Objects::nonNull)
                .forEach(l -> l.sort(Comparator.naturalOrder()));
    }

    @Override
    public int numSecond()
    {
        return this.firstIdxList.size();
    }

    @Override
    public Stream<Integer> getAllSecond()
    {
        List<Integer> first = new ArrayList<>();
        int size = this.firstIdxList.size();
        for (int i = 0; i < size; ++i)
        {
            first.add(i);
        }
        return first.stream();
    }

    @Override
    public boolean addSecondItem(int secondIdx)
    {
        int size = this.firstIdxList.size();
        if (secondIdx < size && this.firstIdxList.get(secondIdx) != null)
        {
            return false;
        }
        if (secondIdx > size)
        {
            return false;
        }

        this.firstIdxList.add(secondIdx, new ArrayList<>());

        return true;
    }
}
