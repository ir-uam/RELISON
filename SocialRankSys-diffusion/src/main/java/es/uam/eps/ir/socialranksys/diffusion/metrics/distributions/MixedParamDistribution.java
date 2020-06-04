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
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Distribution that combines information and user parameters.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class MixedParamDistribution<U extends Serializable,I extends Serializable,P> extends AbstractDistribution<U,I,P> 
{
    /**
     * Name of the distribution
     */
    private final static String INFOPIECES = "mixed-";
    /**
     * Name of the information pieces parameter.
     */
    private final String infoParameter;
    /**
     * Name of the user parameter.
     */
    private final String userParameter;
    /**
     * Relation between users and tags.
     */
    private final Map<Pair<P>, Double> relation;
    
    /**
     * Constructor.
     * @param infoParameter Name of the information pieces parameter.
     * @param userParameter Name of the user parameter.
     */
    public MixedParamDistribution(String infoParameter, String userParameter)
    {
        super(INFOPIECES + "-" + infoParameter + "-" + userParameter);
        this.infoParameter = infoParameter;
        this.userParameter = userParameter;
        this.initialized = false;
        this.relation = new HashMap<>();
    }
    
    @Override
    public void update(Iteration<U, I, P> iteration)
    {
        if(this.isInitialized())
        {
            iteration.getReceivingUsers().forEach(u ->
                iteration.getSeenInformation(u).forEach(i ->
                    data.getInfoPiecesFeatures(i.v1(), infoParameter).forEach(infopar ->
                        data.getCreators(i.v1()).forEach(v ->
                            data.getUserFeatures(v, userParameter).forEach(userpar -> 
                            {
                                Pair<P> pair = new Pair<>(infopar.v1, userpar.v1);
                                this.relation.put(pair, this.relation.get(pair) + infopar.v2*userpar.v2);
                            })
                        )
                    )
                )
            );
        }
    }

    @Override
    public void print(String file) 
    {
        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file))))
        {
            for(Pair<P> pair : this.relation.keySet())
            {
                bw.write(pair.v1() + "\t" + pair.v2() + "\t" + this.relation.get(pair) + "\n");
            }
        } 
        catch (IOException ex) 
        {
            ex.printStackTrace();
        }
    }

    @Override
    protected void initialize() 
    {
        if(!this.isInitialized() && data.isUserFeature(userParameter) && data.isInfoPieceFeature(infoParameter))
        {
            this.relation.clear();
            data.getAllFeatureValues(infoParameter).forEach(info ->
               data.getAllFeatureValues(userParameter).forEach(user ->
                   this.relation.put(new Pair<>(info, user), 0.0)
               )
            );
            this.initialized = true;
        }
    }
    
    
    @Override
    public void clear() 
    {
        this.relation.clear();
        this.initialized = false;
    } 
}
