/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.metrics.pair;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.metrics.PairMetric;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Checks if a graph has the reciprocal edge of a pair.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ReciprocityRate<U> implements PairMetric<U>
{
    @Override
    public double compute(Graph<U> graph, U orig, U dest)
    {
        if (graph.containsEdge(dest, orig))
        {
            return 1.0;
        }
        else
        {
            return 0.0;
        }
    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph)
    {
        Map<Pair<U>, Double> res = new HashMap<>();
        graph.getAllNodes().forEach(u -> graph.getAllNodes().forEach(v -> res.put(new Pair<>(u, v), this.compute(graph, u, v))));
        return res;
    }

    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph, Stream<Pair<U>> pairs)
    {
        Map<Pair<U>, Double> res = new ConcurrentHashMap<>();
        pairs.forEach(pair -> res.put(pair, this.compute(graph, pair.v1(), pair.v2())));
        return res;
    }

    @Override
    public double averageValue(Graph<U> graph)
    {
        Map<Pair<U>, Double> res = this.compute(graph);
        OptionalDouble opt = res.values().stream().mapToDouble(v -> v).average();
        return opt.isPresent() ? opt.getAsDouble() : Double.NaN;
    }

    @Override
    public double averageValue(Graph<U> graph, Stream<Pair<U>> pairs, int pairCount)
    {
        OptionalDouble opt = pairs.mapToDouble(pair -> this.compute(graph, pair.v1(), pair.v2())).average();
        return opt.isPresent() ? opt.getAsDouble() : Double.NaN;
    }

    @Override
    public Function<U, Double> computeOrig(Graph<U> graph, U orig)
    {
        Object2DoubleMap<U> map = new Object2DoubleOpenHashMap<>();
        map.defaultReturnValue(0.0);
        graph.getMutualNodes(orig).forEach(v -> map.put(v, 1.0));
        return v -> map.getOrDefault(v, map.defaultReturnValue());
    }

    @Override
    public Function<U, Double> computeDest(Graph<U> graph, U dest)
    {
        Object2DoubleMap<U> map = new Object2DoubleOpenHashMap<>();
        map.defaultReturnValue(0.0);
        graph.getMutualNodes(dest).forEach(v -> map.put(v, 1.0));
        return v -> map.getOrDefault(v, map.defaultReturnValue());
    }
}
