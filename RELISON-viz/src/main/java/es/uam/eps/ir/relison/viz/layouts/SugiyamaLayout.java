/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.*;
import java.util.*;

/**
 * Directed layered drawing following Sugiyama, Tagawa and Toda (1981,
 * DOI 10.1109/TSMC.1981.4308636): proper layering, alternating barycentre
 * ordering and degree-priority barycentre coordinate sweeps. Layers use longest
 * paths in the acyclic orientation; feedback arcs use Eades, Lin and Smyth's
 * greedy ordering (1993, DOI 10.1016/0020-0190(93)90079-O).
 * Original edge directions are restored in output polylines. Self-loops are
 * excluded from layering and routed beside their node. These are heuristics,
 * with no minimum-crossing or minimum-feedback-set guarantee. Edge weights and
 * node radii are not used. Pins override placement and may disrupt hierarchy.
 * @param <U> node type
 */
public final class SugiyamaLayout<U> implements Layout<U>
{
    private final double spacing, levelSpacing;
    private final int sweeps;
    private final LayoutDescriptor descriptor = new LayoutDescriptor("sugiyama", "Sugiyama (directed layers)");

    /** Uses unit spacing and eight pairs of sweeps. */
    public SugiyamaLayout() { this(1, 1, 8); }

    /**
     * @param spacing minimum horizontal centre separation, including bend points
     * @param levelSpacing vertical layer separation
     * @param sweeps number of downward/upward ordering and coordinate passes
     */
    public SugiyamaLayout(double spacing, double levelSpacing, int sweeps)
    {
        this.spacing = AbstractStaticLayout.positive(spacing, "spacing");
        this.levelSpacing = AbstractStaticLayout.positive(levelSpacing, "levelSpacing");
        if (sweeps < 0) throw new IllegalArgumentException("sweeps must be nonnegative");
        this.sweeps = sweeps;
    }

    @Override public LayoutDescriptor getDescriptor() { return descriptor; }

    @Override public LayoutResult<U> compute(Graph<U> graph, LayoutRequest<U> request)
    {
        IndexedGraphSnapshot<U> snapshot = IndexedGraphSnapshot.capture(graph, request);
        if (!snapshot.isDirected()) throw new IllegalArgumentException("Sugiyama requires a directed graph");
        int n = snapshot.getNodes().size();
        List<Arc> arcs = new ArrayList<>();
        for (int s = 0; s < n; s++)
        {
            Map<Integer, Integer> counts = new HashMap<>();
            for (int t : snapshot.getTargets(s))
            {
                int occurrence = counts.getOrDefault(t, 0);
                counts.put(t, occurrence + 1);
                arcs.add(new Arc(s, t, occurrence));
            }
        }
        int[] order = acyclicOrder(n, arcs);
        List<List<Integer>> outgoing = lists(n);
        int[] indegree = new int[n], rank = new int[n];
        for (Arc arc : arcs) if (arc.source != arc.target)
        {
            arc.reversed = order[arc.source] > order[arc.target];
            outgoing.get(arc.from()).add(arc.to());
            indegree[arc.to()]++;
        }
        Deque<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < n; i++) if (indegree[i] == 0) queue.add(i);
        int levels = n == 0 ? 0 : 1;
        while (!queue.isEmpty())
        {
            int v = queue.remove();
            for (int w : outgoing.get(v))
            {
                rank[w] = Math.max(rank[w], rank[v] + 1);
                levels = Math.max(levels, rank[w] + 1);
                if (--indegree[w] == 0) queue.add(w);
            }
        }
        List<List<Integer>> layers = lists(levels);
        List<List<Integer>> upper = lists(n), lower = lists(n);
        for (int i = 0; i < n; i++) layers.get(rank[i]).add(i);
        for (Arc arc : arcs) if (arc.source != arc.target)
        {
            arc.chain.add(arc.from());
            for (int layer = rank[arc.from()] + 1; layer < rank[arc.to()]; layer++)
            {
                int dummy = upper.size();
                upper.add(new ArrayList<>()); lower.add(new ArrayList<>());
                layers.get(layer).add(dummy); arc.chain.add(dummy);
            }
            arc.chain.add(arc.to());
            for (int i = 1; i < arc.chain.size(); i++)
            {
                int u = arc.chain.get(i-1), v = arc.chain.get(i);
                lower.get(u).add(v); upper.get(v).add(u);
            }
        }
        int[] positions = new int[upper.size()];
        updatePositions(layers, positions);
        long bestCrossings = crossings(layers, lower, positions);
        List<List<Integer>> best = copy(layers);
        for (int pass = 0; pass < sweeps; pass++)
        {
            orderLayers(layers, upper, positions, true);
            long count = crossings(layers, lower, positions);
            if (count < bestCrossings) { bestCrossings = count; best = copy(layers); }
            orderLayers(layers, lower, positions, false);
            count = crossings(layers, lower, positions);
            if (count < bestCrossings) { bestCrossings = count; best = copy(layers); }
        }
        layers = best;
        updatePositions(layers, positions);
        double[] x = new double[upper.size()], y = new double[upper.size()];
        for (int l = 0; l < layers.size(); l++) for (int v : layers.get(l))
        { x[v] = (positions[v] - (layers.get(l).size()-1)/2.0) * spacing; y[v] = l * levelSpacing; }
        for (int pass = 0; pass < sweeps; pass++)
        {
            assignCoordinates(layers, upper, positions, x, true);
            assignCoordinates(layers, lower, positions, x, false);
        }
        Map<U, Pair<Double>> coordinates = new LinkedHashMap<>();
        for (int i = 0; i < n; i++)
        {
            U node = snapshot.getNodes().get(i);
            coordinates.put(node, request.getPinnedNodes().contains(node)
                ? request.getInitialPositions().get(node) : new Pair<>(x[i], y[i]));
        }
        List<EdgeRoute<U>> routes = new ArrayList<>();
        for (Arc arc : arcs)
        {
            U source = snapshot.getNodes().get(arc.source), target = snapshot.getNodes().get(arc.target);
            List<Pair<Double>> points = new ArrayList<>();
            points.add(coordinates.get(source));
            if (arc.source == arc.target)
            {
                Pair<Double> p = coordinates.get(source);
                double offset = spacing * (arc.occurrence + 1) / 2;
                points.add(new Pair<>(p.v1()+offset, p.v2()-levelSpacing/3));
                points.add(new Pair<>(p.v1()+offset, p.v2()+levelSpacing/3));
            }
            else for (int i = 1; i < arc.chain.size()-1; i++)
            {
                int v = arc.chain.get(arc.reversed ? arc.chain.size()-1-i : i);
                points.add(new Pair<>(x[v], y[v]));
            }
            points.add(coordinates.get(target));
            routes.add(new EdgeRoute<>(source, target, arc.occurrence, points));
        }
        return new LayoutResult<>(coordinates, new LayoutDiagnostics("sugiyama", LayoutDiagnostics.Termination.COMPLETED), routes);
    }

    // Eades-Lin-Smyth: remove sinks to the right, sources to the left, then
    // remove a vertex maximizing outdegree minus indegree. Ties use node order.
    private static int[] acyclicOrder(int n, List<Arc> arcs)
    {
        List<List<Integer>> out = lists(n), in = lists(n);
        int[] outDegree = new int[n], inDegree = new int[n];
        for (Arc arc : arcs) if (arc.source != arc.target)
        {
            out.get(arc.source).add(arc.target); in.get(arc.target).add(arc.source);
            outDegree[arc.source]++; inDegree[arc.target]++;
        }
        boolean[] removed = new boolean[n];
        int[] result = new int[n];
        int left = 0, right = n-1;
        while (left <= right)
        {
            int v;
            while ((v = zeroDegree(outDegree, removed)) >= 0)
            {
                result[v] = right--;
                remove(v, removed, out, in, outDegree, inDegree);
            }
            while ((v = zeroDegree(inDegree, removed)) >= 0)
            {
                result[v] = left++;
                remove(v, removed, out, in, outDegree, inDegree);
            }
            if (left > right) break;
            v = -1;
            for (int i = 0; i < n; i++) if (!removed[i]
                && (v < 0 || outDegree[i]-inDegree[i] > outDegree[v]-inDegree[v])) v = i;
            result[v] = left++;
            remove(v, removed, out, in, outDegree, inDegree);
        }
        return result;
    }

    private static int zeroDegree(int[] degree, boolean[] removed)
    {
        for (int i = 0; i < degree.length; i++) if (!removed[i] && degree[i] == 0) return i;
        return -1;
    }
    private static void remove(int v, boolean[] removed, List<List<Integer>> out, List<List<Integer>> in, int[] outDegree, int[] inDegree)
    {
        removed[v] = true;
        for (int w : out.get(v)) if (!removed[w]) inDegree[w]--;
        for (int w : in.get(v)) if (!removed[w]) outDegree[w]--;
    }

    private static void orderLayers(List<List<Integer>> layers, List<List<Integer>> neighbours, int[] positions, boolean down)
    {
        double[] barycentre = new double[positions.length];
        for (int step = 1; step < layers.size(); step++)
        {
            List<Integer> layer = layers.get(down ? step : layers.size()-1-step);
            for (int v : layer) barycentre[v] = neighbours.get(v).isEmpty() ? positions[v]
                : neighbours.get(v).stream().mapToInt(w -> positions[w]).average().orElse(positions[v]);
            // List.sort is stable; equal barycentres preserve the previous order.
            layer.sort(Comparator.comparingDouble(v -> barycentre[v]));
            for (int i = 0; i < layer.size(); i++) positions[layer.get(i)] = i;
        }
    }

    // The published priority method: higher degree vertices move first; lower
    // priority neighbours in the same layer may be pushed, preserving spacing.
    private void assignCoordinates(List<List<Integer>> layers, List<List<Integer>> neighbours, int[] positions, double[] x, boolean down)
    {
        for (int step = 1; step < layers.size(); step++)
        {
            List<Integer> layer = layers.get(down ? step : layers.size()-1-step);
            List<Integer> priority = new ArrayList<>(layer);
            priority.sort(Comparator.comparingInt((Integer v) -> neighbours.get(v).size()).reversed());
            boolean[] fixed = new boolean[layer.size()];
            for (int v : priority)
            {
                int p = positions[v];
                if (!neighbours.get(v).isEmpty())
                {
                    double target = neighbours.get(v).stream().mapToDouble(w -> x[w]).average().orElse(x[v]);
                    if (target > x[v])
                    {
                        for (int j = p+1; j < layer.size(); j++) if (fixed[j])
                        { target = Math.min(target, x[layer.get(j)] - (j-p)*spacing); break; }
                        x[v] = target;
                        for (int j = p+1; j < layer.size() && !fixed[j]; j++)
                            x[layer.get(j)] = Math.max(x[layer.get(j)], x[layer.get(j-1)]+spacing);
                    }
                    else
                    {
                        for (int j = p-1; j >= 0; j--) if (fixed[j])
                        { target = Math.max(target, x[layer.get(j)] + (p-j)*spacing); break; }
                        x[v] = target;
                        for (int j = p-1; j >= 0 && !fixed[j]; j--)
                            x[layer.get(j)] = Math.min(x[layer.get(j)], x[layer.get(j+1)]-spacing);
                    }
                }
                fixed[p] = true;
            }
        }
    }

    // Inversions between adjacent layers; shared endpoints are not crossings.
    private static long crossings(List<List<Integer>> layers, List<List<Integer>> lower, int[] positions)
    {
        long total = 0;
        for (int l = 0; l+1 < layers.size(); l++)
        {
            long[] tree = new long[layers.get(l+1).size()+1];
            long seen = 0;
            for (int v : layers.get(l))
            {
                for (int w : lower.get(v))
                {
                    long prefix = 0;
                    for (int i = positions[w]+1; i > 0; i -= i & -i) prefix += tree[i];
                    total += seen - prefix;
                }
                for (int w : lower.get(v))
                {
                    for (int i = positions[w]+1; i < tree.length; i += i & -i) tree[i]++;
                    seen++;
                }
            }
        }
        return total;
    }

    private static List<List<Integer>> lists(int size)
    {
        List<List<Integer>> result = new ArrayList<>();
        for (int i = 0; i < size; i++) result.add(new ArrayList<>());
        return result;
    }
    private static List<List<Integer>> copy(List<List<Integer>> layers)
    {
        List<List<Integer>> result = new ArrayList<>();
        for (List<Integer> layer : layers) result.add(new ArrayList<>(layer));
        return result;
    }
    private static void updatePositions(List<List<Integer>> layers, int[] positions)
    {
        for (List<Integer> layer : layers) for (int i = 0; i < layer.size(); i++) positions[layer.get(i)] = i;
    }
    private static final class Arc
    {
        final int source, target, occurrence;
        final List<Integer> chain = new ArrayList<>();
        boolean reversed;
        Arc(int source, int target, int occurrence) { this.source = source; this.target = target; this.occurrence = occurrence; }
        int from() { return reversed ? target : source; }
        int to() { return reversed ? source : target; }
    }
}
