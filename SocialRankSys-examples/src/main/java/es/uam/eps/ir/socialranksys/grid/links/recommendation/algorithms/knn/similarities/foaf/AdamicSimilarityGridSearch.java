/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn.similarities.foaf;


import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.nn.sim.Similarity;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn.similarities.SimilarityFunction;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn.similarities.SimilarityGridSearch;
import es.uam.eps.ir.socialranksys.links.recommendation.algorithms.knn.similarities.foaf.AdamicSimilarity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn.similarities.SimilarityIdentifiers.ADAMIC;


/**
 * Grid search generator for Adamic-Adar similarity.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see es.uam.eps.ir.socialranksys.links.recommendation.algorithms.knn.similarities.foaf.AdamicSimilarity
 */
public class AdamicSimilarityGridSearch<U> implements SimilarityGridSearch<U>
{
    /**
     * Identifier for the orientation of the target user neighborhood.
     */
    private static final String USEL = "uSel";
    /**
     * Identifier for the orientation of the neighbor user neighborhood.
     */
    private static final String VSEL = "vSel";
    /**
     * Identifier for the orientation of the intersection user neighborhood.
     */
    private static final String WSEL = "wSel";
    
    @Override
    public Map<String, SimilarityFunction<U>> grid(Grid grid)
    {
        Map<String, SimilarityFunction<U>> sims = new HashMap<>();
        
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        List<EdgeOrientation> wSels = grid.getOrientationValues(WSEL);
        if(uSels == null || uSels.isEmpty() || vSels == null || vSels.isEmpty() || wSels == null || wSels.isEmpty())
        {
            return sims;
        }
        
        uSels.forEach(uSel ->
            vSels.forEach(vSel ->
                wSels.forEach(wSel ->
                    sims.put(ADAMIC + "_" + uSel + "_" + vSel + "_" + wSel, (FastGraph<U> graph, FastPreferenceData<U,U> prefData) ->
                       new AdamicSimilarity(graph, uSel, vSel, wSel)))));
        
        return sims;
    }

    @Override
    public Map<String, Supplier<Similarity>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        Map<String, Supplier<Similarity>> sims = new HashMap<>();
        
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        List<EdgeOrientation> wSels = grid.getOrientationValues(WSEL);
        if(uSels == null || uSels.isEmpty() || vSels == null || vSels.isEmpty() || wSels == null || wSels.isEmpty())
        {
            return sims;
        }


        uSels.forEach(uSel ->
            vSels.forEach(vSel ->
                wSels.forEach(wSel ->
                    sims.put(ADAMIC + "_" + uSel + "_" + vSel + "_" + wSel, () ->
                        new AdamicSimilarity(graph, uSel, vSel, wSel)))));

        return sims;
    }
    
}
