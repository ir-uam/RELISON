/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.local.RandomRerankerGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.local.communities.StrongTiesRerankerGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.local.communities.WeakTiesRerankerGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.swap.edge.AverageEmbeddednessRerankerGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.swap.edge.AverageWeaknessRerankerGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.swap.edge.HeuristicAverageEmbeddednessRerankerGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.swap.edge.HeuristicAverageWeaknessRerankerGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.swap.graph.ClusteringCoefficientComplementRerankerGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.swap.graph.ClusteringCoefficientRerankerGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.swap.graph.DegreeGiniComplementRerankerGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.swap.graph.comms.gini.degree.CompleteCommunityDegreeGiniRerankerGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.swap.graph.comms.gini.degree.CompleteCommunityOuterDegreeGiniRerankerGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.swap.graph.comms.gini.degree.InterCommunityDegreeGiniRerankerGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.swap.graph.comms.gini.degree.InterCommunityOuterDegreeGiniRerankerGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.swap.graph.comms.gini.degree.sizenorm.SizeNormCompleteCommunityDegreeGiniRerankerGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.swap.graph.comms.gini.degree.sizenorm.SizeNormCompleteCommunityOuterDegreeGiniRerankerGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.swap.graph.comms.gini.degree.sizenorm.SizeNormInterCommunityDegreeGiniRerankerGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.swap.graph.comms.gini.degree.sizenorm.SizeNormInterCommunityOuterDegreeGiniRerankerGridSearch;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.swap.graph.comms.gini.edge.*;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.swap.graph.comms.gini.edge.sizenorm.*;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.GlobalReranker;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.RerankerIdentifiers.*;


/**
 * Class that translates from a grid to the different contact recommendation algorithns.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class RerankerGridSelector<U>
{
    /**
     * Algorithm name
     */
    private final String reranker;
    /**
     * Grid that contains the different possible parameters for the algorithm.
     */
    private final Grid grid;
    /**
     * Maximum number of edges in the definitive ranking
     */
    private final int cutoff;
    /**
     * Indicates if scores have to be normalized
     */
    private final boolean norm;
    /**
     * Training graph.
     */
    private final Graph<U> graph;
    /**
     * Communities
     */
    private final Communities<U> comms;
    /**
     * Indicates if the normalization is done by ranking or by score.
     */
    private final boolean rank;
    
    /**
     * Constructor.
     * @param reranker name of the reranker
     * @param grid grid parameters.
     * @param cutoff The cutoff to apply to the reranker.
     * @param norm true if the scores have to be normalized or not.
     * @param graph the training graph.
     * @param comms community partition of the graph
     * @param rank true if the normalization is by ranking or false if it is done by score
     */
    public RerankerGridSelector(String reranker, Grid grid, int cutoff, boolean norm, boolean rank, Graph<U> graph, Communities<U> comms)
    {
        this.reranker = reranker;
        this.grid = grid;
        this.cutoff = cutoff;
        this.norm = norm;
        this.rank = rank;
        this.graph = graph;
        this.comms = comms;
        
    }
    
    /**
     * Obtains the different variants of the available algorithms using the different
     * parameters in the grid.
     * @return a map containing the different algorithm suppliers.
     */
    public Map<String, Supplier<GlobalReranker<U,U>>> getRecommenders()
    {
        // Default behavior
        RerankerGridSearch<U> gridsearch = switch (this.reranker)
                {
                    case RANDOM -> new RandomRerankerGridSearch<>(cutoff, norm, rank);
                    case WEAKTIES -> new WeakTiesRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    case STRONGTIES -> new StrongTiesRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    case CLUSTCOEF -> new ClusteringCoefficientRerankerGridSearch<>(cutoff, norm, rank, graph);
                    case CLUSTCOEFCOMPL -> new ClusteringCoefficientComplementRerankerGridSearch<>(cutoff, norm, rank, graph);
                    case DEGREEGINICOMPL -> new DegreeGiniComplementRerankerGridSearch<>(cutoff, norm, rank, graph);
                    case AVGEMBEDDEDNESS -> new AverageEmbeddednessRerankerGridSearch<>(cutoff, norm, rank, graph);
                    case AVGWEAKNESS -> new AverageWeaknessRerankerGridSearch<>(cutoff, norm, rank, graph);
                    case HEURISTICAVGEMBEDDEDNESS -> new HeuristicAverageEmbeddednessRerankerGridSearch<>(cutoff, norm, rank, graph);
                    case HEURISTICAVGWEAKNESS -> new HeuristicAverageWeaknessRerankerGridSearch<>(cutoff, norm, rank, graph);
                    case ICDEGREEGINI -> new InterCommunityDegreeGiniRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    case OUTERICDEGREEGINI -> new InterCommunityOuterDegreeGiniRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    case CDEGREEGINI -> new CompleteCommunityDegreeGiniRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    case OUTERCDEGREEGINI -> new CompleteCommunityOuterDegreeGiniRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    case SNICDEGREEGINI -> new SizeNormInterCommunityDegreeGiniRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    case OUTERSNICDEGREEGINI -> new SizeNormInterCommunityOuterDegreeGiniRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    case SNCDEGREEGINI -> new SizeNormCompleteCommunityDegreeGiniRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    case OUTERSNCDEGREEGINI -> new SizeNormCompleteCommunityOuterDegreeGiniRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    case ICEDGEGINI -> new InterCommunityEdgeGiniRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    case OUTERICEDGEGINI -> new InterCommunityOuterEdgeGiniRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    case CEDGEGINI -> new CompleteCommunityEdgeGiniRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    case OUTERCEDGEGINI -> new CompleteCommunityOuterEdgeGiniRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    case SCEDGEGINI -> new SemiCompleteCommunityEdgeGiniRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    case ALTSCEDGEGINI -> new AlternativeSemiCompleteCommunityEdgeGiniRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    case OUTERSCEDGEGINI -> new SemiCompleteCommunityOuterEdgeGiniRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    case ALTOUTERSCEDGEGINI -> new AlternativeSemiCompleteCommunityOuterEdgeGiniRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    case SNICEDGEGINI -> new SizeNormInterCommunityEdgeGiniRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    case OUTERSNICEDGEGINI -> new SizeNormInterCommunityOuterEdgeGiniRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    case SNCEDGEGINI -> new SizeNormCompleteCommunityEdgeGiniRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    case OUTERSNCEDGEGINI -> new SizeNormCompleteCommunityOuterEdgeGiniRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    case SNSCEDGEGINI -> new SizeNormSemiCompleteCommunityEdgeGiniRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    case OUTERSNSCEDGEGINI -> new SizeNormSemiCompleteCommunityOuterEdgeGiniRerankerGridSearch<>(cutoff, norm, rank, graph, comms);
                    default -> null;
                };

        if(gridsearch != null)
            return gridsearch.grid(grid);
        return new HashMap<>();
    }
}
