/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.algorithms.knn.similarities;

import es.uam.eps.ir.socialranksys.grid.Configurations;
import es.uam.eps.ir.socialranksys.grid.ConfigurationsReader;
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
 * Reads the grids for several algorithms.
 * @author Javier Sanz-Cruzado Puig
 */
public class SimilarityConfigurationReader extends ConfigurationsReader
{
    /**
     * Algorithms grid. Uses a grid for each algorithm.
     */
    private final Map<String, Configurations> similarityConfigs;
    /**
     * The name of the file
     */
    private final String file;
    
    /**
     * Constructor.
     * @param file File that contains the grid data 
     */
    public SimilarityConfigurationReader(String file)
    {
        this.file = file;
        this.similarityConfigs = new HashMap<>();
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
            for(int i = 0; i < nodeList.getLength(); ++i)
            {
                Node node = nodeList.item(i);
                if(node.getNodeType() == Node.ELEMENT_NODE)
                {
                    Element element = (Element) node;
                    this.readSimilarity(element);
                }
            }
            
        } catch (ParserConfigurationException | SAXException | IOException ex)
        {
            ex.printStackTrace();
        }
    }
    
    /**
     * Reads the grid for a single algorithm.
     * @param element The XML Element containing the algorithm information
     */
    private void readSimilarity(Element element)
    {
        String algorithmName = element.getElementsByTagName("name").item(0).getTextContent();
        NodeList parametersNodes = element.getElementsByTagName("configs");
        if(parametersNodes == null || parametersNodes.getLength() == 0)
        {
            this.similarityConfigs.put(algorithmName, new Configurations());
        }
        else
        {
            Element parametersNode = (Element) parametersNodes.item(0);
            NodeList parameters = parametersNode.getElementsByTagName("config");
            ParametersReader paramReader = new SimilarityParametersReader();
            Configurations conf = this.readConfigurationGrid(parameters, paramReader);
            this.similarityConfigs.put(algorithmName, conf);
        }
    }

    /**
     * Gets the set of algorithms previously read.
     * @return The set of algorithms previously read from the grid file.
     */
    public Set<String> getSimilarities()
    {
        return this.similarityConfigs.keySet();
    }
    
    /**
     * Gets the grid for a given algorithm
     * @param algorithm The algorithm to search
     * @return The grid if exists, an empty grid if not.
     */
    public Configurations getConfigurations(String algorithm)
    {
        return this.similarityConfigs.getOrDefault(algorithm, new Configurations());
    }
}
