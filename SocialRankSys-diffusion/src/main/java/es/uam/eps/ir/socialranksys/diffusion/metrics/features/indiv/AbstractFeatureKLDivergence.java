/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.metrics.features.indiv;

import es.uam.eps.ir.socialranksys.diffusion.metrics.features.AbstractFeatureIndividualSimulationMetric;
import es.uam.eps.ir.socialranksys.diffusion.simulation.Iteration;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * This individual metric computes the KL divergence of the priori distribution of the parameter
 * values over the whole set of information pieces, and the frequency of receival of
 * these parameters for a single user.
 * 
 * Depending on the nature of the parameter, we differ two cases:
 * 
 * <ul>
 *  <li><b>User parameters:</b>
 *      <ul>
 *          <li><i>Priori distribution:</i> The priori value for an individual user parameter
 *          is computed as the sum over the set of users as the product of the number of pieces published
 *          by the user and the value of the user parameter for the user.
 *          For example, let's suppose we have four different users, with two different communites:<br>
 * 
 *          User 1: Comm 1 (value 1), (i1,i2,i3), User 2: Comm 1 (value 2), (i4,i5), User 3: Comm 1 (value 2), (i6), User 4: Comm 2 (value 3), (i7,i8,i9,i10) <br>
 *  
 *          Then: Comm1 = 3*1 + 2*2 + 1*2 = 9; Comm2 = 3*4 = 12
 *          </li>
 *          <li><i>Simulation distribution of a user:</i> In this case, for each received and seen information
 *          piece, we sum the value of each parameter for the creator of the piece.
 *          </li>
 *      </ul>
 *  </li>
 *  <li><b>Information piece parameters:</b>
 *      <ul>
 *          <li><i>Priori distribution:</i>The priori value for an individual information piece
 *          parameter is computed as the sum over the set of information pieces of the value of the parameter
 *          for each parameter. For example, let's suppose we have four information pieces, and four different hashtags: <br>
 * 
 *          Piece 1: (h1,h1,h3), Piece 2:(h1,h2), Piece  3: (h3,h4), Piece 4: (h1,h2,h3,h3,h3) <br>
 * 
 *          Then: h1 = 2 + 1 + 0 + 1 = 4; h2 = 0 + 1 + 0 + 1 = 2; h3 = 1 + 0 + 1 + 3 = 5; h4 = 0 + 0 + 1 + 0 = 1
 *          </li>
 *          <li><i>Simulation distribution of a user:</i> In this case, for each received and seen information piece,
 *          we sum the value of each parameter for the information piece.
 *          </li>
 *      </ul>
 *  </li>
 * </ul>
 * 
 * In order to prevent divisions by zero, we apply an additive or Laplace smoothing over the distribution obtained from
 * the simulation. In case of the prior distribution had a zero value, then, it would mean that the parameter would not spread,
 * and, therefore, the probability of obtaining that value during simulation is 0 as well.
 * 
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 */
public abstract class AbstractFeatureKLDivergence<U extends Serializable,I extends Serializable,P> extends AbstractFeatureIndividualSimulationMetric<U,I,P>
{
    /**
     * Times each parameter has been received.
     */
    protected final Map<P,Map<U,Double>> qvalues;
    
    /**
     * The distribution we are trying to approximate.
     */
    protected final Map<P, Double> pvalues;
    
    /**
     * The total number of external parameters that have reached each user.
     */
    protected final Map<U, Double> sumQ;
    
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
     * @param name the name of the metric.
     * @param userparam true if we are using a user parameter, false if we are using an information piece parameter.
     * @param parameter the name of the parameter.
     * @param unique true if a piece of information is considered once, false if it is considered each time it appears.
     */
    public AbstractFeatureKLDivergence(String name, String parameter, boolean userparam, boolean unique) 
    {
        super(name + "-" + (unique ? "unique" : "repetitions"), userparam, parameter);
        this.qvalues = new HashMap<>();
        this.pvalues = new HashMap<>();
        this.sumQ = new HashMap<>();
        this.unique = unique;
    }
    
    @Override
    public double calculate() 
    {
        OptionalDouble opt = this.calculateIndividuals().values().stream().mapToDouble(x -> x).average();
        return opt.isPresent() ? opt.getAsDouble() : Double.NaN;
    }
    
    @Override
    protected void updateUserFeatures(Iteration<U,I,P> iteration)
    {
        if(iteration == null) return;
     
        iteration.getReceivingUsers().forEach(u ->
            iteration.getSeenInformation(u).forEach(i -> 
            {
                double val = (unique ? 1.0 : i.v2().size());

                data.getCreators(i.v1()).forEach(creator ->
                    data.getUserFeatures(creator, this.getParameter()).forEach(p ->
                    {
                        this.qvalues.get(p.v1).put(u, this.qvalues.get(p.v1).get(u) + p.v2*val);
                        this.sumQ.put(u, this.sumQ.get(u) + p.v2*val);
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
                        data.getUserFeatures(creator, this.getParameter()).forEach(p -> 
                        {
                            this.qvalues.get(p.v1).put(u, this.qvalues.get(p.v1).get(u) + p.v2*val);
                            this.sumQ.put(u, this.sumQ.get(u) + p.v2*val);
                        })
                    );
                })
            );
        }
    }
    
    @Override
    protected void updateInfoFeatures(Iteration<U,I,P> iteration)
    {
        if(iteration == null) return;
     
        iteration.getReceivingUsers().forEach(u ->
            iteration.getSeenInformation(u).forEach(i -> 
            {
                double val = (unique ? 1.0 : i.v2().size());
                
                data.getInfoPiecesFeatures(i.v1(), this.getParameter()).forEach(p ->
                {
                    this.qvalues.get(p.v1).put(u, this.qvalues.get(p.v1).get(u) + p.v2*val);
                    this.sumQ.put(u, this.sumQ.get(u) + p.v2*val);
                });
            })
        );
        
        if(!unique)
        {
            iteration.getReReceivingUsers().forEach(u ->
                iteration.getReReceivedInformation(u).forEach(i -> 
                {
                    double val = i.v2().size();
                    
                    data.getInfoPiecesFeatures(i.v1(), this.getParameter()).forEach(p -> 
                    {
                        this.qvalues.get(p.v1).put(u, this.qvalues.get(p.v1).get(u) + p.v2*val);
                        this.sumQ.put(u, this.sumQ.get(u) + p.v2*val);
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
        this.sumQ.clear();
        this.initialized = false;
    }
    
    
    @Override
    protected void initialize() 
    {
        if(!this.isInitialized())
        {
            this.pvalues.clear();
            this.sumP = 0.0;
            this.qvalues.clear();
            data.getAllFeatureValues(this.getParameter()).forEach(p -> 
            {
                Map<U,Double> map = new HashMap<>();
                data.getAllUsers().forEach(u -> map.put(u, 1.0));
                this.pvalues.put(p, 0.0);
                this.qvalues.put(p, map);
            });
            
            data.getAllUsers().forEach(u -> this.sumQ.put(u, this.data.numFeatureValues(this.getParameter()) + 0.0));
            
            if(this.usesUserParam())
            {
                this.data.getAllUsers().forEach(u -> 
                {
                    long numPieces = data.getPieces(u).count();
                    this.data.getUserFeatures(u, this.getParameter()).forEach(p -> 
                    {
                        this.pvalues.put(p.v1, this.pvalues.get(p.v1) + numPieces*p.v2 + 0.0);
                        this.sumP += numPieces*p.v2 + 0.0;
                    });
                });
            }
            else
            {
                this.data.getAllInformationPieces().forEach(i ->
                    this.data.getInfoPiecesFeatures(i, this.getParameter()).forEach(p -> 
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
