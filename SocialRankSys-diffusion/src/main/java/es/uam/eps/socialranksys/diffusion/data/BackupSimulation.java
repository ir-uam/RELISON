/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Representation of the backup of a simulation
 * @author Javier Sanz-Cruzado Puig
 */
public class BackupSimulation
{
    /**
     * The backup of the simulation
     */
    private final List<Map<Integer, List<Integer>>> iterations;
    
    /**
     * Constructor.
     */
    public BackupSimulation()
    {
        this.iterations = new ArrayList<>();
    }
    
    /**
     * Adds a new iteration
     * @param iteration the iteration of the simulation
     */
    public void addIteration(Map<Integer, List<Integer>> iteration)
    {
        this.iterations.add(iteration);
    }
    
    /**
     * Gets an iteration
     * @param iteration the iteration of the simulation
     * @return iterations.
     */
    public Map<Integer, List<Integer>> getIteration(int iteration)
    {
        if(iteration >= 0 && iteration < this.iterations.size())
        {
            return this.iterations.get(iteration);
        }
        else
        {
            return new HashMap<>();
        }
    }
    
    /**
     * Gets a stream of the users that have received a new information piece during a given iteration
     * @param iteration the iteration.
     * @return the users that have received new information pieces during that iteration
     */
    public Stream<Integer> getModifiedUsers(int iteration)
    {
        if(iteration >= 0 && iteration < this.iterations.size())
        {
            return this.iterations.get(iteration).keySet().stream();
        }
        else
        {
            return Stream.empty();
        }
    }
    
    /**
     * Gets a stream with the information pieces received by a single user during an iteration
     * @param iteration the iteration
     * @param user the user
     * @return the new pieces received during the iteration.
     */
    public Stream<Integer> getNewPieces(int iteration, int user)
    {
        if(iteration >= 0 && iteration < this.iterations.size())
        {
            if(this.iterations.get(iteration).containsKey(user))
            {
                return this.iterations.get(iteration).get(user).stream();
            }
        }
        return Stream.empty();
    }
}
