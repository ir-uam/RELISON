/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.metrics.features;

import es.uam.eps.ir.sonalire.diffusion.metrics.AbstractGlobalSimulationMetric;
import es.uam.eps.ir.sonalire.diffusion.simulation.Iteration;

import java.io.Serializable;

/**
 * Class that represents a global metric that considers the existence of several features.
 * We differ two types of features:
 * 
 * <ul>
 *  <li><b>User features:</b> (Ex.: Communities) In this case, we take the values of the feature
 *  for the creators of the received information pieces. </li>
 *  <li><b>Information features:</b> (Ex: hashtags) In this case, we take the values of the features
 *  for the different information pieces which are received and observed by each individual user.</li>
 * </ul>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user / information pieces features.
 */
public abstract class AbstractFeatureGlobalSimulationMetric<U extends Serializable, I extends Serializable, F> extends AbstractGlobalSimulationMetric<U,I, F>
{
    /**
     * Indicates if the feature we are analyzing depends on the user (true) or the information piece (false).
     */
    private final boolean userFeats;
    /**
     * Feature name.
     */
    private final String feature;
    
    /**
     * Constructor.
     * @param name      the name of the metric.
     * @param userFeats true if it uses the features of the users, false if it uses the features of the information pieces.
     * @param feature   the name of the feature.
     */
    public AbstractFeatureGlobalSimulationMetric(String name, boolean userFeats, String feature)
    {
        super(name);
        this.userFeats = userFeats;
        this.feature = feature;
    }

    /**
     * Indicates if we are using a user feature (true) or an information piece feature (false).
     * @return true if we use a user feature, false if we use an information piece feature.
     */
    protected boolean usesUserFeatures()
    {
        return userFeats;
    }

    /**
     * Obtains the name of the feature we are using.
     * @return the name of the feature.
     */
    protected String getFeature()
    {
        return feature;
    }
    
    @Override
    public void update(Iteration<U,I, F> iteration)
    {
        if(this.isInitialized())
        {
            if(this.usesUserFeatures())
                this.updateUserFeature(iteration);
            else
                this.updateInfoFeature(iteration);
        }
    }

    /**
     * Updates the necessary variables to compute a metric, in case the feature
     * values we are using belongs to the creators of the information pieces received
     * by the users in the network.
     * @param iteration the new iteration.
     */
    protected abstract void updateUserFeature(Iteration<U, I, F> iteration);

    /**
     * Updates the necessary variables to compute a metric, in case the feature 
     * values we are using belong to the information pieces received by the users in
     * the network.
     * @param iteration the new iteration. 
     */
    protected abstract void updateInfoFeature(Iteration<U, I, F> iteration);
}
