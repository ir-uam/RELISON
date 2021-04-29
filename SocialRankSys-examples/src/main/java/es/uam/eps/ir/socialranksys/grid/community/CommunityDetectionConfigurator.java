/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.community;

import es.uam.eps.ir.socialranksys.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.socialranksys.grid.Grid;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Interface for configuring community detection algorithms.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 *
 * @see es.uam.eps.ir.socialranksys.community.detection.CommunityDetectionAlgorithm
 */
public interface CommunityDetectionConfigurator<U extends Serializable> 
{
    /**
     * Configures a community detection algorithm.
     * @param grid the parameters of the algorithm.
     * @return a pair containing the name of the
     */
    Map<String, Supplier<CommunityDetectionAlgorithm<U>>> configure(Grid grid);
}
