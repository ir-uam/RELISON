/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.letor.sampling;

import es.uam.eps.ir.socialranksys.graph.Graph;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Samples the all the possible links (all links not included in the test set).
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 */
public class AllSampler<U> extends AbstractIndividualSampler<U>
{

    /**
     * Constructor.
     * @param graph the graph.
     */
    public AllSampler(Graph<U> graph)
    {
        super(graph);
    }
    
    @Override
    public Set<U> sampleUsers(U u, Predicate<U> filter)
    {
        Set<U> sample = new HashSet<>();
        this.graph.getAllNodes().filter(v -> !graph.containsEdge(u, v)).forEach(sample::add);
        return sample;
    }
    
}
