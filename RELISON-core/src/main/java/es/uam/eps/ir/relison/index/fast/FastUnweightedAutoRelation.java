/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.index.fast;

import es.uam.eps.ir.relison.index.AutoRelation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Fast implementation for an unweighted relation of objects with themselves.
 *
 * @param <W> Type of the weights.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class FastUnweightedAutoRelation<W> extends FastUnweightedRelation<W> implements AutoRelation<W>
{
    /**
     * Constructor. Builds an empty autorelation.
     */
    public FastUnweightedAutoRelation()
    {
        super();
    }

    /**
     * Constructor. Builds an autorelation from previous information.
     *
     * @param weightList List of relations and their weights.
     */
    public FastUnweightedAutoRelation(List<List<Integer>> weightList)
    {
        super(new ArrayList<>(), weightList);

        int size = secondIdxList.size();
        for (int i = 0; i < size; ++i)
        {
            this.firstIdxList.add(new ArrayList<>());
        }

        for (int i = 0; i < size; ++i)
        {
            List<Integer> list = this.secondIdxList.get(i);
            for (Integer integer : list)
            {
                this.firstIdxList.get(integer).add(i);
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
    public IntStream getIsolated()
    {
        return IntStream.range(0, this.numFirst()).filter(i -> this.firstIdxList.get(i).isEmpty() && this.secondIdxList.get(i).isEmpty());
    }

    @Override
    public IntStream firstsWithSeconds()
    {
        return IntStream.range(0, this.numFirst()).filter(i -> !this.secondIdxList.get(i).isEmpty());
    }

    @Override
    public IntStream secondsWithFirsts()
    {
        return IntStream.range(0, this.numFirst()).filter(i -> !this.firstIdxList.get(i).isEmpty());
    }

    @Override
    public boolean remove(int idx)
    {
        if (idx < 0 || idx >= this.numFirst())
        {
            return false;
        }

        // First, we remove the lists corresponding to the element to delete.
        this.firstIdxList.remove(idx);
        this.secondIdxList.remove(idx);

        // Then, we run over the rest of the lists, and update the elements by
        // reducing sizes.
        IntStream.range(0, this.numFirst()).forEach(i ->
                                                    {
                                                        List<Integer> auxFList = new ArrayList<>();
                                                        List<Integer> fList = this.firstIdxList.get(i);
                                                        IntStream.range(0, fList.size()).filter(j -> j != idx).forEach(j ->
                                                                                                                       {
                                                                                                                           if (fList.get(j) < idx)
                                                                                                                           {
                                                                                                                               auxFList.add(fList.get(j));
                                                                                                                           }
                                                                                                                           else if (fList.get(j) > idx)
                                                                                                                           {
                                                                                                                               auxFList.add(fList.get(j) - 1);
                                                                                                                           }
                                                                                                                       });

                                                        this.firstIdxList.set(i, auxFList);

                                                        List<Integer> auxSList = new ArrayList<>();
                                                        List<Integer> sList = this.secondIdxList.get(i);
                                                        IntStream.range(0, sList.size()).filter(j -> j != idx).forEach(j ->
                                                                                                                       {
                                                                                                                           if (sList.get(j) < idx)
                                                                                                                           {
                                                                                                                               auxSList.add(sList.get(j));
                                                                                                                           }
                                                                                                                           else if (sList.get(j) > idx)
                                                                                                                           {
                                                                                                                               auxSList.add(sList.get(j) - 1);
                                                                                                                           }
                                                                                                                       });

                                                        this.secondIdxList.set(i, auxSList);

                                                    });
        return true;
    }
}
