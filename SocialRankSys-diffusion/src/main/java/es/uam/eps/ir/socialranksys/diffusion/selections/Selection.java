/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.selections;


import es.uam.eps.ir.socialranksys.diffusion.data.PropagatedInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Selection of information pieces to propagate.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Selection
{
    int numProp;
    /**
     * Selection of own information pieces to propagate.
     */
    private final List<PropagatedInformation> ownSelection;
    /**
     * Selection of received information pieces to propagate
     */
    private final List<PropagatedInformation> propagateSelection;
    /**
     * Selection of already propagated pieces to propagate.
     */
    private final List<PropagatedInformation> repropagateSelection;
    
    /**
     * Constructor.
     * @param ownSelection Selection of own information pieces to propagate.
     * @param propagateSelection Selection of received information pieces to propagate.
     */
    public Selection(List<PropagatedInformation> ownSelection, List<PropagatedInformation> propagateSelection)
    {
        this(ownSelection,propagateSelection,new ArrayList<>());
    }
    
    /**
     * Constructor.
     * @param ownSelection Selection of own information pieces to propagate.
     * @param propagateSelection Selection of received information pieces to propagate.
     * @param repropagateSelection Selection of already propagated pieces to repropagate.
     */
    public Selection(List<PropagatedInformation> ownSelection, List<PropagatedInformation> propagateSelection, List<PropagatedInformation> repropagateSelection)
    {
        this.ownSelection = ownSelection;
        this.propagateSelection = propagateSelection;
        this.repropagateSelection = repropagateSelection;
        this.numProp = ownSelection.size() + propagateSelection.size() + repropagateSelection.size();
    }

    /**
     * Obtains the list of selected information pieces created by the user
     * @return The list of selected information pieces created by the user.
     */
    public Stream<PropagatedInformation> getOwnSelection()
    {
        return ownSelection.stream();
    }

    /**
     * Obtains the list of selected information pieces received by the user
     * @return The list of selected information pieces received by the user.
     */
    public Stream<PropagatedInformation> getPropagateSelection()
    {
        return propagateSelection.stream();
    }
    
    /**
     * Obtains the list of selected information pieces to repropagate.
     * @return The list of selected information pieces to repropagate.
     */
    public Stream<PropagatedInformation> getRepropagateSelection()
    {
        return repropagateSelection.stream();
    }
    
    /**
     * The total number of pieces of information to be propagated/repropagated.
     * @return the number of pieces of information to be propagated/repropagated
     */
    public int numPropagated()
    {
        return this.numProp;
    }
    
    /**
     * Returns the concatenated information of all lists.
     * @return an stream containing all the selected pieces to propagate/repropagate.
     */
    public Stream<PropagatedInformation> getAll()
    {
        List<PropagatedInformation> allList = new ArrayList<>(this.ownSelection);
        allList.addAll(this.propagateSelection);
        allList.addAll(this.repropagateSelection);
        return allList.stream();
    }   
}
