/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.io.backup;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;
import es.uam.eps.ir.socialranksys.diffusion.simulation.Iteration;
import es.uam.eps.ir.socialranksys.diffusion.simulation.Simulation;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Writes a simulation into a file. Data is stored in binary fields,
 * storing the identifiers of each object.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class BinarySimulationWriter<U extends Serializable, I extends Serializable, P> implements SimulationWriter<U,I,P>
{
    /**
     * Object output stream for writing into file
     */
    private DataOutputStream oos;

    /**
     * Constructor.
     */
    public BinarySimulationWriter()
    {
        oos = null;
    }
    
    @Override
    public boolean initialize(String file)
    {
        if(file == null || oos != null)
            return false;
        try 
        {
            File f = new File(file);
            if(f.exists())
            {
                f.delete();
            }
            this.oos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
            return true;
        } 
        catch (IOException ex) 
        {
            return false;
        }
    }
    
    @Override
    public boolean writeSimulation(Simulation<U,I,P> simulation)
    {
        if(simulation == null || this.oos == null)
            return false;
        
        try
        {
            int initial = simulation.getInitialNumber();
            oos.writeInt(initial);
            int numIter = simulation.getNumIterations();
            // Write the number of iterations
            oos.writeInt(numIter);
            boolean outcome = true;
            
            // Write each iteration
            for(int i = initial; i < (numIter + initial); ++i)
            {
                outcome = outcome && this.writeIteration(simulation, i);
            }
            return outcome;
        }
        catch(IOException exception)
        {
            return false;
        }
        
    }
    
    @Override
    public boolean writeIteration(Simulation<U,I,P> simulation, int numIter)
    {
        if(this.oos == null || simulation == null || numIter < simulation.getInitialNumber() || numIter > (simulation.getNumIterations()+simulation.getInitialNumber()))
        {
            return false;
        }
        try
        {
            Data<U,I,P> data = simulation.getData();
            Iteration<U,I,P> iteration = simulation.getIteration(numIter);
            if(iteration == null)
            {
                return false;
            }
            
            // Write the iteration number
            oos.writeInt(iteration.getIterationNumber());
            
            /* **************** Users that receive new information pieces ********************/
            
            // Write the number of users that receive at least one new information piece.
            int numUsers = iteration.getNumReceivingUsers();
            oos.writeInt(numUsers);
            
            // Write the information about the newly received pieces of information.
            if(numUsers > 0)
            {
                List<U> users = iteration.getReceivingUsers().collect(Collectors.toCollection(ArrayList::new));
                // For each user which has received at least a new piece...
                for(U u : users)
                {
                    // Get the pieces received by the user
                    Map<I, Set<U>> infoPieces = new HashMap<>();
                    iteration.getSeenInformation(u).forEach(p -> infoPieces.put(p.v1(), p.v2()));
                    
                    if(!this.writeReceivedDataUser(data, u, infoPieces))
                    {
                        return false;
                    }                
                }
            }
            
            /* **************** Users that receive previously received information pieces ********************/
            
            // Write the number of users that receive at least one previously received information piece.
            numUsers = iteration.getNumReReceivingUsers();
            oos.writeInt(numUsers);
            
            // Write the information about the previously received pieces of information.
            if(numUsers > 0)
            {
                List<U> users = iteration.getReReceivingUsers().collect(Collectors.toCollection(ArrayList::new));
                // For each user which has received at least a previously received piece...
                for(U u : users)
                {
                    // Get the pieces received by the user
                    Map<I, Set<U>> infoPieces = new HashMap<>();
                    iteration.getReReceivedInformation(u).forEach(p -> infoPieces.put(p.v1(), p.v2()));
                    
                    if(!this.writeReceivedDataUser(data, u, infoPieces))
                    {
                        return false;
                    }
                }
            }
            
            /* **************** Users that propagate information pieces ********************/
            
            // Write the number of users that propagate at least one new information piece.
            numUsers = iteration.getNumPropagatingUsers();
            oos.writeInt(numUsers);
            
            // Write the information about the propagated pieces of information.
            if(numUsers > 0)
            {
                List<U> users = iteration.getPropagatingUsers().collect(Collectors.toCollection(ArrayList::new));
                // For each user which has propagated at least a new piece...
                for(U u : users)
                {
                    // Get the pieces propagated by the user
                    List<I> infoPieces = iteration.getPropagatedInformation(u).collect(Collectors.toCollection(ArrayList::new));
                    if(!this.writeUserActions(data, u, infoPieces))
                    {
                        return false;
                    }
                }
            }            
            
            /* **************** Users that discard information pieces ********************/
            
            // Write the number of users that discarded at least one information piece.
            numUsers = iteration.getNumDiscardingUsers();
            oos.writeInt(numUsers);
            
            // Write the information about the discarded pieces of information.
            if(numUsers > 0)
            {
                List<U> users = iteration.getDiscardingUsers().collect(Collectors.toCollection(ArrayList::new));
                // For each user which has discarded at least a piece...
                for(U u : users)
                {
                    // Get the pieces discarded by the user
                    List<I> infoPieces = iteration.getDiscardedInformation(u).collect(Collectors.toCollection(ArrayList::new));
                    if(!this.writeUserActions(data, u, infoPieces))
                    {
                        return false;
                    }
                }
            }            
            
            return true;
        } 
        catch (IOException ex) 
        {
            return false;
        }
    }
    
    /**
     * Given a single user, and the data he has received, writes it to a binary file
     * @param data the full data.
     * @param u the user to write
     * @param infoPieces the set of received information pieces
     * @return true if everything went OK, false if not.
     */
    private boolean writeReceivedDataUser(Data<U,I,P> data, U u, Map<I,Set<U>> infoPieces)
    {
        try
        {
            int uidx = data.getUserIndex().object2idx(u);
            // Write the id. of the user
            oos.writeInt(uidx);
            // Write the number of received pieces.
            oos.writeInt(infoPieces.size());
            // Write the information for each piece
            for(I i : infoPieces.keySet())
            {
                // Write the identifier of each piece.
                int iidx = data.getInformationPiecesIndex().object2idx(i);
                oos.writeInt(iidx);

                // Write the creators
                Set<U> creators = infoPieces.get(i);
                // Write the number of creators
                oos.writeInt(creators.size());
                for(U v : creators)
                {
                    int vidx = data.getUserIndex().object2idx(v);
                    // Write the identifier of each creator.
                    oos.writeInt(vidx);
                }
            }          
            return true;
        }
        catch(IOException ex)
        {
            return false;
        }
    }
    
    /**
     * Given a single user, writes about the information pieces the user has interacted with
     * (propagated or discarded) into a binary file.
     * @param data the full data.
     * @param u the user
     * @param infoPieces the list of interaction pieces the user has interacted with
     * @return true if everything went OK, false otherwise.
     */
    private boolean writeUserActions(Data<U,I,P> data, U u, List<I> infoPieces)
    {
        try
        {
            int uidx = data.getUserIndex().object2idx(u);
            oos.writeInt(uidx);
            // Write the number of propagated pieces.
            oos.writeInt(infoPieces.size());
            // For each piece, write its identifier.
            for(I i : infoPieces)
            {
                int iidx = data.getInformationPiecesIndex().object2idx(i);
                oos.writeInt(iidx);
            }
            
            return true;
        }
        catch(IOException ex)
        {
            return false;
        }
                            
    }
    
    @Override
    public boolean close()
    {
        if(oos != null)
        {
            try 
            {
                oos.close();
                oos = null;
                return true;
            } 
            catch (IOException ex) 
            {
                return false;
            }
        }
        return false;
    }
}
