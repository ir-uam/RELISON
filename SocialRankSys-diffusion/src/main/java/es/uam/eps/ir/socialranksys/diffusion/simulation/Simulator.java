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
     * Full constructor.
     * @param protocol the communication protocol to apply.
     * @param stop     the stop condition of the simulation.
     */
    public Simulator(Protocol<U,I,P> protocol, StopCondition<U,I,P> stop)
    {
        this.protocol = protocol;
        this.stop = stop;
        this.state = new SimulationState<>();
    }
    
    /**
     * Initializes and prepares the data.
     * @param data the complete data for the simulation.
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
     * @param data       the complete data for the simulation.
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
     * @param backup file where we want to backup the simulation, to prevent errors.
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

        // Start propagation
        do
        {
            /*
                Each iteration, we store the state of the simulation in an Iteration object. For instance, we store the
                newly received pieces of information by the different users.
             */
            Iteration<U,I,P> iteration = new SimpleIteration<>(this.numIter);
            
            long initialTime = System.currentTimeMillis();

            // As a first step, we indicate that no user has propagated information during the current iteration.
            this.currentPropagated = 0;
            this.currentPropagatingUsers = 0;

            // Then, for each user in the network, we select the users towards whom they might propagate their information
            // pieces.

            // We reset the selections, i.e. if the propagation model has to select, for each user, a fixed set of users
            // towards whom propagate information, this method does it.
            this.protocol.getProp().resetSelections(data);

            // Map containing the information propagated by each user:
            Map<U, Selection> allPropInfo = new HashMap<>();
            
            // We first select the set of users in the system that will propagate information:
            this.protocol.getSelection().getSelectableUsers(data, state, numIter, this.currentTimestamp).forEach(u ->
            {
                // We get the current state of the user.
                UserState<U> user = state.getUser(u);
                // Select which information pieces that the user will propagat.
                Selection sel = this.protocol.getSelection().select(user, data, state, numIter, this.currentTimestamp);

                // If any...
                int numProp = sel.numPropagated();
                if(numProp > 0)
                {
                    // a) we add it to the list
                    ++this.currentPropagatingUsers;
                    this.currentPropagated += sel.numPropagated();
                    allPropInfo.put(user.getUserId(), sel);

                    // b) we move the given pieces to the propagated list.
                    user.updateReceivedToPropagated(sel.getPropagateSelection().map(Information::getInfoId));
                    user.updateOwnToPropagated(sel.getOwnSelection().map(Information::getInfoId));

                    // c) we update the list of users who have information which can expire
                    long propCount = sel.getPropagateSelection().count();
                    if(propCount > 0)
                    {
                        long c = receivedCount.getOrDefault(u, 0L) - propCount;
                        if(c <= 0) receivedCount.remove(u);
                        else receivedCount.put(u, c);
                    }
                }
            });

            List<U> toRemove = new ArrayList<>();

            // Once we have selected the information pieces that are going to be propagated, we can then select which
            // of them shall not be propagated in the future.

            // We run over the set of users who have information in the received list.
            receivedCount.keySet().forEach(u ->
            {
                // We get the actual state of the user.
                UserState<U> user = state.getUser(u);

                // Expire the information

                // Apply the expiration of the received information pieces.
                List<Integer> deleted = this.protocol.getExpiration().expire(user, data, numIter, this.currentTimestamp)
                        .collect(Collectors.toCollection(ArrayList::new));
              
                // Store the information about the discarded pieces this iteration.
                List<I> discardedInfo = new ArrayList<>();
                deleted.forEach(i -> discardedInfo.add(this.data.getInformationPiecesIndex().idx2object(i)));

                // If we can discard information:
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

            // If users can only observe information pieces from some users, then, we update these values.
            this.protocol.getSight().resetSelections(data);

            Set<U> receivedUsers = new HashSet<>();
            Map<U, List<PropagatedInformation>> receivedInfo = new HashMap<>();

            // First, we identify the propagated information that reaches each user.
            allPropInfo.keySet().forEach(u ->
            {
                Selection sel = allPropInfo.get(u);
                List<I> propInfo = new ArrayList<>();

                if(!this.protocol.getProp().dependsOnInformationPiece())
                {
                    sel.getAll().forEach(piece -> propInfo.add(this.data.getInformationPiecesIndex().idx2object(piece.getInfoId())));
                    PropagatedInformation auxpp = new PropagatedInformation(-1,0,0);
                    this.protocol.getProp().getUsersToPropagate(auxpp, state.getUser(u), data).forEach(v ->
                    {
                        if(!receivedInfo.containsKey(v)) receivedInfo.put(v, new ArrayList<>());
                        sel.getAll().forEach(piece -> receivedInfo.get(v).add(piece));
                    });
                }
                else
                {
                    sel.getAll().forEach(info ->
                    {
                        this.protocol.getProp().getUsersToPropagate(info, state.getUser(u), data).forEach(v ->
                        {
                            if(!receivedInfo.containsKey(v)) receivedInfo.put(v, new ArrayList<>());
                            receivedInfo.get(v).add(info);
                        });
                        propInfo.add(this.data.getInformationPiecesIndex().idx2object(info.getInfoId()));
                    });
                }

                if(!propInfo.isEmpty())
                {
                    iteration.addPropagatingUser(u, propInfo);
                }
            });

            // As a second step, for each receiving user, we determine which information is seen and which is not
            receivedInfo.keySet().forEach(u ->
            {
                UserState<U> uState = this.state.getUser(u);
                List<PropagatedInformation> prop = this.protocol.getSight().seesInformation(uState, data, receivedInfo.get(u));
                if(!prop.isEmpty())
                {
                    this.newlyPropagatedInfo += prop.size();
                    receivedUsers.add(u);
                    prop.forEach(uState::updateSeen);
                }
            });

            // Then, for each received users...
            receivedUsers.forEach(user -> 
            {
                // We identify which information
                Map<I, Set<U>> seenInfo = new HashMap<>();
                Map<I, Set<U>> rereceivedInfo = new HashMap<>();
                
                UserState<U> uState = state.getUser(user);

                // Now:
                int numRec = uState.getSeenInformation().mapToInt(propInfo -> 
                {
                    int numRereceived = 0;
                    int info = propInfo.getInfoId();
                    Set<U> authors = new HashSet<>();
                    propInfo.getCreators().forEach(cidx -> authors.add(data.getUserIndex().idx2object(cidx)));

                    // We check whether the information is new or it has been re-received.
                    if(!uState.containsDiscardedInformation(info) && !uState.containsOwnInformation(info) && !uState.containsPropagatedInformation(info) && !uState.containsReceivedInformation(info))
                    {
                        // The information is new:
                        numRereceived++;
                        seenInfo.put(data.getInformationPiecesIndex().idx2object(info), authors);
                    }
                    else
                    {
                        if(!uState.containsReceivedInformation(info)) numRereceived++;
                        rereceivedInfo.put(data.getInformationPiecesIndex().idx2object(info), authors);
                    }

                    // We move from seen to received the information piece, using the update mechanism.
                    uState.updateSeenToReceived(info, this.protocol.getUpdate());
                    
                    return numRereceived;
                }).sum();
                
                uState.clearSeenInformation();

                // We add to the iteration the set of information pieces that the user has seen:
                if(!seenInfo.isEmpty())
                {
                    iteration.addReceivingUser(user, seenInfo);
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
                
        } // Checks whether the simulation has finished.
        while(!this.stop.stop(numIter, currentPropagated, currentPropagatingUsers, this.newlyPropagatedInfo, totalpropagated, data, currentTimestamp));

        return simulation;
    }
}
