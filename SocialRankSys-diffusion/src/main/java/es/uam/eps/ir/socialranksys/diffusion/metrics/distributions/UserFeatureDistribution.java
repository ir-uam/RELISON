/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
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
 * Distribution for user features. It measures how many times each information pieces created by a user
 * with this feature have been propagated.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the the user / information pieces features.
 */
public class UserFeatureDistribution<U extends Serializable,I extends Serializable, F> extends AbstractDistribution<U,I, F>
{
    /**
     * Name of the distribution.
     */
    private final static String USERFEAT = "user-feat";
    /**
     * Name of the user feature.
     */
    private final String feature;
    /**
     * The distribution of features.
     */
    private final Map<F, Double> distrib;

    
    /**
     * Constructor. 
     * @param feature The name of the user feature whose distribution we want to find.
     */
    public UserFeatureDistribution(String feature)
    {
        super(USERFEAT + "-" + feature);
        this.feature = feature;
        this.distrib = new HashMap<>();
        this.initialized = false;
    }
    
    @Override
    public void update(Iteration<U,I, F> iteration)
    {
        if(this.isInitialized())
        {
            iteration.getReceivingUsers().forEach(u ->
                iteration.getSeenInformation(u).forEach(i ->
                    data.getCreators(i.v1()).forEach(v ->
                        data.getUserFeatures(u, feature).forEach(p ->
                            this.distrib.put(p.v1, this.distrib.get(p.v1) + p.v2)
                        )
                    )
                )
            );
        }
    }
    
    @Override
    public void print(String file)
    {
        if(this.isInitialized())
        {
            try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file))))
            {
                List<F> params = data.getAllFeatureValues(feature).collect(Collectors.toCollection(ArrayList::new));
                for(F param : params)
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
        return "distribuser-" + this.feature;
    }

    @Override
    protected void initialize() 
    {
        if(!this.isInitialized() && data.isUserFeature(feature))
        {
            this.distrib.clear();
            this.data.getAllFeatureValues(feature).forEach(p -> this.distrib.put(p, 0.0));
            this.initialized = true;
        }
    }

    
}
