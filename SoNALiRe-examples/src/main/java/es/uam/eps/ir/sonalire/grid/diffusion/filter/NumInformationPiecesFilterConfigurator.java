/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.diffusion.filter;

import es.uam.eps.ir.sonalire.diffusion.data.filter.DataFilter;
import es.uam.eps.ir.sonalire.diffusion.data.filter.NumInformationPiecesFilter;
import es.uam.eps.ir.sonalire.grid.Parameters;

import java.io.Serializable;

/**
 * Configures filter which only leaves a fixed number of information pieces for each user to
 * propagate.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 * @param <I> type of the information pieces.
 * @param <F> Type of the features of the users / information pieces.
 *
 * @see NumInformationPiecesFilter
 */
public class NumInformationPiecesFilterConfigurator<U extends Serializable,I extends Serializable, F> implements FilterConfigurator<U,I, F>
{
    /**
     * Identifier for the field for retrieving the number of pieces.
     */
    private final static String NUMPIECES = "numPieces";
    
    @Override
    public DataFilter<U, I, F> getFilter(Parameters fgr)
    {
        Integer numTweets = fgr.getIntegerValue(NUMPIECES);
        return new NumInformationPiecesFilter<>(numTweets);
    }
}
