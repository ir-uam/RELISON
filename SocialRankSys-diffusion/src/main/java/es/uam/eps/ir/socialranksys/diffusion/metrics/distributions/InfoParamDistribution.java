/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.metrics.distributions;

import es.uam.eps.ir.socialranksys.diffusion.simulation.Iteration;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Distribution for user params. It measures how many times a information has arrived
 * from users with a certain parameter.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the items.
 * @param <P> Type of the parameters.
 */
public class InfoParamDistribution<U extends Serializable,I extends Serializable,P> extends AbstractDistribution<U,I,P> 
{
    /**
     * Name of the distribution
     */
    private final static String INFOPARAM = "info-feat";
    /**
     * Name of the tag parameter.
     */
    private final String parameter;
    /**
     * Relation between users and tags.
     */
    private final Map<P, Double> distrib;

    
    /**
     * Constructor. 
     * @param parameter The name of the user parameter whose distribution we want to find.
     */
    public InfoParamDistribution(String parameter)
    {
        super(INFOPARAM + "-" + parameter);
        this.parameter = parameter;
        this.distrib = new HashMap<>();
        this.initialized = false;
    }
    
    @Override
    public void update(Iteration<U,I,P> iteration)
    {
        if(this.isInitialized())
        {
            iteration.getReceivingUsers().forEach(u ->
                iteration.getSeenInformation(u).forEach(i ->
                   data.getInfoPiecesFeatures(i.v1(), parameter).forEach(p ->
                       this.distrib.put(p.v1, this.distrib.get(p.v1) + p.v2))));
        }
    }
    
    @Override
    public void print(String file)
    {
        if(this.isInitialized())
        {
            try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file))))
            {
                List<P> params = data.getAllFeatureValues(parameter).collect(Collectors.toCollection(ArrayList::new));
                for(P param : params)
                {
                    bw.write(param + "\t" + this.distrib.get(param) + "\n");
                }
            } 
            catch (IOException ex) 
            {
                ex.printStackTrace();
            }
        }
    }
    

    @Override
    public void clear() 
    {
        this.distrib.clear();
        this.initialized = false;
    }
    
    @Override
    public String getName() 
    {
        return "distribuser-" + this.parameter;
    }

    @Override
    protected void initialize() 
    {
        if(!this.isInitialized() && data.isUserFeature(parameter))
        {
            this.distrib.clear();
            this.data.getAllFeatureValues(parameter).forEach(p -> this.distrib.put(p, 0.0));
            this.initialized = true;
        }
    }

    
}
