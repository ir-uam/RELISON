/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.selections;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;
import es.uam.eps.ir.socialranksys.diffusion.data.PropagatedInformation;
import es.uam.eps.ir.socialranksys.diffusion.simulation.SimulationState;
import es.uam.eps.ir.socialranksys.diffusion.simulation.UserState;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Selects the information pieces to propagate according to the independent cascade protocol, i.e. given an information
 * piece received by a user, it propagates it with a probability that only depends on the endpoints of the link.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * <p>
 * <b>Reference:</b>  J. Goldenberg, B. Libai, and E. Muller. Talk of the Network: A Complex Systems Look at the Underlying Process of Word-of-Mouth, Marketing Letters, 12(3), pp. 211–223 (2001).
 * </p>
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters
 */
public class IndependentCascadeModelSelectionMechanism<U extends Serializable,I extends Serializable, P> extends CountSelectionMechanism<U,I,P>
{
    /**
     * Weighted graph. The weights represent the probability that an information piece
     * proceeding of a certain node is spread by the node that has received.
     */
    private final Graph<U> graph;
    /**
     * Uniform probability.
     */
    private final double prob;
    /**
     * Neighborhood where the information pieces come from.
     */
    private final EdgeOrientation orient;
    
    /**
     * Constructor.
     * @param prob      probability that each information piece is selected.
     * @param numOwn    number of own pieces of information to propagate.
     */
    public IndependentCascadeModelSelectionMechanism(double prob, int numOwn)
    {
        super(numOwn, SelectionConstants.NONE, SelectionConstants.NONE);
        this.prob = prob;
        this.graph = null;
        this.orient = EdgeOrientation.UND;
    }
    
    /**
     * Constructor.
     * @param prob      probability that each information piece is selected.
     * @param numOwn    number of own pieces of information to propagate.
     * @param numRepr   number of propagated pieces to repropagate.
     */
    public IndependentCascadeModelSelectionMechanism(double prob, int numOwn, int numRepr)
    {
        super(numOwn, SelectionConstants.NONE, numRepr);
        this.prob = prob;
        this.graph = null;
        this.orient = EdgeOrientation.UND;
    }
    
    /**
     * Constructor.
     * @param graph     a weighted graph containing the probabilities of spreading information between users in the network.
     * @param numOwn    number of own pieces of information to propagate.
     * @param orient    neighborhood where the information pieces come from.
     */
    public IndependentCascadeModelSelectionMechanism(Graph<U> graph, int numOwn, EdgeOrientation orient)
    {
        super(numOwn, SelectionConstants.NONE, SelectionConstants.NONE);
        this.graph = graph;
        this.prob = -1.0;
        this.orient = orient;
    }
    
    /**
     * Constructor.
     * @param graph     a weighted graph containing the probabilities of spreading information between users in a graph.
     * @param numOwn    number of own pieces of information to propagate.
     * @param numRepr   number of propagated pieces to repropagate.
     * @param orient    neighborhood where the information pieces come from.
     */
    public IndependentCascadeModelSelectionMechanism(Graph<U> graph, int numOwn, int numRepr, EdgeOrientation orient)
    {
        super(numOwn, SelectionConstants.NONE, numRepr);
        this.graph = graph;
        this.prob = -1.0;
        this.orient = orient;
    }
    
    @Override
    protected List<PropagatedInformation> getReceivedInformation(UserState<U> user, Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp)
    {
        if(graph == null && (prob >= 0.0 && prob <= 1.0))
        {
            // Case 1: the probability for propagating a piece is fixed (and independent from the users).
            return this.getReceivedInformationProb(user, data, state, numIter);
        }
        else if(graph != null)
        {
            // Case 2: the probability for propagating a piece is fixed for each pair of users connected in the network.
            return this.getReceivedInformationGraph(user, data, state, numIter);
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Using a weighted graph (where the weights represent diffusion probabilities), it obtains the list of received
     * information pieces that we want to propagate.
     *
     * @param user      the user propagating the information.
     * @param data      the data for the simulation.
     * @param state     the current state of the simulation.
     * @param numIter   the number of the current iteration.
     *
     * @return a selection of information pieces to be propagated
     */
    protected List<PropagatedInformation> getReceivedInformationGraph(UserState<U> user, Data<U,I,P> data, SimulationState<U,I,P> state, int numIter)
    {
        List<PropagatedInformation> toPropagate = new ArrayList<>();
        int userId = data.getUserIndex().object2idx(user.getUserId());

        U u = user.getUserId();

        // We check each received information piece
        user.getReceivedInformation().forEach(info -> 
        {
            // We get each of the creators:
            for(int creator : info.getCreators())
            {
                double r = rng.nextDouble();
                double edgeweight;
                U v = data.getUserIndex().idx2object(creator);
                if(this.orient.equals(EdgeOrientation.IN)) // Information piece comes from your followers
                {
                    assert this.graph != null;
                    edgeweight = this.graph.getEdgeWeight(v, u);
                    if(r < edgeweight)
                    {
                        toPropagate.add(new PropagatedInformation(info.getInfoId(), numIter, userId));
                        break;
                    }
                        
                }
                else if(this.orient.equals(EdgeOrientation.OUT)) // Information piece comes from your followee
                {
                    assert this.graph != null;
                    edgeweight = this.graph.getEdgeWeight(u, v);
                    if(r < edgeweight)
                    {
                        toPropagate.add(new PropagatedInformation(info.getInfoId(), numIter, userId));
                        break;
                    }
                }
                else // information piece might come from either of them.
                {
                    assert this.graph != null;
                    if(this.graph.containsEdge(u, v))
                    {
                        edgeweight = this.graph.getEdgeWeight(u, v);
                        if(r < edgeweight)
                        {
                            toPropagate.add(new PropagatedInformation(info.getInfoId(), numIter, userId));
                            break;
                        }
                        else // we independently treat each of the possible edges.
                        {
                            r = rng.nextDouble();
                        }
                    }
                    
                    if(this.graph.containsEdge(v, u))
                    {
                        edgeweight = this.graph.getEdgeWeight(v,u);
                        if(r < edgeweight)
                        {
                            toPropagate.add(new PropagatedInformation(info.getInfoId(), numIter, userId));
                            break;
                        }
                    }
                }
            }
                
        });
        
        return toPropagate;
    }
    
    /**
     * Given a fixed value for the probability, it obtains the list of pieces to repropagate.
     * @param user      the user to analyze
     * @param data      the full data
     * @param state     current simulation state
     * @param numIter   number of the iteration
     * @return a selection of the received tweets to be propagated
     */
    protected List<PropagatedInformation> getReceivedInformationProb(UserState<U> user, Data<U,I,P> data, SimulationState<U,I,P> state, int numIter)
    {
        List<PropagatedInformation> toPropagate = new ArrayList<>();
        int userId = data.getUserIndex().object2idx(user.getUserId());

        user.getReceivedInformation().forEach(info -> 
        {
            for(int creator : info.getCreators())
            {
                double r = rng.nextDouble();
                if(r < this.prob)
                {
                    toPropagate.add(new PropagatedInformation(info.getInfoId(), numIter, userId));
                    break;
                }
            }
        });
        
        return toPropagate;
    }
}
