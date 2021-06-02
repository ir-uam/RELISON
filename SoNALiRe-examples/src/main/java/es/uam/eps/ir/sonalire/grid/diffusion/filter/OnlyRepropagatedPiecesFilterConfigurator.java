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
import es.uam.eps.ir.sonalire.diffusion.data.filter.OnlyRepropagatedPiecesFilter;
import es.uam.eps.ir.sonalire.grid.Parameters;

import java.io.Serializable;

 /**
 * Configures a filter which keeps only those pieces which have been repropagated in a real setting.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 * @param <I> type of the information pieces.
 * @param <F> Type of the features of the users / information pieces.
 *
 * @see OnlyRepropagatedPiecesFilter
 */
public class OnlyRepropagatedPiecesFilterConfigurator<U extends Serializable,I extends Serializable, F> implements FilterConfigurator<U,I, F>
{
    @Override
    public DataFilter<U, I, F> getFilter(Parameters fgs)
    {
        return new OnlyRepropagatedPiecesFilter<>();
    }
    
}
