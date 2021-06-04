/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.metrics.pair;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.metrics.PairMetric;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Abstract implementation of a pair metric.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class AbstractPairMetric<U> implements PairMetric<U>
{
    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph)
    {
        Map<Pair<U>, Double> values = new HashMap<>();
        if (!graph.isMultigraph())
        {
            graph.getAllNodes().forEach((orig) -> graph.getAllNodes().forEach(dest ->
            {
                if (!orig.equals(dest))
                {
                    values.put(new Pair<>(orig, dest), this.compute(graph, orig, dest));
                }
            }));
        }
        return values;
    }

    @Override
    public Map<Pair<U>, Double> computeOnlyLinks(Graph<U> graph)
    {
        Map<Pair<U>, Double> values = new HashMap<>();
        if (!graph.isMultigraph())
        {
            graph.getAllNodes().forEach((orig) -> graph.getAdjacentNodes(orig).forEach(dest ->
            {
                if (!orig.equals(dest))
                {
                    values.put(new Pair<>(orig, dest), this.compute(graph, orig, dest));
                }
            }));
        }
        return values;
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
        double value = this.compute(graph).entrySet().stream().filter(x -> !x.getKey().v1().equals(x.getKey().v2())).mapToDouble(Map.Entry::getValue).sum();
        return value / (graph.getVertexCount() * (graph.getVertexCount() - 1));
    }

    @Override
    public double averageValue(Graph<U> graph, Stream<Pair<U>> pairs, int pairCount)
    {
        double value = pairs.mapToDouble(pair -> this.compute(graph, pair.v1(), pair.v2())).sum();
        return value / (pairCount + 0.0);
    }

    @Override
    public double averageValueOnlyLinks(Graph<U> graph)
    {
        List<Pair<U>> pairs = new ArrayList<>();
        graph.getAllNodes().forEach(u -> graph.getAdjacentNodes(u).forEach(v -> pairs.add(new Pair<>(u,v))));
        return averageValue(graph, pairs.stream(), pairs.size());
    }

    @Override
    public Function<U, Double> computeOrig(Graph<U> graph, U orig)
    {
        Object2DoubleMap<U> map = new Object2DoubleOpenHashMap<>();
        graph.getAllNodes().forEach(dest -> map.put(dest, this.compute(graph, orig, dest)));

        return (map::getDouble);
    }

    @Override
    public Function<U, Double> computeDest(Graph<U> graph, U dest)
    {
        Object2DoubleMap<U> map = new Object2DoubleOpenHashMap<>();
        graph.getAllNodes().forEach(orig -> map.put(dest, this.compute(graph, orig, dest)));

        return (map::getDouble);
    }

}
