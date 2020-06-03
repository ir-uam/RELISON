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
        GlobalCommunityMetricGridSearch<U> gridsearch;
        switch(metric)
        {
            // Comm. Degree Gini
            case INTERCOMMUNITYDEGREEGINI:
                gridsearch = new InterCommunityDegreeGiniGridSearch<>();
                break;
            case COMPLETECOMMUNITYDEGREEGINI:
                gridsearch = new CompleteCommunityDegreeGiniGridSearch<>();
                break;
            // Size-norm. Comm. Degree Gini
            case SIZENORMINTERCOMMUNITYDEGREEGINI:
                gridsearch = new SizeNormalizedInterCommunityDegreeGiniGridSearch<>();
                break;
            case SIZENORMCOMPLETECOMMUNITYDEGREEGINI:
                gridsearch = new SizeNormalizedCompleteCommunityDegreeGiniGridSearch<>();
                break;
            // Comm. Edge Gini
            case INTERCOMMUNITYEDGEGINI:
                gridsearch = new InterCommunityEdgeGiniGridSearch<>();
                break;
            case COMPLETECOMMUNITYEDGEGINI:
                gridsearch = new CompleteCommunityEdgeGiniGridSearch<>();
                break;
            case SEMICOMPLETECOMMUNITYEDGEGINI:
                gridsearch = new SemiCompleteCommunityEdgeGiniGridSearch<>();
                break;
            // Dice Comm. Edge Gini
            case DICEINTERCOMMUNITYEDGEGINI:
                gridsearch = new DiceInterCommunityEdgeGiniGridSearch<>();
                break;
            case DICECOMPLETECOMMUNITYEDGEGINI:
                gridsearch = new DiceCompleteCommunityEdgeGiniGridSearch<>();
                break;
            case DICESEMICOMPLETECOMMUNITYEDGEGINI:
                gridsearch = new DiceSemiCompleteCommunityEdgeGiniGridSearch<>();
                break;
            // Size-norm. Comm. Edge Gini
            case SIZENORMINTERCOMMUNITYEDGEGINI:
                gridsearch = new SizeNormalizedInterCommunityEdgeGiniGridSearch<>();
                break;
            case SIZENORMCOMPLETECOMMUNITYEDGEGINI:
                gridsearch = new SizeNormalizedCompleteCommunityEdgeGiniGridSearch<>();
                break;
            case SIZENORMSEMICOMPLETECOMMUNITYEDGEGINI:
                gridsearch = new SizeNormalizedSemiCompleteCommunityEdgeGiniGridSearch<>();
                break;       
            // Other metrics
            case NUMCOMMS:
                gridsearch = new NumCommunitiesGridSearch<>();
                break;
            case MODULARITY:
                gridsearch = new ModularityGridSearch<>();
                break;
            case WEAKTIES:
                gridsearch = new WeakTiesGridSearch<>();
                break;
            case COMMDESTSIZE:
                gridsearch = new CommunityDestinySizeGridSearch<>();
                break;
            case COMMSIZEGINI:
                gridsearch = new CommunitySizeGiniGridSearch<>();
                break;
            // Default behavior
            default:
                gridsearch = null;
        }
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
