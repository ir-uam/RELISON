/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.data;

import es.uam.eps.ir.ranksys.core.preference.IdPref;
import es.uam.eps.ir.ranksys.core.preference.SimplePreferenceData;
import es.uam.eps.ir.sonalire.graph.Graph;
import org.jooq.lambda.function.Function4;
import org.jooq.lambda.tuple.Tuple3;
import org.jooq.lambda.tuple.Tuple4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Simple map-based preference data for social network evaluation.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class GraphSimplePreferenceData<U> extends SimplePreferenceData<U, U>
{
    /**
     * Constructor.
     *
     * @param userMap        map that links users to their followees.
     * @param itemMap        map that links users to their followers.
     * @param numPreferences number of preferences (the number of edges in the graph if directed, twice that number if undirected).
     */
    protected GraphSimplePreferenceData(Map<U, List<IdPref<U>>> userMap, Map<U, List<IdPref<U>>> itemMap, int numPreferences)
    {
        super(userMap, itemMap, numPreferences);
    }

    /**
     * Loads the preference data from a graph.
     *
     * @param <U>   Type of the users.
     * @param graph the graph containing the preference data.
     *
     * @return a preference data object.
     */
    public static <U> GraphSimplePreferenceData<U> load(Graph<U> graph)
    {
        Map<U, List<IdPref<U>>> userMap = new HashMap<>();
        Map<U, List<IdPref<U>>> itemMap = new HashMap<>();
        AtomicInteger numPreferences = new AtomicInteger(0);

        graph.getAllNodes().forEach(u ->
            graph.getAdjacentNodesWeights(u).forEach(vWeight ->
            {
                U v = vWeight.getIdx();
                double weight = vWeight.getValue();
                userMap.computeIfAbsent(u, x -> new ArrayList<>()).add(new IdPref<>(v, weight));
                itemMap.computeIfAbsent(v, x -> new ArrayList<>()).add(new IdPref<>(u, weight));
                numPreferences.incrementAndGet();
            }));

        return new GraphSimplePreferenceData<>(userMap, itemMap, numPreferences.intValue());
    }

    /**
     * Loads a SimplePreferenceData from a stream of user-item-value triples.
     *
     * @param <U>      user type
     * @param tuples   user-item-value triples
     * @param directed indicates if the graph is directed or not.
     * @param weighted indicates if the graph is weighted or not.
     *
     * @return instance of SimplePreferenceData containing the information in the input
     */
    public static <U> GraphSimplePreferenceData<U> load(Stream<Tuple3<U, U, Double>> tuples, boolean directed, boolean weighted)
    {
        return load(tuples.map(t -> t.concat((Void) null)),
                weighted ? (u, i, v, o) -> new IdPref<>(i, v) : (u, i, v, o) -> new IdPref<>(i, 1.0), directed, weighted);
    }

    /**
     * Loads an instance of the class from a stream of tuples possibly containing extra information.
     *
     * @param <U>      type of user
     * @param <O>      type of additional information
     * @param tuples   stream of user-item-value triples
     * @param uPrefFun creator of preference objects
     * @param directed indicates if the graph is directed or not.
     * @param weighted indicates if the graph is weighted or not.
     *
     * @return a preference data object
     */
    public static <U, O> GraphSimplePreferenceData<U> load(Stream<Tuple4<U, U, Double, O>> tuples,
                                                           Function4<U, U, Double, O, ? extends IdPref<U>> uPrefFun,
                                                           boolean directed, boolean weighted)
    {
        AtomicInteger numPreferences = new AtomicInteger(0);
        Map<U, List<IdPref<U>>> userMap = new HashMap<>();
        Map<U, List<IdPref<U>>> itemMap = new HashMap<>();

        if (directed) // Directed graph: u1 interacts u2. the opposite is not necessary true.
        {
            tuples.forEach(t -> {
                numPreferences.incrementAndGet();
                userMap.computeIfAbsent(t.v1, v1 -> new ArrayList<>()).add(uPrefFun.apply(t));
                itemMap.computeIfAbsent(t.v2, v2 -> new ArrayList<>()).add(uPrefFun.apply(t));
            });
        }
        else // Undirected graph: if u1 interacts u2, u2 also interacts u1 (we add both preferences)
        {
            tuples.forEach(t -> {
                Tuple4<U, U, Double, O> t2 = new Tuple4<>(t.v2, t.v1, t.v3, t.v4);
                numPreferences.incrementAndGet();
                numPreferences.incrementAndGet();
                userMap.computeIfAbsent(t.v1, v1 -> new ArrayList<>()).add(uPrefFun.apply(t));
                itemMap.computeIfAbsent(t.v2, v2 -> new ArrayList<>()).add(uPrefFun.apply(t));
                userMap.computeIfAbsent(t2.v1, v1 -> new ArrayList<>()).add(uPrefFun.apply(t2));
                itemMap.computeIfAbsent(t2.v2, v2 -> new ArrayList<>()).add(uPrefFun.apply(t2));
            });
        }

        return new GraphSimplePreferenceData<>(userMap, itemMap, numPreferences.intValue());
    }

}
