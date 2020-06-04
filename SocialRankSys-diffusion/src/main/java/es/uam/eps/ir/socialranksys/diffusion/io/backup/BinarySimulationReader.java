/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.io.backup;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;
import es.uam.eps.ir.socialranksys.diffusion.simulation.Iteration;
import es.uam.eps.ir.socialranksys.diffusion.simulation.SimpleIteration;
import es.uam.eps.ir.socialranksys.diffusion.simulation.Simulation;

import java.io.*;
import java.util.*;

/**
 * Reads a simulation from a file. Data is retrieved using binary fields,
 * which contain the identifiers of each object.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class BinarySimulationReader<U extends Serializable, I extends Serializable, P> implements SimulationReader<U,I,P>
{
    /**
     * Object input stream for reading into file.
     */
    private DataInputStream ois;

    @Override
    public boolean initialize(String file)
    {
        if(file == null || ois != null)
            return false;
        try 
        {
            this.ois = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            return true;
        } 
        catch (IOException ex) 
        {
            return false;
        }
    }
    
    @Override
    public Simulation<U,I,P> readSimulation(Data<U,I,P> data)
    {
        if(ois == null || data == null)
            return null;
        
        try 
        {
            int initial = ois.readInt();
            Simulation<U,I,P> sim = new Simulation<>(data, initial);
            int numIter = ois.readInt();
            for(int i = 0; i < numIter; ++i)
            {
                Iteration<U,I,P> iter = this.readIteration(data);
                if(iter == null)
                {
                    return null;
                }
                sim.addIteration(iter);
            }
            
            return sim;
        } 
        catch (IOException ex) 
        {
            return null;
        }
    }
    

    @Override
    public Iteration<U,I,P> readIteration(Data<U,I,P> data)
    {
        if(ois == null || data == null )
            return null;
        
        try
        {
            // Read the iteration number
            int numIter = ois.readInt(); 
            Iteration<U,I,P> iter = new SimpleIteration<>(numIter);

            // Read the newly received pieces
            Map<U,Map<I,Set<U>>> receptionMap = this.readReceivedPiecesField(data);
            if(receptionMap == null)
            {
                return null;
            }
            else
            {
                receptionMap.forEach(iter::addReceivingUser);
            }
            
            // Read the re-received pieces
            receptionMap = this.readReceivedPiecesField(data);
            if(receptionMap == null)
            {
                return null;
            }
            else
            {
                receptionMap.forEach(iter::addReReceivingUser);
            }
            
            // Read the propagated pieces
            Map<U, List<I>> actionMap = this.readUserActionField(data);
            if(actionMap == null)
            {
                return null;
            }
            else
            {
                actionMap.forEach(iter::addPropagatingUser);
            }
            
            // Read the discarded pieces
            actionMap = this.readUserActionField(data);
            if(actionMap == null)
            {
                return null;
            }
            else
            {
                actionMap.forEach(iter::addDiscardingUser);
            }            
            
            return iter;
        }
        catch(IOException ex)
        {
            return null;
        }
    }
    

    @Override
    public boolean close()
    {
        if(ois != null)
        {
            try 
            {
                ois.close();
                ois = null;
                return true;
            } 
            catch (IOException ex) 
            {
                return false;
            }
        }
        return false;
    }
    
    /**
     * Reads a particular field concerning received data by users.
     * @param data the data.
     * @return a mapping between users and information pieces whole information, null if something went wrong
     */
    private Map<U, Map<I,Set<U>>> readReceivedPiecesField(Data<U,I,P> data)
    {
        Map<U, Map<I, Set<U>>> map = new HashMap<>();
        
        if(ois == null)
        {
            return null;
        }
        else
        {
            try
            {
                // Read the number of users which have received at least one piece on this list
                int numUsers = ois.readInt();
                // For each one of them
                for(int i = 0; i < numUsers; ++i)
                {
                    // Read its identifier
                    int uidx = ois.readInt();
                    U user = data.getUserIndex().idx2object(uidx);
                    
                    // Read the number of received pieces
                    int numPieces = ois.readInt();
                    Map<I, Set<U>> pieces = new HashMap<>();
                    
                    // Retrieve the different pieces
                    for(int j = 0; j < numPieces; ++j)
                    {
                        // Obtain the identifier
                        int iidx = ois.readInt();
                        I piece = data.getInformationPiecesIndex().idx2object(iidx);

                        Set<U> creators = new HashSet<>();
                        // Obtain the number of creators
                        int numCreators = ois.readInt();
                        // Retrieve each creator.
                        for(int k = 0; k < numCreators; ++k)
                        {
                            int vidx = ois.readInt();
                            U creator = data.getUserIndex().idx2object(vidx);
                            creators.add(creator);
                        }
                        
                        pieces.put(piece, creators);
                    }
                    
                    map.put(user, pieces);
                }               
                return map;
            }
            catch(IOException ioe)
            {
                return null;
            }
        } 
    }
    
    
    
    /**
     * Reads a particular field concerning actions made by the users.
     * @param data the data.
     * @return a mapping between users and information pieces, null if something went wrong
     */
    private Map<U, List<I>> readUserActionField(Data<U,I,P> data)
    {
        Map<U, List<I>> map = new HashMap<>();
        
        try
        {
            // Read the number of users that have at least one piece on their list
            int numUsers = ois.readInt();
            // For each one of them:
            for(int i = 0; i < numUsers; ++i)
            {
                // Read its identifier
                int uidx = ois.readInt();
                U user = data.getUserIndex().idx2object(uidx);
                // Read the number of pieces
                int numPieces = ois.readInt();
                List<I> pieces = new ArrayList<>();
                // Retrieve the different pieces.
                for(int j = 0; j < numPieces; ++j)
                {
                    int iidx = ois.readInt();
                    I piece = data.getInformationPiecesIndex().idx2object(iidx);
                    pieces.add(piece);
                }

                map.put(user, pieces);
            }
            return map;
        }
        catch(IOException ioe)
        {
            return null;
        }
    }
}
