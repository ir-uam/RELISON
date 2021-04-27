/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.standalone.ir;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmIdentifiers;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.ir.BM25;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Grid search generator for the BM25 algorithm.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.ir.BM25
 */
public class BM25GridSearch<U> implements AlgorithmGridSearch<U>
{
    /**
     * Identifier for parameter b
     */
    private static final String B = "b";
    /**
     * Identifier for parameter k
     */
    private static final String K = "k";
    /**
     * Identifier for the orientation of the target user neighborhood
     */
    private static final String USEL = "uSel";
    /**
     * Identifier for the orientation of the candidate user neighborhood
     */
    private static final String VSEL = "vSel";
    /**
     * Identifier for the orientation for the document length
     */
    private static final String DLSEL = "dlSel";
    /**
     * Identifier for indicating whether the result is weighted or not.
     */
    private static final String WEIGHTED = "weighted";

    @Override
    public Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        Map<String, Supplier<Recommender<U, U>>> recs = new HashMap<>();
        List<Double> bs = grid.getDoubleValues(B);
        List<Double> ks = grid.getDoubleValues(K);
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        List<EdgeOrientation> dlSels = grid.getOrientationValues(DLSEL);

        bs.forEach(b ->
            ks.forEach(k ->
                uSels.forEach(uSel ->
                    vSels.forEach(vSel ->
                        dlSels.forEach(dlSel ->
                            recs.put(AlgorithmIdentifiers.BM25 + "_" + uSel + "_" + vSel + "_" + dlSel + "_" + b + "_" + k, () -> new BM25<>(graph, uSel, vSel, dlSel, b, k)))))));

        return recs;
    }

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid)
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();
        List<Double> bs = grid.getDoubleValues(B);
        List<Double> ks = grid.getDoubleValues(K);
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        List<EdgeOrientation> dlSels = grid.getOrientationValues(DLSEL);
        List<Boolean> weighted = grid.getBooleanValues(WEIGHTED);

        if(weighted.isEmpty())
            bs.forEach(b ->
                ks.forEach(k ->
                    uSels.forEach(uSel ->
                        vSels.forEach(vSel ->
                            dlSels.forEach(dlSel ->
                                recs.put(AlgorithmIdentifiers.BM25 + "_" + uSel + "_" + vSel + "_" + dlSel + "_" + b + "_" + k, (graph, prefData) -> new BM25<>(graph, uSel, vSel, dlSel, b, k)))))));
        else
            bs.forEach(b ->
                ks.forEach(k ->
                    uSels.forEach(uSel ->
                        vSels.forEach(vSel ->
                            dlSels.forEach(dlSel ->
                                weighted.forEach(weight ->
                                     recs.put(AlgorithmIdentifiers.BM25 + "_" + (weight ? "wei" : "unw") + "_" + uSel + "_" + vSel + "_" + dlSel + "_" + b + "_" + k, new RecommendationAlgorithmFunction<>()
                                     {
                                         @Override
                                         public Recommender<U, U> apply(FastGraph<U> graph, FastPreferenceData<U, U> prefData)
                                         {
                                             return new BM25<>(graph, uSel, vSel, dlSel, b, k);
                                         }

                                         @Override
                                         public boolean isWeighted()
                                         {
                                             return weight;
                                         }
                                     })))))));




        return recs;
    }

}
