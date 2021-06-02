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
import es.uam.eps.ir.sonalire.diffusion.metrics.SimulationMetric;
import es.uam.eps.ir.sonalire.diffusion.metrics.distributions.Distribution;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.grid.Parameters;
import es.uam.eps.ir.sonalire.grid.diffusion.filter.FilterParameterReader;
import es.uam.eps.ir.sonalire.grid.diffusion.filter.FilterSelector;
import es.uam.eps.ir.sonalire.grid.diffusion.metrics.MetricSelector;
import es.uam.eps.ir.sonalire.grid.diffusion.metrics.distributions.DistributionSelector;
import es.uam.eps.ir.sonalire.utils.datatypes.Triplet;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;
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
    public Triplet<DataFilter<U,I,P>,List<SimulationMetric<U,I,P>>,List<Tuple2<Distribution<U,I,P>, List<Integer>>>> select(SimulationMetricsParameterReader spr, P defaultValue)
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
    public Triplet<DataFilter<U,I,P>,List<SimulationMetric<U,I,P>>,List<Tuple2<Distribution<U,I,P>, List<Integer>>>> select(SimulationMetricsParameterReader spr, P defaultValue, Graph<U> testGraph)
    {
        if(spr == null)
            return null;
        
        FilterSelector<U,I,P> filSel = new FilterSelector<>(this.parser, defaultValue, testGraph);
        List<FilterParameterReader> filters = spr.getFiltersParameters();
        List<DataFilter<U,I,P>> filterList = new ArrayList<>();
        for(FilterParameterReader fpr : filters)
        {
            Tuple2<String, DataFilter<U,I,P>> filPair = filSel.select(fpr.getName(), fpr.getParameter());
            filterList.add(filPair.v2());
        }
        
        // Select the definitive filter.
        DataFilter<U,I,P> filter = new CombinedFilter<>(filterList);
        
        MetricSelector<U,I,P> metricSel = new MetricSelector<>();
        List<String> metrics = spr.getMetrics();
        List<SimulationMetric<U,I,P>> metricList = new ArrayList<>();
        for(String mpr : metrics)
        {
            Tuple2<String, SimulationMetric<U,I,P>> tuple = metricSel.select(mpr, spr.getMetricParameters(mpr));
            metricList.add(tuple.v2());
        }
        
        DistributionSelector<U,I,P> distribSel = new DistributionSelector<>();
        List<String> distribs = spr.getDistributions();
        List<Tuple2<Distribution<U,I,P>,List<Integer>>> distribList = new ArrayList<>();
        for(String dpr : distribs)
        {
            Tuple2<Parameters, List<Integer>> params = spr.getDistributionParameters(dpr);
            Tuple3<String, Distribution<U,I,P>, List<Integer>> tuple = distribSel.select(dpr, params.v1, params.v2);
            distribList.add(new Tuple2<>(tuple.v2, tuple.v3));
        }
        
        return new Triplet<>(filter, metricList, distribList);
    }
    
        

    
}
