/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.uam.eps.ir.socialranksys.grid.metrics.comm.global;


import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.metrics.comm.global.gini.degree.CompleteCommunityDegreeGiniGridSearch;
import es.uam.eps.ir.socialranksys.grid.metrics.comm.global.gini.degree.InterCommunityDegreeGiniGridSearch;
import es.uam.eps.ir.socialranksys.grid.metrics.comm.global.gini.degree.sizenormalized.SizeNormalizedCompleteCommunityDegreeGiniGridSearch;
import es.uam.eps.ir.socialranksys.grid.metrics.comm.global.gini.degree.sizenormalized.SizeNormalizedInterCommunityDegreeGiniGridSearch;
import es.uam.eps.ir.socialranksys.grid.metrics.comm.global.gini.edge.CompleteCommunityEdgeGiniGridSearch;
import es.uam.eps.ir.socialranksys.grid.metrics.comm.global.gini.edge.InterCommunityEdgeGiniGridSearch;
import es.uam.eps.ir.socialranksys.grid.metrics.comm.global.gini.edge.SemiCompleteCommunityEdgeGiniGridSearch;
import es.uam.eps.ir.socialranksys.grid.metrics.comm.global.gini.edge.dice.DiceCompleteCommunityEdgeGiniGridSearch;
import es.uam.eps.ir.socialranksys.grid.metrics.comm.global.gini.edge.dice.DiceInterCommunityEdgeGiniGridSearch;
import es.uam.eps.ir.socialranksys.grid.metrics.comm.global.gini.edge.dice.DiceSemiCompleteCommunityEdgeGiniGridSearch;
import es.uam.eps.ir.socialranksys.grid.metrics.comm.global.gini.edge.sizenormalized.SizeNormalizedCompleteCommunityEdgeGiniGridSearch;
import es.uam.eps.ir.socialranksys.grid.metrics.comm.global.gini.edge.sizenormalized.SizeNormalizedInterCommunityEdgeGiniGridSearch;
import es.uam.eps.ir.socialranksys.grid.metrics.comm.global.gini.edge.sizenormalized.SizeNormalizedSemiCompleteCommunityEdgeGiniGridSearch;
import es.uam.eps.ir.socialranksys.grid.metrics.comm.global.gini.size.CommunitySizeGiniGridSearch;
import es.uam.eps.ir.socialranksys.metrics.CommunityMetric;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.socialranksys.grid.metrics.comm.global.GlobalCommunityMetricIdentifiers.*;

/**
 * Class that translates from a grid to the different contact recommendation algorithns.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class GlobalCommunityMetricSelector<U>
{   
    /**
     * Obtains a grid search for the metric.
     * @param metric the name of the metric.
     * @return the grid search for the metric if exists, null otherwise.
     */
    public GlobalCommunityMetricGridSearch<U> getGridSearch(String metric)
    {
        // Comm. Degree Gini
        // Size-norm. Comm. Degree Gini
        // Comm. Edge Gini
        // Dice Comm. Edge Gini
        // Size-norm. Comm. Edge Gini
        // Other metrics
        // Default behavior
        GlobalCommunityMetricGridSearch<U> gridsearch = switch (metric)
        {
            case INTERCOMMUNITYDEGREEGINI -> new InterCommunityDegreeGiniGridSearch<>();
            case COMPLETECOMMUNITYDEGREEGINI -> new CompleteCommunityDegreeGiniGridSearch<>();
            case SIZENORMINTERCOMMUNITYDEGREEGINI -> new SizeNormalizedInterCommunityDegreeGiniGridSearch<>();
            case SIZENORMCOMPLETECOMMUNITYDEGREEGINI -> new SizeNormalizedCompleteCommunityDegreeGiniGridSearch<>();
            case INTERCOMMUNITYEDGEGINI -> new InterCommunityEdgeGiniGridSearch<>();
            case COMPLETECOMMUNITYEDGEGINI -> new CompleteCommunityEdgeGiniGridSearch<>();
            case SEMICOMPLETECOMMUNITYEDGEGINI -> new SemiCompleteCommunityEdgeGiniGridSearch<>();
            case DICEINTERCOMMUNITYEDGEGINI -> new DiceInterCommunityEdgeGiniGridSearch<>();
            case DICECOMPLETECOMMUNITYEDGEGINI -> new DiceCompleteCommunityEdgeGiniGridSearch<>();
            case DICESEMICOMPLETECOMMUNITYEDGEGINI -> new DiceSemiCompleteCommunityEdgeGiniGridSearch<>();
            case SIZENORMINTERCOMMUNITYEDGEGINI -> new SizeNormalizedInterCommunityEdgeGiniGridSearch<>();
            case SIZENORMCOMPLETECOMMUNITYEDGEGINI -> new SizeNormalizedCompleteCommunityEdgeGiniGridSearch<>();
            case SIZENORMSEMICOMPLETECOMMUNITYEDGEGINI -> new SizeNormalizedSemiCompleteCommunityEdgeGiniGridSearch<>();
            case NUMCOMMS -> new NumCommunitiesGridSearch<>();
            case MODULARITY -> new ModularityGridSearch<>();
            case MODULARITYCOMPL -> new ModularityComplementGridSearch<>();
            case WEAKTIES -> new WeakTiesGridSearch<>();
            case COMMDESTSIZE -> new CommunityDestinySizeGridSearch<>();
            case COMMSIZEGINI -> new CommunitySizeGiniGridSearch<>();
            default -> null;
        };
        return gridsearch;
    }
    
    /**
     * Obtains the different variants of a given community metric depending on the 
     * parameters selected in a grid.
     * @param metric the name of the metric.
     * @param grid the grid containing the different parameters.
     * @return a map containing the different metric suppliers.
     */
    public Map<String, Supplier<CommunityMetric<U>>> getMetrics(String metric, Grid grid)
    {
        GlobalCommunityMetricGridSearch<U> gridsearch = this.getGridSearch(metric);
        if(gridsearch != null)
            return gridsearch.grid(grid);
        return new HashMap<>();
    }
}
