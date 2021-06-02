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
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.ir.DFReeKLIM;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmIdentifiers.DFREEKLIM;


/**
 * Grid search generator for the DFReeKLIM Divergence From Randomness method.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.ir.DFReeKLIM
 */
public class DFReeKLIMGridSearch<U> implements AlgorithmGridSearch<U>
{
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

        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);

        uSels.forEach(uSel ->
            vSels.forEach(vSel ->
                recs.put(DFREEKLIM + "_" + uSel + "_" + vSel, () -> new DFReeKLIM<>(graph, uSel, vSel))));

        return recs;
    }

    @Override
    public Map<String, RecommendationAlgorithmFunction<U>> grid(Grid grid)
    {
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();

        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        List<Boolean> weighted = grid.getBooleanValues(WEIGHTED);

        if(weighted.isEmpty())
            uSels.forEach(uSel ->
                vSels.forEach(vSel ->
                    recs.put(DFREEKLIM + "_" + uSel + "_" + vSel, (graph, prefData) -> new DFReeKLIM<>(graph, uSel, vSel))));
        else
            uSels.forEach(uSel ->
                vSels.forEach(vSel ->
                    weighted.forEach(weight ->
                        recs.put(DFREEKLIM + "_" + (weight ? "wei" : "unw") +"_" + uSel + "_" + vSel, new RecommendationAlgorithmFunction<>()
                        {
                            @Override
                            public Recommender<U, U> apply(FastGraph<U> graph, FastPreferenceData<U, U> prefData)
                            {
                                return new DFReeKLIM<>(graph, uSel, vSel);
                            }

                            @Override
                            public boolean isWeighted()
                            {
                                return weight;
                            }
                        }))));

        return recs;
    }

}
