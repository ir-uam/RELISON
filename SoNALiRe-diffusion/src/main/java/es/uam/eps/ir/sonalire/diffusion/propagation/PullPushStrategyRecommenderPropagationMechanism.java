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
import es.uam.eps.ir.sonalire.diffusion.simulation.SimulationEdgeTypes;
import es.uam.eps.ir.sonalire.diffusion.simulation.UserState;
import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

/**
 * Propagation mechanism for the so-called rumour spreading propagation mechanism. Following this strategy, each user
 * selects a user each iteration: he catches the information from such user, and shares with him the information he has.
 * It is a combination of the pull and push propagation mechanisms. A certain amount of time has to pass before a
 * neighbor is selected again.
 *
 * With a given probability, it selects a neighbor using recommended links. Otherwise, it recommends any link.
 *
 * <p>
 *      <b>Reference:</b> A. Demers, D. Greene, C. Hauser, W. Irish, J. Larson. Epidemic algorithms for replicated database maintenance. ACM PODC 1987, pp. 1-12 (1987)
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
public class PullPushStrategyRecommenderPropagationMechanism<U extends Serializable,I extends Serializable,P> implements PropagationMechanism<U,I,P>
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
     * Probability of selecting a recommended link for propagation.
     */
    private final double recProb;
    /**
     * Random number generator.
     */
    private final Random rng;
    
     /**
     * Constructor.
     * @param waitTime number of iterations to wait until a profile can be revisited.
     * @param recProb  probability of selecting a recommended link for propagation.
     */
    public PullPushStrategyRecommenderPropagationMechanism(int waitTime, double recProb)
    {
        this(waitTime, EdgeOrientation.IN, recProb);
    }  
    
    /**
     * Constructor.
     * @param waitTime      number of iterations to wait until a profile can be revisited.
     * @param orientation   selection of the visits available for contact.
     * @param recProb       probability of selecting a recommended link for propagation.
     */
    public PullPushStrategyRecommenderPropagationMechanism(int waitTime, EdgeOrientation orientation, double recProb)
    {
        this.waitTime = waitTime;
        this.orientation = orientation;
        this.lastIterations = new HashMap<>();
        this.propagationList = new HashMap<>();
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
        propagationList.clear();

        // For each user in the network:
        data.getAllUsers().forEach((u)-> 
        {
            // We first obtain the list of neighbors obtained through recommendation (and the rest of them).
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
                else if(orientation.equals(EdgeOrientation.OUT))
                {
                    if(data.getGraph().getEdgeType(u,v) == SimulationEdgeTypes.RECOMMEND)
                    {
                        recNeighs.add(v);
                    }
                    else
                    {
                        nonRecNeighs.add(v);
                    }
                }
                else
                {
                    if(data.getGraph().containsEdge(u, v) && data.getGraph().getEdgeType(u,v) == SimulationEdgeTypes.RECOMMEND)
                    {
                        recNeighs.add(v);

                    }
                    else if(data.getGraph().containsEdge(v,u) && data.getGraph().getEdgeType(v,u) == SimulationEdgeTypes.RECOMMEND)
                    {
                        recNeighs.add(v);

                    }
                    else
                    {
                        nonRecNeighs.add(v);
                    }
                }
            });

            // We check whether the last iterations contains a key:
            if(!this.lastIterations.containsKey(u))
            {
                this.lastIterations.put(u, new ArrayList<>());
            }

            // Then, we obtain the visited users in the last cases:
            List<U> alreadyVisited = this.lastIterations.get(u);
            U neigh;
            
            recNeighs.removeAll(alreadyVisited);
            nonRecNeighs.removeAll(alreadyVisited);

            // To ensure that we can choose a neighbor, if there is no eligible neighbors among the
            // recommended ones, we choose from the rest.
            if(recNeighs.isEmpty())
            {
                recNeighs.addAll(nonRecNeighs);
            }

            // The same applies to the rest (in the opposite direction.
            if(nonRecNeighs.isEmpty())
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

                alreadyVisited.add(neigh);
            }
            else
            {
                alreadyVisited.add(null);
            }
            
            // Prune the list
            if(alreadyVisited.size() > this.waitTime)
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
