/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.expiration;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;
import es.uam.eps.ir.socialranksys.diffusion.data.Information;
import es.uam.eps.ir.socialranksys.diffusion.simulation.UserState;

import java.io.Serializable;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Expiration mechanism that increases exponentially the probability of discarding 
 * an information piece as the number of iterations since its creation increases.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @param <U> type of the users
 * @param <I> type of the information
 * @param <P> type of the parameters
 */
public class ExponentialDecayExpirationMechanism<U extends Serializable,I extends Serializable,P> implements ExpirationMechanism<U,I,P>
{
    /**
     * Time before expiration
     */
    private final double decay;
    /**
     * Random number generator.
     */
    private final Random rng;
    /**
     * Constructor.
     * @param halfLife the time required for the piece to have p = 0.5 of being removed
     */
    public ExponentialDecayExpirationMechanism(double halfLife)
    {
        this.rng = new Random();
        
        this.decay = halfLife >= 0.0 ? Math.log(2.0)/halfLife : Double.POSITIVE_INFINITY;
    }
    
    @Override
    public Stream<Integer> expire(UserState<U> user, Data<U,I,P> data, int numIter, Long timestamp)
    {
        return user.getReceivedInformation().filter(piece -> 
        {
            long time = numIter - piece.getTimestamp();
            double rnd = rng.nextDouble();
            return this.expdecay(time) < rnd;
        }).map(Information::getInfoId);
    }
    
    /**
     * Computes the probability that the information piece stays in the received list.
     * @param time the difference between creation time and current time.
     * @return the probability that the information piece stays in the received list.
     */
    private double expdecay(long time)
    {
        return Math.exp(-this.decay*time);
    }
    
}