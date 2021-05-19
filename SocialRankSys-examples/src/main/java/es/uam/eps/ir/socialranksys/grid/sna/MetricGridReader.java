/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.uam.eps.ir.socialranksys.grid.sna;

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
 * Reads the grids for several graph metrics.
 * @author Javier Sanz-Cruzado Puig
 */
public class MetricGridReader extends GridReader
{
    /**
     * Metrics grid. Uses a grid for each metric, and classifies each metric
     * according to its category.
     */
    private final Map<String, Map<String, Grid>> metricsGrid;
    /**
     * The name of the file
     */
    private final String file;
    
    /**
     * Constructor
     * @param file File that contains the grid data 
     */
    public MetricGridReader(String file)
    {
        this.file = file;
        this.metricsGrid = new HashMap<>();
        MetricTypeIdentifiers.values().forEach(type -> this.metricsGrid.put(type, new HashMap<>()));
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
                    this.readMetric(element);
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
    private void readMetric(Element element)
    {
        String metricName = element.getElementsByTagName("name").item(0).getTextContent();
        String metricType = element.getElementsByTagName("type").item(0).getTextContent();
        
        if(metricsGrid.containsKey(metricType))
        {
            NodeList parametersNodes = element.getElementsByTagName("params");
            if(parametersNodes == null || parametersNodes.getLength() == 0)
            {
                this.metricsGrid.get(metricType).put(metricName, new Grid());
            }
            else
            {
                Element parametersNode = (Element) parametersNodes.item(0);
                NodeList parameters = parametersNode.getElementsByTagName("param");
                Grid g = readParameterGrid(parameters);
                this.metricsGrid.get(metricType).put(metricName, g);
            }
        }
        else 
        { 
            System.err.println("Invalid metric type");
        }
    }
    
    /**
     * Gets the set of algorithms previously read.
     * @param type Metric type
     * @return The set of algorithms previously read from the grid file.
     */
    public Set<String> getMetrics(String type)
    {
        return this.metricsGrid.get(type).keySet();
    }
    
    /**
     * Gets the grid for a given algorithm
     * @param algorithm The algorithm to search
     * @param type Metric type
     * @return The grid if exists, an empty grid if not.
     */
    public Grid getGrid(String algorithm, String type)
    {
        return this.metricsGrid.get(type).getOrDefault(algorithm, new Grid());
    }
}
