/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers;

import es.uam.eps.ir.sonalire.community.Communities;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.grid.Configurations;
import es.uam.eps.ir.sonalire.grid.Grid;
import es.uam.eps.ir.sonalire.grid.Parameters;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.local.RandomRerankerGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.local.communities.StrongTiesRerankerGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.local.communities.WeakTiesRerankerGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.swap.edge.AverageEmbeddednessRerankerGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.swap.edge.AverageWeaknessRerankerGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.swap.edge.HeuristicAverageEmbeddednessRerankerGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.swap.edge.HeuristicAverageWeaknessRerankerGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.swap.graph.ClusteringCoefficientComplementRerankerGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.swap.graph.ClusteringCoefficientRerankerGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.swap.graph.DegreeGiniComplementRerankerGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.swap.graph.comms.gini.degree.CompleteCommunityDegreeGiniRerankerGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.swap.graph.comms.gini.degree.CompleteCommunityOuterDegreeGiniRerankerGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.swap.graph.comms.gini.degree.InterCommunityDegreeGiniRerankerGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.swap.graph.comms.gini.degree.InterCommunityOuterDegreeGiniRerankerGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.swap.graph.comms.gini.degree.sizenorm.SizeNormCompleteCommunityDegreeGiniRerankerGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.swap.graph.comms.gini.degree.sizenorm.SizeNormCompleteCommunityOuterDegreeGiniRerankerGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.swap.graph.comms.gini.degree.sizenorm.SizeNormInterCommunityDegreeGiniRerankerGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.swap.graph.comms.gini.degree.sizenorm.SizeNormInterCommunityOuterDegreeGiniRerankerGridSearch;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.swap.graph.comms.gini.edge.*;
import es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.swap.graph.comms.gini.edge.sizenorm.*;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.global.GlobalReranker;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.normalizer.Normalizer;
import org.jooq.lambda.tuple.Tuple2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.sonalire.grid.links.recommendation.rerankers.RerankerIdentifiers.*;


/**
 * Class that translates from a grid to the different contact recommendation algorithns.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class RerankerGridSelector<U>
{
    /**
     * Given some initial data, obtains the reranking algorithms.
     *
     * @param reranker  the name of the reranking approach.
     * @param grid      the parameter grid of the algorithm.
     * @param cutoff    the size of the definitive ranking.
     * @param norm      suppliers for the normalization scheme.
     * @param graph     the training graph.
     * @param comms     the community partition for the training graph.
     *
     * @return the suppliers for the different reranker variants, indexed by name.
     */
    public Map<String, Supplier<GlobalReranker<U,U>>> getRerankers(String reranker, Grid grid, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> comms)
    {
        RerankerGridSearch<U> gridsearch = this.getRerankerGridSearch(reranker);
        if(gridsearch != null)
        {
            return gridsearch.grid(grid, cutoff, norm, graph, comms);
        }
        return null;
    }

    /**
     * Given some initial data, obtains a single reranking algorithm.
     *
     * @param reranker  the name of the reranking approach.
     * @param params    the parameters of the algorithm.
     * @param cutoff    the size of the definitive ranking.
     * @param norm      suppliers for the normalization scheme.
     * @param graph     the training graph.
     * @param comms     the community partition for the training graph.
     *
     * @return the suppliers for the different reranker variants, indexed by name.
     */
    public Tuple2<String, Supplier<GlobalReranker<U,U>>> getReranker(String reranker, Parameters params, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> comms)
    {
        RerankerGridSearch<U> gridsearch = this.getRerankerGridSearch(reranker);
        if(gridsearch != null)
        {
            Grid grid = params.toGrid();
            Map<String, Supplier<GlobalReranker<U,U>>> map = this.getRerankers(reranker, grid, cutoff, norm, graph, comms);
            if(map == null || map.isEmpty()) return null;

            List<String> algs = new ArrayList<>(map.keySet());
            String name = algs.get(0);
            Supplier<GlobalReranker<U,U>> supplier = map.get(name);
            return new Tuple2<>(name, supplier);
        }
        return null;
    }

    /**
     * Given some initial data, obtains the reranking algorithms.
     *
     * @param reranker  the name of the reranking approach.
     * @param configs   the different configurations for the algorithm.
     * @param cutoff    the size of the definitive ranking.
     * @param norm      suppliers for the normalization scheme.
     * @param graph     the training graph.
     * @param comms     the community partition for the training graph.
     *
     * @return the suppliers for the different reranker variants, indexed by name.
     */
    public Map<String, Supplier<GlobalReranker<U,U>>> getRerankers(String reranker, Configurations configs, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> comms)
    {
        Map<String, Supplier<GlobalReranker<U,U>>> rerankers = new HashMap<>();
        RerankerGridSearch<U> gridsearch = this.getRerankerGridSearch(reranker);
        if(gridsearch != null)
        {
            for (Parameters params : configs.getConfigurations())
            {
                Tuple2<String, Supplier<GlobalReranker<U,U>>> rer = this.getReranker(reranker, params, cutoff, norm, graph, comms);
                if (rer == null) continue;
                rerankers.put(rer.v1, rer.v2);
            }
        }
        return rerankers;
    }

    /**
     * Given some initial data, obtains the reranking algorithms.
     *
     * @param reranker  the name of the reranking approach.
     * @param grid      the parameter grid of the algorithm.
     *
     * @return the suppliers for the different reranker variants, indexed by name.
     */
    public Map<String, GlobalRerankerFunction<U>> getRerankers(String reranker, Grid grid)
    {
        RerankerGridSearch<U> gridsearch = this.getRerankerGridSearch(reranker);
        if(gridsearch != null)
        {
            return gridsearch.grid(grid);
        }
        return null;
    }

    /**
     * Given some initial data, obtains a single reranking algorithm.
     *
     * @param reranker  the name of the reranking approach.
     * @param params    the parameters of the algorithm.
     *
     * @return the suppliers for the different reranker variants, indexed by name.
     */
    public Tuple2<String, GlobalRerankerFunction<U>> getReranker(String reranker, Parameters params)
    {
        RerankerGridSearch<U> gridsearch = this.getRerankerGridSearch(reranker);
        if(gridsearch != null)
        {
            Grid grid = params.toGrid();
            Map<String, GlobalRerankerFunction<U>> map = this.getRerankers(reranker, grid);
            if(map == null || map.isEmpty()) return null;

            List<String> algs = new ArrayList<>(map.keySet());
            String name = algs.get(0);
            GlobalRerankerFunction<U> supplier = map.get(name);
            return new Tuple2<>(name, supplier);
        }
        return null;
    }

    /**
     * Given some initial data, obtains the reranking algorithms.
     *
     * @param reranker  the name of the reranking approach.
     * @param configs   the different configurations for the algorithm.

     *
     * @return the suppliers for the different reranker variants, indexed by name.
     */
    public Map<String, GlobalRerankerFunction<U>> getRerankers(String reranker, Configurations configs)
    {
        Map<String, GlobalRerankerFunction<U>> rerankers = new HashMap<>();
        RerankerGridSearch<U> gridsearch = this.getRerankerGridSearch(reranker);
        if(gridsearch != null)
        {
            for (Parameters params : configs.getConfigurations())
            {
                Tuple2<String, GlobalRerankerFunction<U>> rer = this.getReranker(reranker, params);
                if (rer == null) continue;
                rerankers.put(rer.v1, rer.v2);
            }
        }
        return rerankers;
    }


    /**
     * Obtains the different variants of the available algorithms using the different
     * parameters in the grid.
     * @return a map containing the different algorithm suppliers.
     */
    public RerankerGridSearch<U> getRerankerGridSearch(String reranker)
    {
        // Default behavior

        return switch (reranker)
        {
            case RANDOM -> new RandomRerankerGridSearch<>();
            case WEAKTIES -> new WeakTiesRerankerGridSearch<>();
            case STRONGTIES -> new StrongTiesRerankerGridSearch<>();
            case CLUSTCOEF -> new ClusteringCoefficientRerankerGridSearch<>();
            case CLUSTCOEFCOMPL -> new ClusteringCoefficientComplementRerankerGridSearch<>();
            case DEGREEGINICOMPL -> new DegreeGiniComplementRerankerGridSearch<>();
            case AVGEMBEDDEDNESS -> new AverageEmbeddednessRerankerGridSearch<>();
            case AVGWEAKNESS -> new AverageWeaknessRerankerGridSearch<>();
            case HEURISTICAVGEMBEDDEDNESS -> new HeuristicAverageEmbeddednessRerankerGridSearch<>();
            case HEURISTICAVGWEAKNESS -> new HeuristicAverageWeaknessRerankerGridSearch<>();
            case ICDEGREEGINI -> new InterCommunityDegreeGiniRerankerGridSearch<>();
            case OUTERICDEGREEGINI -> new InterCommunityOuterDegreeGiniRerankerGridSearch<>();
            case CDEGREEGINI -> new CompleteCommunityDegreeGiniRerankerGridSearch<>();
            case OUTERCDEGREEGINI -> new CompleteCommunityOuterDegreeGiniRerankerGridSearch<>();
            case SNICDEGREEGINI -> new SizeNormInterCommunityDegreeGiniRerankerGridSearch<>();
            case OUTERSNICDEGREEGINI -> new SizeNormInterCommunityOuterDegreeGiniRerankerGridSearch<>();
            case SNCDEGREEGINI -> new SizeNormCompleteCommunityDegreeGiniRerankerGridSearch<>();
            case OUTERSNCDEGREEGINI -> new SizeNormCompleteCommunityOuterDegreeGiniRerankerGridSearch<>();
            case ICEDGEGINI -> new InterCommunityEdgeGiniRerankerGridSearch<>();
            case OUTERICEDGEGINI -> new InterCommunityOuterEdgeGiniRerankerGridSearch<>();
            case CEDGEGINI -> new CompleteCommunityEdgeGiniRerankerGridSearch<>();
            case OUTERCEDGEGINI -> new CompleteCommunityOuterEdgeGiniRerankerGridSearch<>();
            case SCEDGEGINI -> new SemiCompleteCommunityEdgeGiniRerankerGridSearch<>();
            case ALTSCEDGEGINI -> new AlternativeSemiCompleteCommunityEdgeGiniRerankerGridSearch<>();
            case OUTERSCEDGEGINI -> new SemiCompleteCommunityOuterEdgeGiniRerankerGridSearch<>();
            case ALTOUTERSCEDGEGINI -> new AlternativeSemiCompleteCommunityOuterEdgeGiniRerankerGridSearch<>();
            case SNICEDGEGINI -> new SizeNormInterCommunityEdgeGiniRerankerGridSearch<>();
            case OUTERSNICEDGEGINI -> new SizeNormInterCommunityOuterEdgeGiniRerankerGridSearch<>();
            case SNCEDGEGINI -> new SizeNormCompleteCommunityEdgeGiniRerankerGridSearch<>();
            case OUTERSNCEDGEGINI -> new SizeNormCompleteCommunityOuterEdgeGiniRerankerGridSearch<>();
            case SNSCEDGEGINI -> new SizeNormSemiCompleteCommunityEdgeGiniRerankerGridSearch<>();
            case OUTERSNSCEDGEGINI -> new SizeNormSemiCompleteCommunityOuterEdgeGiniRerankerGridSearch<>();
            default -> null;
        };
    }
}
