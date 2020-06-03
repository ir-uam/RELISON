/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.propagation;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.socialranksys.diffusion.data.Data;
import es.uam.eps.socialranksys.diffusion.data.PropagatedInformation;
import es.uam.eps.socialranksys.diffusion.simulation.UserState;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Also known as the rumour spreading propagation mechanism, this is the 
 * propagation mechanism for the pull strategy propagation mechanism.
 * Each iteration, each user selects another one which has not visited in a certain time.
 * The users share all the information between them. 
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 * @param <I> type of the information pieces
 * @param <P> type of the parameters.
 */
public class PullPushStrategyPropagationMechanism<U extends Serializable,I extends Serializable,P> implements PropagationMechanism<U,I,P>
{
    /**
     * Number of iterations to wait until a profile can be revisited
     */
    private final int waitTime;
    /**
     * Edge direction of the neighbors to ask for information
     */
    private final EdgeOrientation orientation;
    /**
     * For each user, the list of users that user will propagate the information to.
     */
    private Map<U, List<U>> propagationList;
    /**
     * The list of users in the last iterations
     */
    private final Map<U, List<U>> lastIterations;
    
     /**
     * Constructor.
     * @param waitTime Number of iterations to wait until a profile can be revisited.
     */
    public PullPushStrategyPropagationMechanism(int waitTime)
    {
        this(waitTime, EdgeOrientation.IN);
    }  
    
    /**
     * Constructor.
     * @param waitTime Number of iterations to wait until a profile can be revisited.
     * @param orientation Selection of the visits available for contact.
     */
    public PullPushStrategyPropagationMechanism(int waitTime, EdgeOrientation orientation)
    {
        this.waitTime = waitTime;
        this.orientation = orientation;
        this.lastIterations = new HashMap<>();
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
            List<U> neighbours = data.getGraph().getNeighbourhood(u, orientation).collect(Collectors.toCollection(ArrayList::new));
            if(!this.lastIterations.containsKey(u)) this.lastIterations.put(u, new ArrayList<>());
            List<U> alreadyVisited = this.lastIterations.get(u);
            U neigh;
            
            neighbours.removeAll(alreadyVisited);
            
            if(neighbours.size() > 0)
            {
                int index = rng.nextInt(neighbours.size());
                neigh = neighbours.get(index);
            }
            else
            {
                neigh = null;
            }
            
            if(neigh != null)
            {
                if(!propagationList.containsKey(neigh))
                    propagationList.put(neigh, new ArrayList<>());
                if(!propagationList.get(neigh).contains(u))
                    propagationList.get(neigh).add(u);

                if(!propagationList.containsKey(u))
                    propagationList.put(u, new ArrayList<>());
                if(!propagationList.get(u).contains(neigh))
                    propagationList.get(u).add(neigh);

                alreadyVisited.add(0, neigh);
            }
            
            // Prune the list
            int maxSize = Math.min(this.waitTime, neighbours.size());
            if(alreadyVisited.size() > maxSize)
            {
                alreadyVisited.subList(maxSize, alreadyVisited.size()).clear();
            }
        });
        
    }

    @Override
    public boolean dependsOnInformationPiece() 
    {
        return false;
    }
}
