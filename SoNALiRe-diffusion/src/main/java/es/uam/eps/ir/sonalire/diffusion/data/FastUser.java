/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.data;

import es.uam.eps.ir.sonalire.diffusion.simulation.UserState;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Fast implementation for users. To improve performance, only information identifiers are
 * stored for the own information, the propagated information and the discarded 
 * information.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 */
public class FastUser<U> extends UserState<U> implements Serializable, Cloneable
{
    /**
     * Map that contains the information created by this user.
     */
    private Map<Integer, PropagatedInformation> ownInfo;
    /**
     * Map that contains the previously received information that has not been 
     * propagated or discarded.
     */
    private Map<Integer, PropagatedInformation> receivedInfo;
    /**
     * Map that contains the information propagated by this user.
     */
    private Map<Integer, PropagatedInformation> propagatedInfo;
    /**
     * Map that contains the identifiers of the information discarded by this user.
     */
    private Map<Integer, PropagatedInformation> discardedInfo;
    /**
     * Map of the newly seen information.
     */
    private Map<Integer, PropagatedInformation> seenInfo;
    /**
     * Map containing all the previously received information.
     */
    private Map<Integer, PropagatedInformation> allInfo;
   
    /**
     * Constructor.
     * @param userId Identifier of the user. 
     */
    public FastUser(U userId)
    {
        super(userId);
        this.ownInfo = new HashMap<>();
        this.receivedInfo = new HashMap<>();
        this.propagatedInfo = new HashMap<>();
        this.discardedInfo = new HashMap<>();
        this.seenInfo = new HashMap<>();
        this.allInfo = new HashMap<>();
    }

    
    // GETTERS
    @Override
    public boolean containsOwnInformation(int info)
    {
        return this.ownInfo.containsKey(info);
    }
    
    @Override
    protected PropagatedInformation getOwnInformation(int info)
    {
        return this.ownInfo.get(info);
    }
    
    @Override
    public Stream<PropagatedInformation> getOwnInformation()
    {
        return this.ownInfo.values().stream();
    }
    
    
    @Override
    public boolean containsReceivedInformation(int info)
    {
        return this.receivedInfo.containsKey(info);
    }
    
    @Override   
    protected PropagatedInformation getReceivedInformation(int info)
    {
        return this.receivedInfo.get(info);
    }
    
    @Override
    public Stream<PropagatedInformation> getReceivedInformation()
    {
        return this.receivedInfo.values().stream();
    }
    
    @Override
    public boolean containsSeenInformation(int info)
    {
        return this.seenInfo.containsKey(info);
    }
    
    @Override
    protected PropagatedInformation getSeenInformation(int info)
    {
        return this.seenInfo.get(info);
    }
    
    @Override
    public Stream<PropagatedInformation> getSeenInformation()
    {
        return this.seenInfo.values().stream();
    }
   
    @Override
    public boolean containsPropagatedInformation(int info)
    {
        return this.propagatedInfo.containsKey(info);
    }
    
    @Override
    protected PropagatedInformation getPropagatedInformation(int info)
    {
        return this.propagatedInfo.get(info);
    }
    
    @Override
    public Stream<PropagatedInformation> getPropagatedInformation()
    {
        return this.propagatedInfo.values().stream();
    }
    
    @Override
    public boolean containsDiscardedInformation(int info)
    {
        return this.discardedInfo.containsKey(info);
    }
    
    @Override
    protected PropagatedInformation getDiscardedInformation(int info)
    {
        return this.discardedInfo.get(info);
    }
    
    @Override
    public Stream<PropagatedInformation> getDiscardedInformation()
    {
        return this.discardedInfo.values().stream();
    }
    // ADDS
    
    @Override
    protected boolean addOwnInformation(PropagatedInformation info)
    {
        this.ownInfo.put(info.getInfoId(), info);
        return true;
    }

    @Override
    protected boolean addReceivedInformation(PropagatedInformation info)
    {
        this.receivedInfo.put(info.getInfoId(), info);
        return true;
    }

    @Override
    protected boolean addSeenInformation(PropagatedInformation info)
    {
        PropagatedInformation auxInfo = info;
        if(this.seenInfo.containsKey(info.getInfoId()))
        {
            List<Integer> authors = new ArrayList<>();
            authors.addAll(this.seenInfo.get(info.getInfoId()).getCreators());
            authors.addAll(info.getCreators());
            auxInfo = new PropagatedInformation(info.getInfoId(), info.getTimestamp(), authors);
            
        }
        this.seenInfo.put(auxInfo.getInfoId(), auxInfo);
        return true;
    }
    
    @Override
    protected boolean addDiscardedInformation(PropagatedInformation info)
    {
        this.discardedInfo.put(info.getInfoId(), info);
        return true;
    }

    @Override
    protected boolean addPropagatedInformation(PropagatedInformation info)
    {
        this.propagatedInfo.put(info.getInfoId(), info);
        return true;
    }

    // DELETES
    
    @Override
    protected boolean deleteOwnInformation(int info)
    {
        if(ownInfo.containsKey(info))
        {
            ownInfo.remove(info);
            return true;
        }
        return false;
    }
    
    @Override
    protected boolean deleteReceivedInformation(int info)
    {
        if(receivedInfo.containsKey(info))
        {
            receivedInfo.remove(info);
            return true;
        }
        return false;
    }
    
    @Override
    protected boolean deleteSeenInformation(int info)
    {
        if(seenInfo.containsKey(info))
        {
            seenInfo.remove(info);
            return true;
        }
        return false;
    }
    
    @Override
    protected boolean deletePropagatedInformation(int info)
    {
        if(propagatedInfo.containsKey(info))
        {
            propagatedInfo.remove(info);
            return true;
        }
        return false;
    }
    
    @Override
    protected boolean deleteDiscardedInformation(int info)
    {
        if(discardedInfo.containsKey(info))
        {
            discardedInfo.remove(info);
            return true;
        }
        return false;
    }

    @Override
    protected boolean addAllInformation(PropagatedInformation info)
    {
        this.allInfo.put(info.getInfoId(), info);
        return true;
    }

    @Override
    protected PropagatedInformation getAllInformation(int info)
    {
        return this.allInfo.get(info);
    }

    @Override
    public boolean containsAllInformation(int info)
    {
        return this.allInfo.containsKey(info);
    }

    @Override
    public Stream<PropagatedInformation> getAllInformation()
    {
        return this.allInfo.values().stream();
    }
    
    @Override
    public boolean equals(Object obj)
    {
        if(this.getClass() == obj.getClass())
        {
            FastUser<U> user = (FastUser<U>) obj;
            return user.getUserId().equals(this.getUserId());
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 17 * hash + Objects.hashCode(this.getUserId());
        return hash;
    }

    @Override
    protected void clearSeenInformation()
    {
        this.seenInfo.clear();
       // this.newInfo.clear();
    }

    @Override
    protected void clear()
    {
        this.allInfo.clear();
        this.discardedInfo.clear();
        this.seenInfo.clear();
        this.propagatedInfo.clear();
        this.receivedInfo.clear();
        this.ownInfo.clear();
    }
    
    @Override
    public FastUser<U> clone() throws CloneNotSupportedException
    {
        FastUser<U> fastUser = (FastUser<U>) super.clone();
        
        fastUser.allInfo = new HashMap<>();
        this.allInfo.forEach((key, value) ->
        {
            try
            {
                fastUser.allInfo.put(key, value.clone());
            }
            catch (CloneNotSupportedException ex)
            {
                Logger.getLogger(FastUser.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        
        fastUser.discardedInfo = new HashMap<>();
        this.discardedInfo.forEach((key, value) ->
        {
            try
            {
                fastUser.discardedInfo.put(key, value.clone());
            }
            catch (CloneNotSupportedException ex)
            {
                Logger.getLogger(FastUser.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        
        fastUser.seenInfo = new HashMap<>();
        this.seenInfo.forEach((key, value) ->
        {
            try
            {
                fastUser.seenInfo.put(key, value.clone());
            }
            catch (CloneNotSupportedException ex)
            {
                Logger.getLogger(FastUser.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        
        fastUser.receivedInfo = new HashMap<>();
        this.receivedInfo.forEach((key, value) ->
        {
            try
            {
                fastUser.seenInfo.put(key, value.clone());
            }
            catch (CloneNotSupportedException ex)
            {
                Logger.getLogger(FastUser.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        
        fastUser.propagatedInfo = new HashMap<>();
        this.propagatedInfo.forEach((key, value) ->
        {
            try
            {
                fastUser.propagatedInfo.put(key, value.clone());
            }
            catch (CloneNotSupportedException ex)
            {
                Logger.getLogger(FastUser.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        
        fastUser.ownInfo = new HashMap<>();
        this.ownInfo.forEach((key, value) ->
        {
            try
            {
                fastUser.ownInfo.put(key, value.clone());
            }
            catch (CloneNotSupportedException ex)
            {
                Logger.getLogger(FastUser.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        return fastUser;
    }
}
