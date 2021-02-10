/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.protocols;

import es.uam.eps.ir.socialranksys.diffusion.expiration.AllNotPropagatedExpirationMechanism;
import es.uam.eps.ir.socialranksys.diffusion.propagation.AllFollowersPropagationMechanism;
import es.uam.eps.ir.socialranksys.diffusion.selections.IndependentCascadeModelSelectionMechanism;
import es.uam.eps.ir.socialranksys.diffusion.sight.AllSightMechanism;
import es.uam.eps.ir.socialranksys.diffusion.update.IndependentCascadeModelUpdateMechanism;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;

import java.io.Serializable;

/**
 * Independent Cascade Model Protocol. 
 * <br> 
 * Main characteristics:
 * <ul>
 *  <li><b>Selection mechanism:</b> Each user takes a fixed number of information pieces from the own list. In the case of the received list,
 *  each piece of information is propagated with a probability that depends only on the user that propagated the information to the user.</li>
 *  <li><b>Expiration mechanism:</b> In this case, every information piece that is not propagated is discarded.</li>
 *  <li><b>Update mechanism:</b> As information pieces are immediately deleted after not being propagated, received information pieces are not updated at all.
 *  In the case of the previously discarded fragments, a tweet emited by an user can only be propagated if the user has not already appeared in the received set as
 *  a creator of the information piece.</li>
 *  <li><b>Propagation mechanism:</b> The information is propagated to every follower on the graph. This simulates the information propagation of networks such as Twitter
 *  or Facebook, where all the information produced by an user is shown to all its followers or mutual followers.
 * </ul>
 *
 * <p>
 * <b>Reference:</b>  J. Goldenberg, B. Libai, and E. Muller. Talk of the Network: A Complex Systems Look at the Underlying Process of Word-of-Mouth, Marketing Letters, 12(3), pp. 211–223 (2001).
 * </p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information.
 * @param <P> type of the parameters.
 */
public class IndependentCascadeModelProtocol<U extends Serializable,I extends Serializable,P> extends Protocol<U,I,P>
{
    /**
     * Constructor for the Independent Cascade Model simulator.
     * @param prob Probability of propagating an information piece.
     * @param numOwn Number of own tweets to propagate randomly each iteration.
     */
    public IndependentCascadeModelProtocol(double prob, int numOwn)
    {
        super(  new IndependentCascadeModelSelectionMechanism<>(prob, numOwn),
                new AllNotPropagatedExpirationMechanism<>(),
                new IndependentCascadeModelUpdateMechanism(),
                new AllFollowersPropagationMechanism<>(),
                new AllSightMechanism<>());
    }
    
    /**
     * Constructor for the Independent Cascade Model simulator.
     * @param graph Graph that contains the probability of propagating an information piece across an edge.
     * @param numOwn Number of own tweets to propagate randomly each iteration.
     */
    public IndependentCascadeModelProtocol(Graph<U> graph, int numOwn)
    {
        super(  new IndependentCascadeModelSelectionMechanism<>(graph, numOwn, EdgeOrientation.OUT),
                new AllNotPropagatedExpirationMechanism<>(),
                new IndependentCascadeModelUpdateMechanism(),
                new AllFollowersPropagationMechanism<>(),
                new AllSightMechanism<>());
    }
    
    
}
