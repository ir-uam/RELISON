/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.simulation;

import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;
import es.uam.eps.socialranksys.diffusion.data.Data;
import es.uam.eps.socialranksys.diffusion.data.PropagatedInformation;
import es.uam.eps.socialranksys.diffusion.update.UpdateMechanism;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class that stores the evolution of a simulation over time. It just stores the
 * pieces that are read by the different users in the network.
 * @author Javier Sanz-Cruzado Puig
 * 
 * @param <U> type of the users
 * @param <I> type of the information pieces
 * @param <P> type of the parameter values
 */
public class Simulation<U extends Serializable,I extends Serializable,P> 
{
    /**
     * Total number of iterations in the simulation
     */
    private int numIterations;
    /**
     * List of iterations
     */
    private final List<Iteration<U,I,P>> iterations;
    /**
     * Data.
     */
    private final Data<U,I,P> data;

    /**
     * Initial iteration number
     */
    private final int initialNumber;
    
    /**
     * Constructor.
     * @param data the real data.
     * @param initialNumber initial iteration number
     */
    public Simulation(Data<U,I,P> data, int initialNumber)
    {
        this.data = data;
        this.numIterations = 0;
        this.iterations = new ArrayList<>();
        this.initialNumber = initialNumber;
    }
    
    /**
     * Constructor.
     * @param data the real data
     */
    public Simulation(Data<U,I,P> data)
    {
        this(data, 0);
    }
    
    /**
     * Adds an iteration. The iteration must have the correct iteration number (initial + number of iterations).
     * @param iteration the iteration to add.
     * @return true if the iteration is correctly added, false if it is not.
     */
    public boolean addIteration(Iteration<U,I,P> iteration)
    {
        if(iteration != null && iteration.getIterationNumber() == (this.numIterations + this.initialNumber))
        {
            this.numIterations++;
            this.iterations.add(iteration);
            return true;
        }
        return false;
    }
    
    /**
     * Gets an individual iteration in the simulation.
     * @param number the number of the iteration.
     * @return the iteration if it exists, null if it does not.
     */
    public Iteration<U,I,P> getIteration(int number)
    {
        if(number >= this.initialNumber && number < (this.numIterations + this.initialNumber))
        {
            return this.iterations.get(number - this.initialNumber);
        }
        
        return null;
    }
    
    /**
     * Obtains the current number of iterations in the simulation.
     * @return the current number of iterations in the simulation.
     */
    public int getNumIterations()
    {
        return this.numIterations;
    }
           
    /**
     * Obtains the data for the simulation.
     * @return the data for the simulation.
     */
    public Data<U,I,P> getData()
    {
        return this.data;
    }
    
    /**
     * Number of the first iteration.
     * @return the number of the first iteration.
     */
    public int getInitialNumber()
    {
        return this.initialNumber;
    }
        
    /**
     * Finds a final state of the simulation.
     * @param upd update mechanism.
     * @return the final state of the simulation.
     */
    public SimulationState<U,I,P> getFinalState(UpdateMechanism upd)
    {
        SimulationState<U,I,P> state = new SimulationState<>();
        state.initialize(this.data);
                               
        Map<U, Map<I, Long>> propagated = new HashMap<>();
        Map<U, Map<I, Tuple2oo<Set<U>,Long>>> discarded = new HashMap<>();
        Map<U, Map<I, Tuple2oo<Set<U>,Long>>> received = new HashMap<>();
        
        for(int i = 0; i < this.numIterations; ++i)
        {
            Iteration<U,I,P> iteration = this.getIteration(i);           
            this.updatePropagated(iteration, propagated, discarded, received);
            this.updateDiscarded(iteration, propagated, discarded, received);
            this.updateReceived(iteration, propagated, discarded, received, upd);
        }
        
        // Establish the state.
        state.getAllUsers().forEach(st -> 
        {
            U u = st.getUserId();
            int uidx = this.data.getUserIndex().object2idx(u);
            
            // Indicate the values for propagated information
            if(propagated.containsKey(u))
            {
                propagated.get(u).keySet().forEach(info ->
                {
                    int iidx = this.data.getInformationPiecesIndex().object2idx(info);
                    st.addPropagatedInformation(new PropagatedInformation(iidx, propagated.get(u).get(info), uidx));
                    st.deleteOwnInformation(iidx);
                });
            }
            
            // Indicate the values for discarded information
            if(discarded.containsKey(u))
            {
                discarded.get(u).keySet().forEach(info ->
                {
                    int iidx = this.data.getInformationPiecesIndex().object2idx(info);
                    Set<Integer> creators = discarded.get(u).get(info).v1().stream().map(v -> this.data.getUserIndex().object2idx(v))
                                                     .collect(Collectors.toCollection(HashSet::new));
                    long timestamp = discarded.get(u).get(info).v2();
                    
                    PropagatedInformation propInfo = new PropagatedInformation(iidx, timestamp, creators);
                    st.addDiscardedInformation(propInfo);
                    st.addAllInformation(propInfo);
                });
            }
            
            // Indicate the values for received information
            if(received.containsKey(u))
            {
                received.get(u).keySet().forEach(info ->
                {
                    int iidx = this.data.getInformationPiecesIndex().object2idx(info);
                    Set<Integer> creators = received.get(u).get(info).v1().stream().map(v -> this.data.getUserIndex().object2idx(v))
                                                    .collect(Collectors.toCollection(HashSet::new));
                    long timestamp = received.get(u).get(info).v2();
                    
                    PropagatedInformation propInfo = new PropagatedInformation(iidx, timestamp, creators);
                    st.addReceivedInformation(propInfo);
                    st.addAllInformation(propInfo);                    
                });
            }
            
        });

        return state;
    }

    /**
     * Updates the values for propagated pieces of information.
     * @param iteration the current iteration.
     * @param propagated the set of propagated pieces.
     * @param discarded the set of discarded pieces.
     * @param received the set of received pieces.
     */
    private void updatePropagated(Iteration<U, I, P> iteration, Map<U, Map<I, Long>> propagated, Map<U, Map<I, Tuple2oo<Set<U>, Long>>> discarded, Map<U, Map<I, Tuple2oo<Set<U>, Long>>> received) 
    {
        long iter = iteration.getIterationNumber();
        // Update information concerning propagated pieces of information.
        iteration.getPropagatingUsers().forEach(u -> 
        {
            if(!propagated.containsKey(u))
            {
                propagated.put(u, new HashMap<>());
            }

            iteration.getPropagatedInformation(u).forEach(info -> 
            {
                if(received.containsKey(u) && received.get(u).containsKey(info))
                {
                    received.get(u).remove(info);
                }

                propagated.get(u).put(info, iter);
            });
        });    
    }
    
    /**
     * Updates the values for discarded pieces of information.
     * @param iteration the current iteration.
     * @param propagated the set of propagated pieces.
     * @param discarded the set of discarded pieces.
     * @param received the set of received pieces.
     */
    private void updateDiscarded(Iteration<U, I, P> iteration, Map<U, Map<I, Long>> propagated, Map<U, Map<I, Tuple2oo<Set<U>, Long>>> discarded, Map<U, Map<I, Tuple2oo<Set<U>, Long>>> received) 
    {
        // Update information concerning discarded pieces of information.
        iteration.getDiscardingUsers().forEach(u -> 
        {
            if(!discarded.containsKey(u))
            {
                discarded.put(u, new HashMap<>());
            }

            iteration.getDiscardedInformation(u).forEach(info -> 
            {
                if(received.containsKey(u) && received.get(u).containsKey(info))
                {
                    Set<U> creators = received.get(u).get(info).v1();
                    long time = received.get(u).get(info).v2();
                    discarded.get(u).put(info, new Tuple2oo<>(creators, time));
                    received.get(u).remove(info);   
                }
            });
        });
    }

    /**
     * Updates the values for received pieces of information.
     * @param iteration the current iteration.
     * @param propagated the set of propagated pieces.
     * @param discarded the set of discarded pieces.
     * @param received the set of received pieces.
     * @param upd the update mechanism
     */
    private void updateReceived(Iteration<U, I, P> iteration, Map<U, Map<I, Long>> propagated, Map<U, Map<I, Tuple2oo<Set<U>, Long>>> discarded, Map<U, Map<I, Tuple2oo<Set<U>, Long>>> received, UpdateMechanism upd) 
    {
        long iter = iteration.getIterationNumber();
        // Update information concerning newly received pieces of information.
        iteration.getReceivingUsers().forEach(u -> 
        {
            if(!received.containsKey(u))
            {
                received.put(u, new HashMap<>());
            }

            iteration.getSeenInformation(u).forEach(t -> 
            {
                I info = t.v1();
                Set<U> creators = t.v2();

                received.get(u).put(info, new Tuple2oo<>(creators, iter));
            });
        });

        // Update information concerning old pieces of information received again.
        iteration.getReReceivingUsers().forEach(u -> 
        {
            if(!received.containsKey(u))
            {
                received.put(u, new HashMap<>());
            }

            iteration.getReReceivedInformation(u).forEach(t -> 
            {
                I info = t.v1();
                long numIter = iter;

                int iidx = this.data.getInformationPiecesIndex().object2idx(info);
                Set<Integer> setCreatorsB = t.v2().stream().map(v -> this.data.getUserIndex().object2idx(v))
                                                  .collect(Collectors.toCollection(HashSet::new));

                // If the information was previously discarded...
                if(discarded.get(u) != null && discarded.get(u).containsKey(info))
                {
                    Set<Integer> setCreatorsA = discarded.get(u).get(info).v1()
                                                        .stream().map(v -> this.data.getUserIndex().object2idx(v))
                                                        .collect(Collectors.toCollection(HashSet::new));
                    PropagatedInformation propA = new PropagatedInformation(iidx, received.get(u).get(info).v2(), setCreatorsA);
                    PropagatedInformation propB = new PropagatedInformation(iidx, numIter, setCreatorsB);

                    PropagatedInformation def = upd.updateDiscarded(propA, propB);

                    setCreatorsB = def.getCreators();
                    numIter = def.getTimestamp();
                }

                // If the information was previously received...
                if(received.get(u) != null && received.get(u).containsKey(info))
                {
                    Set<Integer> setCreatorsA = received.get(u).get(info).v1()
                                                        .stream().map(v -> this.data.getUserIndex().object2idx(v))
                                                        .collect(Collectors.toCollection(HashSet::new));
                    PropagatedInformation propA = new PropagatedInformation(iidx, received.get(u).get(info).v2(), setCreatorsA);
                    PropagatedInformation propB = new PropagatedInformation(iidx, numIter, setCreatorsB);

                    PropagatedInformation def = upd.updateSeen(propA, propB);

                    setCreatorsB = def.getCreators();
                    numIter = def.getTimestamp();
                }

                Set<U> creators = setCreatorsB.stream().map(vidx -> this.data.getUserIndex().idx2object(vidx))
                                              .collect(Collectors.toCollection(HashSet::new));
                received.get(u).put(info, new Tuple2oo<>(creators, numIter));
            });
        });    
    }
}
