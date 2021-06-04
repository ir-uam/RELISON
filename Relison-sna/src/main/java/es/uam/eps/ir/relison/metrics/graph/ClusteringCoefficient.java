/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.metrics.graph;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.metrics.GraphMetric;

/**
 * Computes the global clustering coefficient of a graph.
 *
 * <p>
 * <b>Reference:</b> M.E.J. Newman. Networks: an introduction (2010)
 * </p>
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ClusteringCoefficient<U> implements GraphMetric<U>
{
    /**
     * Orientation for selecting the neighbours of the studied node.
     */
    private final EdgeOrientation vSel;
    /**
     * Orientation for selecting the neighbours of the studied node.
     */
    private final EdgeOrientation wSel;

    /**
     * Number of triangles in the graph.
     */
    private int triangles;

    /**
     * Number of triplets in the graph.
     */
    private int triplets;

    /**
     * Constructor. Applies the usual directed clustering coefficient of
     * a graph.
     */
    public ClusteringCoefficient()
    {
        this(EdgeOrientation.IN, EdgeOrientation.OUT);
    }

    /**
     * Constructor. This constructor allows to specify the direction of the clustering
     * coefficient metrics.
     *
     * @param vSel First selection of nodes.
     * @param wSel Second selection of nodes.
     */
    public ClusteringCoefficient(EdgeOrientation vSel, EdgeOrientation wSel)
    {
        this.vSel = vSel;
        this.wSel = wSel;
    }

    @Override
    public double compute(Graph<U> graph)
    {
        this.triangles = 0;
        this.triplets = 0;

        graph.getAllNodes().forEach((u) -> graph.getNeighbourhood(u, vSel).forEach(v -> graph.getNeighbourhood(u, wSel).forEach(w ->
        {
            if (!w.equals(v) && !u.equals(v) && !u.equals(w))
            {
                ++this.triplets;
                if (graph.containsEdge(v, w))
                {
                    ++this.triangles;
                }
            }
        })));

        if (triplets > 0)
        {
            return (triangles + 0.0) / (triplets + 0.0);
        }
        return 0.0;
    }
}
