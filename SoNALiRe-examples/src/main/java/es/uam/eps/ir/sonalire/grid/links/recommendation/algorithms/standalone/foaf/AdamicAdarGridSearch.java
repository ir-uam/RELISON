/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.standalone.foaf;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.grid.Grid;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.AlgorithmGridSearch;
import es.uam.eps.ir.sonalire.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.sonalire.links.recommendation.algorithms.standalone.foaf.AdamicAdar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.AlgorithmIdentifiers.ADAMIC;


/**
 * Grid search generator for Adamic-Adar algorithm.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see AdamicAdar
 */
public class AdamicAdarGridSearch<U> implements AlgorithmGridSearch<U>
{
    /**
     * Identifier for the orientation of the target user neighborhood.
     */
    private static final String USEL = "uSel";
    /**
     * Identifier for the orientation of the candidate user neighborhood.
     */
    private static final String VSEL = "vSel";
    /**
     * Identifier for the orientation of the intersection user neighborhood.
     */
    private static final String WSEL = "wSel";

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid)
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();

        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        List<EdgeOrientation> wSels = grid.getOrientationValues(WSEL);

        uSels.forEach(uSel ->
            vSels.forEach(vSel ->
                wSels.forEach(wSel ->
                    recs.put(ADAMIC + "_" + uSel + "_" + vSel + "_" + wSel, (graph, prefData) -> new AdamicAdar<>(graph, uSel, vSel, wSel)))));

        return recs;
    }

    @Override
    public Map<String, Supplier<Recommender<U, U>>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        Map<String, Supplier<Recommender<U, U>>> recs = new HashMap<>();

        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        List<EdgeOrientation> wSels = grid.getOrientationValues(WSEL);

        uSels.forEach(uSel ->
                vSels.forEach(vSel ->
                        wSels.forEach(wSel ->
                                recs.put(ADAMIC + "_" + uSel + "_" + vSel + "_" + wSel, () -> new AdamicAdar<>(graph, uSel, vSel, wSel)))));

        return recs;
    }
}
