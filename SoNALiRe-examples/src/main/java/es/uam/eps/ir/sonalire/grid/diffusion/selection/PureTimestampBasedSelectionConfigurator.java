/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.diffusion.selection;

import es.uam.eps.ir.sonalire.diffusion.selections.PureTimestampBasedSelectionMechanism;
import es.uam.eps.ir.sonalire.diffusion.selections.SelectionMechanism;
import es.uam.eps.ir.sonalire.grid.Parameters;

import java.io.Serializable;

/**
 * Configures a selection mechanism that only propagates and repropagates information
 * when the timestamp of the simulation corresponds to the real one.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 *
 * @see PureTimestampBasedSelectionMechanism
 */
public class PureTimestampBasedSelectionConfigurator<U extends Serializable,I extends Serializable, F> implements SelectionConfigurator<U,I, F>
{
    @Override
    public SelectionMechanism<U,I, F> configure(Parameters params)
    {
        return new PureTimestampBasedSelectionMechanism<>();
    }
}
