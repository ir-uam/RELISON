/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.grid.diffusion.filter.FilterParamReader;
import es.uam.eps.ir.socialranksys.grid.diffusion.filter.FilterSelector;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.MetricParamReader;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.MetricSelector;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.distributions.DistributionParamReader;
import es.uam.eps.ir.socialranksys.grid.diffusion.metrics.distributions.DistributionSelector;
import es.uam.eps.ir.socialranksys.utils.datatypes.Triplet;
import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;
import es.uam.eps.socialranksys.diffusion.data.filter.CombinedFilter;
import es.uam.eps.socialranksys.diffusion.data.filter.DataFilter;
import es.uam.eps.socialranksys.diffusion.metrics.SimulationMetric;
import es.uam.eps.socialranksys.diffusion.metrics.distributions.Distribution;
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
public class SimulationMetricsSelector<U extends Serializable,I extends Serializable,P> 
{
    /**
     * Parameter parser (for the filter)
     */
    private final Parser<P> parser;
    
    /**
     * Constructor.
     * @param parser Parameter parser (for the filter) 
     */
    public SimulationMetricsSelector(Parser<P> parser)
    {
        this.parser = parser;
    }
    
    /**
     * Selects a protocol.
     * @param spr Parameters for the simulation.
     * @param defaultValue default value for the parameter values.
     * @return A triple containing a data filter, a list of simulation metrics and a list of distributions
     */
    public Triplet<DataFilter<U,I,P>,List<SimulationMetric<U,I,P>>,List<Tuple2oo<Distribution<U,I,P>, List<Integer>>>> select(SimulationMetricsParamReader spr, P defaultValue)
    {
        return this.select(spr, defaultValue, null);
    }

    
    /**
     * Selects a protocol.
     * @param spr Parameters for the simulation.
     * @param defaultValue default value for the parameter values.
     * @param testGraph the test graph.
     * @return A triple containing a data filter, a list of simulation metrics and a list of distributions
     */
    public Triplet<DataFilter<U,I,P>,List<SimulationMetric<U,I,P>>,List<Tuple2oo<Distribution<U,I,P>, List<Integer>>>> select(SimulationMetricsParamReader spr, P defaultValue, Graph<U> testGraph)
    {
        if(spr == null)
            return null;
        
        
        FilterSelector<U,I,P> filSel = new FilterSelector<>(this.parser, defaultValue, testGraph);
        List<FilterParamReader> filters = spr.getFiltersParameters();
        List<DataFilter<U,I,P>> filterList = new ArrayList<>();
        for(FilterParamReader fpr : filters)
        {
            Tuple2oo<String, DataFilter<U,I,P>> filPair = filSel.select(fpr);
            filterList.add(filPair.v2());
        }
        
        // Select the definitive filter.
        DataFilter<U,I,P> filter = new CombinedFilter<>(filterList);
        
        MetricSelector<U,I,P> metricSel = new MetricSelector<>();
        List<MetricParamReader> metrics = spr.getMetricsParameters();
        List<SimulationMetric<U,I,P>> metricList = new ArrayList<>();
        for(MetricParamReader mpr : metrics)
        {
            Tuple2oo<String, SimulationMetric<U,I,P>> tuple = metricSel.select(mpr);
            metricList.add(tuple.v2());
        }
        
        DistributionSelector<U,I,P> distribSel = new DistributionSelector<>();
        List<DistributionParamReader> distribs = spr.getDistributionsParameters();
        List<Tuple2oo<Distribution<U,I,P>,List<Integer>>> distribList = new ArrayList<>();
        for(DistributionParamReader dpr : distribs)
        {
            Tuple2oo<String, Tuple2oo<Distribution<U,I,P>, List<Integer>>> tuple = distribSel.select(dpr);
            distribList.add(tuple.v2());
        }
        
        return new Triplet<>(filter, metricList, distribList);
    }
    
        

    
}
