/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.sight;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;
import es.uam.eps.ir.socialranksys.diffusion.data.PropagatedInformation;
import es.uam.eps.ir.socialranksys.diffusion.simulation.SimulationEdgeTypes;
import es.uam.eps.ir.socialranksys.diffusion.simulation.UserState;
import es.uam.eps.ir.socialranksys.graph.Graph;

import java.io.Serializable;
import java.util.*;

/**
 * This mechanism applies two different probabilities: one for observing information
 * pieces from recommended users, and other for observing information pieces from
 * training users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class RecommendedSightMechanism<U extends Serializable,I extends Serializable,P> implements SightMechanism<U,I,P>
{

    /**
     * For each user, the set of users they are neighbors to see information from.
     */
    private final Map<U, Set<U>> neighbors;
    /**
     * Indicates if the selections have been initialized or not.
     */
    private boolean initialized = false;
    /**
     * Probability of observing a piece of information that comes from a recommended user.
     */
    private final double probRec;
    /**
     * Probability of observing a piece of information that comes from a training user.
     */
    private final double probTrain;
    /**
     * Random number generator
     */
    private final Random rng;
    
    /**
     * Constructor. 
     * @param probRec probability of observing a piece of information that comes from a recommended user.
     * @param probTrain probability of observing a piece of information that comes from a training user.
     */
    public RecommendedSightMechanism(double probRec, double probTrain)
    {
        this.neighbors = new HashMap<>();
        this.probRec = probRec;
        this.probTrain = probTrain;
        this.rng = new Random();
    }
    
    @Override
    public void resetSelections(Data<U,I,P> data)
    {
        // Store the information for speeding up the simulation
        if(!this.initialized)
        {
            Graph<U> graph = data.getGraph();
            graph.getAllNodes().forEach(u ->
            {
                this.neighbors.put(u, new HashSet<>());
                graph.getAdjacentNodes(u).forEach(v ->
                {
                    int edgeType = graph.getEdgeType(u, v);
                    if(edgeType == SimulationEdgeTypes.RECOMMEND)
                        this.neighbors.get(u).add(v);
                });
            });
            this.initialized = true;
        }
    }
    
    @Override
    public boolean seesInformation(UserState<U> user, Data<U,I,P> data, PropagatedInformation prop)
    {
        boolean propagate = prop.getCreators().stream().map(creator -> 
        {
            double rnd = rng.nextDouble();
            U cr = data.getUserIndex().idx2object(creator);
            if(this.neighbors.get(user.getUserId()).contains(cr))
            {
                return rnd < probRec;
            }
            else
            {
                return rnd < probTrain;
            }
        }).reduce(false, (x,y) -> x || y);
            
        return propagate && !user.containsPropagatedInformation(prop.getInfoId());
    }
}
