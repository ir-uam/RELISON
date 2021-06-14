/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.metrics.communities.graph.gini.size;

import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.metrics.communities.indiv.Size;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.metrics.CommunityMetric;
import es.uam.eps.ir.relison.utils.indexes.GiniIndex;

import java.util.Map;

/**
 * Computes the Gini coefficient of the distribution of community sizes.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class CommunitySizeGini<U> implements CommunityMetric<U>
{
    @Override
    public double compute(Graph<U> graph, Communities<U> comm)
    {
        Size<U> commSize = new Size<>();
        Map<Integer, Double> sizes = commSize.compute(graph, comm);

        GiniIndex gini = new GiniIndex();
        return 1.0 - gini.compute(sizes.values().stream(), false);
    }

}
