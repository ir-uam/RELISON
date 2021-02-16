/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.grid.Configurations;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.Parameters;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn.ItemBasedCFGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn.UserBasedCFGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.standalone.baselines.PopularityGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.standalone.baselines.RandomGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.standalone.distance.ShortestDistanceGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.standalone.foaf.*;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.standalone.ir.*;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.standalone.mf.ImplicitMFGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.standalone.pathbased.KatzGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.standalone.pathbased.LocalPathIndexGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.standalone.randomwalks.PersonalizedHITSGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.standalone.randomwalks.PersonalizedPageRankGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.standalone.randomwalks.PersonalizedSALSAGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.standalone.randomwalks.PropFlowGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.standalone.twitter.*;
import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.AlgorithmIdentifiers.*;

/**
 * Class that translates from a grid to the different contact recommendation algorithns.
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class AlgorithmGridSelector<U>
{
    /**
     * Given preference data, obtains recommenders.
     *
     * @param algorithm the name of the algorithm.
     * @param grid      the parameter grid for the algorithm.
     * @param graph     the training graph.
     * @param prefData  the training data
     *
     * @return the suppliers for the different algorithm variants, indexed by name.
     */
    public Map<String, Supplier<Recommender<U, U>>> getRecommenders(String algorithm, Grid grid, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        AlgorithmGridSearch<U> gridsearch = this.selectGridSearch(algorithm);
        if (gridsearch != null)
            return gridsearch.grid(grid, graph, prefData);
        return null;
    }

    /**
     * Given preference data, obtains a single algorithm.
     *
     * @param algorithm the name of the algorithm.
     * @param params    the parameters of the algorithm.
     * @param graph     the training graph.
     * @param prefData  the training data.
     *
     * @return a tuple containing the name and a supplier of the algorithm.
     */
    public Tuple2oo<String, Supplier<Recommender<U, U>>> getRecommender(String algorithm, Parameters params, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        AlgorithmGridSearch<U> gridsearch = this.selectGridSearch(algorithm);
        if (gridsearch != null)
        {
            Grid grid = params.toGrid();
            Map<String, Supplier<Recommender<U, U>>> map = getRecommenders(algorithm, grid, graph, prefData);
            if (map == null || map.isEmpty()) return null;

            List<String> algs = new ArrayList<>(map.keySet());
            String name = algs.get(0);
            Supplier<Recommender<U, U>> supplier = map.get(name);
            return new Tuple2oo<>(name, supplier);
        }

        return null;
    }

    /**
     * Given preference data and a graph, obtains a set of algorithms.
     *
     * @param algorithm the name of the algorithm.
     * @param configs   the different configurations for the algorithm.
     * @param graph     the training graph.
     * @param prefData  the training data.
     *
     * @return a map containing the suppliers of the algorithms, ordered by name.
     */
    public Map<String, Supplier<Recommender<U, U>>> getRecommenders(String algorithm, Configurations configs, FastGraph<U> graph, FastPreferenceData<U, U> prefData)
    {
        Map<String, Supplier<Recommender<U, U>>> recs = new HashMap<>();
        AlgorithmGridSearch<U> gridSearch = this.selectGridSearch(algorithm);
        if (gridSearch != null)
        {
            for (Parameters params : configs.getConfigurations())
            {
                Grid grid = params.toGrid();
                Map<String, Supplier<Recommender<U, U>>> map = getRecommenders(algorithm, grid, graph, prefData);
                if (map == null || map.isEmpty()) return null;

                List<String> algs = new ArrayList<>(map.keySet());
                String name = algs.get(0);
                Supplier<Recommender<U, U>> supplier = map.get(name);
                recs.put(name, supplier);
            }
        }

        return recs;
    }

    /**
     * Given preference data, obtains recommenders.
     *
     * @param algorithm the name of the algorithm.
     * @param grid      the parameter grid for the algorithm.
     *
     * @return functions for obtaining for the different algorithm variants given the graph and preference data, indexed by name.
     */
    public Map<String, RecommendationAlgorithmFunction<U>> getRecommenders(String algorithm, Grid grid)
    {
        AlgorithmGridSearch<U> gridsearch = this.selectGridSearch(algorithm);
        if (gridsearch != null)
            return gridsearch.grid(grid);
        return null;
    }

    /**
     * Given preference data, obtains recommenders.
     *
     * @param algorithm the name of the algorithm.
     * @param configs   configurations for the algorithm.
     *
     * @return functions for obtaining for the different algorithm variants given the graph and preference data, indexed by name.
     */
    public Map<String, RecommendationAlgorithmFunction<U>> getRecommenders(String algorithm, Configurations configs)
    {
        AlgorithmGridSearch<U> gridsearch = this.selectGridSearch(algorithm);
        Map<String, RecommendationAlgorithmFunction<U>> recs = new HashMap<>();
        if (gridsearch != null)
        {
            for (Parameters params : configs.getConfigurations())
            {
                Grid grid = params.toGrid();
                Map<String, RecommendationAlgorithmFunction<U>> map = getRecommenders(algorithm, grid);
                if (map == null || map.isEmpty()) return null;

                List<String> algs = new ArrayList<>(map.keySet());
                String name = algs.get(0);

                recs.put(name, map.get(name));
            }

            return recs;
        }

        return null;
    }

    /**
     * Obtains a single algorithm.
     *
     * @param algorithm the name of the algorithm.
     * @param params    the parameters of the algorithm.
     *
     * @return a tuple containing the name and a function for obtaining the algorithm.
     */
    public Tuple2oo<String, RecommendationAlgorithmFunction<U>> getRecommender(String algorithm, Parameters params)
    {
        AlgorithmGridSearch<U> gridsearch = this.selectGridSearch(algorithm);
        if (gridsearch != null)
        {
            Grid grid = params.toGrid();
            Map<String, RecommendationAlgorithmFunction<U>> map = getRecommenders(algorithm, grid);
            if (map == null || map.isEmpty()) return null;

            List<String> algs = new ArrayList<>(map.keySet());
            String name = algs.get(0);
            return new Tuple2oo<>(name, map.get(name));
        }
        return null;
    }

    /**
     * Selects a grid search given the name of an algorithm.
     *
     * @param algorithm the name of the algorithm.
     *
     * @return if the algorithm exists, returns its grid search, null otherwise.
     */
    public AlgorithmGridSearch<U> selectGridSearch(String algorithm)
    {
        return switch (algorithm)
        {
            // IR algorithms
            case BIR -> new BIRGridSearch<>();
            case BM25 -> new BM25GridSearch<>();
            case EBM25 -> new EBM25GridSearch<>();
            case QLJM -> new QLJMGridSearch<>();
            case QLD -> new QLDGridSearch<>();
            case QLL -> new QLLGridSearch<>();
            case PL2 -> new PL2GridSearch<>();
            case DLH -> new DLHGridSearch<>();
            case DPH -> new DPHGridSearch<>();
            case DFREE -> new DFReeGridSearch<>();
            case DFREEKLIM -> new DFReeKLIMGridSearch<>();
            case VSM -> new VSMGridSearch<>();

            // Friends of friends algorithms
            case ADAMIC -> new AdamicAdarGridSearch<>();
            case JACCARD -> new JaccardGridSearch<>();
            case MCN -> new MostCommonNeighborsGridSearch<>();
            case COSINE -> new CosineGridSearch<>();
            case LOCALLHN -> new LocalLHNIndexGridSearch<>();
            case RESALLOC -> new ResourceAllocationGridSearch<>();
            case HPI -> new HubPromotedIndexGridSearch<>();
            case HDI -> new HubDepressedIndexGridSearch<>();
            case SORENSEN -> new SorensenGridSearch<>();

            // Path-based algorithms
            case DISTANCE -> new ShortestDistanceGridSearch<>();
            case KATZ -> new KatzGridSearch<>();
            case LPI -> new LocalPathIndexGridSearch<>();

            // Random walks
            case PERSPAGERANK -> new PersonalizedPageRankGridSearch<>();
            case PERSHITS -> new PersonalizedHITSGridSearch<>();
            case PERSSALSA -> new PersonalizedSALSAGridSearch<>();
            case PROPFLOW -> new PropFlowGridSearch<>();

            // Twitter algorithms
            case MONEY -> new MoneyGridSearch<>();
            case LOVE -> new LoveGridSearch<>();
            case TWITTERAVGCOS -> new TwitterAverageCosineSimilarityGridSearch<>();
            case TWITTERCENTROIDCOS -> new TwitterCentroidCosineSimilarityGridSearch<>();
            case TWITTERMAXCOS -> new TwitterMaximumCosineSimilarityGridSearch<>();

            // Matrix factorization
            case IMF -> new ImplicitMFGridSearch<>();

            // kNN
            case UB -> new UserBasedCFGridSearch<>();
            case IB -> new ItemBasedCFGridSearch<>();

            // Baselines
            case RANDOM -> new RandomGridSearch<>();
            case POP -> new PopularityGridSearch<>();
            default -> null;
        };
    }
}
