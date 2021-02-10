/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.graph;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.metrics.GraphMetric;

/**
 * Computes the complementary of the global clustering coefficient of a graph.
 *
 * <p>
 * <b>References: </b> </p>
 *     <ol>
 *         <li>J. Sanz-Cruzado, P. Castells. Beyond accuracy in link prediction. 3rd Workshop on Social Media for Personalization and Search (SoMePEaS 2019).</li>
 *         <li>J. Sanz-Cruzado, S.M. Pepa, P. Castells. Structural novelty and diversity in link prediction. 0th International Workshop on Modeling Social Media (MSM 2018) at The Web Conference (WWW 2018)</li>
 *     </ol>
 *
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ClusteringCoefficientComplement<U> implements GraphMetric<U>
{
    /**
     * Clustering coefficient metric.
     */
    private final ClusteringCoefficient<U> clustCoef;

    /**
     * Constructor. Applies the usual directed clustering coefficient of
     * a graph.
     */
    public ClusteringCoefficientComplement()
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
    public ClusteringCoefficientComplement(EdgeOrientation vSel, EdgeOrientation wSel)
    {
        this.clustCoef = new ClusteringCoefficient<>(vSel, wSel);
    }

    @Override
    public double compute(Graph<U> graph)
    {
        return 1.0 - this.clustCoef.compute(graph);
    }
}
