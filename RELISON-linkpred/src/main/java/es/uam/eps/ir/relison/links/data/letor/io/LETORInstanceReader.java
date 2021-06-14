/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.data.letor.io;

import es.uam.eps.ir.relison.links.data.letor.FeatureInformation;
import es.uam.eps.ir.relison.links.data.letor.FeatureType;
import es.uam.eps.ir.relison.links.data.letor.Instance;
import es.uam.eps.ir.relison.links.data.letor.InstanceSet;
import es.uam.eps.ir.relison.utils.generator.Generator;
import org.ranksys.formats.parsing.Parser;
import org.ranksys.formats.parsing.Parsers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static es.uam.eps.ir.relison.links.data.letor.io.LETORFormatConstants.COMMENT;
import static es.uam.eps.ir.relison.links.data.letor.io.LETORFormatConstants.IDSEP;


/**
 * Class for reading the different patterns for ML algorithms as contact
 * recommendation / link prediction algorithms, using the LETOR
 * format. <br>
 *
 * Format: <br>
 * 
 * #featId1: description1 stats1 <br>
 * #featId2: description2 stats2 <br>
 * ... <br>
 * #featIdN: descriptionN statsN <br>
 * [relevance(u,v) or class1(u,v)] qid:[user u] [featId1]:[value1(u,v)] [featId2]:[value2(u,v)] ... [featIdN]:[valueN(u,v)] #docId=[user v] <br>
 * ... <br>
 * [relevance(u,v) or class1(u,v)] qid:[user u] [featId1]:[value1(u,v)] [featId2]:[value2(u,v)] ... [featIdN]:[valueN(u,v)] #docId=[user v] <br>
 *
 * @see <a href="https://www.microsoft.com/en-us/research/project/letor-learning-rank-information-retrieval/#!letor-4-0">LETOR v.4.0</a>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 *
 * @param <U> Type of the users.
 */
public class LETORInstanceReader<U> implements InstanceSetReader<U>
{
    /**
     * Parser for obtaining the identifiers of users.
     */
    private final Parser<U> parser;

    /**
     * The number of features.
     */
    private final Integer counter;

    /**
     * User identifier generator.
     */
    private final Generator<U> gen;
    
    /**
     * Constructor.
     * @param parser parser for the users. 
     */
    public LETORInstanceReader(Parser<U> parser)
    {
        this.parser = parser;
        this.counter = null;
        this.gen = null;
    }

    /**
     * Constructor.
     * @param parser    user parser.
     * @param counter   number of features.
     * @param gen       user identifier generator.
     */
    public LETORInstanceReader(Parser<U> parser, int counter, Generator<U> gen)
    {
        this.parser = parser;
        this.counter = counter;
        this.gen = gen;
    }

    @Override
    public InstanceSet<U> read(String file) throws IOException
    {
        try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file))))
        {

            List<String> featureDescr = new ArrayList<>();
            List<FeatureType> featureTypes = new ArrayList<>();
            
            // First, read the headers containing the feature information.
            String line = "";
            FeatureInformation featInfo;
            if(counter == null)
            {
                List<String> header = new ArrayList<>();
                while ((line = br.readLine()).startsWith(COMMENT))
                {
                    header.add(line);
                }
                featInfo = this.readHeader(header);
            }
            else
            {
                List<String> descriptions = new ArrayList<>();
                List<FeatureType> types = new ArrayList<>();
                for(int i = 0; i < counter; ++i)
                {
                    descriptions.add("" + (i+1));
                    types.add(FeatureType.CONTINUOUS);
                }

                featInfo = new FeatureInformation(descriptions, types);
            }
            

            int numFeats = featInfo.numFeats();
            
            // Then, read all the patterns.
            InstanceSet<U> patternSet = new InstanceSet<>(featInfo);

            if(counter != null)
            {
                line = br.readLine();
            }
            do
            {
                Instance<U> pattern = this.readInstance(line, numFeats);
                patternSet.addInstance(pattern);
            }
            while((line = br.readLine()) != null);
            
            return patternSet;
        }
    }
    

    /**
     * Reads the header of the file into an object containing all.
     * information about features.
     * @param header the header lines.
     * @return an object containing information about features.
     */
    @Override
    public FeatureInformation readHeader(List<String> header)
    {
        List<String> featureDescr = new ArrayList<>();
        List<FeatureType> featureTypes = new ArrayList<>();
        for(String line : header)
        {
            int nominal = line.indexOf("NOMINAL");
            int cont = line.indexOf("CONTINUOUS");
            
            int idx = Math.max(nominal, cont);
            String description = line.substring(line.indexOf(" ") + 1, idx-1);
            FeatureType type;
            if(idx == nominal) 
                type = FeatureType.NOMINAL;
            else
                type = FeatureType.CONTINUOUS;
            featureDescr.add(description);
            featureTypes.add(type);
        }
        return new FeatureInformation(featureDescr, featureTypes);
 }
    
    /**
     * Reads an individual instance.
     * @param line      the line string containing the instance.
     * @param numFeats  the number of features.
     * @return the instance.
     */
    @Override
    public Instance<U> readInstance(String line, int numFeats)
    {
        // Initialize the features to 0.0
        List<Double> values = new ArrayList<>();
        for (int i = 0; i < numFeats; ++i)
        {
            values.add(0.0);
        }

        // First, split the line using spaces.
        String[] split = line.split("\\s+");

        // Obtain the category (relevance in this case)
        Double category = Parsers.dp.parse(split[0]);

        // Obtain the origin user
        String[] qid = split[1].split(":");
        U u = parser.parse(qid[1]);

        // Obtain the feature values.
        int i = 2;

        U v;
        if (this.counter == null)
        {
            while (!split[i].startsWith(COMMENT))
            {
                String[] split2 = split[i].split(IDSEP);
                int featId = Parsers.ip.parse(split2[0]);
                double val = Parsers.dp.parse(split2[1]);

                values.set(featId - 1, val);
                ++i;
            }

            String[] did = split[i].split(IDSEP);
            v = parser.parse(did[1]);
        }
        else
        {
            v = null;
            int splitSize = split.length;
            for(i = 2; i < splitSize; ++i)
            {
                String[] split2 = split[i].split(IDSEP);

                if(i == (splitSize - 1))
                {
                    v = parser.parse(split2[2]);
                    split2[1] = split2[1].split(COMMENT)[0];
                }

                int featId = Parsers.ip.parse(split2[0]);
                double val = Parsers.dp.parse(split2[1]);

                values.set(featId - 1, val);
            }
        }
        if(v == null)
        {
            assert gen != null;
            v = gen.generate();
        }

        return new Instance<>(u,v,values,category.intValue());
    }
    
}
