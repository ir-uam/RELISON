/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.metrics.user.individual;

import es.uam.eps.ir.socialranksys.diffusion.metrics.AbstractIndividualSimulationMetric;
import es.uam.eps.ir.socialranksys.diffusion.simulation.Iteration;

import java.io.Serializable;
import java.util.*;

/**
 * For each user in the network, the user-recall metric the proportion of users
 * in the network which have published at least an information piece that has 
 * been read by a user (all these users have to be different from the receiver).
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 */
public class UserRecall<U extends Serializable,I extends Serializable,P> extends AbstractIndividualSimulationMetric<U,I,P>
{
    /**
     * Name fixed value.
     */
    private final static String RECALL = "user-recall";
    
    /**
     * The different "retrieved" users.
     */
    private final Map<U, Set<U>> retrieved;
    /**
     * Constructor.
     */
    public UserRecall() 
    {
        super(RECALL);
        this.retrieved = new HashMap<>();
    }

    @Override
    public double calculate(U user) 
    {
        if(!this.isInitialized() || !data.containsUser(user))
            return Double.NaN;
        else if(data.numUsers() > 1)
            return (retrieved.get(user).size() + 0.0)/ (data.numUsers() - 1.0);
        else
            return 0.0;
    }

    @Override
    public double calculate() 
    {
        OptionalDouble opt = this.calculateIndividuals().values().stream().mapToDouble(x -> x).average();
        return opt.isPresent() ? opt.getAsDouble() : Double.NaN;
    }

    @Override
    public void update(Iteration<U, I, P> iteration)
    {
        iteration.getReceivingUsers().forEach(u ->
            iteration.getSeenInformation(u).forEach(i ->
                data.getCreators(i.v1()).forEach(v ->
                    this.retrieved.get(u).add(v)
                )
            )
        );
    }

    @Override
    public void clear() 
    {
        this.retrieved.clear();
        this.initialized = false;
    }
    
    @Override
    protected void initialize() 
    {
        if(!this.isInitialized() && data != null)
        {
            this.retrieved.clear();
            data.getAllUsers().forEach(u -> this.retrieved.put(u, new HashSet<>()));
            this.initialized = true;
        }
    }
}
