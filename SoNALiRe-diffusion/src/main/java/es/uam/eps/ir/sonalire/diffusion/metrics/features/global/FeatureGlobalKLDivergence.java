/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.metrics.features.global;

import es.uam.eps.ir.sonalire.utils.indexes.KLDivergence;

import java.io.Serializable;
import java.util.stream.Stream;


/**
 * This global metric computes the number of bytes of information we expect to lose
 * if we approximate the real distribution of features with the estimated distribution
 * obtained from simulating. It uses KL Divergence for that.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user / information pieces features.
 */
public class FeatureGlobalKLDivergence<U extends Serializable,I extends Serializable, F> extends AbstractFeatureGlobalKLDivergence<U,I, F>
{
    /**
     * Name fixed value.
     */
    private final static String ENTROPY = "feat-gl-kld";
    
    /**
     * Constructor.
     * @param userFeat true if we are using a user feature, false if we are using an information piece feature.
     * @param feature the name of the feature.
     * @param unique true if a piece of information is considered once, false if it is considered each time it appears.
     */
    public FeatureGlobalKLDivergence(String feature, boolean userFeat, boolean unique)
    {
        super(ENTROPY + "-" + (userFeat ? "user" : "info") + "-" + feature, feature, userFeat, unique);
    }

    @Override
    public double calculate() 
    {
        if(!this.isInitialized())
            return Double.NaN;
        
        KLDivergence kldiv = new KLDivergence();
        Stream<Double> pdistr = this.data.getAllFeatureValues(this.getFeature()).map(this.pvalues::get);
        Stream<Double> qdistr = this.data.getAllFeatureValues(this.getFeature()).map(this.qvalues::get);
        
        return kldiv.compute(pdistr, qdistr, this.sumP, this.sumQ);
    }
}
