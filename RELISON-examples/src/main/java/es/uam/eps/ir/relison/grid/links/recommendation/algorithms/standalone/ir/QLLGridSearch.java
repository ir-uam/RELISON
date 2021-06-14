/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.links.recommendation.algorithms.standalone.ir;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.links.recommendation.algorithms.AlgorithmGridSearch;
import es.uam.eps.ir.relison.grid.links.recommendation.algorithms.AlgorithmIdentifiers;
import es.uam.eps.ir.relison.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.relison.links.recommendation.algorithms.standalone.ir.QLL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Grid search generator for Query Likelihood algorithm with Laplace smoothing.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see QLL
 * */
public class QLLGridSearch<U> implements AlgorithmGridSearch<U>
{
    /**
     * Identifier for the trade-off between the regularization term and the original term in
     * the Query Likelihood Laplace formula.
     */
    private static final String PHI = "phi";
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
        List<Double> phis = grid.getDoubleValues(PHI);
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);

        phis.forEach(phi ->
            uSels.forEach(uSel ->
                vSels.forEach(vSel ->
                    recs.put(AlgorithmIdentifiers.QLL + "_" + uSel + "_" + vSel + "_" + phi, () -> new QLL<>(graph, uSel, vSel, phi)))));

        return recs;
    }

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid)
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();
        List<Double> phis = grid.getDoubleValues(PHI);
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        List<Boolean> weighted = grid.getBooleanValues(WEIGHTED);

        if(weighted.isEmpty())
            phis.forEach(phi ->
                uSels.forEach(uSel ->
                    vSels.forEach(vSel ->
                        recs.put(AlgorithmIdentifiers.QLL + "_" + uSel + "_" + vSel + "_" + phi, (graph, prefData) -> new QLL<>(graph, uSel, vSel, phi)))));
        else
            phis.forEach(phi ->
                uSels.forEach(uSel ->
                    vSels.forEach(vSel ->
                        weighted.forEach(weight ->
                            recs.put(AlgorithmIdentifiers.QLL + "_" + (weight ? "wei" : "unw") +"_" + uSel + "_" + vSel + "_" + phi, new RecommendationAlgorithmFunction<>()
                            {
                                @Override
                                public Recommender<U, U> apply(FastGraph<U> graph, FastPreferenceData<U, U> prefData)
                                {
                                    return new QLL<>(graph, uSel, vSel, phi);
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
