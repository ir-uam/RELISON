/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.links.recommendation.algorithms.standalone.foaf;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.links.recommendation.algorithms.AlgorithmGridSearch;
import es.uam.eps.ir.relison.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.relison.links.recommendation.algorithms.standalone.foaf.Cosine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.relison.grid.links.recommendation.algorithms.AlgorithmIdentifiers.COSINE;


/**
 * Grid search generator for Cosine similarity algorithm.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see Cosine
 */
public class CosineGridSearch<U> implements AlgorithmGridSearch<U>
{
    /**
     * Identifier for indicating whether the result is weighted or not.
     */
    private static final String WEIGHTED = "weighted";
    /**
     * Identifier for the orientation of the target user neighborhood.
     */
    private static final String USEL = "uSel";
    /**
     * Identifier for the orientation of the candidate user neighborhood.
     */
    private static final String VSEL = "vSel";

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid)
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();

        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        List<Boolean> weighted = grid.getBooleanValues(WEIGHTED);

        if(weighted.isEmpty()) // We assume unweighted
        {
            uSels.forEach(uSel ->
                vSels.forEach(vSel ->
                    recs.put(COSINE + "_" + uSel + "_" + vSel, (graph, prefData) -> new Cosine<>(graph, uSel, vSel))));
        }
        else
        {
            uSels.forEach(uSel ->
                vSels.forEach(vSel ->
                    weighted.forEach(weight ->
                        recs.put(COSINE + "_" + (weight ? "wei" : "unw") + "_" + uSel + "_" + vSel,
                             new RecommendationAlgorithmFunction<>()
                             {
                                 @Override
                                 public Recommender<U, U> apply(FastGraph<U> graph, FastPreferenceData<U, U> prefData)
                                 {
                                     return new Cosine<>(graph, uSel, vSel);
                                 }

                                 @Override
                                 public boolean isWeighted()
                                 {
                                     return weight;
                                 }
                             }
                         )
                    )
                )
            );

        }


        return recs;
    }

    @Override
    public Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        Map<String, Supplier<Recommender<U, U>>> recs = new HashMap<>();

        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);

        uSels.forEach(uSel ->
            vSels.forEach(vSel ->
                recs.put(COSINE + "_" + uSel + "_" + vSel, () -> new Cosine<>(graph, uSel, vSel))));

        return recs;
    }

}
