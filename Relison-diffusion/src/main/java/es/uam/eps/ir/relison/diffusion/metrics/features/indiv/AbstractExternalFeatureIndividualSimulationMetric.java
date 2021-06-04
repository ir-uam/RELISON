/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.metrics.features.indiv;

import es.uam.eps.ir.relison.diffusion.metrics.features.AbstractFeatureIndividualSimulationMetric;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Abstract class representing individual feature-based metrics which do not take into account features that the user already knows
 * (with already knows meaning that the user has the feature, in case of user features, or the user has an information piece containing
 * the feature, in case of information features).
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the user / information pieces features.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class AbstractExternalFeatureIndividualSimulationMetric<U extends Serializable,I extends Serializable,P> extends AbstractFeatureIndividualSimulationMetric<U,I,P>
{
    /**
     * The set of own parameters for each user.
     */
    private final Map<U, Set<P>> ownFeats;

    /**
     * Constructor.
     * @param name Name of the metric.
     * @param parameter Name of the metric parameter.
     * @param userparam True if the parameter is a user parameter, false if it is not.
     */
    public AbstractExternalFeatureIndividualSimulationMetric(String name, String parameter, boolean userparam)
    {
        super(name, userparam, parameter);
        this.ownFeats = new HashMap<>();
    }

    /**
     * Obtains the map identifying the features of all users.
     * @return the features of all users.
     */
    protected Map<U, Set<P>> getOwnFeats()
    {
        return this.ownFeats;
    }

    /**
     * Obtains the features that an individual user already knows.
     * @param u the user.
     * @return the set of features the user already knows. If the user does not exist, an empty set is returned.
     */
    protected Set<P> getOwnFeats(U u)
    {
        return this.ownFeats.getOrDefault(u, new HashSet<>());
    }

    /**
     * Computes and stores the own features for every user in the network.
     */
    protected void computeOwnFeatures()
    {
        this.data.getAllUsers().forEach(u ->
        {
            Set<P> userParams = this.computeOwnFeatures(u);
            this.ownFeats.put(u, userParams);
        });
    }

    /**
     * Computes the features for a user
     * @param u the user.
     * @return the feature set for the user
     */
    protected Set<P> computeOwnFeatures(U u)
    {
        if(this.usesUserParam())
        {
            return this.computeOwnUserFeatures(u);
        }
        else
        {
            return this.computeOwnInfoFeatures(u);
        }
    }

    /**
     * Computes the user features for an individual user.
     * @param u the user.
     * @return the feature set.
     */
    protected Set<P> computeOwnUserFeatures(U u)
    {
        Set<P> parameters = new HashSet<>();

        data.getUserFeatures(u, this.getParameter()).forEach(p -> parameters.add(p.v1));

        return parameters;
    }

    /**
     * Computes information piece features for an individual user.
     * @param u the user.
     * @return the feature set.
     */
    protected Set<P> computeOwnInfoFeatures(U u)
    {
        Set<P> parameters = new HashSet<>();

        data.getPieces(u).forEach(i ->
            data.getInfoPiecesFeatures(i, this.getParameter()).forEach(p ->
                parameters.add(p.v1)
            )
        );

        return parameters;
    }

    protected void clearOwnFeatures()
    {
        this.ownFeats.clear();
    }

    /**
     * Adds features for an individual user.
     * @param u         the user.
     * @param features  the features.
     */
    protected void setOwnFeatures(U u, Set<P> features)
    {
        this.ownFeats.put(u, features);
    }
}
