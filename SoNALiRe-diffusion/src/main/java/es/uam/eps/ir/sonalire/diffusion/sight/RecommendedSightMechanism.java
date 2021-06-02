/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.sight;

import es.uam.eps.ir.sonalire.diffusion.data.Data;
import es.uam.eps.ir.sonalire.diffusion.data.PropagatedInformation;
import es.uam.eps.ir.sonalire.diffusion.simulation.SimulationEdgeTypes;
import es.uam.eps.ir.sonalire.diffusion.simulation.UserState;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;

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
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 */
public class RecommendedSightMechanism<U extends Serializable,I extends Serializable,P> extends IndividualSightMechanism<U,I,P>
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
     * Orientation for indicating whih neighbors of the user propagate the information.
     */
    private final EdgeOrientation orientation;
    /**
     * Constructor. 
     * @param probRec   probability of observing a piece of information that comes from a recommended user.
     * @param probTrain probability of observing a piece of information that comes from a training user.
     * @param orient    orientation for indicating which neighbors of the user propagate the information.
     */
    public RecommendedSightMechanism(double probRec, double probTrain, EdgeOrientation orient)
    {
        this.neighbors = new HashMap<>();
        this.probRec = probRec;
        this.probTrain = probTrain;
        this.rng = new Random();
        this.orientation = orient;
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
                Set<U> set = new HashSet<>();
                graph.getNeighbourhood(u, orientation).filter(v ->
                {
                    if(orientation == EdgeOrientation.IN)
                        return graph.getEdgeType(v, u) == SimulationEdgeTypes.RECOMMEND;
                    else if(orientation == EdgeOrientation.OUT)
                        return graph.getEdgeType(u, v) == SimulationEdgeTypes.RECOMMEND;
                    else
                        return (graph.containsEdge(u,v) && graph.getEdgeType(u,v) == SimulationEdgeTypes.RECOMMEND) ||
                               (graph.containsEdge(v,u) && graph.getEdgeType(v,u) == SimulationEdgeTypes.RECOMMEND);
                }).forEach(set::add);
                this.neighbors.put(u, set);
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
