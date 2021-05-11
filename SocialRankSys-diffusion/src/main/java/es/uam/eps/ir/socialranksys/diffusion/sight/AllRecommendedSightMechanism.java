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
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Sees the pieces of information that come from recommended users and the user has not previously propagated.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 * @param <I> type of the information pieces
 * @param <P> type of the parameters
 */
public class AllRecommendedSightMechanism<U extends Serializable,I extends Serializable,P> extends IndividualSightMechanism<U,I,P>
{

    /**
     * For each user, the set of users they are allowed to see information from
     */
    private final Map<U, Set<U>> allowed;
    /**
     * Indicates if the selections have been initialized or not.
     */
    private boolean initialized = false;

    /**
     * Orientation for indicating whih neighbors of the user propagate the information.
     */
    private final EdgeOrientation orientation;

    /**
     * Constructor.
     * @param orient    orientation for indicating which neighbors of the user propagate the information.
     */
    public AllRecommendedSightMechanism(EdgeOrientation orient)
    {
        this.allowed = new HashMap<>();
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
                this.allowed.put(u, set);
            });
            this.initialized = true;
        }
    }
    
    @Override
    public boolean seesInformation(UserState<U> user, Data<U,I,P> data, PropagatedInformation prop)
    {
        U u = user.getUserId();
        long count = prop.getCreators()
            .stream()
            .filter(cidx -> this.allowed.get(u).contains(data.getUserIndex().idx2object(cidx)))
            .count();
        
        return (count > 0) && !user.containsPropagatedInformation(prop.getInfoId());
    }
    
}
