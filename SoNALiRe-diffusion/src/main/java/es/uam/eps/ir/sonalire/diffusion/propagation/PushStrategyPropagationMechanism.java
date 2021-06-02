/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.propagation;

import es.uam.eps.ir.sonalire.diffusion.data.Data;
import es.uam.eps.ir.sonalire.diffusion.data.PropagatedInformation;
import es.uam.eps.ir.sonalire.diffusion.simulation.UserState;
import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Propagation mechanism that follows the push strategy propagation mechanism. Each iteration, each user selects a single
 * neighbor in the network. It selects that neighbor among those who have not been visited in a certain amount of time.
 * Then, the selected neighbor is the destination of the information pieces in the current iteration (i.e. the user
 * "pushes" the information to that user).
 *
 * <p>
 *      <b>Reference:</b> A. Demers, D. Greene, C. Hauser, W. Irish, J. Larson. Epidemic algorithms for replicated database maintenance. ACM PODC 1987, pp. 1-12 (1987)
 * </p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 *
 */
public class PushStrategyPropagationMechanism<U extends Serializable,I extends Serializable,P> implements PropagationMechanism<U,I,P>
{
    /**
     * Number of iterations to wait until a profile can be revisited.
     */
    private final int waitTime;
    /**
     * For each user, the list of users that user will propagate the information to.
     */
    private Map<U, List<U>> propagationList;
    /**
     * The list of users in the last iterations.
     */
    private final Map<U, List<U>> lastIterations;
    /**
     * The orientation for selecting the neighbors.
     */
    private final EdgeOrientation orientation;
    /**
     * Constructor.
     * @param waitTime number of iterations to wait until a profile can be revisited.
     */
    public PushStrategyPropagationMechanism(int waitTime, EdgeOrientation orientation)
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
        data.getAllUsers().forEach((u)->
        {
            // We first get the list of neighbors of the user:
            List<U> neighbours = data.getGraph().getNeighbourhood(u, orientation).collect(Collectors.toCollection(ArrayList::new));

            // We obtain the set of users who the user has already visited:
            List<U> alreadyVisited = lastIterations.containsKey(u) ? lastIterations.get(u) : new ArrayList<>();
            U neigh;

            // We select a neighbor to propagate the information to.

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

            // If we have chosen a neighbor:
            if(neigh != null)
            {
                propagationList.put(u, new ArrayList<>());
                propagationList.get(u).add(neigh);
                alreadyVisited.add(neigh);
            }
            else
            {
                // In order to keep the algorithm working, we add a void user to the already visited list
                // when the neighbor does not exist:
                alreadyVisited.add(null);
            }

            if(alreadyVisited.size() > this.waitTime)
            {
                // We remove the first item.
                alreadyVisited.remove(0);
            }

            // We add this for user u.
            if(!this.lastIterations.containsKey(u))
            {
                alreadyVisited.add(u);
            }
        });
        
    }

    @Override
    public boolean dependsOnInformationPiece() 
    {
        return false;
    }
}
