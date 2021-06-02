/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.standalone.ir;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.grid.Grid;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.AlgorithmGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.AlgorithmIdentifiers;
import es.uam.eps.ir.sonalire.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.sonalire.links.recommendation.algorithms.standalone.ir.QLJM;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Grid search generator for Query Likelihood algorithm with Jelinek-Mercer smoothing.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see QLJM
 */
public class QLJMGridSearch<U> implements AlgorithmGridSearch<U>
{
    /**
     * Identifier for the trade-off between the regularization term and the original term in
     * the Query Likelihood Jelinek-Mercer formula.
     */
    private static final String LAMBDA = "lambda";
    /**
     * Identifier for the orientation of the target user neighborhood
     */
    private static final String USEL = "uSel";
    /**
     * Identifier for the orientation of the candidate user neighborhood
     */
    private static final String VSEL = "vSel";
    /**
     * Identifier for indicating whether the result is weighted or not.
     */
    private static final String WEIGHTED = "weighted";

    @Override
    public Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        Map<String, Supplier<Recommender<U, U>>> recs = new HashMap<>();
        List<Double> lambdas = grid.getDoubleValues(LAMBDA);
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);

        lambdas.forEach(lambda ->
            uSels.forEach(uSel ->
                vSels.forEach(vSel ->
                    recs.put(AlgorithmIdentifiers.QLJM + "_" + uSel + "_" + vSel + "_" + lambda, () -> new QLJM<>(graph, uSel, vSel, lambda)))));

        return recs;
    }

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid)
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();
        List<Double> lambdas = grid.getDoubleValues(LAMBDA);
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        List<Boolean> weighted = grid.getBooleanValues(WEIGHTED);

        if(weighted.isEmpty())
            lambdas.forEach(lambda ->
                uSels.forEach(uSel ->
                    vSels.forEach(vSel ->
                        recs.put(AlgorithmIdentifiers.QLJM + "_" + uSel + "_" + vSel + "_" + lambda, (graph, prefData) -> new QLJM<>(graph, uSel, vSel, lambda)))));
        else
            lambdas.forEach(lambda ->
                uSels.forEach(uSel ->
                    vSels.forEach(vSel ->
                        weighted.forEach(weight ->
                            recs.put(AlgorithmIdentifiers.QLJM + "_" + (weight ? "wei" : "unw") +"_" + uSel + "_" + vSel + "_" + lambda, new RecommendationAlgorithmFunction<>()
                            {
                                @Override
                                public Recommender<U, U> apply(FastGraph<U> graph, FastPreferenceData<U, U> prefData)
                                {
                                    return new QLJM<>(graph, uSel, vSel, lambda);
                                }

                                @Override
                                public boolean isWeighted()
                                {
                                    return weight;
                                }
                            })))));
        return recs;
    }

}
