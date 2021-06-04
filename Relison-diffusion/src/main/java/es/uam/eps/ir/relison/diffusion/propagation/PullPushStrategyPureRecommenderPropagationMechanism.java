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
import es.uam.eps.ir.relison.diffusion.simulation.SimulationEdgeTypes;
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
 * This variant only allows propagating information through recommended links.
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
 */
public class PullPushStrategyPureRecommenderPropagationMechanism<U extends Serializable,I extends Serializable,P> implements PropagationMechanism<U,I,P>
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
     * The orientation for selecting the neighborhood.
     */
    private final EdgeOrientation orientation;
    
    /**
     * Constructor.
     * @param waitTime    number of iterations to wait until a profile can be revisited.
     * @param orientation the orientation for selecting the neighborhood.
     */
    public PullPushStrategyPureRecommenderPropagationMechanism(int waitTime, EdgeOrientation orientation)
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

        // For all the users in the system:
        data.getAllUsers().forEach((u)-> 
        {
            // We first select a list of neighbors:
            List<U> neighbours = data.getGraph().getNeighbourhood(u, orientation).filter(v ->
            {
                if(orientation.equals(EdgeOrientation.IN))
                {
                    return data.getGraph().getEdgeType(v, u) == SimulationEdgeTypes.RECOMMEND;
                }
                else if(orientation.equals(EdgeOrientation.OUT))
                {
                    return data.getGraph().getEdgeType(u, v) == SimulationEdgeTypes.RECOMMEND;
                }
                else
                {
                    return (data.getGraph().containsEdge(u, v) && data.getGraph().getEdgeType(u,v) == SimulationEdgeTypes.RECOMMEND) ||
                           (data.getGraph().containsEdge(v, u) && data.getGraph().getEdgeType(v,u) == SimulationEdgeTypes.RECOMMEND);
                }
            }).collect(Collectors.toCollection(ArrayList::new));

            // Then, we obtain the already visited.
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

            if(!this.lastIterations.containsKey(u))
            {
                this.lastIterations.put(u, alreadyVisited);
            }
        });
        
    }

    @Override
    public boolean dependsOnInformationPiece()
    {
        return false;
    }
}
