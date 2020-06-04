/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms;

import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.GridReader;
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
 * Reads the grids for several algorithms.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class AlgorithmGridReader extends GridReader
{
    /**
     * Algorithms grid. Uses a grid for each algorithm.
     */
    private final Map<String, Grid> algorithmsGrid;
    /**
     * The name of the file
     */
    private final String file;

    /**
     * Constructor
     *
     * @param file File that contains the grid data
     */
    public AlgorithmGridReader(String file)
    {
        this.file = file;
        this.algorithmsGrid = new HashMap<>();
    }

    /**
     * Reads a XML document containing a grid
     */
    public void readDocument()
    {
        try
        {
            // First of all, obtain the XML Document
            File inputFile = new File(file);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);

            Element parent = doc.getDocumentElement();
            parent.normalize();

            NodeList nodeList = parent.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); ++i)
            {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE)
                {
                    Element element = (Element) node;
                    this.readAlgorithm(element);
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
     *
     * @param element The XML Element containing the algorithm information
     */
    private void readAlgorithm(Element element)
    {
        String algorithmName = element.getElementsByTagName("name").item(0).getTextContent();
        NodeList parametersNodes = element.getElementsByTagName("params");
        if (parametersNodes == null || parametersNodes.getLength() == 0)
        {
            this.algorithmsGrid.put(algorithmName, new Grid());
        }
        else
        {
            Element parametersNode = (Element) parametersNodes.item(0);
            NodeList parameters = parametersNode.getElementsByTagName("param");
            Grid g = readParameterGrid(parameters);
            this.algorithmsGrid.put(algorithmName, g);
        }
    }

    /**
     * Gets the set of algorithms previously read.
     *
     * @return The set of algorithms previously read from the grid file.
     */
    public Set<String> getAlgorithms()
    {
        return this.algorithmsGrid.keySet();
    }

    /**
     * Gets the grid for a given algorithm
     *
     * @param algorithm The algorithm to search
     *
     * @return The grid if exists, an empty grid if not.
     */
    public Grid getGrid(String algorithm)
    {
        return this.algorithmsGrid.getOrDefault(algorithm, new Grid());
    }
}
