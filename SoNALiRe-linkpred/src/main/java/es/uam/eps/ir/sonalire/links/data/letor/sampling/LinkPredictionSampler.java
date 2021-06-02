/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.data.letor.sampling;

import es.uam.eps.ir.sonalire.graph.Graph;

import java.util.*;
import java.util.function.Predicate;

/**
 * Samples all the links created in a test graph,
 * and the same amount of links which have not been created.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 */
public class LinkPredictionSampler<U> extends AbstractIndividualSampler<U>
{
    /**
     * The graph to check the positive / negative examples
     */
    private final Graph<U> testGraph;

    /**
     * Constructor.
     * @param graph     the graph.
     * @param testGraph the test graph containing the set of positive links.

     */
    public LinkPredictionSampler(Graph<U> graph, Graph<U> testGraph)
    {
        super(graph);
        this.testGraph = testGraph;
    }

    @Override
    public Set<U> sampleUsers(U u, Predicate<U> filter)
    {
        Set<U> sample = new HashSet<>();

        this.testGraph.getAdjacentNodes(u).filter(v -> !graph.containsEdge(u,v)).forEach(sample::add);

        if(sample.size()*2 > graph.getVertexCount())
        {
            graph.getAllNodes().forEach(sample::add);
        }
        else
        {
            List<U> users = new ArrayList<>();
            this.graph.getAllNodes().filter(v -> !sample.contains(v)).forEach(users::add);

            Collections.shuffle(users);
            sample.addAll(users.subList(0, sample.size()));
        }

        return sample;
    }

}
