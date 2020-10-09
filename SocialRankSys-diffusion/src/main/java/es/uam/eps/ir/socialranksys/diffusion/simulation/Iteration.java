/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.simulation;

import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Class that represents a single iteration.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public interface Iteration<U,I,P> 
{   
    /* ******************* Updating information pieces information *************/
    
    /**
     * Adds the received pieces of a single user to the iteration.
     * @param u the user identifier.
     * @param seenPieces the list of pieces that this user has newly seen this iteration (and their authors).
     * @return true if everything is OK, false if it is not.
     */
    boolean addReceivingUser(U u, Map<I, Set<U>> seenPieces);
    
    /**
     * Adds the re-received pieces of a single user discarded pieces to the iteration
     * @param u the user identifier.
     * @param rereceivedPieces the list of pieces that this user has re-received this iteration (and their authors).
     * @return true if everything is OK, false if it is not.
     */
    boolean addReReceivingUser(U u, Map<I, Set<U>> rereceivedPieces);
    
    /**
     * Adds the propagated pieces of a single user to the iteration
     * @param u the user identifier.
     * @param propagatedPieces the list of pieces that this user has propagated this iteration.
     * @return true if everything is OK, false if it is not.
     */
    boolean addPropagatingUser(U u, List<I> propagatedPieces);

    /**
     * Adds the discarded pieces of a single user to the iteration
     * @param u the user identifier.
     * @param discardedPieces the list of pieces that this user has discarded this iteration.
     * @return true if everything is OK, false if it is not.
     */
    boolean addDiscardingUser(U u, List<I> discardedPieces);
    
    /* ******************* Getting information *********************************/

    /**
     * Obtains the iteration number.
     * @return the iteration number.
     */
    int getIterationNumber();
    
    /* ******************* Seen information pieces during the iteration*/
    
    /**
     * Obtains the number of users which have received and read new information in this iteration
     * @return the number of users which have received and read new information in this iteration
     */ 
    int getNumReceivingUsers();
    /**
     * Obtains the total of number of information pieces which have been read by the different users in the network
     * for the first time during this iteration.
     * @return the number of newly seen information pieces.
     */    
    int getNumSeen();
    /**
     * Obtains the total number of information pieces which have been read by a user in the network 
     * for the first time during this iteration.
     * @param u the user
     * @return the number of information pieces which have been read by the user for the first time during this iteration.
     */
    int getNumSeen(U u);
    /**
     * Obtains the number of information pieces which have been read by a user in the network (without taking into acount how
     * many times it has been received during this iteration).
     * @return the number of newly seen information pieces (without frequency).
     */
    int getNumUniqueSeen();
    /**
     * Obtains the total number of different information pieces which have been read by a user in the network 
     * for the first time during this iteration.
     * @param u the user
     * @return the number of different information pieces which have been read by the user for the first time during this iteration.
     */
    int getNumUniqueSeen(U u);
    
    /**
     * Obtains the set of users which have received and read, at least, one piece of information which they had not read before.
     * @return a stream containing users which have received and read, at least, one piece of information which they had not read before.
     */
    Stream<U> getReceivingUsers();
    /**
     * Obtains the set of information pieces that a given user has read for the first time during this iteration.
     * @param u the user
     * @return the set of information pieces that the user has read for the first time during this iteration.
     */
    Stream<Tuple2oo<I, Set<U>>> getSeenInformation(U u);

    /* ******************* Previously seen information pieces received during the iteration*/

    /**
     * Obtains the number of users which have received and read information pieces they had read before.
     * @return the number of users which have received and read information pieces they had read before.
     */
    int getNumReReceivingUsers();
    /**
     * Obtains the total number of information pieces which have been read by the different users in the network
     * after they had read them in previous iterations.
     * @return the number of seen information pieces which had been read before.
     */
    int getNumReReceived();
    
    /**
     * Obtains the total number of information pieces which have been seen by a user (considering that the user
     * had read them before).
     * @param u the user
     * @return the total number of information pieces which have been seen by the user (considering that the user
     * had read them before).
     */
    int getNumReReceived(U u);
    /**
     * Obtains the number of information pieces which have been read by a user in the network (without taking into acount how
     * many times it has been received during this iteration), considering that the user had read them before.
     * @return the number of seen information pieces (without frequency) which had been read before.
     */
    int getNumUniqueReReceived();
    /**
     * Obtains the total number of different information pieces which have been read by a user in the network 
     * during this iteration after they were read in a previous iteration.
     * @param u the user
     * @return the number of different information pieces which have been read by the user after they were read in a previous iteration..
     */
    int getNumUniqueReReceived(U u);
    
    
    /**
     * Obtains the set of users which have received and read, at least, one piece of information they had read before.
     * @return the set of users which have received and read, at least, one piece of information they had read before.
     */
    Stream<U> getReReceivingUsers();
    /**
     * Obtains the set of information that a given user read in previous iterations, and have been re-read in this one.
     * @param u the user
     * @return the set of information that the user read in previous iterations, and have been re-read in this one.
     */
    Stream<Tuple2oo<I, Set<U>>> getReReceivedInformation(U u);
    
    /* ******************* Propagated information pieces during the iteration*/
    
    /**
     * Obtains the total number of users which have propagated information pieces.
     * @return the total number of users which have propagated information pieces.
     */
    int getNumPropagatingUsers();
    /**
     * Obtains the total number of propagated information pieces (it does not take into account how many people received them).
     * @return the total number of propagated information pieces (it does not take into account how many people received them).
     */
    int getNumPropagated();
    /**
     * Obtains the total number of information pieces that a given user has propagated during this iteration.
     * It does not take into account how many people received (just how many different pieces there are).
     * @param u the user
     * @return the total number of information pieces that the user has propagated during this iteration.
     */
    int getNumPropagated(U u);
    /**
     * Obtains the set of users which have propagated, at least, one information piece during this iteration.
     * @return the set of users which have propagated, at least, one information piece during this iteration.
     */
    Stream<U> getPropagatingUsers();
    /**
     * Obtains the set of information pieces that a given user has propagated during this iteration.
     * @param u the user
     * @return the set of information pieces that the user has propagated during this iteration.
     */
    Stream<I> getPropagatedInformation(U u);

    /* ******************* Discarded information pieces during the iteration*/

    /**
     * Obtains the total number of users which have discarded information pieces.
     * @return the total number of users which have discarded information pieces.
     */   
    int getNumDiscardingUsers();
    /**
     * Obtains the total number of discarded information pieces.
     * @return the total number of discarded information pieces.
     */
    int getNumDiscarded();
    /**
     * Obtains the total number of information pieces that a given user has discarded during this iteration.
     * @param u the user
     * @return the total number of information pieces that the user has discarded during this iteration.
     */
    int getNumDiscarded(U u);
    /**
     * Obtains the set of users which have discarded, at least, one information piece during this iteration.
     * @return the set of users which have discarded, at least, one information piece during this iteration.
     */
    Stream<U> getDiscardingUsers();
    
    /**
     * Obtains the set of information pieces that a given user has discarded during this iteration.
     * @param u the user
     * @return the set of information pieces that the user has discarded during this iteration.
     */
    Stream<I> getDiscardedInformation(U u);
    
    /* ******************* Other getters*/

    /**
     * Obtains the total number of read information pieces during this iteration.
     * @return the total number of read information pieces during this iteration.
     */
    default int getTotalReceived()
    { 
        return this.getNumSeen() + this.getNumReReceived(); 
    }
    
    /**
     * Obtains the total number of information pieces that a given user has read during this iteration.
     * @param u the user
     * @return the total number of information pieces that a given user has read during this iteration.
     */
    default int getTotalReceived(U u)
    {
        return this.getNumSeen(u) + this.getNumReReceived(u);
    }
}
