/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.knn.similarities.ir;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.nn.sim.Similarity;
import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.grid.Grid;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.knn.similarities.SimilarityFunction;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.knn.similarities.SimilarityGridSearch;
import es.uam.eps.ir.sonalire.links.recommendation.algorithms.knn.similarities.ir.BIRSimilarity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.knn.similarities.SimilarityIdentifiers.BIR;

/**
 * Grid search generator for Binary Independent Retrieval (BIR) similarity.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see BIRSimilarity ;
 */
public class BIRSimilarityGridSearch<U> implements SimilarityGridSearch<U>
{
    /**
     * Identifier for the orientation of the target user neighborhood
     */
    private static final String USEL = "uSel";
    /**
     * Identifier for the orientation of the neighbor user neighborhood
     */
    private static final String VSEL = "vSel";
    
    @Override
    public Map<String, SimilarityFunction<U>> grid(Grid grid)
    {
        Map<String, SimilarityFunction<U>> sims = new HashMap<>();
        
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        if(uSels == null || uSels.isEmpty() || vSels == null || vSels.isEmpty())
        {
            return sims;
        }

        uSels.forEach(uSel ->
            vSels.forEach(vSel ->
                sims.put(BIR + "_" + uSel + "_" + vSel, (FastGraph<U> graph, FastPreferenceData<U,U> prefData) ->
                   new BIRSimilarity(graph, uSel, vSel))));

        return sims;
    }

    @Override
    public Map<String, Supplier<Similarity>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        Map<String, Supplier<Similarity>> sims = new HashMap<>();
        
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        if(uSels == null || uSels.isEmpty() || vSels == null || vSels.isEmpty())
        {
            return sims;
        }

        uSels.forEach(uSel ->
            vSels.forEach(vSel ->
                sims.put(BIR + "_" + uSel + "_" + vSel, () ->
                    new BIRSimilarity(graph, uSel, vSel))));
        
        return sims;
    }
    
}