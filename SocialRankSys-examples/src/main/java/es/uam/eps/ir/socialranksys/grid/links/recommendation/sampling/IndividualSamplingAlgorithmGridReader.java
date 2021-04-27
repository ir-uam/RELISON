/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.sampling;

import es.uam.eps.ir.socialranksys.grid.Parameters;
import es.uam.eps.ir.socialranksys.grid.ParametersReader;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Reads the grid for sampling algorithms from an XML file.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class IndividualSamplingAlgorithmGridReader extends ParametersReader
{
    /**
     * Algorithms grid. Uses a grid for each algorithm.
     */
    private final Map<String, Parameters> partitionsGrid;
    
    /**
     * Constructor.
     */
    public IndividualSamplingAlgorithmGridReader()
    {
        this.partitionsGrid = new HashMap<>();
    }
    
    /**
     * Reads a XML document containing a grid.
     * @param file the XML file.
     */
    public void readDocument(String file)
    {
        try
        {
            this.partitionsGrid.clear();
            // First of all, obtain the XML Document
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            
            Element parent = doc.getDocumentElement();
            parent.normalize();
            
            NodeList nodeList = parent.getChildNodes();
            for(int i = 0; i < nodeList.getLength(); ++i)
            {
                Node node = nodeList.item(i);
                if(node.getNodeType() == Node.ELEMENT_NODE)
                {
                    Element element = (Element) node;
                    this.readIndividualSamplingAlgorithm(element);
                }
            }
            
        } catch (ParserConfigurationException | SAXException | IOException ex)
        {
            ex.printStackTrace();
        }
    }
    
    /**
     * Reads the grid for a single algorithm.
     * @param element The XML Element containing the algorithm information.
     */
    private void readIndividualSamplingAlgorithm(Element element)
    {
        String algorithmName = element.getElementsByTagName("name").item(0).getTextContent();
        NodeList parametersNodes = element.getElementsByTagName("params");
        if(parametersNodes == null || parametersNodes.getLength() == 0)
        {
            this.partitionsGrid.put(algorithmName, new Parameters());
        }
        else
        {
            Element parametersNode = (Element) parametersNodes.item(0);
            NodeList parameters = parametersNode.getElementsByTagName("param");
            Parameters g = readParameterGrid(parameters);
            this.partitionsGrid.put(algorithmName, g);
        }
    }

    /**
     * Gets the set of algorithms previously read.
     * @return The set of algorithms previously read from the grid file.
     */
    public Set<String> getIndividualSamplingAlgorithms()
    {
        return this.partitionsGrid.keySet();
    }
    
    /**
     * Gets the grid for a given algorithm.
     * @param algorithm The algorithm to search.
     * @return The grid if exists, an empty grid if not.
     */
    public Parameters getParameters(String algorithm)
    {
        return this.partitionsGrid.getOrDefault(algorithm, new Parameters());
    }
}
