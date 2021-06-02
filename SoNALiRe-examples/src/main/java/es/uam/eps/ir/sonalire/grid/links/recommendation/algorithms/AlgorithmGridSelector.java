/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms;

import es.uam.eps.ir.ranksys.fast.preference.FastPreferenceData;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.grid.Configurations;
import es.uam.eps.ir.sonalire.grid.Grid;
import es.uam.eps.ir.sonalire.grid.Parameters;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.knn.ItemBasedCFGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.knn.UserBasedCFGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.standalone.baselines.PopularityGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.standalone.baselines.RandomGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.standalone.contentbased.CentroidCBGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.standalone.contentbased.TwittomenderGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.standalone.foaf.*;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.standalone.ir.*;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.standalone.mf.FastImplicitMFGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.standalone.mf.ImplicitMFGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.standalone.pathbased.*;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.standalone.randomwalks.*;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.standalone.twitter.*;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.supervised.LambdaMARTGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.algorithms.supervised.WekaMLGridSearch;
import es.uam.eps.ir.sonalire.links.recommendation.algorithms.RecommendationAlgorithmFunction;
import es.uam.eps.ir.sonalire.utils.datatypes.Tuple2oo;
import org.ranksys.formats.parsing.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Class that translates from a grid to the different contact recommendation algorithms.
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class AlgorithmGridSelector<U>
{
    /**
     * A parser for users.
     */
    private final Parser<U> uParser;

    /**
     * Constructor.
     * @param uParser a parser for reading users from text.
     */
    public AlgorithmGridSelector(Parser<U> uParser)
    {
        this.uParser = uParser;
    }

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
            case AlgorithmIdentifiers.BIR -> new BIRGridSearch<>();
            case AlgorithmIdentifiers.BM25 -> new BM25GridSearch<>();
            case AlgorithmIdentifiers.EBM25 -> new EBM25GridSearch<>();
            case AlgorithmIdentifiers.QLJM -> new QLJMGridSearch<>();
            case AlgorithmIdentifiers.QLD -> new QLDGridSearch<>();
            case AlgorithmIdentifiers.QLL -> new QLLGridSearch<>();
            case AlgorithmIdentifiers.PL2 -> new PL2GridSearch<>();
            case AlgorithmIdentifiers.DLH -> new DLHGridSearch<>();
            case AlgorithmIdentifiers.DPH -> new DPHGridSearch<>();
            case AlgorithmIdentifiers.DFREE -> new DFReeGridSearch<>();
            case AlgorithmIdentifiers.DFREEKLIM -> new DFReeKLIMGridSearch<>();
            case AlgorithmIdentifiers.VSM -> new VSMGridSearch<>();
            case AlgorithmIdentifiers.PIVOTEDVSM -> new PivotedNormalizationVSMGridSearch<>();

            // Friends of friends algorithms
            case AlgorithmIdentifiers.ADAMIC -> new AdamicAdarGridSearch<>();
            case AlgorithmIdentifiers.JACCARD -> new JaccardGridSearch<>();
            case AlgorithmIdentifiers.MCN -> new MostCommonNeighborsGridSearch<>();
            case AlgorithmIdentifiers.COSINE -> new CosineGridSearch<>();
            case AlgorithmIdentifiers.LOCALLHN -> new LocalLHNIndexGridSearch<>();
            case AlgorithmIdentifiers.RESALLOC -> new ResourceAllocationGridSearch<>();
            case AlgorithmIdentifiers.HPI -> new HubPromotedIndexGridSearch<>();
            case AlgorithmIdentifiers.HDI -> new HubDepressedIndexGridSearch<>();
            case AlgorithmIdentifiers.SORENSEN -> new SorensenGridSearch<>();

            // Path-based algorithms
            case AlgorithmIdentifiers.DISTANCE -> new ShortestDistanceGridSearch<>();
            case AlgorithmIdentifiers.KATZ -> new KatzGridSearch<>();
            case AlgorithmIdentifiers.LPI -> new LocalPathIndexGridSearch<>();
            case AlgorithmIdentifiers.GLOBALLHN -> new GlobalLHNIndexGridSearch<>();
            case AlgorithmIdentifiers.PIC -> new PseudoInverseCosineGridSearch<>();
            case AlgorithmIdentifiers.MATRIXFOREST -> new MatrixForestGridSearch<>();

            // Random walks
            case AlgorithmIdentifiers.PAGERANK -> new PageRankGridSearch<>();
            case AlgorithmIdentifiers.HITS -> new HITSGridSearch<>();
            case AlgorithmIdentifiers.SALSA -> new SALSAGridSearch<>();

            case AlgorithmIdentifiers.COMMUTE -> new CommuteTimeGridSearch<>();
            case AlgorithmIdentifiers.HITTING -> new HittingTimeGridSearch<>();
            case AlgorithmIdentifiers.COMMUTEPERS -> new CommuteTimePersPageRankGridSearch<>();
            case AlgorithmIdentifiers.HITTINGPERS -> new HittingTimePersPageRankGridSearch<>();

            case AlgorithmIdentifiers.PERSPAGERANK -> new PersonalizedPageRankGridSearch<>();
            case AlgorithmIdentifiers.PERSHITS -> new PersonalizedHITSGridSearch<>();
            case AlgorithmIdentifiers.PERSSALSA -> new PersonalizedSALSAGridSearch<>();
            case AlgorithmIdentifiers.PROPFLOW -> new PropFlowGridSearch<>();

            // Twitter algorithms
            case AlgorithmIdentifiers.MONEY -> new MoneyGridSearch<>();
            case AlgorithmIdentifiers.LOVE -> new LoveGridSearch<>();
            case AlgorithmIdentifiers.TWITTERAVGCOS -> new TwitterAverageCosineSimilarityGridSearch<>();
            case AlgorithmIdentifiers.TWITTERCENTROIDCOS -> new TwitterCentroidCosineSimilarityGridSearch<>();
            case AlgorithmIdentifiers.TWITTERMAXCOS -> new TwitterMaximumCosineSimilarityGridSearch<>();

            // Matrix factorization
            case AlgorithmIdentifiers.IMF -> new ImplicitMFGridSearch<>();
            case AlgorithmIdentifiers.FASTIMF -> new FastImplicitMFGridSearch<>();

            // kNN
            case AlgorithmIdentifiers.UB -> new UserBasedCFGridSearch<>(uParser);
            case AlgorithmIdentifiers.IB -> new ItemBasedCFGridSearch<>(uParser);

            // Baselines
            case AlgorithmIdentifiers.RANDOM -> new RandomGridSearch<>();
            case AlgorithmIdentifiers.POP -> new PopularityGridSearch<>();

            // Content-based approaches
            case AlgorithmIdentifiers.CENTROIDCB -> new CentroidCBGridSearch<>(uParser);
            case AlgorithmIdentifiers.TWITTOMENDER -> new TwittomenderGridSearch<>(uParser);

            // Supervised approaches:
            case AlgorithmIdentifiers.LAMBDAMART -> new LambdaMARTGridSearch<>(uParser);
            case AlgorithmIdentifiers.WEKA -> new WekaMLGridSearch<>(uParser);

            default -> null;
        };
    }
}
