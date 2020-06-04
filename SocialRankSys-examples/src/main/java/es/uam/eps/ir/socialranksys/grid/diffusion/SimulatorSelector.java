/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion;

import es.uam.eps.ir.socialranksys.diffusion.data.filter.CombinedFilter;
import es.uam.eps.ir.socialranksys.diffusion.data.filter.DataFilter;
import es.uam.eps.ir.socialranksys.diffusion.protocols.Protocol;
import es.uam.eps.ir.socialranksys.diffusion.simulation.Simulator;
import es.uam.eps.ir.socialranksys.diffusion.stop.StopCondition;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.grid.diffusion.filter.FilterParamReader;
import es.uam.eps.ir.socialranksys.grid.diffusion.filter.FilterSelector;
import es.uam.eps.ir.socialranksys.grid.diffusion.protocol.ProtocolSelector;
import es.uam.eps.ir.socialranksys.grid.diffusion.stop.StopConditionSelector;
import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;
import org.ranksys.formats.parsing.Parser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that selects an individual filter from a grid.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 * @param <I> Type of the information pieces
 * @param <P> Type of the parameters
 */
public class SimulatorSelector<U extends Serializable,I extends Serializable,P> 
{
    /**
     * Parameter parser (for the filter)
     */
    private final Parser<P> parser;
    
    /**
     * Constructor.
     * @param parser Parameter parser (for the filter) 
     */
    public SimulatorSelector(Parser<P> parser)
    {
        this.parser = parser;
    }
    
    /**
     * Selects a protocol.
     * @param spr Parameters for the simulation.
     * @param num Simulation index number.
     * @param defaultValue default value for the parameter values.
     * @return A pair containing a simulator and a data filter.
     */
    public Tuple2oo<Simulator<U,I,P>, DataFilter<U,I,P>> select(SimulationParamReader spr, int num, P defaultValue)
    {
        return this.select(spr, num, defaultValue, null);
    }
    
    /**
     * Selects a protocol.
     * @param spr Parameters for the simulation.
     * @param num Simulation index number.
     * @param defaultValue default value for the parameter values.
     * @param testGraph the test graph
     * @return A pair containing a simulator and a data filter.
     */
    public Tuple2oo<Simulator<U,I,P>,DataFilter<U,I,P>> select(SimulationParamReader spr, int num, P defaultValue, Graph<U> testGraph)
    {
        if(spr == null)
            return null;
        
        if(num < 0 || num > spr.numberSimulations()) // Invalid simulation selection
        {
            return null;
        }
        
        // Select the protocol
        ProtocolSelector<U,I,P> protSel = new ProtocolSelector<>();
        Tuple2oo<String, Protocol<U,I,P>> protPair = protSel.select(spr.getProtocolParameters(num));
        if(protPair == null || protPair.v2() == null)
            return null;
        Protocol<U,I,P> protocol = protPair.v2();
        
        // Select the filter
        
        FilterSelector<U,I,P> filSel = new FilterSelector<>(this.parser, defaultValue, testGraph);
        List<FilterParamReader> filters = spr.getFilterParameters(num);
        List<DataFilter<U,I,P>> filterList = new ArrayList<>();
        for(FilterParamReader fpr : filters)
        {
            Tuple2oo<String,DataFilter<U,I,P>> filPair = filSel.select(fpr);
            if(filPair == null || filPair.v2() == null)
                return null;
            DataFilter<U,I,P> f = filPair.v2();
            filterList.add(f);
        }
        
        DataFilter<U,I,P> filter = new CombinedFilter<>(filterList);
        
        // Select the stop condition
        StopConditionSelector<U,I,P> stopSel = new StopConditionSelector<>();
        Tuple2oo<String, StopCondition<U,I,P>> stopPair = stopSel.select(spr.getStopConditionParameters(num));
        if(stopPair == null || stopPair.v2() == null)
            return null;
        StopCondition<U,I,P> stop = stopPair.v2();
                
        Simulator<U,I,P> simulator = new Simulator<>(protocol, stop);
        return new Tuple2oo<>(simulator, filter);
    }
}
