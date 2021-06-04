/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.selections;

import es.uam.eps.ir.relison.diffusion.data.Data;
import es.uam.eps.ir.relison.diffusion.data.PropagatedInformation;
import es.uam.eps.ir.relison.diffusion.simulation.SimulationEdgeTypes;
import es.uam.eps.ir.relison.diffusion.simulation.SimulationState;
import es.uam.eps.ir.relison.diffusion.simulation.UserState;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Selects information pieces to propagate depending on the original users and whether they have been propagated through
 * recommended links. A user can only propagate an information piece owned by another user if it comes from one of such
 * links.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information.
 * @param <P> type of the parameters.
 */
public class PureRecommenderSelectionMechanism<U extends Serializable,I extends Serializable, P> extends CountSelectionMechanism<U,I,P>
{
    /**
     * Neighborhood the information pieces come from. In case of IN, information comes from the followers of the users. In case of OUT (usual)
     * from the followees. Finally, in case of UND, from any of them.
     */
    private final EdgeOrientation orientation;
    
    /**
     * Constructor.
     * @param numOwn        number of own information pieces to propagate for each user and iteration.
     * @param numPropagate  number of received information to propagate for each user and iteration.
     * @param orientation   neighborhood the information pieces come from
     */
    public PureRecommenderSelectionMechanism(int numOwn, int numPropagate, EdgeOrientation orientation)
    {
        super(numOwn, numPropagate);
        this.orientation = orientation;
    }
    
    /**
     * Constructor.
     * @param numOwn        number of own information pieces to propagate for each user and iteration.
     * @param numPropagate  number of received information to propagate for each user and iteration.
     * @param numRepr       number of propagated information pieces to propagate for each user and iteration.
     * @param orientation   neighborhood the information pieces come from
     */
    public PureRecommenderSelectionMechanism(int numOwn, int numPropagate, int numRepr, EdgeOrientation orientation)
    {
        super(numOwn, numPropagate, numRepr);
        this.orientation = orientation;
    }
    
    @Override
    protected List<PropagatedInformation> getReceivedInformation(UserState<U> user, Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp)
    {
        List<PropagatedInformation> receivedToPropagate = new ArrayList<>();

        List<PropagatedInformation> fromRec = new ArrayList<>();

        HashSet<Integer> setInfo = new HashSet<>();
            
        U u = user.getUserId();
        int userId = data.getUserIndex().object2idx(u);
        user.getReceivedInformation().forEach(info -> 
        {
            List<Integer> creators = new ArrayList<>(info.getCreators());
            for (Integer creator : creators)
            {
                if (creator != null)
                {
                    U creatorUser = data.getUserIndex().idx2object(creator);
                    boolean rec = false;
                    if (this.orientation == EdgeOrientation.IN)
                    {
                        if (data.getGraph().getEdgeType(creatorUser, u) == SimulationEdgeTypes.RECOMMEND)
                            rec = true;
                    }
                    else if (this.orientation == EdgeOrientation.OUT)
                    {
                        if (data.getGraph().getEdgeType(u, creatorUser) == SimulationEdgeTypes.RECOMMEND)
                            rec = true;
                    }
                    else
                    {
                        if (data.getGraph().containsEdge(u, creatorUser))
                            rec = data.getGraph().getEdgeType(u, creatorUser) == SimulationEdgeTypes.RECOMMEND;
                        if (data.getGraph().containsEdge(creatorUser, u))
                            rec = rec || (data.getGraph().getEdgeType(creatorUser, u) == SimulationEdgeTypes.RECOMMEND);
                    }

                    if (rec)
                        fromRec.add(info);
                }
            }
        });

        return this.getPropagatedInformation(userId, this.getNumReceived(), numIter, fromRec);
    }
}
