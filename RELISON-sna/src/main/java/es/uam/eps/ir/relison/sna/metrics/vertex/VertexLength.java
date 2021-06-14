/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.metrics.vertex;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.Weight;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.sna.metrics.VertexMetric;

/**
 * Class that measures the length (sum of weights of a selection of the edges concerning it) of a vertex.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class VertexLength<U> implements VertexMetric<U>
{
    /**
     * Edge orientation which determines the set of edges we consider for each user.
     */
    private final EdgeOrientation uSel;

    /**
     * Constructor.
     *
     * @param uSel Edge orientation which determines the set of edges we consider for each user.
     */
    public VertexLength(EdgeOrientation uSel)
    {
        this.uSel = uSel;
    }

    @Override
    public double compute(Graph<U> graph, U user)
    {
        return graph.isWeighted() ? graph.getNeighbourhoodWeights(user, uSel).mapToDouble(Weight::getValue).sum() :
                graph.getNeighbourhoodSize(user, uSel);
    }

}
