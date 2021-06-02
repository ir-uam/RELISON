/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.metrics.features.global;

import es.uam.eps.ir.sonalire.diffusion.metrics.features.AbstractFeatureGlobalSimulationMetric;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Abstract class for representing global feature-based metrics which consider those features that the user already knows.
 * Depending on the nature of the feature, we consider that a user already knows a feature when:
 * <ul>
 *     <li>The user has the feature, in the case of user features.</li>
 *     <li>The user has created an information piece containing the feature, in the case of information pieces features.</li>
 * </ul>
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the user / information pieces features.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class AbstractExternalFeatureGlobalSimulationMetric<U extends Serializable,I extends Serializable,P> extends AbstractFeatureGlobalSimulationMetric<U,I,P>
{
    /**
     * The set of own features for each user.
     */
    private final Map<U, Set<P>> ownFeatures;

    /**
     * Constructor.
     * @param name      the name of the metric.
     * @param feature   the name of the feature field to consider.
     * @param userFeat true if the feature is a user feature, false otherwise.
     */
    public AbstractExternalFeatureGlobalSimulationMetric(String name, String feature, boolean userFeat)
    {
        super(name, userFeat, feature);
        this.ownFeatures = new HashMap<>();
    }

    /**
     * Obtains the map identifying the parameters of all users.
     * @return the parameters of all users.
     */
    protected Map<U, Set<P>> getOwnFeatures()
    {
        return this.ownFeatures;
    }

    /**
     * Obtains the features that an individual user already knows.
     * @param u the user.
     * @return the set of features the user already knows. If the user does not exist, an empty set is returned.
     */
    protected Set<P> getOwnFeatures(U u)
    {
        return this.ownFeatures.getOrDefault(u, new HashSet<>());
    }

    /**
     * Computes and stores the own features for every user in the network.
     */
    protected void computeOwnFeatures()
    {
        this.data.getAllUsers().forEach(u ->
        {
            Set<P> userParams = this.computeOwnFeatures(u);
            this.ownFeatures.put(u, userParams);
        });
    }

    /**
     * Computes the features for a user
     * @param u the user.
     * @return the feature set for the user
     */
    protected Set<P> computeOwnFeatures(U u)
    {
        if(this.usesUserFeatures())
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
        Set<P> features = new HashSet<>();

        data.getUserFeatures(u, this.getFeature()).forEach(p -> features.add(p.v1));

        return features;
    }

    /**
     * Computes information piece features for an individual user.
     * @param u the user.
     * @return the feature set.
     */
    protected Set<P> computeOwnInfoFeatures(U u)
    {
        Set<P> features = new HashSet<>();

        data.getPieces(u).forEach(i ->
            data.getInfoPiecesFeatures(i, this.getFeature()).forEach(p ->
                features.add(p.v1)
            )
        );

        return features;
    }

    /**
     * Clears the set of features of all users.
     */
    protected void clearOwnFeatures()
    {
        this.ownFeatures.clear();
    }

    /**
     * Adds params for an individual user.
     * @param u         the user.
     * @param features  the features.
     */
    protected void setOwnFeatures(U u, Set<P> features)
    {
        this.ownFeatures.put(u, features);
    }
}
