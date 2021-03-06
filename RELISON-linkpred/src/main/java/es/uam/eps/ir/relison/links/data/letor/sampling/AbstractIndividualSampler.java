/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.data.letor.sampling;


import es.uam.eps.ir.relison.graph.Graph;

/**
 * Abstract implementation of the IndividualSampler interface.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 *
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
