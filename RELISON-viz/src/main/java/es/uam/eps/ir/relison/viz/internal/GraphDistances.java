/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.internal;
import java.util.*;

/** Shortest paths on a structural undirected projection; loops are ignored. */
public final class GraphDistances
{
    private GraphDistances() { }
    /**
     * @param graph topology snapshot
     * @param weighted whether positive edge weights represent lengths
     * @param checkpoint cancellation check
     * @return all-pairs distances; unreachable pairs have positive infinity
     */
    public static double[][] compute(IndexedGraphSnapshot<?> graph, boolean weighted, Runnable checkpoint)
    {
        int n = graph.getNodes().size();
        List<Map<Integer, Double>> adjacent = new ArrayList<>();
        for (int i = 0; i < n; i++) adjacent.add(new TreeMap<>());
        for (int i = 0; i < n; i++)
        {
            checkpoint.run();
            int[] targets = graph.getTargets(i); double[] weights = graph.getWeights(i);
            for (int e = 0; e < targets.length; e++)
            {
                int j = targets[e]; if (i == j) continue;
                double w = weighted ? weights[e] : 1;
                if (!Double.isFinite(w) || w <= 0) throw new IllegalArgumentException("Distance edge lengths must be finite and positive");
                adjacent.get(i).merge(j, w, Math::min);
                adjacent.get(j).merge(i, w, Math::min);
            }
        }
        double[][] distances = new double[n][n];
        for (int source = 0; source < n; source++)
        {
            checkpoint.run();
            double[] d = distances[source]; Arrays.fill(d, Double.POSITIVE_INFINITY); d[source] = 0;
            PriorityQueue<Entry> queue = new PriorityQueue<>(Comparator.comparingDouble((Entry v) -> v.distance).thenComparingInt(v -> v.node));
            queue.add(new Entry(source, 0));
            while (!queue.isEmpty())
            {
                checkpoint.run();
                Entry v = queue.remove(); if (v.distance != d[v.node]) continue;
                for (Map.Entry<Integer, Double> edge : adjacent.get(v.node).entrySet())
                {
                    double next = v.distance + edge.getValue();
                    if (!Double.isFinite(next)) throw new IllegalArgumentException("Shortest-path length overflow");
                    if (next < d[edge.getKey()])
                    { d[edge.getKey()] = next; queue.add(new Entry(edge.getKey(), next)); }
                }
            }
        }
        return distances;
    }
    private static final class Entry
    {
        final int node; final double distance;
        Entry(int node, double distance) { this.node = node; this.distance = distance; }
    }
}
