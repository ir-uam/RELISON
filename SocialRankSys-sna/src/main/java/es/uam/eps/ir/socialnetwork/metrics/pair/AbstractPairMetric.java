package es.uam.eps.ir.socialnetwork.metrics.pair;

import es.uam.eps.ir.socialnetwork.metrics.PairMetric;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class AbstractPairMetric<U> implements PairMetric<U>
{
    @Override
    public Map<Pair<U>, Double> compute(Graph<U> graph)
    {
        Map<Pair<U>, Double> values = new HashMap<>();
        if(!graph.isMultigraph())
        {
            graph.getAllNodes().forEach((orig) ->
                graph.getAllNodes().forEach(dest ->
                {
                    if(!orig.equals(dest))
                        values.put(new Pair<>(orig, dest), this.compute(graph, orig, dest));
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
        return value/(graph.getVertexCount()*(graph.getVertexCount()-1));
    }

    @Override
    public double averageValue(Graph<U> graph, Stream<Pair<U>> pairs, int pairCount)
    {
        double value = pairs.mapToDouble(pair -> this.compute(graph, pair.v1(), pair.v2())).sum();
        return value / (pairCount + 0.0);
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
