/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.diffusion;

import es.uam.eps.ir.sonalire.diffusion.data.filter.CombinedFilter;
import es.uam.eps.ir.sonalire.diffusion.data.filter.DataFilter;
import es.uam.eps.ir.sonalire.diffusion.protocols.Protocol;
import es.uam.eps.ir.sonalire.diffusion.simulation.Simulator;
import es.uam.eps.ir.sonalire.diffusion.stop.StopCondition;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.grid.diffusion.filter.FilterParameterReader;
import es.uam.eps.ir.sonalire.grid.diffusion.filter.FilterSelector;
import es.uam.eps.ir.sonalire.grid.diffusion.protocol.ProtocolSelector;
import es.uam.eps.ir.sonalire.grid.diffusion.stop.StopConditionSelector;
import es.uam.eps.ir.sonalire.utils.datatypes.Tuple2oo;
import org.jooq.lambda.tuple.Tuple2;
import org.ranksys.formats.parsing.Parser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that selects a single simulator for a grid.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the features.
 */
public class SimulatorSelector<U extends Serializable,I extends Serializable, F>
{
    /**
     * Parameter parser (for the filter)
     */
    private final Parser<F> parser;
    
    /**
     * Constructor.
     * @param parser Parameter parser (for the filter) 
     */
    public SimulatorSelector(Parser<F> parser)
    {
        this.parser = parser;
    }
    
    /**
     * Selects a protocol.
     * @param simParams     parameters for the simulation.
     * @param num           simulation index number.
     * @param defaultValue  default value for the parameter values.
     * @return a pair containing the simulator and the data filter.
     */
    public Tuple2oo<Simulator<U,I, F>, DataFilter<U,I, F>> select(SimulationParameterReader simParams, int num, F defaultValue)
    {
        return this.select(simParams, num, defaultValue, null);
    }
    
    /**
     * Selects a protocol.
     * @param simParams     parameters for the simulation.
     * @param num           simulation index number.
     * @param defaultValue  default value for the parameter values.
     * @param filterGraph   a graph for filtering the data (for instance, the test graph in a recommendation).
     * @return a pair containing a simulator and a data filter.
     */
    public Tuple2oo<Simulator<U,I, F>,DataFilter<U,I, F>> select(SimulationParameterReader simParams, int num, F defaultValue, Graph<U> filterGraph)
    {
        if(simParams == null)
            return null;
        
        if(num < 0 || num > simParams.numberSimulations()) // Invalid simulation selection
        {
            return null;
        }
        
        // Select the protocol
        ProtocolSelector<U,I, F> protSel = new ProtocolSelector<>();
        Tuple2<String, Protocol<U,I, F>> protPair = protSel.select(simParams.getProtocolParameters(num));
        if(protPair == null || protPair.v2() == null)
            return null;
        Protocol<U,I, F> protocol = protPair.v2();
        
        // Select the filter
        FilterSelector<U,I, F> filSel = new FilterSelector<>(this.parser, defaultValue, filterGraph);
        List<FilterParameterReader> filters = simParams.getFilterParameters(num);
        List<DataFilter<U,I, F>> filterList = new ArrayList<>();
        for(FilterParameterReader fpr : filters)
        {
            Tuple2<String,DataFilter<U,I, F>> filPair = filSel.select(fpr.getName(), fpr.getParameter());
            if(filPair == null || filPair.v2() == null)
                return null;
            DataFilter<U,I, F> f = filPair.v2();
            filterList.add(f);
        }
        
        DataFilter<U,I, F> filter = new CombinedFilter<>(filterList);
        
        // Select the stop condition
        StopConditionSelector<U,I, F> stopSel = new StopConditionSelector<>();
        Tuple2<String, StopCondition<U,I, F>> stopPair = stopSel.select(simParams.getStopConditionParameters(num).getName(), simParams.getStopConditionParameters(num).getParameter());
        if(stopPair == null || stopPair.v2() == null)
            return null;
        StopCondition<U,I, F> stop = stopPair.v2();
                
        Simulator<U,I, F> simulator = new Simulator<>(protocol, stop);
        return new Tuple2oo<>(simulator, filter);
    }
}
