/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers;


import es.uam.eps.ir.socialranksys.grid.Grid;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.GlobalReranker;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Class for performing the grid search for a given algorithm.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public interface RerankerGridSearch<U>
{
    /**
     * Obtains the different recommendation algorithms to execute in a grid.
     * @param grid The grid for the algorithm
     * @return the grid parameters.
     */
    Map<String, Supplier<GlobalReranker<U,U>>> grid(Grid grid);
}
