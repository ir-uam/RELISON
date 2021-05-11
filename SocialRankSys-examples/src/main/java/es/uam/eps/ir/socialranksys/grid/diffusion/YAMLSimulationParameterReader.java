/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion;

import es.uam.eps.ir.socialranksys.grid.YAMLParametersReader;
import es.uam.eps.ir.socialranksys.grid.diffusion.filter.YAMLFilterParameterReader;
import es.uam.eps.ir.socialranksys.grid.diffusion.protocol.YAMLProtocolParameterReader;
import es.uam.eps.ir.socialranksys.grid.diffusion.stop.YAMLStopConditionParameterReader;
import es.uam.eps.ir.socialranksys.utils.datatypes.Triplet;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Reads a YAML file containing the configuration for simulating information diffusion.
 *
 * File format:<br/>
 * simulations:<br/>
 * - protocol: ...<br />
 *   filters: <br/>
 *      filter_name: <br/>
 *          filter_param_1: ... <br/>
 *          filter_param_2: ... <br/>
 *          ... <br/>
 *      filter_name: <br/>
 *          ... <br/>
 *   stop: <br/>
 * - protocol: ...<br/>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class YAMLSimulationParameterReader extends YAMLParametersReader
{
    /**
     * Parameters for the different simulations.
     */
    private final List<YAMLProtocolParameterReader> protocolParams;
    /**
     * Parameters for the different filters.
     */
    private final List<List<YAMLFilterParameterReader>> filterParams;
    /**
     * Parameters for the different stop conditions.
     */
    private final List<YAMLStopConditionParameterReader> stopParams;

    /**
     * Identifier for the protocol list
     */
    private final static String SIMULATIONS = "simulations";
    /**
     * Identifier for the protocol
     */
    private final static String PROTOCOL = "protocol";
    /**
     * Identifier for filters
     */
    private final static String FILTERS = "filters";
    /**
     * Identifier for the stop condition
     */
    private final static String STOP = "stop";


    /**
     * Constructor
     */
    public YAMLSimulationParameterReader()
    {
        this.protocolParams = new ArrayList<>();
        this.filterParams = new ArrayList<>();
        this.stopParams = new ArrayList<>();
    }
    
    /**
     * Reads the configuration parameters from a file.
     *
     * @param map a map containing the configurations for a simulation.
     */
    public void read(Map<String, Object> map)
    {
        protocolParams.clear();
        filterParams.clear();
        stopParams.clear();

        List<Object> sims = (List<Object>) map.get(SIMULATIONS);
        for (Object entry : sims)
        {
            this.readSimulation((Map<String, Object>) entry);
        }
    }

    
    /**
     * Reads the grid for a single simulation.
     * @param element a map containing the configuration of the simulation.
     */
    private void readSimulation(Map<String, Object> element)
    {
        // First, we obtain the protocol:
        Map<String, Object> protocol = (Map<String, Object>) element.get(PROTOCOL);
        YAMLProtocolParameterReader ppr = new YAMLProtocolParameterReader();
        ppr.readProtocol(protocol);
        this.protocolParams.add(ppr);

        // Then, the data filter:
        Map<String, Object> filters = (Map<String, Object>) element.get(FILTERS);
        List<YAMLFilterParameterReader> fprs = new ArrayList<>();
        for(Map.Entry<String, Object> filter : filters.entrySet())
        {
            YAMLFilterParameterReader fpr = new YAMLFilterParameterReader();
            fpr.readFilter(filter);
            fprs.add(fpr);
        }

        Map<String, Object> stop = (Map<String, Object>) element.get(STOP);
        YAMLStopConditionParameterReader scpr = new YAMLStopConditionParameterReader();
        scpr.readStopCondition(stop);
    }
       
    /**
     * Obtains the number of available simulations.
     * @return the number of available simulations.
     */
    public int numberSimulations()
    {
        return this.protocolParams.size();
    }
    
    /**
     * Obtains the set of preconfigured protocols previously read.
     * @return The set of preconfigured protocols previously read.
     */
    public Set<YAMLProtocolParameterReader> getPreconfiguredProtocols()
    {
        return this.protocolParams.stream()
                    .filter(YAMLProtocolParameterReader::isPreconfigured)
                    .collect(Collectors.toCollection(HashSet::new));     
    }
    
    /**
     * Obtains the set of custom protocols previously read.
     * @return The set of custom protocols previously read.
     */
    public Set<YAMLProtocolParameterReader> getCustomProtocols()
    {
        return this.protocolParams.stream()
                    .filter(prot -> !prot.isPreconfigured())
                    .collect(Collectors.toCollection(HashSet::new));  
    }
    
    /**
     * Gets the different elements for a simulation.
     * @param num the index of the simulation.
     * @return a triplet containing: <ol>
     * <li>The parameters for the protocol</li>
     * <li>The parameters for the filter </li>
     * <li>The parameters for the stop condition </li>
     * </ol> if the index is correct, null if not
     */
    public Triplet<YAMLProtocolParameterReader, List<YAMLFilterParameterReader>, YAMLStopConditionParameterReader> getSimulation(int num)
    {
        if(num >= 0 && num < this.numberSimulations())
        {
            return new Triplet<>(this.protocolParams.get(num), this.filterParams.get(num), this.stopParams.get(num));
        }
        return null;
    }
    
    /**
     * Gets the protocol parameters for a given simulation.
     * @param num the index of the simulation.
     * @return the protocol parameters.
     */
    public YAMLProtocolParameterReader getProtocolParameters(int num)
    {
        if(num >= 0 && num < this.numberSimulations())
        {
            return this.protocolParams.get(num);
        }
        return null;
    }
    
    /**
     * Gets the filter parameters for a given simulation.
     * @param num the index of the simulation.
     * @return the filter parameters.
     */
    public List<YAMLFilterParameterReader> getFilterParameters(int num)
    {
        if(num >= 0 && num < this.numberSimulations())
        {
            return this.filterParams.get(num);
        }
        return null;
    }
    
    /**
     * Gets the stop condition parameters for a given simulation.
     * @param num the index of the simulation.
     * @return the stop condition parameters.
     */
    public YAMLStopConditionParameterReader getStopConditionParameters(int num)
    {
        if(num >= 0 && num < this.numberSimulations())
        {
            return this.stopParams.get(num);
        }
        return null;
    }
    
    /**
     * Obtains a string detailing the simulation parameters.
     * @param num the index of the simulation.
     * @return a string detailing the simulation parameters.
     */
    public String printSimulation(int num)
    {
        StringBuilder sim = new StringBuilder();
        if(num >= 0 && num < this.numberSimulations())
        {
            sim.append("Protocol: ").append(this.protocolParams.get(num).printProtocol()).append("\n");
            sim.append("Filters: ");
            for(YAMLFilterParameterReader fpr : this.filterParams.get(num)) sim.append(fpr.printFilter()).append("\n");
            sim.append("Stop Condition:").append(this.stopParams.get(num).printStopCondition()).append("\n");
        }
        return sim.toString();
        
    }


}
