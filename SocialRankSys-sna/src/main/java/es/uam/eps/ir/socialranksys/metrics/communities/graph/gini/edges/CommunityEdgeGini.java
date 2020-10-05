/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.communities.graph.gini.edges;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.graph.CommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.community.graph.CompleteCommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.community.graph.CompleteCommunityNoSelfLoopsGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.multigraph.MultiGraph;
import es.uam.eps.ir.socialranksys.metrics.CommunityMetric;
import es.uam.eps.ir.socialranksys.metrics.graph.EdgeGini;
import es.uam.eps.ir.socialranksys.metrics.graph.EdgeGiniMode;

/**
 * Computes the community edge Gini of the graph, i.e. the Gini coefficient for the
 * number of edges between each pair of communities.
 * <p>
 * <b>References: </b></p>
 *     <ol>
 *         <li>J. Sanz-Cruzado, P. Castells. Beyond accuracy in link prediction. 3rd Workshop on Social Media for Personalization and Search (SoMePEaS 2019).</li>
 *         <li>J. Sanz-Cruzado, P. Castells. Enhancing structural diversity in social networks by recommending weak ties. 12th ACM Conference on Recommender Systems (RecSys 2018),pp. 233-241 (2018) </li>
 *         <li>J. Sanz-Cruzado, S.M. Pepa, P. Castells. Structural novelty and diversity in link prediction. 0th International Workshop on Modeling Social Media (MSM 2018) at The Web Conference (WWW 2018)</li>
 *     </ol>
 *
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class CommunityEdgeGini<U> implements CommunityMetric<U>
{
    /**
     * EdgeGini execution mode
     *
     * @see EdgeGiniMode
     */
    private final EdgeGiniMode mode;

    /**
     * Indicates if autoloops are allowed.
     */
    private final boolean selfloops;

    /**
     * Constructor.
     *
     * @param mode      EdgeGini execution mode.
     * @param selfloops true if autoloops are allowed, false if they are not.
     */
    public CommunityEdgeGini(EdgeGiniMode mode, boolean selfloops)
    {
        this.mode = mode;
        this.selfloops = selfloops;
    }

    @Override
    public double compute(Graph<U> graph, Communities<U> comm)
    {
        CommunityGraphGenerator<U> cgg = selfloops ? new CompleteCommunityGraphGenerator<>() : new CompleteCommunityNoSelfLoopsGraphGenerator<>();
        MultiGraph<Integer> commGraph = cgg.generate(graph, comm);

        EdgeGini<Integer> pairGini = new EdgeGini<>(mode);
        return pairGini.compute(commGraph);
    }
}
