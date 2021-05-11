/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.metrics.features.global;

import es.uam.eps.ir.socialranksys.diffusion.metrics.features.AbstractFeatureGlobalSimulationMetric;
import es.uam.eps.ir.socialranksys.diffusion.simulation.Iteration;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This global metric computes the KL divergence of the priori distribution of the feature values over the whole set of information pieces,
 * and the frequency of receival of these parameters for the set of users.
 * 
 * Depending on the nature of the parameter, we differ two cases:
 * 
 * <ul>
 *  <li><b>User parameters:</b>
 *      <ul>
 *          <li><i>Priori distribution:</i> The priori value for an individual user feature is computed as the sum over
 *          the set of users of the product of the number of pieces published by the user and the value of the
 *          user feature for the user.
 *          For example, let's suppose we have four different users, with two different communites:<br/>
 * 
 *          User 1: Comm 1 (value 1), (i1,i2,i3), User 2: Comm 1 (value 2), (i4,i5), User 3: Comm 1 (value 2), (i6), User 4: Comm 2 (value 3), (i7,i8,i9,i10) <br/>
 *  
 *          Then: Comm1 = 3*1 + 2*2 + 1*2 = 9; Comm2 = 3*4 = 12
 *          </li>
 *          <li><i>Simulation distribution:</i> In this case, for each received and seen information piece (by any user),
 *          we sum the value of each feature for the creator of the piece.
 *          </li>
 *      </ul>
 *  </li>
 *  <li><b>Information piece parameters:</b>
 *      <ul>
 *          <li><i>Priori distribution:</i>The priori value for an individual information piece
 *          feature is computed as the sum over the set of information pieces of the value of the feature
 *          for each feature. For example, let's suppose we have four information pieces, and four different hashtags: <br>
 * 
 *          Piece 1: (h1,h1,h3), Piece 2:(h1,h2), Piece  3: (h3,h4), Piece 4: (h1,h2,h3,h3,h3) <br>
 * 
 *          Then: h1 = 2 + 1 + 0 + 1 = 4; h2 = 0 + 1 + 0 + 1 = 2; h3 = 1 + 0 + 1 + 3 = 5; h4 = 0 + 0 + 1 + 0 = 1
 *          </li>
 *          <li><i>Simulation distribution of a user:</i> In this case, for each received and seen information piece by any user,
 *          we sum the value of each feature for the information piece.
 *          </li>
 *      </ul>
 *  </li>
 * </ul>
 * 
 * In order to prevent divisions by zero, we apply an additive or Laplace smoothing over the
 * different distributions. Doing so, the KL Divergence is set to be a real number (never
 * NaN or Infinity).
 * 
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user / information pieces features.
 */
public abstract class AbstractFeatureGlobalKLDivergence<U extends Serializable,I extends Serializable, F> extends AbstractFeatureGlobalSimulationMetric<U,I, F>
{
    /**
     * Times each parameter has been received.
     */
    protected final Map<F,Double> qvalues;
    
    /**
     * The distribution we are trying to approximate.
     */
    protected final Map<F, Double> pvalues;
    
    /**
     * The total number of features that have reached the different users.
     */
    protected double sumQ;
    
    /**
     * The sum of the values we are trying to approximate
     */
    protected double sumP;

    /**
     * Indicates if a piece of information is considered once (or each time it appears if false).
     */
    private final boolean unique;
    
    /**
     * Constructor.
     * @param name      the name of the metric.
     * @param userFeat  true if we are using a user feature, false if we are using an information piece feature.
     * @param feature   the name of the feature.
     * @param unique    true if a piece of information is considered once, false if it is considered each time it appears.
     */
    public AbstractFeatureGlobalKLDivergence(String name, String feature, boolean userFeat, boolean unique)
    {
        super(name + "-" + (unique ? "unique" : "repetitions"), userFeat, feature);
        this.qvalues = new HashMap<>();
        this.pvalues = new HashMap<>();
        this.unique = unique;
    }
    
    @Override
    protected void updateUserFeature(Iteration<U,I, F> iteration)
    {
        if(iteration == null) return;
     
        iteration.getReceivingUsers().forEach(u ->
            iteration.getSeenInformation(u).forEach(i -> 
            {
                double val = (unique ? 1.0 : i.v2().size());
                data.getCreators(i.v1()).forEach(creator ->
                    data.getUserFeatures(creator, this.getFeature()).forEach(p ->
                    {
                        this.qvalues.put(p.v1, this.qvalues.get(p.v1) + p.v2*val);
                        this.sumQ += p.v2*val;
                    })
                );
            })
        );
        
        if(!unique)
        {
            iteration.getReReceivingUsers().forEach(u -> 

                iteration.getReReceivedInformation(u).forEach(i -> 
                {
                    double val = i.v2().size();
                    data.getCreators(i.v1()).forEach(creator -> 

                        data.getUserFeatures(creator, this.getFeature()).forEach(p ->
                        {
                            this.qvalues.put(p.v1, this.qvalues.get(p.v1) + p.v2*val);
                            this.sumQ += p.v2*val;
                        })
                    );
                })
            );
        }
    }
    
    @Override
    protected void updateInfoFeature(Iteration<U,I, F> iteration)
    {
        if(iteration == null) return;
     
        iteration.getReceivingUsers().forEach(u -> 

            iteration.getSeenInformation(u).forEach(i -> 
            {
                double val = (unique ? 1.0 : i.v2().size());

                data.getInfoPiecesFeatures(i.v1(), this.getFeature()).forEach(p ->
                {
                    this.qvalues.put(p.v1, this.qvalues.get(p.v1) + p.v2*val);
                    this.sumQ += p.v2*val;
                });
            })
        );
        
        if(!unique)
        {
            iteration.getReReceivingUsers().forEach(u -> 

                iteration.getReReceivedInformation(u).forEach(i -> 
                {
                    double val = i.v2().size();

                    data.getInfoPiecesFeatures(i.v1(), this.getFeature()).forEach(p ->
                    {
                        this.qvalues.put(p.v1, this.qvalues.get(p.v1) + p.v2*val);
                        this.sumQ += p.v2*val;
                    });
                })
            );
        }
    }
    
    @Override
    public void clear() 
    {
        this.pvalues.clear();
        this.qvalues.clear();
        this.sumP = 0.0;
        this.sumQ = 0.0;
        this.initialized = false;
    }
    
    
    @Override
    protected void initialize() 
    {
        if(!this.isInitialized())
        {
            this.pvalues.clear();
            this.qvalues.clear();

            int aux = data.numFeatureValues(this.getFeature());
            
            /* In the Laplace smoothing, we add 1.0 to each element in the 
             * distribution. As a consequence, the sum increases in the number
             * of different elements.
             */
            this.sumP = 0.0;
            this.sumQ = aux + 0.0;
            
            data.getAllFeatureValues(this.getFeature()).forEach(f ->
            {
                // We initialize values at 1.0 for applying the Laplace smoothing
                this.pvalues.put(f, 0.0);
                this.qvalues.put(f, 1.0);
            });
            
            
            
            if(this.usesUserFeatures())
            {
                this.data.getAllUsers().forEach(u -> 
                {
                    long numPieces = data.getPieces(u).count();
                    this.data.getUserFeatures(u, this.getFeature()).forEach(p ->
                    {
                        this.pvalues.put(p.v1, this.pvalues.get(p.v1) + numPieces*p.v2 + 0.0);
                        this.sumP += numPieces*p.v2 + 0.0;
                    });
                });
            }
            else
            {
                this.data.getAllInformationPieces().forEach(i ->
                    this.data.getInfoPiecesFeatures(i, this.getFeature()).forEach(p ->
                    {
                        this.pvalues.put(p.v1, this.pvalues.get(p.v1) + p.v2);
                        this.sumP += p.v2;
                    })
                );
            }
            
            this.initialized = true;
        }
    }

    
    
}
