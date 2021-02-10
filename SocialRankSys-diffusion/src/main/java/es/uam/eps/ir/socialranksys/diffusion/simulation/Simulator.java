/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.simulation;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;
import es.uam.eps.ir.socialranksys.diffusion.data.Information;
import es.uam.eps.ir.socialranksys.diffusion.data.PropagatedInformation;
import es.uam.eps.ir.socialranksys.diffusion.io.backup.BinarySimulationWriter;
import es.uam.eps.ir.socialranksys.diffusion.protocols.Protocol;
import es.uam.eps.ir.socialranksys.diffusion.selections.Selection;
import es.uam.eps.ir.socialranksys.diffusion.stop.StopCondition;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class for the execution of information propagation simulations.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 */
public class Simulator<U extends Serializable,I extends Serializable,P> implements Serializable
{
    /**
     * Stop condition for the simulation.
     */
    private final StopCondition<U,I,P> stop;
    /**
     * Communication protocol.
     */
    private final Protocol<U,I,P> protocol;
    /**
     * The data to use in the simulation.
     */
    private Data<U,I,P> data;
    /**
     * The current iteration.
     */
    private int numIter;
    /**
     * Number of items that have propagated information in an iteration.
     */
    private int currentPropagated;
    /**
     * Number of users that have propagated information in an iteration.
     */
    private int currentPropagatingUsers;
    /**
     * Number of newly propagated information in an iteration.
     */
    private long newlyPropagatedInfo;
    /**
     * Current state of the simulation.
     */
    private SimulationState<U,I,P> state;
    
    /**
     * For time-based simulations, the current timestamp.
     */
    private Long currentTimestamp;
    /**
     * Full Constructor.
     * @param protocol Communication protocol.
     * @param stop Stop condition for the simulation.
     * If you only want to print a distribution at the end of the simulation, then, 
     * do not include them in this map.
     */
    public Simulator(Protocol<U,I,P> protocol, StopCondition<U,I,P> stop)
    {
        this.protocol = protocol;
        this.stop = stop;
        this.state = new SimulationState<>();
    }
    
    /**
     * Initializes and prepares the data.
     * @param data Full data.
     */
    public void initialize(Data<U,I,P> data)
    {
        this.data = data;
        this.state.initialize(this.data);
        System.out.println("Filtering done");
        System.out.println(this.data.dataSummary());
        this.numIter = 0;
        this.currentTimestamp = data.getTimestamps().first();
    }
    
    /**
     * Initializes and prepares the data.
     * @param data Full data.
     * @param simulation the current state of the simulation.
     */
    public void initialize(Data<U,I,P> data, Simulation<U,I,P> simulation)
    {
        this.data = data;
        this.state = simulation.getFinalState(this.protocol.getUpdate());
        System.out.println("Filtering done");
        System.out.println(this.data.dataSummary());
        this.numIter = simulation.getInitialNumber() + simulation.getNumIterations();
        long timestamp = data.getTimestamps().first();
        for(int i = 0; i < simulation.getInitialNumber() + simulation.getNumIterations(); ++i)
        {
            timestamp = data.getTimestamps().higher(timestamp);
        }
        this.currentTimestamp = timestamp;
    }
    
    /**
     * Executes the simulation and stores the results in a file. This method does not backup the simulation.
     * @return the simulation evolution.
     */
    public Simulation<U,I,P> simulate()
    {
        return this.simulate(null);
    }
    
    /**
     * Executes the simulation and stores the results in a file.
     * @param backup File where we want to backup the simulation, to prevent errors.
     * @return the simulation evolution.
     */
    public Simulation<U,I,P> simulate(String backup) 
    {
        Simulation<U,I,P> simulation = new Simulation<>(this.data, this.numIter);
        
        long initTime = System.currentTimeMillis();
        long alarmTime = 0L;
        long totalpropagated = 0L;
        
        Map<U, Long> receivedCount = new HashMap<>();
        this.state.getAllUsers().forEach(u -> 
        {
            long count = u.getReceivedInformation().count();
            if(count > 0)
                receivedCount.put(u.getUserId(), count);
        });
        
        do // Start propagation
        {
            // Store the state of the simulation in this iteration (i.e. the newly received pieces of information)
            Iteration<U,I,P> iteration = new SimpleIteration<>(this.numIter);
            
            long initialTime = System.currentTimeMillis();
            
            // First, states that no user nor piece has been propagated during the iteration
            this.currentPropagated = 0;
            this.currentPropagatingUsers = 0;
    
            // Resets the selected users in the propagated protocol
            this.protocol.getProp().resetSelections(data);
            
            Map<U, Selection> allPropInfo = new HashMap<>();
            
            // for each user, select the pieces to propagate.
            this.protocol.getSelection().getSelectableUsers(data, state, numIter, this.currentTimestamp).forEach(u ->
            {
                UserState<U> user = state.getUser(u);
                // Select which information pieces will be propagated for the user.
                Selection sel = this.protocol.getSelection().select(user, data, state, numIter, this.currentTimestamp);
                int numProp = sel.numPropagated();
                if(numProp > 0)
                {
                    ++this.currentPropagatingUsers;
                    this.currentPropagated += sel.numPropagated();
                
                    // Update the user: the selected pieces are moved to the propagated list.
                    user.updateReceivedToPropagated(sel.getPropagateSelection().map(Information::getInfoId));
                    user.updateOwnToPropagated(sel.getOwnSelection().map(Information::getInfoId));
                
                    allPropInfo.put(user.getUserId(), sel);
                }
            });
           
            List<U> toRemove = new ArrayList<>();
            // for each user, expire the selected information.
            receivedCount.keySet().forEach(u ->
            {
                UserState<U> user = state.getUser(u);
                // Expire the information
                // Apply the expiration of the received information pieces.
                List<Integer> deleted = this.protocol.getExpiration().expire(user, data, numIter, this.currentTimestamp)
                        .collect(Collectors.toCollection(ArrayList::new));
              
                // Store the information about the discarded pieces this iteration.
                List<I> discardedInfo = new ArrayList<>();
                deleted.forEach(i -> discardedInfo.add(this.data.getInformationPiecesIndex().idx2object(i)));
                
                if(!discardedInfo.isEmpty())
                {
                    iteration.addDiscardingUser(user.getUserId(), discardedInfo);
                    long c = receivedCount.get(u) - discardedInfo.size();
                    if(c <= 0) toRemove.add(u);
                    receivedCount.put(u, receivedCount.get(u) - discardedInfo.size());
                }
                user.discardReceivedInformation(deleted.stream());
            });
            
            toRemove.forEach(receivedCount::remove);
            
            this.newlyPropagatedInfo = 0;
            // Resets the users to obtain information pieces from.
            this.protocol.getSight().resetSelections(data);

            Set<U> receivedUsers = new HashSet<>();
            // Now that information pieces have been selected for propagation, and the remaining
            // have been pruned, now, we can distribute the pieces among the different users.
            allPropInfo.keySet().forEach(u -> 
            {
                Selection sel = allPropInfo.get(u);
                List<I> propInfo = new ArrayList<>();
                sel.getAll().forEach(info -> propInfo.add(this.data.getInformationPiecesIndex().idx2object(info.getInfoId())));
                if(!propInfo.isEmpty())
                {
                    iteration.addPropagatingUser(u,propInfo);
                }
                
                // In case the selection of users that receive the information do not depend on which piece do we send
                if(!this.protocol.getProp().dependsOnInformationPiece())
                {
                    PropagatedInformation auxpp = new PropagatedInformation(-1, 0, 0);
                    this.protocol.getProp().getUsersToPropagate(auxpp, state.getUser(u), data).forEach(v ->
                    {
                        UserState<U> uState = this.state.getUser(v);
                        sel.getAll().filter(piece -> this.protocol.getSight().seesInformation(uState, data, piece)).forEach(piece ->
                        {
                            uState.updateSeen(piece);
                            this.newlyPropagatedInfo++;
                            receivedUsers.add(v);
                        });
                    });
                }
                else // In case different pieces could go to different users.
                {
                    sel.getAll().forEach(info -> 
                    {
                        
                        I i = this.data.getInformationPiecesIndex().idx2object(info.getInfoId());
                        propInfo.add(i);

                        this.protocol.getProp().getUsersToPropagate(info, state.getUser(u), data).forEach(v -> 
                        {
                            UserState<U> vState = this.state.getUser(v);
                            if(this.protocol.getSight().seesInformation(vState, data, info))
                            {
                                vState.updateSeen(info);
                                this.newlyPropagatedInfo++;
                                receivedUsers.add(v);
                            }
                        });
                    });
                }
            });
                      
            
            receivedUsers.forEach(user -> 
            {
                Map<I, Set<U>> seenInfo = new HashMap<>();
                Map<I, Set<U>> rereceivedInfo = new HashMap<>();
                
                UserState<U> uState = state.getUser(user);
                

                int numRec = uState.getSeenInformation().mapToInt(propInfo -> 
                {
                    int numRereceived = 0;
                    int info = propInfo.getInfoId();
                    Set<U> authors = new HashSet<>();
                    propInfo.getCreators().forEach(cidx -> authors.add(data.getUserIndex().idx2object(cidx)));
                    
                    if(!uState.containsDiscardedInformation(info) && !uState.containsOwnInformation(info) && !uState.containsPropagatedInformation(info) && !uState.containsReceivedInformation(info))
                    {
                        seenInfo.put(data.getInformationPiecesIndex().idx2object(info), authors);
                    }
                    else if((uState.containsReceivedInformation(info) || uState.containsDiscardedInformation(info)) && !uState.containsOwnInformation(info) && !uState.containsPropagatedInformation(info))
                    {
                        if(!uState.containsReceivedInformation(info)) numRereceived++;
                        rereceivedInfo.put(data.getInformationPiecesIndex().idx2object(info), authors);
                    }
                    
                    uState.updateSeenToReceived(info, this.protocol.getUpdate());
                    
                    return numRereceived;
                }).sum();
                
                uState.clearSeenInformation();
                
                if(!seenInfo.isEmpty())
                {
                    iteration.addReceivingUser(user, seenInfo);
                    numRec += seenInfo.size();
                }
                
                if(!rereceivedInfo.isEmpty())
                {
                    iteration.addReReceivingUser(user, rereceivedInfo);
                }
                
                if(receivedCount.containsKey(user))
                {
                    receivedCount.put(user, numRec + receivedCount.get(user));
                }
                else
                {
                    receivedCount.put(user, (long) numRec);
                }
            });

            totalpropagated += this.currentPropagated;
            simulation.addIteration(iteration);
            
            // Select the information that the different users see
            /*this.state.getAllUsers().parallel().forEach(user -> 
            {
                user.updateSeen(this.protocol.getSight().seeInformation(user, data));
            });
            this.newlyPropagatedInfo = this.state.getAllUsers().parallel().mapToLong(user -> user.getSeenInformation().count()).sum();
            */
                
            // Move all the newly observed pieces to the received set.
            
            alarmTime += (System.currentTimeMillis() - initialTime);
            
            if(numIter%100 == 0)
            {
                System.out.println("Iteration " + numIter + " finished (" + alarmTime + " ms.)");
            }
            
            numIter++;
            this.currentTimestamp = this.data.getTimestamps().higher(this.currentTimestamp);
            
            long endTime = System.currentTimeMillis();
            if(backup != null && (endTime - initTime) > 3600 * 1000) // Each hour of simulation, store a backup
            {
                BinarySimulationWriter<U,I,P> bsw = new BinarySimulationWriter<>();
                bsw.initialize(backup);
                bsw.writeSimulation(simulation);
                initTime = System.currentTimeMillis();
            }
                
        }
        while(!this.stop.stop(numIter, currentPropagated, currentPropagatingUsers, this.newlyPropagatedInfo, totalpropagated, data, currentTimestamp));
        return simulation;
    }
}
