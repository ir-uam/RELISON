/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.distance.pair;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.metrics.distance.DistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.distance.FastDistanceCalculator;
import es.uam.eps.ir.socialranksys.metrics.pair.AbstractPairMetric;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Computes the Reciprocal Shortest Path Length.
 *
 * <p>
 * <b>References: </b></p>
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
public class ReciprocalShortestPathLength<U> extends AbstractPairMetric<U>
{
    /**
     * Distance calculator.
     */
    private final DistanceCalculator<U> dc;

    /**
     * Constructor.
     */
    public ReciprocalShortestPathLength()
    {
        this.dc = new FastDistanceCalculator<>();
    }

    /**
     * Constructor.
     *
     * @param dc distance calculator.
     */
    public ReciprocalShortestPathLength(DistanceCalculator<U> dc)
    {
        this.dc = dc;
    }

    @Override
    public double compute(Graph<U> graph, U orig, U dest)
    {
        if (!graph.containsVertex(orig) || !graph.containsVertex(dest))
        {
            return Double.NaN;
        }
        dc.computeDistances(graph);
        Double dist = dc.getDistances(orig, dest);
        if (dist.isInfinite())
        {
            return 0.0;
        }
        else if (dist.equals(0.0) || orig.equals(dest))
        {
            return Double.POSITIVE_INFINITY;
        }
        else
        {
            return dist;
        }
    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph)
    {
        Map<Pair<U>, Double> map = new HashMap<>();
        graph.getAllNodes().forEach(u -> graph.getAllNodes().forEach(v ->
        {
            if (!u.equals(v))
            {
                map.put(new Pair<>(u, v), this.compute(graph, u, v));
            }
        }));

        return map;
    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph, Stream<Pair<U>> pairs)
    {
        Map<Pair<U>, Double> map = new ConcurrentHashMap<>();
        pairs.forEach(pair -> map.put(pair, this.compute(graph, pair.v1(), pair.v2())));
        return map;
    }

    @Override
    public double averageValue(Graph<U> graph)
    {
        double sum = graph.getAllNodes().mapToDouble(u -> graph.getAllNodes().mapToDouble(v -> u.equals(v) ? 0.0 : this.compute(graph, u, v)).sum()).sum();

        double numN = graph.getVertexCount() + 0.0;
        return sum / (numN * (numN - 1));
    }

    @Override
    public double averageValue(Graph<U> graph, Stream<Pair<U>> pairs, int pairCount)
    {
        return 1.0 / (pairCount + 0.0) * pairs.mapToDouble(pair -> this.compute(graph, pair.v1(), pair.v2())).sum();
    }


}
