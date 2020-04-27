/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.letor.sampling;


import es.uam.eps.ir.socialranksys.graph.Graph;

/**
 * Abstract implementation of the IndividualSampler interface.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 */
public abstract class AbstractIndividualSampler<U> implements IndividualSampler<U>
{
    /**
     * The graph to sample.
     */
    protected final Graph<U> graph;
    
    /**
     * Constructor.
     * @param graph the graph to sample. 
     */
    public AbstractIndividualSampler(Graph<U> graph)
    {
        this.graph = graph;
    }
}
