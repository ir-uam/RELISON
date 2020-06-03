/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.metrics.features;

import es.uam.eps.socialranksys.diffusion.metrics.AbstractIndividualSimulationMetric;
import es.uam.eps.socialranksys.diffusion.simulation.Iteration;

import java.io.Serializable;

/**
 * Class that represents an individual metric that considers the existence of several features.
 * We differ two types of features:
 * 
 * <ul>
 *  <li><b>User parameters:</b> (Ex.: Communities) In this case, we take the values of the parameter
 *  for the creators of the received information pieces. </li>
 *  <li><b>Information parameters:</b> (Ex: hashtags) In this case, we take the values of the parameters
 *  for the different information pieces which are received and observed by each individual user.</li>
 * </ul>
 * 
 * @author Javier Sanz-Cruzado Puig
 * 
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public abstract class AbstractFeatureIndividualSimulationMetric<U extends Serializable, I extends Serializable, P> extends AbstractIndividualSimulationMetric<U,I,P>
{
    /**
     * Indicates if the parameter we are analyzing depends on the user (true) or the information piece (false).
     */
    private final boolean userParam;
    /**
     * Parameter name
     */
    private final String parameter;
    
    /**
     * Constructor
     * @param name the name of the metric.
     * @param userParam true if it uses the parameters of the users, false if it uses the parameters of the information pieces.
     * @param parameter the name of the parameter.
     */
    public AbstractFeatureIndividualSimulationMetric(String name, boolean userParam, String parameter) 
    {
        super(name);
        this.userParam = userParam;
        this.parameter = parameter;
    }

    /**
     * Indicates if we are using a user parameter (true) or an information piece parameter (false).
     * @return true if we use a user parameter, false if we use an information piece parameter.
     */
    protected boolean usesUserParam() 
    {
        return userParam;
    }

    /**
     * Obtains the name of the parameter we are using.
     * @return the name of the parameter.
     */
    protected String getParameter() 
    {
        return parameter;
    }
    
    @Override
    public void update(Iteration<U,I,P> iteration)
    {
        if(this.isInitialized())
        {
            if(this.usesUserParam())
                this.updateUserParam(iteration);
            else
                this.updateInfoParam(iteration);
        }
    }

    /**
     * Updates the necessary variables to compute a metric, in case the feature
     * values we are using belongs to the creators of the information pieces received
     * by the users in the network.
     * @param iteration the new iteration.
     */
    protected abstract void updateUserParam(Iteration<U, I, P> iteration);

    /**
     * Updates the necessary variables to compute a metric, in case the feature 
     * values we are using belong to the information pieces received by the users in
     * the network.
     * @param iteration the new iteration. 
     */
    protected abstract void updateInfoParam(Iteration<U, I, P> iteration);
    
    
    
    
}
