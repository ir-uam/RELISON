/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.simulation;

import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Class for storing the basic information of a simulation iteration.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the features.
 */
public class SimpleIteration<U,I,P> implements Iteration<U,I,P> 
{
    /**
     * Number of the current iteration
     */
    private final int iterationNumber;
    
    /**
     * The set of information pieces observed by each user (and their creators).
     */
    private final Map<U, Map<I, Set<U>>> seenPieces;
    /**
     * The set of re-received information pieces observed by each user (and their creators).
     */
    private final Map<U, Map<I, Set<U>>> rereceivedPieces;
    /**
     * The set of propagated pieces by each user.
     */
    private final Map<U, List<I>> propagatedPieces;
    /**
     * The set of discarded information pieces observed by each user.
     */
    private final Map<U, List<I>> discardedPieces;
    
    /**
     * Sum of the number of different newly observed pieces by each user
     */
    private int numUniqueSeen;
    /**
     * Total newly observed pieces for the whole set of users.
     */
    private int numSeen;
    
    /**
     * Sum of the number of unique re-read information pieces of each user.
     */
    private int numUniqueReReceived;
    /**
     * Total re-read information pieces for the whole set of users.
     */
    private int numReReceived;
    
    /**
     * Number of propagated information pieces
     */
    private int numPropagated;
    /**
     * Number of discarded information pieces
     */
    private int numDiscarded;
    
    /**
     * Number of users which have read, at least, a new information piece.
     */
    private int numReceivingUsers;
    /**
     * Number of users which have read, at least, one information piece which they had read in a previous iteration.
     */
    private int numReReceivingUsers;
    /**
     * Number of users which have propagated, at least, one information piece
     */
    private int numPropagatingUsers;
    /**
     * Number of users which have discarded, at least, one information piece.
     */
    private int numDiscardingUsers;
    
    
    /**
     * Constructor.
     * @param iterationNumber the number of the iteration.
     */
    public SimpleIteration(int iterationNumber)
    {
        this.iterationNumber = iterationNumber;
        seenPieces = new HashMap<>();
        rereceivedPieces = new HashMap<>();
        propagatedPieces = new HashMap<>();
        discardedPieces = new HashMap<>();
        
        numSeen = 0;
        numUniqueSeen = 0;
        numReReceived = 0;
        numUniqueReReceived = 0;
        numPropagated = 0;
        numDiscarded = 0;
        
        numReceivingUsers = 0;
        numReReceivingUsers = 0;
        numPropagatingUsers = 0;
        numDiscardingUsers = 0; 
    }
    
    
    @Override
    public boolean addReceivingUser(U u, Map<I, Set<U>> seenPieces) 
    {
        if(this.seenPieces.containsKey(u) || seenPieces.isEmpty())
        {
            return false;
        }
        else
        {
            ++this.numReceivingUsers;
            this.numUniqueSeen += seenPieces.size();
            this.numSeen += seenPieces.values().stream().mapToInt(Set::size).sum();
            this.seenPieces.put(u, seenPieces);
            return true;
        }
    }

    @Override
    public boolean addReReceivingUser(U u, Map<I, Set<U>> rereceivedPieces) 
    {
        if(this.rereceivedPieces.containsKey(u) || rereceivedPieces.isEmpty())
        {
            return false;
        }
        else
        {
            ++this.numReReceivingUsers;
            this.numUniqueReReceived += rereceivedPieces.size();
            this.numReReceived += rereceivedPieces.values().stream().mapToInt(Set::size).sum();
            this.rereceivedPieces.put(u, rereceivedPieces);
            return true;
        }    
    }

    @Override
    public boolean addPropagatingUser(U u, List<I> propagatedPieces) 
    {
        if(this.propagatedPieces.containsKey(u) || propagatedPieces.isEmpty())
        {
            return false;
        }
        else
        {
            ++this.numPropagatingUsers;
            this.numPropagated += propagatedPieces.size();
            this.propagatedPieces.put(u, propagatedPieces);
            return true;
        }
    }
    
    @Override
    public boolean addDiscardingUser(U u, List<I> discardedPieces) 
    {
        if(this.discardedPieces.containsKey(u) || discardedPieces.isEmpty())
        {
            return false;
        }
        else
        {
            ++this.numDiscardingUsers;
            this.numDiscarded += discardedPieces.size();
            this.discardedPieces.put(u, discardedPieces);
            return true;
        }
    }

    @Override
    public int getIterationNumber() 
    {
        return this.iterationNumber;
    }

    @Override
    public int getNumReceivingUsers() 
    {
        return this.numReceivingUsers;
    }

    @Override
    public int getNumSeen() 
    {
        return this.numSeen;
    }

    @Override
    public int getNumSeen(U u) 
    {
        if(this.seenPieces.containsKey(u))
        {
            return this.seenPieces.get(u).values().stream().mapToInt(Set::size).sum();
        }
        return 0;
    }
    
    @Override
    public int getNumUniqueSeen() 
    {
        return this.numUniqueSeen;
    }

    @Override
    public int getNumUniqueSeen(U u) 
    {
        return (this.seenPieces.containsKey(u) ? this.seenPieces.get(u).size() : 0);
    }

    @Override
    public Stream<U> getReceivingUsers() 
    {
        return this.seenPieces.keySet().stream();
    }

    @Override
    public Stream<Tuple2oo<I, Set<U>>> getSeenInformation(U u) 
    {
        if(this.seenPieces.containsKey(u))
        {
            return this.seenPieces.get(u).entrySet().stream().map(entry -> new Tuple2oo<>(entry.getKey(), entry.getValue()));
        }
        return Stream.empty();
    }

    @Override
    public int getNumReReceivingUsers() 
    {
        return this.numReReceivingUsers;
    }

    @Override
    public int getNumReReceived() 
    {
        return this.numReReceived;
    }

    @Override
    public int getNumReReceived(U u) 
    {
        if(this.rereceivedPieces.containsKey(u))
        {
            return this.rereceivedPieces.get(u).values().stream().mapToInt(Set::size).sum();
        }
        return 0;
    }
    
    @Override
    public int getNumUniqueReReceived() 
    {
        return this.numUniqueReReceived;
    }

    @Override
    public int getNumUniqueReReceived(U u) 
    {
        if(this.rereceivedPieces.containsKey(u))
        {
            return this.rereceivedPieces.get(u).size();
        }
        return 0;
    }
    

    @Override
    public Stream<U> getReReceivingUsers() 
    {
        return this.rereceivedPieces.keySet().stream();
    }

    @Override
    public Stream<Tuple2oo<I, Set<U>>> getReReceivedInformation(U u) 
    {
        if(this.rereceivedPieces.containsKey(u))
        {
            return this.rereceivedPieces.get(u).entrySet().stream().map(entry -> new Tuple2oo<>(entry.getKey(),entry.getValue()));
        }
        return Stream.empty();
    }

    @Override
    public int getNumPropagatingUsers() 
    {
        return this.numPropagatingUsers;
    }

    @Override
    public int getNumPropagated()
    {
        return this.numPropagated;
    }

    @Override
    public int getNumPropagated(U u)
    {
        if(this.propagatedPieces.containsKey(u))
        {
            return this.propagatedPieces.get(u).size();
        }
        return 0;
    }

    @Override
    public Stream<U> getPropagatingUsers() 
    {
        return this.propagatedPieces.keySet().stream();
    }

    @Override
    public Stream<I> getPropagatedInformation(U u) 
    {
        if(this.propagatedPieces.containsKey(u))
        {
            return this.propagatedPieces.get(u).stream();
        }
        return Stream.empty();
    }

    @Override
    public int getNumDiscardingUsers() 
    {
        return this.numDiscardingUsers;
    }

    @Override
    public int getNumDiscarded() 
    {
        return this.numDiscarded;
    }

    @Override
    public int getNumDiscarded(U u) 
    {
        if(this.discardedPieces.containsKey(u))
        {
            return this.discardedPieces.get(u).size();
        }
        return 0;
    }

    @Override
    public Stream<U> getDiscardingUsers()
    {
        return this.discardedPieces.keySet().stream();
    }

    @Override
    public Stream<I> getDiscardedInformation(U u) 
    {
        if(this.discardedPieces.containsKey(u))
        {
            return this.discardedPieces.get(u).stream();
        }
        return Stream.empty();
    }


    
    
}
