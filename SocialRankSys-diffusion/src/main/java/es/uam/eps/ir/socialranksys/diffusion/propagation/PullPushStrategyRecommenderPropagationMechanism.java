/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.propagation;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;
import es.uam.eps.ir.socialranksys.diffusion.data.PropagatedInformation;
import es.uam.eps.ir.socialranksys.diffusion.simulation.SimulationEdgeTypes;
import es.uam.eps.ir.socialranksys.diffusion.simulation.UserState;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;

import java.io.Serializable;
import java.util.*;
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
public class PullPushStrategyRecommenderPropagationMechanism<U extends Serializable,I extends Serializable,P> implements PropagationMechanism<U,I,P>
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
     * Probability of selecting a recommended link for propagation
     */
    private final double recProb;
    /**
     * Random number generator
     */
    private final Random rng;
    
     /**
     * Constructor.
     * @param waitTime Number of iterations to wait until a profile can be revisited.
     * @param recProb probability of selecting a recommended link for propagation.
     */
    public PullPushStrategyRecommenderPropagationMechanism(int waitTime, double recProb)
    {
        this(waitTime, EdgeOrientation.IN, recProb);
    }  
    
    /**
     * Constructor.
     * @param waitTime Number of iterations to wait until a profile can be revisited.
     * @param orientation Selection of the visits available for contact.
     * @param recProb probability of selecting a recommended link for propagation.
     */
    public PullPushStrategyRecommenderPropagationMechanism(int waitTime, EdgeOrientation orientation, double recProb)
    {
        this.waitTime = waitTime;
        this.orientation = orientation;
        this.lastIterations = new HashMap<>();
        this.recProb = recProb;
        this.rng = new Random();

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
        propagationList = new HashMap<>();
        data.getAllUsers().forEach((u)-> 
        {
            List<U> recNeighs = new ArrayList<>();
            List<U> nonRecNeighs = new ArrayList<>();
            data.getGraph().getNeighbourhood(u, orientation).forEach(v -> 
            {
                if(orientation.equals(EdgeOrientation.IN))
                {
                    if(data.getGraph().getEdgeType(v, u) == SimulationEdgeTypes.RECOMMEND)
                    {
                        recNeighs.add(v);
                    }
                    else
                    {
                        nonRecNeighs.add(v);
                    }
                }
                
            });
            
            
            if(!this.lastIterations.containsKey(u)) this.lastIterations.put(u, new ArrayList<>());
            List<U> alreadyVisited = this.lastIterations.get(u);
            U neigh;
            
            recNeighs.removeAll(alreadyVisited);
            nonRecNeighs.removeAll(alreadyVisited);
            
            if(recNeighs.isEmpty())
            {
                recNeighs.addAll(nonRecNeighs);
            }
            else if(nonRecNeighs.isEmpty())
            {
                nonRecNeighs.addAll(recNeighs);
            }
            
            double nextDouble = rng.nextDouble();
            
            if(nextDouble < this.recProb)
            {
                if(!recNeighs.isEmpty())
                {
                    int index = rng.nextInt(recNeighs.size());
                    neigh = recNeighs.get(index);
                }
                else
                {
                    neigh = null;
                }
            }
            else
            {
                if(!nonRecNeighs.isEmpty())
                {
                    int index = rng.nextInt(nonRecNeighs.size());
                    neigh = nonRecNeighs.get(index);
                }
                else
                {
                    neigh = null;
                }
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
            int maxSize = this.waitTime;
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
