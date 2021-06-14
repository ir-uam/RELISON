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
 * Propagation mechanism for the so-called rumour spreading propagation mechanism. Following this strategy, each user
 * selects a user each iteration: he catches the information from such user, and shares with him the information he has.
 * It is a combination of the pull and push propagation mechanisms. A certain amount of time has to pass before a
 * neighbor is selected again.
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
public class PullPushStrategyPropagationMechanism<U extends Serializable,I extends Serializable,P> implements PropagationMechanism<U,I,P>
{
    /**
     * Number of iterations to wait until a profile can be revisited.
     */
    private final int waitTime;
    /**
     * Edge direction of the neighbors to ask for information.
     */
    private final EdgeOrientation orientation;
    /**
     * For each user, the list of users that user will propagate the information to.
     */
    private final Map<U, List<U>> propagationList;
    /**
     * The list of users in the last iterations.
     */
    private final Map<U, List<U>> lastIterations;
    
     /**
     * Constructor.
     * @param waitTime number of iterations to wait until a profile can be revisited.
     */
    public PullPushStrategyPropagationMechanism(int waitTime)
    {
        this(waitTime, EdgeOrientation.UND);
    }  
    
    /**
     * Constructor.
     * @param waitTime      number of iterations to wait until a profile can be revisited.
     * @param orientation   orientation for selecting the neighbors of the users.
     */
    public PullPushStrategyPropagationMechanism(int waitTime, EdgeOrientation orientation)
    {
        this.waitTime = waitTime;
        this.orientation = orientation;
        this.lastIterations = new HashMap<>();
        this.propagationList = new HashMap<>();
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
        propagationList.clear();

        data.getAllUsers().forEach((u)-> 
        {
            // We first obtain the set of neighbors of a given user:
            List<U> neighbours = data.getGraph().getNeighbourhood(u, orientation).collect(Collectors.toCollection(ArrayList::new));

            // Then, the users that were visited in previous iterations:
            if(!this.lastIterations.containsKey(u))
            {
                this.lastIterations.put(u, new ArrayList<>());
            }

            List<U> alreadyVisited = this.lastIterations.get(u);
            U neigh;
            
            neighbours.removeAll(alreadyVisited);

            // if we can choose a neighbor...
            if(neighbours.size() > 0)
            {
                int index = rng.nextInt(neighbours.size());
                neigh = neighbours.get(index);
            }
            else
            {
                neigh = null;
            }

            // Now, we update the propagation lists:
            if(neigh != null)
            {
                // We add u to the list of the neighbor
                if(!propagationList.containsKey(neigh))
                    propagationList.put(neigh, new ArrayList<>());
                if(!propagationList.get(neigh).contains(u))
                    propagationList.get(neigh).add(u);

                // We add the neighbor to the list of u.
                if(!propagationList.containsKey(u))
                    propagationList.put(u, new ArrayList<>());
                if(!propagationList.get(u).contains(neigh))
                    propagationList.get(u).add(neigh);

                alreadyVisited.add(neigh);
            }
            else
            {
                alreadyVisited.add(null);
            }
            
            // Prune the list
            if(alreadyVisited.size() > waitTime)
            {
                alreadyVisited.remove(0);
            }
        });
        
    }

    @Override
    public boolean dependsOnInformationPiece() 
    {
        return false;
    }
}
