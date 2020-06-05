/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers;

import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.grid.GridReader;
import org.openide.util.Exceptions;
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
 * Reads the grids for several rerankers.
 * @author Javier Sanz-Cruzado Puig
 */
public class RerankerGridReader extends GridReader
{
    /**
     * Rerankers grid. Uses a grid for each reranker.
     */
    private final Map<String, Grid> rerankersGrid;
    /**
     * The name of the file
     */
    private final String file;
    
    /**
     * Constructor
     * @param file File that contains the grid data 
     */
    public RerankerGridReader(String file)
    {
        this.file = file;
        this.rerankersGrid = new HashMap<>();
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
                    this.readReranker(element);
                }
            }
            
        } catch (ParserConfigurationException | SAXException | IOException ex)
        {
            Exceptions.printStackTrace(ex);
        }
    }
    
    /**
     * Reads the grid for a single reranker.
     * @param element The XML Element containing the reranker information
     */
    private void readReranker(Element element)
    {
        String algorithmName = element.getElementsByTagName("name").item(0).getTextContent();
        NodeList parametersNodes = element.getElementsByTagName("params");
        if(parametersNodes == null || parametersNodes.getLength() == 0)
        {
            this.rerankersGrid.put(algorithmName, new Grid());
        }
        else
        {
            Element parametersNode = (Element) parametersNodes.item(0);
            NodeList parameters = parametersNode.getElementsByTagName("param");
            Grid g = readParameterGrid(parameters);
            this.rerankersGrid.put(algorithmName, g);
        }
    }

    /**
     * Gets the set of rerankers previously read.
     * @return The set of rerankers previously read from the grid file.
     */
    public Set<String> getRerankers()
    {
        return this.rerankersGrid.keySet();
    }
    
    /**
     * Gets the grid for a given algorithm
     * @param reranker The reranker to search
     * @return The grid if exists, an empty grid if not.
     */
    public Grid getGrid(String reranker)
    {
        return this.rerankersGrid.getOrDefault(reranker, new Grid());
    }
}
