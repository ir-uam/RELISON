/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn.similarities;


import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.nn.sim.Similarity;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.grid.Configurations;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.Parameters;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn.similarities.foaf.AdamicSimilarityGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn.similarities.foaf.CosineSimilarityGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn.similarities.foaf.JaccardSimilarityGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn.similarities.foaf.MostCommonNeighborsSimilarityGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn.similarities.ir.*;
import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn.similarities.SimilarityIdentifiers.*;

/**
 * Class for selecting the similarity to use.
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 * @param <U> Type of the users.
 */
public class SimilarityGridSelector<U>
{
    /**
     * Given preference data, obtains similarities.
     * @param sim the name of the similarity.
     * @param grid the parameter grid for the similarity.
     * @param graph the training graph.
     * @param prefData the training data
     * @return the suppliers for the different similarity variants, indexed by name.
     */
    public Map<String, Supplier<Similarity>> getSimilarities(String sim, Grid grid, FastGraph<U> graph, FastPreferenceData<U,U> prefData)
    {
        SimilarityGridSearch<U> gridsearch = this.selectGridSearch(sim);
        if(gridsearch != null)
            return gridsearch.grid(grid, graph, prefData);
        return null;
    }
    
    /**
     * Obtains a single similarity configuration.
     * @param sim the name of the similarity.
     * @param params the parameters of the similarity.
     * @param graph the training graph.
     * @param prefData the training data.
     * @return a pair containing the full name and the supplier for the similarity.
     */
    public Tuple2oo<String, Supplier<Similarity>> getSimilarity(String sim, Parameters params, FastGraph<U> graph, FastPreferenceData<U,U> prefData)
    {
        SimilarityGridSearch<U> gridsearch = this.selectGridSearch(sim);
        if(gridsearch != null)
        {
            Grid grid = params.toGrid();
            Map<String, Supplier<Similarity>> sims = gridsearch.grid(grid, graph, prefData);
            List<String> names = new ArrayList<>(sims.keySet());
            String name = names.get(0);
            Supplier<Similarity> s = sims.get(name);
            return new Tuple2oo<>(name, s);
        }
        return null;
    }
    
    /**
     * Given preference data, obtains recommenders.
     * @param sim the name of the similarity.
     * @param grid the parameter grid for the similarity.
     * @return functions for obtaining for the different similarity variants given the graph and preference data, indexed by name.
     */
    public Map<String, SimilarityFunction<U>> getSimilarities(String sim, Grid grid)
    {
        SimilarityGridSearch<U> gridsearch = this.selectGridSearch(sim);
        if(gridsearch != null)
            return gridsearch.grid(grid);
        return null;
    }
    
    /**
     * Given preference data and a graph, obtains a set of similarities.
     * @param algorithm the name of the similarity.
     * @param configs the different configurations for the similarity.
     * @param graph the training graph.
     * @param prefData the training data.
     * @return a map containing the suppliers of the algorithms, ordered by name.
     */
    public Map<String, Supplier<Similarity>> getSimilarities(String algorithm, Configurations configs, FastGraph<U> graph, FastPreferenceData<U,U> prefData)
    {
        Map<String, Supplier<Similarity>> recs = new HashMap<>();
        SimilarityGridSearch<U> gridSearch = this.selectGridSearch(algorithm);
        if(gridSearch != null)
        {
            for(Parameters params : configs.getConfigurations())
            {
                Grid grid = params.toGrid();
                Map<String, Supplier<Similarity>> map = getSimilarities(algorithm, grid, graph, prefData);
                if(map == null || map.isEmpty()) return null;
            
                List<String> algs = new ArrayList<>(map.keySet());
                String name = algs.get(0);
                Supplier<Similarity> supplier = map.get(name);
                recs.put(name, supplier);
            }
        }
        
        return recs;
    }
    
    /**
     * Given preference data, obtains recommenders.
     * @param algorithm the name of the algorithm.
     * @param configs configurations for the algorithm.
     * @return functions for obtaining for the different algorithm variants given the graph and preference data, indexed by name.
     */
    public Map<String, SimilarityFunction<U>> getSimilarities(String algorithm, Configurations configs)
    {
        SimilarityGridSearch<U> gridsearch = this.selectGridSearch(algorithm);
        Map<String, SimilarityFunction<U>> recs = new HashMap<>();
        if(gridsearch != null)
        {
            for(Parameters params : configs.getConfigurations())
            {
                Grid grid = params.toGrid();
                Map<String, SimilarityFunction<U>> map = getSimilarities(algorithm, grid);
                if(map == null || map.isEmpty()) return null;
                
                List<String> algs = new ArrayList<>(map.keySet());
                String name = algs.get(0);
                
                recs.put(name, map.get(name));
            }
            
            return recs;
        }
            
        return null;
    }
    
    
    
    /**
     * Obtains a single similarity configuration.
     * @param sim the name of the similarity.
     * @param params the parameters of the similarity.
     * @return a pair containing the full name and the supplier for the similarity.
     */
    public Tuple2oo<String, SimilarityFunction<U>> getSimilarity(String sim, Parameters params)
    {
        SimilarityGridSearch<U> gridsearch = this.selectGridSearch(sim);
        if(gridsearch != null)
        {
            Grid grid = params.toGrid();
            Map<String, SimilarityFunction<U>> sims = gridsearch.grid(grid);
            List<String> names = new ArrayList<>(sims.keySet());
            String name = names.get(0);
            SimilarityFunction<U> s = sims.get(name);
            return new Tuple2oo<>(name, s);
        }
        return null;
    }
    
    /**
     * Obtains a grid search for a similarity.
     * @param sim the name of the similarity.
     * @return the grid search if it exists, null otherwise.
     */
    public SimilarityGridSearch<U> selectGridSearch(String sim)
    {
        if(sim == null) return null;

        return switch (sim)
        {
            // Friends of friends similarities
            case ADAMIC -> new AdamicSimilarityGridSearch<>();
            case MCN -> new MostCommonNeighborsSimilarityGridSearch<>();
            case JACCARD -> new JaccardSimilarityGridSearch<>();
            case VECTORCOSINE -> new CosineSimilarityGridSearch<>();
            // IR similarities
            case VSM -> new VSMSimilarityGridSearch<>();
            case BIR -> new BIRSimilarityGridSearch<>();
            case BM25 -> new BM25SimilarityGridSearch<>();
            case EBM25 -> new EBM25SimilarityGridSearch<>();
            case QLJM -> new QLJMSimilarityGridSearch<>();
            case QLD -> new QLDSimilarityGridSearch<>();
            case QLL -> new QLLSimilarityGridSearch<>();
            case DFREE -> new DFReeSimilarityGridSearch<>();
            case DFREEKLIM -> new DFReeKLIMSimilarityGridSearch<>();
            case DLH -> new DLHSimilarityGridSearch<>();
            case DPH -> new DPHSimilarityGridSearch<>();
            case PL2 -> new PL2SimilarityGridSearch<>();
            // Default case
            default -> null;
        };
    }
}
