/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.sight;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.socialranksys.diffusion.data.Data;
import es.uam.eps.socialranksys.diffusion.data.PropagatedInformation;
import es.uam.eps.socialranksys.diffusion.simulation.SimulationEdgeTypes;
import es.uam.eps.socialranksys.diffusion.simulation.UserState;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Sees the pieces of information that come from recommended users.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 * @param <I> Type of the information pieces
 * @param <P> Type of the parameters
 */
public class AllRecommendedSightMechanism<U extends Serializable,I extends Serializable,P> implements SightMechanism<U,I,P>
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
     * Constructor. 
     */
    public AllRecommendedSightMechanism()
    {
        this.allowed = new HashMap<>();
    }
    
    /*@Override
    public Stream<PropagatedInformation> seeInformation(UserState<U> user, Data<U, I, P> data)
    {
        // Filter the data so that only information from recommended users is seen.
        return user.getNewInformation().map(info -> 
        {
            U u = user.getUserId();
            Set<Integer> count = info.getCreators()
                                     .stream()
                                     .filter(cidx -> this.allowed.get(u).contains(data.getUserIndex().idx2object(cidx)))
                                     .collect(Collectors.toCollection(HashSet::new));
            return new PropagatedInformation(info.getInfoId(), info.getTimestamp(), count);
        }).filter(info -> info.getCreators().size() > 0);
    }*/
    
    @Override
    public void resetSelections(Data<U,I,P> data)
    {
        // Store the information for speeding up the simulation
        if(!this.initialized)
        {
            Graph<U> graph = data.getGraph();
            graph.getAllNodes().forEach(u -> {
                this.allowed.put(u, new HashSet<>());
                graph.getAdjacentNodes(u).forEach(v -> {
                    int edgeType = graph.getEdgeType(u, v);
                    if(edgeType == SimulationEdgeTypes.RECOMMEND)
                        this.allowed.get(u).add(v);
                });
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
