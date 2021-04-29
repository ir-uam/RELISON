/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.community;


import com.esotericsoftware.yamlbeans.YamlReader;
import es.uam.eps.ir.socialranksys.grid.Parameters;
import es.uam.eps.ir.socialranksys.grid.YAMLParametersReader;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Reads the parameter configuration for several community detection algorithms.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class YAMLCommunityDetectionParamReader extends YAMLParametersReader
{
    /**
     * Identifier for the name of the mechanism
     */
    private final static String ALGORITHMS = "algorithms";
    /**
     * The set of parameters for the community detection algorithms.
     */
    private final Map<String, Parameters> communityDetectionAlgorithms;
    /**
     * The name of the file
     */
    private final String file;

    /**
     * Constructor.
     * @param file The name of the file containing the parameters for the different algorithms.
     */
    public YAMLCommunityDetectionParamReader(String file)
    {
        this.file = file;
        this.communityDetectionAlgorithms = new HashMap<>();
    }
    
    /**
     * Reads a XML document containing the parameters
     */
    public void readDocument()
    {
        try
        {
            // First of all, obtain the XML document
            File inputFile = new File(file);
            Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile)));
            YamlReader yaml = new YamlReader(reader);

            Map<String, Object> map = (Map<String, Object>) yaml.read();
            Map<String, Object> algs = (Map<String, Object>) map.get(ALGORITHMS);

            for(Map.Entry<String, Object> entry : algs.entrySet())
            {
                this.readAlgorithm(entry);
            }
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }
    }
    
    
    /**
     * Reads the elements of a algorithms mechanism.
     * @param element the YAML element containing the information for that algorithms mechanism.
     */
    public void readAlgorithm(Map.Entry<String,Object> element)
    {
        String name = element.getKey();

        if(element.getValue().getClass() == String.class)
        {
            Parameters p = new Parameters();
            this.communityDetectionAlgorithms.put(name, p);
        }
        else
        {
            Map<String, Object> params = (Map<String, Object>) element.getValue();
            Parameters p = readParameterValues(params);
            this.communityDetectionAlgorithms.put(name, p);
        }
    }

    /**
     * Gets the set of community detection algorithms previously read from the YAML file.
     * @return the set of algorithms.
     */
    public Set<String> getAlgorithms()
    {
        return this.communityDetectionAlgorithms.keySet();
    }
    
    /**
     * Gets the parameters for the given algorithm.
     * @param algorithm the community detection algorithm to search.
     * @return The parameters if it exists, an empty parameter object if not.
     */
    public Parameters getParameters(String algorithm)
    {
        return this.communityDetectionAlgorithms.getOrDefault(algorithm, new Parameters());
    }
    
    /**
     * Shows the configuration of the different algorithms.
     * @return a string containing the configuration of the different community detection algorithms.
     */
    public String printCommunityDetectionAlgorithms() 
    {
        StringBuilder algorithms = new StringBuilder();
        for(String algorithm : this.communityDetectionAlgorithms.keySet())
        {
            algorithms.append(algorithm).append("\n");
            Parameters values = this.communityDetectionAlgorithms.get(algorithm);
            
            algorithms.append(values.getBooleanValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x, y) -> x + y));
        
            algorithms.append(values.getDoubleValues()
                .entrySet()
                .stream()
                .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                .reduce("", (x, y) -> x + y));

            algorithms.append(values.getIntegerValues()
                 .entrySet()
                 .stream()
                 .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                 .reduce("", (x, y) -> x + y));

            algorithms.append(values.getLongValues()
                 .entrySet()
                 .stream()
                 .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                 .reduce("", (x, y) -> x + y));

            algorithms.append(values.getStringValues()
                 .entrySet()
                 .stream()
                 .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                 .reduce("", (x, y) -> x + y));

            algorithms.append(values.getOrientationValues()
                  .entrySet()
                  .stream()
                  .map(entry -> "\t" + entry.getKey() + ": " + entry.getValue() + "\n")
                  .reduce("", (x, y) -> x + y));
        }
        
        return algorithms.toString();
    }
}
