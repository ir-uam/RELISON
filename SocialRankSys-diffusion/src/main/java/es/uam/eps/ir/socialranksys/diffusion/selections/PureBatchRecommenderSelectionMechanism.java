/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.selections;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;
import es.uam.eps.ir.socialranksys.diffusion.data.PropagatedInformation;
import es.uam.eps.ir.socialranksys.diffusion.simulation.SimulationEdgeTypes;
import es.uam.eps.ir.socialranksys.diffusion.simulation.SimulationState;
import es.uam.eps.ir.socialranksys.diffusion.simulation.UserState;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Selects the propagated pieces depending on the recommendations. When the user has to propagate
 * others information, chooses with some probability the information that has arrived from some users
 * that have been recommended to him, and with the rest of probability the information that has arrived from
 * users of the original graph. The type of user is selected on each iteration. 
 * In case that information from the selected type of users has not been propagated,
 * no further information is propagated. When selecting tweets to propagate, if the tweet has already been selected,
 * turn is skipped.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users.
 * @param <I> type of the information.
 * @param <P> type of the parameters.
 */
public class PureBatchRecommenderSelectionMechanism<U extends Serializable,I extends Serializable,P> extends CountSelectionMechanism<U,I,P>
{
    /**
     * Probability of choosing information to propagate that comes from recommended users.
     * If there are enough pieces, each time a piece of information is selected, with probability prob that piece will come from recommended
     * users, and with probability (1-p) the piece will come from an edge in the training graph.
     */
    private final double prob;
    
    /**
     * Neighborhood the information pieces come from. In case of IN, information comes from the followers of the users. In case of OUT (usual)
     * from the followees. Finally, in case of UND, from any of them.
     */
    private final EdgeOrientation orientation;
    
    /**
     * Constructor.
     * @param numOwn Number of own information pieces to propagate for each user and iteration.
     * @param numPropagate Number of received information to propagate for each user and iteration.
     * @param prob Probability of chosing information to propagate that comes from recommended users.
     * @param orient Neighborhood where the information pieces come from.
     */
    public PureBatchRecommenderSelectionMechanism(int numOwn, int numPropagate, double prob, EdgeOrientation orient)
    {
        super(numOwn,numPropagate,SelectionConstants.NONE);
        this.prob = prob;
        this.orientation = orient;
    }
    
    /**
     * Constructor.
     * @param numOwn Number of own information pieces to propagate for each user and iteration.
     * @param numPropagate Number of received information to propagate for each user and iteration.
     * @param numRepr Number of propagated information pieces to propagate for each user and iteration
     * @param prob Probability of chosing information to propagate that comes from recommended users.
     * @param orient Neighborhood where the information pieces come from.
     */
    public PureBatchRecommenderSelectionMechanism(int numOwn, int numPropagate, int numRepr, double prob, EdgeOrientation orient)
    {
        super(numOwn,numPropagate,numRepr);
        this.prob = prob;
        this.orientation = orient;
        
    }

    @Override
    protected List<PropagatedInformation> getReceivedInformation(UserState<U> user, Data<U, I, P> data, SimulationState<U, I, P> state, int numIter, Long timestamp)
    {
        List<PropagatedInformation> receivedToPropagate = new ArrayList<>();

        List<Integer> fromNeigh = new ArrayList<>();
        List<Integer> fromRec = new ArrayList<>();

        HashSet<Integer> setInfo = new HashSet<>();
            
        U u = user.getUserId();
        int userId = data.getUserIndex().object2idx(u);
        user.getReceivedInformation().forEach(info -> 
        {
            List<Integer> creators = new ArrayList<>(info.getCreators());
            List<Integer> indexesToDelete = new ArrayList<>();
            for(int i = 0; i < creators.size();++i)
            {
                Integer creator = creators.get(i);
                if(creator == null) // Checks if the index has to be deleted (null source)
                {
                    indexesToDelete.add(i);
                }
                else
                {
                    U creatorUser = data.getUserIndex().idx2object(creator);
                    boolean rec = false;
                    if(this.orientation == EdgeOrientation.IN)
                    {
                        if(data.getGraph().getEdgeType(creatorUser, u) == SimulationEdgeTypes.RECOMMEND)
                            rec = true;
                    }
                    else if(this.orientation == EdgeOrientation.OUT)
                    {
                        if(data.getGraph().getEdgeType(u, creatorUser) == SimulationEdgeTypes.RECOMMEND)
                            rec = true;
                    }
                    else
                    {
                        if(data.getGraph().containsEdge(u, creatorUser))
                            rec = data.getGraph().getEdgeType(u, creatorUser) == SimulationEdgeTypes.RECOMMEND;
                        if(data.getGraph().containsEdge(creatorUser, u))
                            rec = rec || (data.getGraph().getEdgeType(creatorUser, u) == SimulationEdgeTypes.RECOMMEND);
                    }
                    
                    if(rec)
                        fromRec.add(info.getInfoId());
                    else
                        fromNeigh.add(info.getInfoId());   
                }
                
            }
            
            // Remove the corresponding null sources
            for(int i = indexesToDelete.size()-1; i >= 0; --i)
            {
                creators.remove((int) indexesToDelete.get(i)); 
                System.out.println(creators);
            }
        });
        
        double r = rng.nextDouble();
        
        if(r < this.prob)
        {
            for(int i = 0; i < Math.min(this.getNumPropagate(), fromRec.size()); ++i)
            {
                // Decides which information pieces we want to propagate

                setInfo.add(fromRec.get(rng.nextInt(fromRec.size())));
                setInfo.forEach(idx -> receivedToPropagate.add(new PropagatedInformation(idx, numIter, userId)));
            }    
        }
        else
        {
            for(int i = 0; i < Math.min(this.getNumPropagate(), fromNeigh.size()); ++i)
            {
                // Decides which information pieces we want to propagate

                setInfo.add(fromRec.get(rng.nextInt(fromNeigh.size())));
                setInfo.forEach(idx -> receivedToPropagate.add(new PropagatedInformation(idx, numIter, userId)));
            }
        }
        
        return receivedToPropagate;
    }
}
