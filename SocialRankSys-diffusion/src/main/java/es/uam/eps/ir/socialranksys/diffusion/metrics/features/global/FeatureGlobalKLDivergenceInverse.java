/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.metrics.features.global;

import es.uam.eps.ir.socialranksys.utils.indexes.KLDivergence;

import java.io.Serializable;
import java.util.stream.Stream;

/**
 * This global metric computes the number of bytes of information we expect to lose
 * if we approximate the observed distribution of the parameters with their prior distribution
 * (i.e. how many times have they appeared over the different information pieces).
 * 
 * We apply a Laplace smoothing to prevent divisions by zero in both distributions.
 * 
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 */
public class FeatureGlobalKLDivergenceInverse<U extends Serializable,I extends Serializable,P> extends AbstractFeatureGlobalKLDivergence<U,I,P> 
{
    /**
     * Name fixed value.
     */
    private final static String ENTROPY = "feat-gl-inv-kld";

    /**
     * Constructor.
     * @param userFeat true if we are using a user feature, false if we are using an information piece feature.
     * @param feature the name of the feature.
     * @param unique true if a piece of information is considered once, false if it is considered each time it appears.
     */
    public FeatureGlobalKLDivergenceInverse(String feature, boolean userFeat, boolean unique)
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
        
        return kldiv.compute(qdistr, pdistr, this.sumQ, this.sumP);
    }
}
