/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;

import es.uam.eps.ir.relison.utils.datatypes.Pair;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;

import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.*;
import java.util.*;

/**
 * Two-column layout with explicit partitions or automatic weak-graph two-colouring.
 * Automatic colouring rejects odd cycles and self-loops. Disconnected components
 * start on the currently smaller side; isolates are included. Ordering is delegated
 * to the multipartite barycentre heuristic.
 * @param <U> node type
 */
public final class BipartiteLayout<U> extends AbstractStaticLayout<U>
{
    private final MultipartiteLayout<U> explicit;
    private final double columnSpacing, rowSpacing;
    private final int sweeps;

    /** Automatically colours the graph, with unit spacing and four ordering passes. */
    public BipartiteLayout() { this(1, 1, 4); }

    /**
     * @param columnSpacing separation between columns
     * @param rowSpacing separation within columns
     * @param sweeps ordering passes; zero preserves initial order
     */
    public BipartiteLayout(double columnSpacing, double rowSpacing, int sweeps)
    {
        super("bipartite", "Bipartite");
        this.columnSpacing = positive(columnSpacing, "columnSpacing");
        this.rowSpacing = positive(rowSpacing, "rowSpacing");
        if (sweeps < 0) throw new IllegalArgumentException("sweeps must be non-negative");
        this.sweeps = sweeps;
        explicit = null;
    }

    /**
     * @param left left partition
     * @param right right partition
     * @param columnSpacing separation between columns
     * @param rowSpacing separation within columns
     * @param sweeps ordering passes; zero preserves request order
     */
    public BipartiteLayout(Collection<U> left, Collection<U> right, double columnSpacing, double rowSpacing, int sweeps)
    {
        super("bipartite", "Bipartite");
        explicit = new MultipartiteLayout<>(Arrays.asList(left, right), columnSpacing, rowSpacing, sweeps);
        this.columnSpacing = columnSpacing;
        this.rowSpacing = rowSpacing;
        this.sweeps = sweeps;
    }

    @Override
    protected Map<U, Pair<Double>> generate(IndexedGraphSnapshot<U> graph, LayoutRequest<U> request)
    {
        if (explicit != null) return explicit.generate(graph, request);
        int[][] adjacent = Topology.neighbours(graph, EdgeOrientation.UND);
        int[] colour = new int[adjacent.length];
        Arrays.fill(colour, -1);
        List<List<U>> sides = Arrays.asList(new ArrayList<>(), new ArrayList<>());
        Deque<Integer> queue = new ArrayDeque<>();
        for (int start = 0; start < colour.length; start++)
        {
            if (colour[start] >= 0) continue;
            colour[start] = sides.get(0).size() <= sides.get(1).size() ? 0 : 1;
            queue.add(start);
            while (!queue.isEmpty())
            {
                int node = queue.remove();
                sides.get(colour[node]).add(graph.getNodes().get(node));
                for (int neighbour : adjacent[node])
                {
                    if (colour[neighbour] < 0) { colour[neighbour] = 1 - colour[node]; queue.add(neighbour); }
                    else if (colour[neighbour] == colour[node]) throw new IllegalArgumentException("Graph is not bipartite: odd cycle or self-loop");
                }
            }
        }
        return new MultipartiteLayout<>(sides, columnSpacing, rowSpacing, sweeps).generate(graph, request);
    }
}
