/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.data;

import es.uam.eps.ir.ranksys.core.preference.IdPref;
import es.uam.eps.ir.ranksys.fast.preference.IdxPref;
import es.uam.eps.ir.ranksys.fast.preference.SimpleFastPreferenceData;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import org.jooq.lambda.function.Function4;
import org.jooq.lambda.tuple.Tuple3;
import org.jooq.lambda.tuple.Tuple4;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Simple implementation of FastPreferenceData backed by nested lists.
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Saúl Vargas (saul.vargas@uam.es)
 */
public class GraphSimpleFastPreferenceData<U> extends SimpleFastPreferenceData<U, U>
{
    /**
     * Constructor with default IdxPref to IdPref converter.
     *
     * @param numPreferences number of total preferences
     * @param uidxList       list of lists of preferences by user index
     * @param iidxList       list of lists of preferences by item index
     * @param uIndex         user index
     */
    protected GraphSimpleFastPreferenceData(int numPreferences, List<List<IdxPref>> uidxList, List<List<IdxPref>> iidxList,
                                            GraphIndex<U> uIndex)
    {
        super(numPreferences, uidxList, iidxList, uIndex, uIndex);
    }

    /**
     * Constructor with custom IdxPref to IdPref converter.
     *
     * @param numPreferences number of total preferences
     * @param uidxList       list of lists of preferences by user index
     * @param iidxList       list of lists of preferences by item index
     * @param uIndex         user index
     * @param uPrefFun       user IdxPref to IdPref converter
     */
    protected GraphSimpleFastPreferenceData(int numPreferences, List<List<IdxPref>> uidxList, List<List<IdxPref>> iidxList,
                                            GraphIndex<U> uIndex,
                                            Function<IdxPref, IdPref<U>> uPrefFun)
    {
        super(numPreferences, uidxList, iidxList, uIndex, uIndex, uPrefFun, uPrefFun);
    }

    /**
     * Loads the preferences from a file.
     *
     * @param <U>   Type of the users.
     * @param graph the graph.
     *
     * @return the corresponding preference data.
     */
    public static <U> GraphSimpleFastPreferenceData<U> load(FastGraph<U> graph)
    {
        AtomicInteger numPreferences = new AtomicInteger();

        List<List<IdxPref>> uidxList = new ArrayList<>();
        List<List<IdxPref>> iidxList = new ArrayList<>();

        GraphIndex<U> index = new FastGraphIndex<>(graph);

        for (int uidx = 0; uidx < graph.getVertexCount(); uidx++)
        {
            uidxList.add(null);
            iidxList.add(null);
        }

        graph.getAllNodes().forEach(u ->
        {
            int uidx = index.user2uidx(u);
            graph.getAdjacentNodesWeights(u).forEach(v ->
            {
                int vidx = index.user2uidx(v.getIdx());
                double value = v.getValue();

                List<IdxPref> uList = uidxList.get(uidx);
                if (uList == null)
                {
                    uList = new ArrayList<>();
                    uidxList.set(uidx, uList);
                }
                uList.add(new IdxPref(vidx, value));

                List<IdxPref> vList = iidxList.get(vidx);
                if (vList == null)
                {
                    vList = new ArrayList<>();
                    iidxList.set(vidx, vList);
                }
                vList.add(new IdxPref(uidx, value));

                numPreferences.incrementAndGet();
            });
        });

        return new GraphSimpleFastPreferenceData<>(numPreferences.get(), uidxList, iidxList, index);
    }

    /**
     * Loads a SimpleFastPreferenceData from a stream of user-item-value triples.
     *
     * @param <U>      user type
     * @param tuples   stream of user-item-value triples
     * @param uIndex   user index
     * @param directed indicates if the graph is directed or undirected
     * @param weighted indicates if the graph is weighted or unweighted
     *
     * @return an instance of SimpleFastPreferenceData containing the data from the input stream
     */
    public static <U> GraphSimpleFastPreferenceData<U> load(Stream<Tuple3<U, U, Double>> tuples, GraphIndex<U> uIndex, boolean directed, boolean weighted)
    {
        return load(tuples.map(t -> t.concat((Void) null)),
                (uidx, iidx, v, o) -> new IdxPref(iidx, v),
                uIndex,
                (Function<IdxPref, IdPref<U>> & Serializable) p -> new IdPref<>(uIndex.uidx2user(p)),
                directed, weighted);
    }

    /**
     * Loads a SimpleFastPreferenceData from a stream of user-item-value-other tuples. It can accomodate other information, thus you need to provide sub-classes of IdxPref IdPref accomodating for this new information.
     *
     * @param <U>         user type
     * @param <O>         additional information type
     * @param tuples      stream of user-item-value-other tuples
     * @param uIdxPrefFun converts a tuple to a user IdxPref
     * @param uIndex      user index
     * @param uIdPrefFun  user IdxPref to IdPref converter
     * @param directed    indicates if the graph is directed or undirected
     * @param weighted    indicates if the graph is weighted or unweighted
     *
     * @return an instance of SimpleFastPreferenceData containing the data from the input stream
     */
    public static <U, O> GraphSimpleFastPreferenceData<U> load(Stream<Tuple4<U, U, Double, O>> tuples,
                                                               Function4<Integer, Integer, Double, O, ? extends IdxPref> uIdxPrefFun,
                                                               GraphIndex<U> uIndex,
                                                               Function<IdxPref, IdPref<U>> uIdPrefFun,
                                                               boolean directed, boolean weighted)
    {
        AtomicInteger numPreferences = new AtomicInteger();

        List<List<IdxPref>> uidxList = new ArrayList<>();
        for (int uidx = 0; uidx < uIndex.numUsers(); uidx++)
        {
            uidxList.add(null);
        }

        List<List<IdxPref>> iidxList = new ArrayList<>();
        for (int iidx = 0; iidx < uIndex.numItems(); iidx++)
        {
            iidxList.add(null);
        }

        tuples.forEach(t ->
        {
            int uidx = uIndex.user2uidx(t.v1);
            int iidx = uIndex.item2iidx(t.v2);

            numPreferences.incrementAndGet();

            List<IdxPref> uList = uidxList.get(uidx);
            if (uList == null)
            {
                uList = new ArrayList<>();
                uidxList.set(uidx, uList);
            }
            uList.add(uIdxPrefFun.apply(uidx, iidx, weighted ? t.v3 : 1.0, t.v4));

            List<IdxPref> iList = iidxList.get(iidx);
            if (iList == null)
            {
                iList = new ArrayList<>();
                iidxList.set(iidx, iList);
            }
            iList.add(uIdxPrefFun.apply(uidx, iidx, weighted ? t.v3 : 1.0, t.v4));

            if (!directed) // If the graph is undirected, treat the opposite direction as a preference.
            {
                int uidx2 = uIndex.user2uidx(t.v2);
                int iidx2 = uIndex.item2iidx(t.v1);

                numPreferences.incrementAndGet();

                List<IdxPref> uList2 = uidxList.get(uidx2);
                if (uList2 == null)
                {
                    uList2 = new ArrayList<>();
                    uidxList.set(uidx2, uList2);
                }
                uList2.add(uIdxPrefFun.apply(uidx2, iidx2, weighted ? t.v3 : 1.0, t.v4));

                List<IdxPref> iList2 = iidxList.get(iidx2);
                if (iList2 == null)
                {
                    iList2 = new ArrayList<>();
                    iidxList.set(iidx2, iList2);
                }
                iList2.add(uIdxPrefFun.apply(uidx2, iidx2, weighted ? t.v3 : 1.0, t.v4));
            }
        });

        return new GraphSimpleFastPreferenceData<>(numPreferences.intValue(), uidxList, iidxList, uIndex, uIdPrefFun);
    }

}