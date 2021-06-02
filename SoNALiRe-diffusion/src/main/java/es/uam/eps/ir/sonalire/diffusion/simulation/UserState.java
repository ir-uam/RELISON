/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.simulation;

import es.uam.eps.ir.sonalire.diffusion.data.Information;
import es.uam.eps.ir.sonalire.diffusion.data.PropagatedInformation;
import es.uam.eps.ir.sonalire.diffusion.update.UpdateMechanism;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract representation for the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the user identifier.
 */
public abstract class UserState<U> implements Serializable, Cloneable
{
    /**
     * Identifier of the user.
     */
    private final U userId;
    
    /**
     * Constructor.
     * @param userId identifier of the user. 
     */
    public UserState(U userId)
    {
        this.userId = userId;
    }
    
    
    //GETTERS
    
    /**
     * Obtains the identifier of the user.
     * @return the identifier.
     */
    public U getUserId()
    {
        return this.userId;
    }
    
    /**
     * Adds a piece of information to the own created information pieces.
     * @param info Information to add.
     * @return true if everything is OK.
     */
    protected abstract boolean addOwnInformation(PropagatedInformation info);
    
    /**
     * Adds a piece of information to the previously received information pieces.
     * This information can only be added if it was previously on the newly received
     * information data. This method also deletes the data from that list.
     * @param info Information to add.
     * @return true if everything is OK.
     */
    protected abstract boolean addReceivedInformation(PropagatedInformation info);
    
    /**
     * Adds a piece of information to the newly seen information pieces.
     * @param info Information to add.
     * @return true if everything is OK.
     */
    protected abstract boolean addSeenInformation(PropagatedInformation info);   
    /**
     * Adds a piece of information to the discarded information pieces. This information
     * can only be
     * @param info Information to add.
     * @return true if everything is OK.
     */
    protected abstract boolean addDiscardedInformation(PropagatedInformation info);
    
    /**
     * Adds a piece of information to the propagated information pieces.
     * @param info Information to add.
     * @return true if everything is OK.
     */
    protected abstract boolean addPropagatedInformation(PropagatedInformation info);
    
    /**
     * Adds a piece of information to all the received information pieces.
     * @param info Information to add.
     * @return true if everything is OK.
     */
    protected abstract boolean addAllInformation(PropagatedInformation info);
    
    /**
     * Gets an individual piece of own created information.
     * @param info The identifier of the information.
     * @return The corresponding piece of information.
     */
    protected abstract PropagatedInformation getOwnInformation(int info);
    
    /**
     * Gets an individual piece of received information.
     * @param info The iodentifier of the information.
     * @return The correspondiing piede of information.
     */
    protected abstract PropagatedInformation getReceivedInformation(int info);
    
    /**
     * Gets an individual piece of newly seen created information
     * @param info The identifier of the information
     * @return The corresponding piece of information.
     */
    protected abstract PropagatedInformation getSeenInformation(int info);   
    /**
     * Gets an individual piece of propagated information.
     * @param info The identifier of the propagated information
     * @return The corresponding piece of information.
     */
    protected abstract PropagatedInformation getPropagatedInformation(int info);
    
    /**
     * Gets an individual piece of discarded information.
     * @param info The identifier of the discarded information.
     * @return The corresponding piece of information.
     */
    protected abstract PropagatedInformation getDiscardedInformation(int info);
    
    /**
     * Gets an individual piece of the set of all previously received information
     * @param info The identifier of the discarded information.
     * @return The corresponding piece of information.
     */
    protected abstract PropagatedInformation getAllInformation(int info);
    
    /**
     * Gets an individual piece of own created information.
     * @param info The identifier of the information.
     * @return The corresponding piece of information.
     */
    public abstract boolean containsOwnInformation(int info);
    
    /**
     * Gets an individual piece of received information.
     * @param info The iodentifier of the information.
     * @return The correspondiing piede of information.
     */
    public abstract boolean containsReceivedInformation(int info);
    
    /**
     * Gets an individual piece of newly seen information.
     * @param info The identifier of the information.
     * @return The corresponding piece of information.
     */
    public abstract boolean containsSeenInformation(int info);
    
    /**
     * Gets an individual piece of propagated information.
     * @param info The identifier of the propagated information.
     * @return The corresponding piece of information.
     */
    public abstract boolean containsPropagatedInformation(int info);
    /**
     * Gets an individual piece of discarded information.
     * @param info The identifier of the discarded information.
     * @return The corresponding piece of information.
     */
    public abstract boolean containsDiscardedInformation(int info);
    
    /**
     * Checks if an individual piece of all the previous information exists.
     * @param info The identifier of the piece of information.
     * @return true if exists, false if not.
     */
    public abstract boolean containsAllInformation(int info);
    
    /**
     * Deletes a piece of own created information.
     * @param info The identifier of the own created information.
     * @return true if the information is deleted, false if not.
     */
    protected abstract boolean deleteOwnInformation(int info);
    /**
     * Deletes a piece of received information.
     * @param info The identifier of the received information
     * @return true if the information is deleted, false if not.
     */
    protected abstract boolean deleteReceivedInformation(int info);
    
    /**
     * Deletes a piece of newly seen information.
     * @param info The identifier of the newly received information.
     * @return true if the information is deleted, false if not. 
     */
    protected abstract boolean deleteSeenInformation(int info);
       
    /**
     * Deletes a piece of propagated information.
     * @param info The identifier of the propagter information.
     * @return true if the information is deleted, false if not.
     */
    protected abstract boolean deletePropagatedInformation(int info);
    /**
     * Deletes a piece of discarded information.
     * @param info The identifier of the discarded information.
     * @return true if the information is deleted, false if not.
     */
    protected abstract boolean deleteDiscardedInformation(int info);
        
    
    /**
     * Resets the list of own information.
     * @param information The new own information.
     */
    public void resetOwnInformation(Stream<PropagatedInformation> information)
    {
        this.clear();
        information.forEach(this::addOwnInformation);
    }
    /**
     * Moves the valid elements from the new set of pieces of information to the received one. 
     * @param update The update mechanism.
     */
    public void updateSeenToReceived(UpdateMechanism update)
    {
        
        this.getSeenInformation().forEach((info) ->
        {
            
            if(!this.containsPropagatedInformation(info.getInfoId()))
            {   
                PropagatedInformation updInfo = info;
                if(this.containsDiscardedInformation(info.getInfoId()))
                    updInfo = update.updateDiscarded(this.getDiscardedInformation(info.getInfoId()), info);
                
                if(updInfo != null && this.containsReceivedInformation(updInfo.getInfoId()))
                    this.addReceivedInformation(update.updateSeen(this.getReceivedInformation(updInfo.getInfoId()), updInfo));
                else
                    this.addReceivedInformation(updInfo);
            }
        });
        
        this.clearSeenInformation();
    }
    /**
     * Updates an individual seen piece of information to received.
     * @param info the information piece.
     * @param update the update mechanism.
     */
    public void updateSeenToReceived(int info, UpdateMechanism update) 
    {
        if(this.containsSeenInformation(info) && !this.containsPropagatedInformation(info))
        {
            PropagatedInformation updInfo = this.getSeenInformation(info);
            if(this.containsDiscardedInformation(info))
            {
                updInfo = update.updateDiscarded(this.getDiscardedInformation(info), updInfo);
            }
            
            if(updInfo != null && this.containsReceivedInformation(updInfo.getInfoId()))
            {
                this.addReceivedInformation(update.updateSeen(this.getReceivedInformation(updInfo.getInfoId()), updInfo));
            }
            else
            {
                this.addReceivedInformation(updInfo);
            }
        }
    }
    
    /**
     * Moves some received information to propagated information.
     * @param toPropagate Information to propagate.
     */
    public void updateReceivedToPropagated(Stream<Integer> toPropagate)
    {
        toPropagate.forEach(infoId ->{
            if(this.containsReceivedInformation(infoId))
            {
                this.addPropagatedInformation(this.getReceivedInformation(infoId));
                this.deleteReceivedInformation(infoId);
            }
        });
    }
    
    /**
     * Moves some own created information to propagated information.
     * @param toPropagate Information to propagate.
     */
    public void updateOwnToPropagated(Stream<Integer> toPropagate)
    {
        toPropagate.forEach(infoId ->{
            if(this.containsOwnInformation(infoId))
            {
                this.addPropagatedInformation(this.getOwnInformation(infoId));
                this.deleteOwnInformation(infoId);
            }
        });
    }
    
    /**
     * Adds and updates new information pieces to the new list.
     * @param information A stream with all the information pieces to add.
     */
    public void updateSeen(Stream<PropagatedInformation> information)
    {
        information.forEach(info -> {
            this.updateSeen(info);
            this.updateAll(info);
        });
    }
    
    public void discardReceivedInformation(Stream<Integer> information)
    {
        List<Integer> list = information.collect(Collectors.toCollection(ArrayList::new));
        for(Integer idx : list)
        {
            if(this.containsReceivedInformation(idx))
            {
                this.addDiscardedInformation(this.getReceivedInformation(idx));
                this.deleteReceivedInformation(idx);  
            }
        }
        /*
        information.forEach(info -> {
           if(this.containsReceivedInformation(info))
           {
               this.addDiscardedInformation(this.getReceivedInformation(info));
               this.deleteReceivedInformation(info);
           }
        });*/
    }
    
    /**
     * Adds and updates new information pieces to the new list. If it is own information or already propagated information, it is
     * not added.
     * @param info An information piece to add.
     */
    public void updateSeen(PropagatedInformation info)
    {
        if(!this.containsOwnInformation(info.getInfoId()) && !this.containsPropagatedInformation(info.getInfoId()))
        {   
            if(this.containsSeenInformation(info.getInfoId()))
                this.addSeenInformation(this.getSeenInformation(info.getInfoId()).update(info));
            else
                this.addSeenInformation(info);
       }
        
       updateAll(info);
    }
    
    /**
     * Adds and updates new information pieces to the list of all previously contained information.
     * @param info An information piece to add.
     */
    public void updateAll(PropagatedInformation info)
    {
        if(!this.containsOwnInformation(info.getInfoId()) && !this.containsPropagatedInformation(info.getInfoId()))
        {
            if(this.containsAllInformation(info.getInfoId()))
                this.addAllInformation(this.getAllInformation(info.getInfoId()).update(info));
            else
                this.addAllInformation(info);
        }
    }
 
    /**
     * Gets all the identifiers of the information created by this user. Every information contained in
     * this list can be sent.
     * @return The stream.
     */
    public Stream<Integer> getOwnInformationIds()
    {
        return this.getOwnInformation().map(Information::getInfoId);
    }
    
    /**
     * Gets all the identifiers of the information received by this user. Every information contained in
     * this list can be sent.
     * @return The stream.
     */
    public Stream<Integer> getReceivedInformationIds()
    {
        return this.getReceivedInformation().map(Information::getInfoId);
    }
    
    /**
     * Gets all the information previously propagated by this user.
     * @return A stream with all the information identifiers.
     */
    public Stream<Integer> getPropagatedInformationIds()
    {
        return this.getPropagatedInformation().map(Information::getInfoId);
    }
    
    /**
     * Gets all the information previously discarded by this user.
     * @return A stream with all the discarded information identifiers.
     */
    public Stream<Integer> getDiscardedInformationIds()
    {
        return this.getDiscardedInformation().map(Information::getInfoId);
    }
    
    /**
     * Gets all the new information seen by this user.
     * @return A stream with all the identifiers.
     */
    public Stream<Integer> getSeenInformationIds()
    {
        return this.getSeenInformation().map(Information::getInfoId);
    }
    
    /**
     * Gets all the information that the user has received along time
     * @return A stream with all the identifiers.
     */
    public Stream<Integer> getAllInformationIds()
    {
        return this.getAllInformation().map(Information::getInfoId);
    }
    /**
     * Gets all the information created by this user. Every tweet contained in
     * this list can be sent.
     * @return The stream.
     */
    public abstract Stream<PropagatedInformation> getOwnInformation();
    
    /**
     * Gets all the information received by this user. Every tweet contained in this 
     * list can be retweeted.
     * @return A stream with all the information identifiers.
     */
    public abstract Stream<PropagatedInformation> getReceivedInformation();
    
    /**
     * Gets all the information previously propagated by this user.
     * @return A stream with all the information identifiers.
     */
    public abstract Stream<PropagatedInformation> getPropagatedInformation();
    
    /**
     * Gets all the information previously discarded by this user.
     * @return A stream with all the discarded information identifiers.
     */
    public abstract Stream<PropagatedInformation> getDiscardedInformation();
    
    /**
     * Gets all the new information received by this user.
     * @return A stream with all the identifiers.
     */
    public abstract Stream<PropagatedInformation> getSeenInformation();
    
    /**
     * Gets all the information that the user has received during the simulations
     * @return A stream with all the information.
     */
    public abstract Stream<PropagatedInformation> getAllInformation();
    
    @Override
    public boolean equals(Object object)
    {
        if(object.getClass() != this.getClass())
            return false;
        
        UserState<U> user = (UserState<U>) object;
        return user.userId.equals(this.userId);
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 79 * hash + Objects.hashCode(this.userId);
        return hash;
    }

    /**
     * Clears all the data for the user.
     */
    protected abstract void clear();
    
    /**
     * Clears the seen information.
     */
    protected abstract void clearSeenInformation();
    
    @Override
    public UserState<U> clone() throws CloneNotSupportedException
    {
        return (UserState<U>) super.clone();
    }

    
}
