/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.propagation;

import es.uam.eps.ir.relison.diffusion.data.Data;
import es.uam.eps.ir.relison.diffusion.data.PropagatedInformation;
import es.uam.eps.ir.relison.diffusion.simulation.UserState;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Propagation mechanism that follows the pull strategy propagation mechanism. Each iteration, each user selects a single
 * neighbor in the network. It selects that neighbor among those who have not been visited in a certain amount of time.
 * Then, the selected neighbor is the origin of the information pieces which arrive to the user in the current iteration
 * (i.e. the user "pulls" the information from that user).
 *
 * <p>
 * <b>Reference:</b> A. Demers, D. Greene, C. Hauser, W. Irish, J. Larson. Epidemic algorithms for replicated database maintenance. ACM PODC 1987, pp. 1-12 (1987)
 * </p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 * @param <I> type of the information pieces
 * @param <P> type of the parameters.
 *
 */
public class PullStrategyPropagationMechanism<U extends Serializable,I extends Serializable,P> implements PropagationMechanism<U,I,P>
{
    /**
     * Number of iterations to wait until a profile can be revisited
     */
    private final int waitTime;
    /**
     * For each user, the list of users that user will propagate the information to.
     */
    private Map<U, List<U>> propagationList;
    /**
     * The list of users in the last iterations
     */
    private final Map<U, List<U>> lastIterations;
    /**
     * The orientation for selecting the neighbors.
     */
    private final EdgeOrientation orientation;
    /**
     * Constructor.
     * @param waitTime      number of iterations to wait until a profile can be revisited.
     * @param orientation   the orientation for selecting the neighbors.
     */
    public PullStrategyPropagationMechanism(int waitTime, EdgeOrientation orientation)
    {
        this.waitTime = waitTime;
        this.lastIterations = new HashMap<>();
        this.orientation = orientation;
    }

    @Override
    public Stream<U> getUsersToPropagate(PropagatedInformation information, UserState<U> originUser, Data<U, I, P> data)
    {
        if(propagationList.containsKey(originUser.getUserId()))
            return propagationList.get(originUser.getUserId()).stream();
        return Stream.empty();
    }
    
    @Override
    public void resetSelections(Data<U,I,P> data)
    {
        Random rng = new Random();
        propagationList = new HashMap<>();

        // For each user in the network:
        data.getAllUsers().forEach((u)->
        {
            // We first select the set of neighbors.
            List<U> neighbours = data.getGraph().getNeighbourhood(u, orientation).collect(Collectors.toCollection(ArrayList::new));
            List<U> alreadyVisited = lastIterations.containsKey(u) ? lastIterations.get(u) : new ArrayList<>();
            U neigh;

            // Select the neighbour
            // First, we get which neighbors we can choose from:
            List<U> actualNeighs = new ArrayList<>();
            for(U v : neighbours)
            {
                if(!alreadyVisited.contains(v)) actualNeighs.add(v);
            }

            if(actualNeighs.size() > 0) // if there are enough neighbors to choose from:
            {
                int index = rng.nextInt(neighbours.size());
                neigh = neighbours.get(index);
            }
            else // otherwise, we do not choose a neighbor, and we exit the loop:
            {
                neigh = null;
            }

            // If the neighbor is selected, we add it to the list of sources, and we add u as destination.
            if(neigh != null)
            {
                if(!propagationList.containsKey(neigh))
                    propagationList.put(neigh, new ArrayList<>());
                propagationList.get(neigh).add(u);
                alreadyVisited.add(neigh);
            }
            else // Otherwise, we just advance the time.
            {
                alreadyVisited.add(null);
            }

            if(alreadyVisited.size() > this.waitTime)
            {
                alreadyVisited.remove(0);
            }

            if(!lastIterations.containsKey(u))
            {
                lastIterations.put(u, alreadyVisited);
            }
        });
        
    }

    @Override
    public boolean dependsOnInformationPiece()
    {
        return false;
    }
}
