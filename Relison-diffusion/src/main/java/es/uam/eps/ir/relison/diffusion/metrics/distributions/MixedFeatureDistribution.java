/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.metrics.distributions;

import es.uam.eps.ir.relison.diffusion.simulation.Iteration;
import es.uam.eps.ir.relison.utils.datatypes.Pair;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
/**
 * Distribution combining user and information pieces features. It measures how many times pieces with each
 * information piece, created by a user with a given user feature have been received.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the the user / information pieces features.
 */
public class MixedFeatureDistribution<U extends Serializable,I extends Serializable, F> extends AbstractDistribution<U,I, F>
{
    /**
     * Name of the distribution.
     */
    private final static String INFOPIECES = "mixed-";
    /**
     * Name of the information pieces parameter.
     */
    private final String infoFeature;
    /**
     * Name of the user parameter.
     */
    private final String userFeature;
    /**
     * Relation between users and tags.
     */
    private final Map<Pair<F>, Double> relation;
    
    /**
     * Constructor.
     * @param infoFeature name of the information pieces feature.
     * @param userFeature name of the user feature.
     */
    public MixedFeatureDistribution(String infoFeature, String userFeature)
    {
        super(INFOPIECES + "-" + infoFeature + "-" + userFeature);
        this.infoFeature = infoFeature;
        this.userFeature = userFeature;
        this.initialized = false;
        this.relation = new HashMap<>();
    }
    
    @Override
    public void update(Iteration<U, I, F> iteration)
    {
        if(this.isInitialized())
        {
            iteration.getReceivingUsers().forEach(u ->
                iteration.getSeenInformation(u).forEach(i ->
                    data.getInfoPiecesFeatures(i.v1(), infoFeature).forEach(infopar ->
                        data.getCreators(i.v1()).forEach(v ->
                            data.getUserFeatures(v, userFeature).forEach(userpar ->
                            {
                                Pair<F> pair = new Pair<>(infopar.v1, userpar.v1);
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
            for(Pair<F> pair : this.relation.keySet())
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
        if(!this.isInitialized() && data.isUserFeature(userFeature) && data.isInfoPieceFeature(infoFeature))
        {
            this.relation.clear();
            data.getAllFeatureValues(infoFeature).forEach(info ->
               data.getAllFeatureValues(userFeature).forEach(user ->
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
