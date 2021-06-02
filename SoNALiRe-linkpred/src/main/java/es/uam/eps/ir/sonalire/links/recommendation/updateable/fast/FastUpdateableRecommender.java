/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.updateable.fast;

import es.uam.eps.ir.ranksys.rec.fast.FastRecommender;
import es.uam.eps.ir.sonalire.links.recommendation.updateable.UpdateableRecommender;

/**
 * Interface for defining recommendation algorithms which can be updated over time.
 *
 * @param <U> Type of the users.
 * @param <I> Type of the items.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface FastUpdateableRecommender<U,I> extends FastRecommender<U,I>, UpdateableRecommender<U,I>
{
    
}
