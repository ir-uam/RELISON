/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.filter;

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

/**
 * Reads the values for a filter
 * @author Javier Sanz-Cruzado Puig
 */
public class FilterParamReader extends ParametersReader
{
    /**
     * Identifier for the name of the mechanism
     */
    private final static String NAME = "name";
    /**
     * Identifier for the parameters
     */
    private final static String PARAM = "param";
    /**
     * Identifiers for the set of filters.
     */
    private final static String FILTERS = "filters";
    /**
     * Name of the filter
     */
    private String name;
    /**
     * Parameter values for the filter.
     */
    private Parameters values;
    
    private String file;
        
    public FilterParamReader()
    {
        this.file = "";
    }
    
    public FilterParamReader(String file)
    {
        this.file = file;
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
            
            // TODO: Preparar filtros para leer desde aqui - Preparar autofiltros.           
            NodeList protocolList = parent.getElementsByTagName(FILTERS);
            protocolList = protocolList.item(0).getChildNodes();
   
            for(int i = 0; i < protocolList.getLength(); ++i)
            {
                Node node = protocolList.item(i);
                
                if(node.getNodeType() == Node.ELEMENT_NODE)
                {
                    Element element = (Element) node;
                    this.readFilter(element);
                }
            }
                        
        } 
        catch (ParserConfigurationException | SAXException | IOException ex)
        {
            ex.printStackTrace();
        }
    }
    
    
    /**
     * Reads the elements of a filter
     * @param node the node containing the information for that filter.
     */
    public void readFilter(Element node)
    {
        this.name = node.getElementsByTagName(NAME).item(0).getTextContent();
        
        NodeList params = node.getElementsByTagName(PARAM);
        if(params == null || params.getLength() == 0)
        {
            this.values = new Parameters();
        }
        else
        {
            this.values = this.readParameterGrid(params);
        }
    }

    /**
     * Obtains the name of the filter.
     * @return the name of the filter.
     */
    public String getName() 
    {
        return name;
    }

    /**
     * Obtains the parameters for the filter.
     * @return the parameters.
     */
    public Parameters getParams() 
    {
        return values;
    }

    /**
     * Shows the configuration of a filter.
     * @return a string containing the configuration of the filter.
     */
    public String printFilter() 
    {
        String filter = "";
        
        filter += this.getName() + "\n";
        
        filter += this.values.getBooleanValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        filter += this.values.getDoubleValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        filter += this.values.getIntegerValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        filter += this.values.getLongValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        filter += this.values.getStringValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);
        
        filter += this.values.getOrientationValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x,y) -> x + y);

        return filter;
    }
    
}
