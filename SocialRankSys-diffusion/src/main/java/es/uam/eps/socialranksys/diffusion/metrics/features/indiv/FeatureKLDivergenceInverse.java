/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.metrics.features.indiv;

import es.uam.eps.ir.socialranksys.utils.indexes.KLDivergence;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This individual metric computes the number of bytes of information we expect to lose
 * if we approximate the observed distribution of the parameters received by the user
 * with their prior distribution (i.e. how many times have they appeared over the 
 * different information pieces).
 * 
 * We apply a Laplace smoothing to prevent divisions by zero in both distributions.
 * 
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 */
public class FeatureKLDivergenceInverse<U extends Serializable,I extends Serializable,P> extends AbstractFeatureKLDivergence<U,I,P> 
{
    /**
     * Name fixed value
     */
    private final static String ENTROPY = "feat-inv-kld";

    /**
     * Constructor.
     * @param userparam true if we are using a user parameter, false if we are using an information piece parameter.
     * @param parameter the name of the parameter.
     * @param unique true if a piece of information is considered once, false if it is considered each time it appears.
     */
    public FeatureKLDivergenceInverse(String parameter, boolean userparam, boolean unique) 
    {
        super(ENTROPY + "-" + (userparam ? "user" : "info") + "-" + parameter, parameter, userparam, unique);
    }
       
    @Override
    public double calculate(U user) 
    {
        if(!this.isInitialized())
            return Double.NaN;
        
        KLDivergence kldiv = new KLDivergence();
        List<P> features = this.data.getAllFeatureValues(this.getParameter()).collect(Collectors.toCollection(ArrayList::new));
        
        Stream<Double> pdistr = features.stream().map(this.pvalues::get);
        Stream<Double> qdistr = features.stream().map(p -> this.qvalues.get(p).get(user));
        
        return kldiv.compute(qdistr, pdistr, this.sumQ.get(user), this.sumP);
    }
}
