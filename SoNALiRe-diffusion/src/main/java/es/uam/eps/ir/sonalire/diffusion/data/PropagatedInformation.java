/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.data;

import java.io.Serializable;
import java.util.*;

/**
 * Class that represents the propagated information in a simulation.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class PropagatedInformation extends Information<Integer> implements Serializable, Cloneable
{   
    /**
     * Creators of the information / Users that have made this information arrive to a given user.
     */
    private Set<Integer> creators;
    /**
     * Number of times the information has arrived to an user.
     */
    private int times;

    /**
     * Propagated information constructor.
     * @param infoId The identifier of the original piece of information.
     * @param timestamp The time of arrival of this piece.
     * @param creator The user that propagated this in last place.
     */
    public PropagatedInformation(Integer infoId, long timestamp, int creator)
    {    
        this(infoId, timestamp, new int[]{creator});
    }
    
    /**
     * Constructor.
     * @param infoId Identifier of the information.
     * @param timestamp Time of arrival.
     * @param creators Array containing the id. of the users that propagated this in last place.
     */
    public PropagatedInformation(Integer infoId, long timestamp, int[] creators)
    {
        super(infoId, timestamp);
        this.creators = new HashSet<>();
        for(Integer creator : creators)
        {
            if(creator != null)
            {
                this.creators.add(creator);
            }
        }
            
            
        this.times = creators.length;
    }
    
    /**
     * Constructor.
     * @param infoId Identifier of the information.
     * @param timestamp Time of the arrival.
     * @param creators List containing the id. of the user that propagated this in last place.
     */
    public PropagatedInformation(Integer infoId, long timestamp, Collection<Integer> creators)
    {
        super(infoId, timestamp);
        this.creators = new HashSet<>();
        this.creators.addAll(creators);
        this.times = creators.size();
    }
    
    /**
     * Updates the propagated information.
     * @param info new data.
     * @return the updated propagated information.
     */
    public PropagatedInformation update(PropagatedInformation info)
    {
        Set<Integer> creatorList = new HashSet<>(this.creators);
        
        for(Integer creator : info.creators)
        {
            if(creator != null && !this.creators.contains(creator))
            {
                creatorList.add(creator);
            }
        }
        this.creators = creatorList;
        this.times = this.creators.size();

        return this;
    }
   
    /**
     * Obtains the list of propagators.
     * @return The list of users that propagated this info in last place.
     */
    public Set<Integer> getCreators()
    {
        return creators;
    }

    /**
     * Sets the list of propagators.
     * @param creators The new list.
     */
    public void setCreator(List<Integer> creators)
    {
        this.creators = new HashSet<>();
        this.creators.addAll(creators);
        this.times = creators.size();
    }

    /**
     * Obtains the number of times this information has arrived to the user.
     * @return the number of times this information has arrived to the user.
     */
    public int getTimes()
    {
        return times;
    }

    /**
     * Modifies the set of times a piece of information has arrived to the user.
     * @param times The number of times a piece of information has arrived to the user.
     */
    public void setTimes(int times)
    {
        this.times = times;
    }
    
    @Override
    public PropagatedInformation clone() throws CloneNotSupportedException
    {
        PropagatedInformation pr = (PropagatedInformation) super.clone();

        List<Integer> listCreators = new ArrayList<>(creators);
        
        pr.setCreator(listCreators);
        return pr;
    }
}
