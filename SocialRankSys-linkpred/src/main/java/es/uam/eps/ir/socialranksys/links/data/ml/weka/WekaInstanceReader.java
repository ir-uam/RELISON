/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.ml.weka;

import es.uam.eps.ir.socialranksys.links.data.ml.attributes.AttrType;
import es.uam.eps.ir.socialranksys.links.data.ml.attributes.Attribute;
import es.uam.eps.ir.socialranksys.links.data.ml.attributes.Attributes;
import es.uam.eps.ir.socialranksys.links.data.ml.io.PatternReader;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;
import org.openide.util.Exceptions;
import org.ranksys.formats.parsing.Parser;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Reads patterns for Weka.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class WekaInstanceReader<U> implements PatternReader<U,Instances,Instance>
{
    
    /**
     * Attribute information for Weka
     */
    FastVector attributes;
    /**
     * Types of the attribute (Nominal, Continuous or Numerical, Class...)
     */
    List<AttrType> types;
    /**
     * Names of the attributes
     */
    List<String> names;
    /**
     * Class index
     */
    int classIndex;
    /**
     * Training set
     */
    private Instances trainSet;
    /**
     * Test set
     */
    private Instances testSet;
    /**
     * Relation between test set nodes and the instances
     */
    private Map<Pair<U>, Integer> instanceIndexer;
    
    /**
     * Constructor.
     */
    public WekaInstanceReader()
    {
        attributes = null;
        types = null;
        classIndex = -1;
        trainSet = null;
        testSet = null;
        instanceIndexer = null;
    }
    
    @Override
    public boolean readAttributes(String attributeFile)
    {
        this.attributes = new FastVector();
        this.types = new ArrayList<>();
        this.names = new ArrayList<>();
        List<FastVector> nominalAttrs = new ArrayList<>();

        try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(attributeFile))))
        {
            String line = br.readLine(); // Header line
            int i = 0;
            while((line = br.readLine()) != null)
            {
                String[] split = line.split("\t");
                String name = split[0];
                AttrType type = AttrType.getValue(split[1]);
                names.add(name);
                types.add(type);
                nominalAttrs.add(new FastVector());
                if(type.equals(AttrType.CONTINUOUS))
                {
                    this.attributes.addElement(new weka.core.Attribute(name));
                }
                else
                {
                    String[] values = split[2].split(",");
                    for(String value : values)
                    {
                        nominalAttrs.get(i).addElement(value);
                    }
                    this.attributes.addElement(new weka.core.Attribute(name, nominalAttrs.get(i)));
                    if(type.equals(AttrType.CLASS))
                        this.classIndex = i;
                }
                ++i;
            }
            
            return true;
            
        } 
        catch (IOException ex) 
        {
            return false;
        }
    }
    
    @Override
    public boolean readTrain(String trainFile)
    {
        if(this.attributes == null || this.types == null || this.classIndex == -1)
        {
            return false;
        }
        
        this.trainSet = new Instances("train", attributes, 0);
        this.trainSet.setClassIndex(this.classIndex);
        try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(trainFile))))
        {
            br.readLine(); // Attribute names
            br.readLine(); // Attribute types
            String line;
            int j = 0;
            while((line = br.readLine()) != null)
            {
                String[] split = line.split("\t");
                Instance inst = new Instance(attributes.size());
                inst.setDataset(trainSet);
                for(int i = 0; i < split.length; ++i)
                {
                    if(types.get(i) == AttrType.CONTINUOUS)
                        inst.setValue(i, Double.parseDouble(split[i]));
                    else
                        inst.setValue(i, split[i]);
                }
                trainSet.add(inst);
                
                ++j;
                if(j % 100000 == 0)
                {
                    System.out.println("Read and processed" + j + " patterns.");
                }
            }
            
            return true;
        }
        catch (IOException ex) 
        {
            return false;
        }
    }
    
    @Override
    public boolean readTest(String testFile, Parser<U> parser)
    {
        if(this.attributes == null || this.types == null || this.classIndex == -1)
        {
            return false;
        }
        
        try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(testFile))))
        {
            this.testSet = new Instances("test", attributes, 0);
            testSet.setClassIndex(trainSet.classIndex());
            this.instanceIndexer = new HashMap<>();
            
            // Read headers
            br.readLine();
            br.readLine();
            
            int j = 0;
            String line;
            while((line = br.readLine()) != null)
            {
                String[] split = line.split("\t");
                U u = parser.parse(split[0]);
                U v = parser.parse(split[1]);
                Instance inst = new Instance(attributes.size());
                inst.setDataset(testSet);
                for(int i = 0; i < split.length - 2; ++i)
                {
                    if(types.get(i) == AttrType.CONTINUOUS)
                        inst.setValue(i, Double.parseDouble(split[i+2]));
                    else
                    {
                        String value = split[i+2];
                        inst.setValue(i, value);
                    }
                }
                this.testSet.add(inst);
                this.instanceIndexer.put(new Pair<>(u,v), j);
                ++j;
                if(j%100000 == 0)
                {
                    System.out.println("" + j + " patterns read");
                }
            }
            System.out.println("Finished processing " + j + " patterns");
            
        } 
        catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return false;
        }
        
        return true;
        
    }

    @Override
    public Instances getTrainSet() 
    {
        return trainSet;
    }

    @Override
    public Instances getTestSet() 
    {
        return testSet;
    }

    /**
     * Obtains the relation between the test set instances and the nodes of the network.
     * @return the relation between the test set instances and the nodes of the network.
     */
    public Map<Pair<U>, Integer> getInstanceIndexer() 
    {
        return instanceIndexer;
    }

    @Override
    public Instance getTestInstance(U u, U v) 
    {
        if(this.testSet == null)
        {
            return null;
        }
        
        Pair<U> pair = new Pair<>(u,v);
        int index = this.instanceIndexer.get(pair);
        return this.testSet.instance(index);
    }

    /**
     * Get the weka version of the attributes (the FastVector)
     * @return the FastVector containing the weka attributes.
     */
    public FastVector getFVAttributes()
    {
        return this.attributes;
    }
    @Override
    public Attributes getAttributes()
    {
        if(this.attributes == null)
            return null;
        
        List<Attribute> attribs = new ArrayList<>();
        
        for(int i = 0; i < this.names.size(); ++i)
        {
            String name = this.names.get(i);
            AttrType type = this.types.get(i);
            Attribute attrib = new Attribute(name,type);
            if(!type.equals(AttrType.CONTINUOUS))
            {
                weka.core.Attribute attr = (weka.core.Attribute) this.attributes.elementAt(i);
                Enumeration<String> values = (Enumeration<String>) attr.enumerateValues();
                while(values.hasMoreElements())
                {
                    attrib.addValue(values.nextElement());
                }
            }
            attribs.add(attrib);
        }
        
        return new Attributes(attribs, this.classIndex);
    }
    
    
    
}
