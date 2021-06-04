/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.uam.eps.ir.relison.grid.sna.comm.global;


import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.sna.comm.global.gini.degree.CompleteCommunityDegreeGiniGridSearch;
import es.uam.eps.ir.relison.grid.sna.comm.global.gini.degree.InterCommunityDegreeGiniGridSearch;
import es.uam.eps.ir.relison.grid.sna.comm.global.gini.degree.sizenormalized.SizeNormalizedCompleteCommunityDegreeGiniGridSearch;
import es.uam.eps.ir.relison.grid.sna.comm.global.gini.degree.sizenormalized.SizeNormalizedInterCommunityDegreeGiniGridSearch;
import es.uam.eps.ir.relison.grid.sna.comm.global.gini.edge.CompleteCommunityEdgeGiniGridSearch;
import es.uam.eps.ir.relison.grid.sna.comm.global.gini.edge.InterCommunityEdgeGiniGridSearch;
import es.uam.eps.ir.relison.grid.sna.comm.global.gini.edge.SemiCompleteCommunityEdgeGiniGridSearch;
import es.uam.eps.ir.relison.grid.sna.comm.global.gini.edge.dice.DiceCompleteCommunityEdgeGiniGridSearch;
import es.uam.eps.ir.relison.grid.sna.comm.global.gini.edge.dice.DiceInterCommunityEdgeGiniGridSearch;
import es.uam.eps.ir.relison.grid.sna.comm.global.gini.edge.dice.DiceSemiCompleteCommunityEdgeGiniGridSearch;
import es.uam.eps.ir.relison.grid.sna.comm.global.gini.edge.sizenormalized.SizeNormalizedCompleteCommunityEdgeGiniGridSearch;
import es.uam.eps.ir.relison.grid.sna.comm.global.gini.edge.sizenormalized.SizeNormalizedInterCommunityEdgeGiniGridSearch;
import es.uam.eps.ir.relison.grid.sna.comm.global.gini.edge.sizenormalized.SizeNormalizedSemiCompleteCommunityEdgeGiniGridSearch;
import es.uam.eps.ir.relison.grid.sna.comm.global.gini.size.CommunitySizeGiniGridSearch;
import es.uam.eps.ir.relison.metrics.CommunityMetric;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

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
        return switch (metric)
        {
            case GlobalCommunityMetricIdentifiers.INTERCOMMUNITYDEGREEGINI -> new InterCommunityDegreeGiniGridSearch<>();
            case GlobalCommunityMetricIdentifiers.COMPLETECOMMUNITYDEGREEGINI -> new CompleteCommunityDegreeGiniGridSearch<>();
            case GlobalCommunityMetricIdentifiers.SIZENORMINTERCOMMUNITYDEGREEGINI -> new SizeNormalizedInterCommunityDegreeGiniGridSearch<>();
            case GlobalCommunityMetricIdentifiers.SIZENORMCOMPLETECOMMUNITYDEGREEGINI -> new SizeNormalizedCompleteCommunityDegreeGiniGridSearch<>();
            case GlobalCommunityMetricIdentifiers.INTERCOMMUNITYEDGEGINI -> new InterCommunityEdgeGiniGridSearch<>();
            case GlobalCommunityMetricIdentifiers.COMPLETECOMMUNITYEDGEGINI -> new CompleteCommunityEdgeGiniGridSearch<>();
            case GlobalCommunityMetricIdentifiers.SEMICOMPLETECOMMUNITYEDGEGINI -> new SemiCompleteCommunityEdgeGiniGridSearch<>();
            case GlobalCommunityMetricIdentifiers.DICEINTERCOMMUNITYEDGEGINI -> new DiceInterCommunityEdgeGiniGridSearch<>();
            case GlobalCommunityMetricIdentifiers.DICECOMPLETECOMMUNITYEDGEGINI -> new DiceCompleteCommunityEdgeGiniGridSearch<>();
            case GlobalCommunityMetricIdentifiers.DICESEMICOMPLETECOMMUNITYEDGEGINI -> new DiceSemiCompleteCommunityEdgeGiniGridSearch<>();
            case GlobalCommunityMetricIdentifiers.SIZENORMINTERCOMMUNITYEDGEGINI -> new SizeNormalizedInterCommunityEdgeGiniGridSearch<>();
            case GlobalCommunityMetricIdentifiers.SIZENORMCOMPLETECOMMUNITYEDGEGINI -> new SizeNormalizedCompleteCommunityEdgeGiniGridSearch<>();
            case GlobalCommunityMetricIdentifiers.SIZENORMSEMICOMPLETECOMMUNITYEDGEGINI -> new SizeNormalizedSemiCompleteCommunityEdgeGiniGridSearch<>();
            case GlobalCommunityMetricIdentifiers.NUMCOMMS -> new NumCommunitiesGridSearch<>();
            case GlobalCommunityMetricIdentifiers.MODULARITY -> new ModularityGridSearch<>();
            case GlobalCommunityMetricIdentifiers.MODULARITYCOMPL -> new ModularityComplementGridSearch<>();
            case GlobalCommunityMetricIdentifiers.WEAKTIES -> new WeakTiesGridSearch<>();
            case GlobalCommunityMetricIdentifiers.COMMDESTSIZE -> new CommunityDestinySizeGridSearch<>();
            case GlobalCommunityMetricIdentifiers.COMMSIZEGINI -> new CommunitySizeGiniGridSearch<>();
            default -> null;
        };
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
