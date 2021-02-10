/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion;

import es.uam.eps.ir.socialranksys.grid.ParametersReader;
import es.uam.eps.ir.socialranksys.grid.diffusion.filter.FilterParamReader;
import es.uam.eps.ir.socialranksys.grid.diffusion.protocol.ProtocolParamReader;
import es.uam.eps.ir.socialranksys.grid.diffusion.stop.StopConditionParamReader;
import es.uam.eps.ir.socialranksys.utils.datatypes.Triplet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads the grids for several algorithms.
 * @author Javier Sanz-Cruzado Puig
 */
public class SimulationParamReader extends ParametersReader
{
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
     * Identifier for the filter
     */
    private final static String FILTER = "filter";
    /**
     * Identifier for the stop condition
     */
    private final static String STOP = "stop";
    /**
     * Parameters for the different simulations.
     */
    private final List<ProtocolParamReader> protocolParams;
    /**
     * Parameters for the different filters.
     */
    private final List<List<FilterParamReader>> filterParams;
    /**
     * Parameters for the different stop conditions.
     */
    private final List<StopConditionParamReader> stopParams;
    /**
     * The name of the file
     */
    private final String file;
    
    /**
     * Constructor
     * @param file File that contains the grid data 
     */
    public SimulationParamReader(String file)
    {
        this.file = file;
        this.protocolParams = new ArrayList<>();
        this.filterParams = new ArrayList<>();
        this.stopParams = new ArrayList<>();

    }
    
    /**
     * Reads a XML document containing a grid
     */
    public void readDocument()
    {
        protocolParams.clear();
        filterParams.clear();
        stopParams.clear();
        
        try
        {
            // First of all, obtain the XML Document
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            
            Element parent = doc.getDocumentElement();
            parent.normalize();
            
            
            
            NodeList protocolList = parent.getElementsByTagName(SIMULATIONS);
            protocolList = protocolList.item(0).getChildNodes();
   
            for(int i = 0; i < protocolList.getLength(); ++i)
            {
                Node node = protocolList.item(i);
                
                if(node.getNodeType() == Node.ELEMENT_NODE)
                {
                    Element element = (Element) node;
                    this.readSimulation(element);
                }
            }
                        
        } 
        catch (ParserConfigurationException | SAXException | IOException ex)
        {
            ex.printStackTrace();
        }
    }
    
    /**
     * Reads the grid for a single algorithm.
     * @param element The XML Element containing the algorithm information
     */
    private void readSimulation(Element element)
    {
        // Read the protocol
        Node protocolElem = element.getElementsByTagName(PROTOCOL).item(0);
        ProtocolParamReader ppr = new ProtocolParamReader();
        ppr.readProtocol((Element) protocolElem);
        this.protocolParams.add(ppr);
        
        // Read the data filter
        Node filtersElem = element.getElementsByTagName(FILTERS).item(0);
        NodeList filterList = ((Element) filtersElem).getElementsByTagName(FILTER);
        List<FilterParamReader> fprs = new ArrayList<>();
        for(int i = 0; i < filterList.getLength(); ++i)
        {
            FilterParamReader fpr = new FilterParamReader();
            fpr.readFilter((Element) filterList.item(i));
            fprs.add(fpr);
        }
        this.filterParams.add(fprs);

        
        // Read the stop condition
        Node stopElem = element.getElementsByTagName(STOP).item(0);
        StopConditionParamReader scpr = new StopConditionParamReader();
        scpr.readStopCondition((Element) stopElem);
        this.stopParams.add(scpr);
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
    public Set<ProtocolParamReader> getPreconfiguredProtocols()
    {
        return this.protocolParams.stream()
                    .filter(ProtocolParamReader::isPreconfigured)
                    .collect(Collectors.toCollection(HashSet::new));     
    }
    
    /**
     * Obtains the set of custom protocols previously read.
     * @return The set of custom protocols previously read.
     */
    public Set<ProtocolParamReader> getCustomProtocols()
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
    public Triplet<ProtocolParamReader, List<FilterParamReader>, StopConditionParamReader> getSimulation(int num)
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
    public ProtocolParamReader getProtocolParameters(int num)
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
    public List<FilterParamReader> getFilterParameters(int num)
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
    public StopConditionParamReader getStopConditionParameters(int num)
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
            for(FilterParamReader fpr : this.filterParams.get(num)) sim.append(fpr.printFilter()).append("\n");
            sim.append("Stop Condition:").append(this.stopParams.get(num).printStopCondition()).append("\n");
        }
        return sim.toString();
        
    }


}
