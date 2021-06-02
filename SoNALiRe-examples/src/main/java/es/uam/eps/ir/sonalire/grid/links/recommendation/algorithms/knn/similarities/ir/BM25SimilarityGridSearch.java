/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
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
import es.uam.eps.ir.sonalire.links.recommendation.algorithms.knn.similarities.ir.BM25Similarity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.knn.similarities.SimilarityIdentifiers.BM25;

/**
 * Grid search generator for the BM25 similarity.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see BM25Similarity
 */
public class BM25SimilarityGridSearch<U> implements SimilarityGridSearch<U>
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
     * Identifier for the orientation of the neighbor user neighborhood
     */
    private static final String VSEL = "vSel";
    /**
     * Identifier for the orientation for the document length
     */
    private static final String DLSEL = "dlSel";
    
    @Override
    public Map<String, SimilarityFunction<U>> grid(Grid grid)
    {
        Map<String, SimilarityFunction<U>> sims = new HashMap<>();
        
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        List<EdgeOrientation> dlSels = grid.getOrientationValues(DLSEL);
        List<Double> bs = grid.getDoubleValues(B);
        List<Double> ks = grid.getDoubleValues(K);
        if(uSels == null || uSels.isEmpty() || vSels == null || vSels.isEmpty() || dlSels == null || dlSels.isEmpty())
        {
            return sims;
        }

        uSels.forEach(uSel ->
            vSels.forEach(vSel ->
                dlSels.forEach(dlSel ->
                    bs.forEach(b ->
                        ks.forEach(k ->
                            sims.put(BM25 + "_" + uSel + "_" + vSel + "_" + dlSel + "_" + b + "_" + k, (FastGraph<U> graph, FastPreferenceData<U,U> prefData) ->
                               new BM25Similarity(graph, uSel, vSel, dlSel, b, k)))))));
        
        return sims;
    }

    @Override
    public Map<String, Supplier<Similarity>> grid(Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        Map<String, Supplier<Similarity>> sims = new HashMap<>();
         
        List<EdgeOrientation> uSels = grid.getOrientationValues(USEL);
        List<EdgeOrientation> vSels = grid.getOrientationValues(VSEL);
        List<EdgeOrientation> dlSels = grid.getOrientationValues(DLSEL);
        List<Double> bs = grid.getDoubleValues(B);
        List<Double> ks = grid.getDoubleValues(K);
        if(uSels == null || uSels.isEmpty() || vSels == null || vSels.isEmpty() || dlSels == null || dlSels.isEmpty())
        {
            return sims;
        }

        uSels.forEach(uSel ->
            vSels.forEach(vSel ->
                dlSels.forEach(dlSel ->
                    bs.forEach(b ->
                        ks.forEach(k ->
                            sims.put(BM25 + "_" + uSel + "_" + vSel + "_" + dlSel + "_" + b + "_" + k, () ->
                                new BM25Similarity(graph, uSel, vSel, dlSel, b, k)))))));

        return sims;
    }
    
}