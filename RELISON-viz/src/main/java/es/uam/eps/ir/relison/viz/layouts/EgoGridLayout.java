/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;

import es.uam.eps.ir.relison.utils.datatypes.Pair;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;

import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.*;
import java.util.*;

/**
 * Breadth-first ego grid. Each column represents a hop distance; unreachable
 * nodes share one final column. The root is at the origin. Weights and parallel
 * multiplicity are ignored. Pins override computed positions.
 * @param <U> node type
 */
public final class EgoGridLayout<U> extends AbstractStaticLayout<U>
{
    private final U root;
    private final EdgeOrientation direction;
    private final double spacing, columnSpacing;

    /** Uses the first node in request order, ignores direction, and uses unit spacing. */
    public EgoGridLayout() { this(null, EdgeOrientation.UND, 1, 1); }

    /**
     * @param root selected root, or null to choose the first node
     * @param direction traversal direction
     * @param columnSpacing horizontal separation per hop
     * @param spacing vertical separation within each column
     */
    public EgoGridLayout(U root, EdgeOrientation direction, double columnSpacing, double spacing)
    {
        super("ego-grid", "Grid / ego");
        this.root = root;
        this.direction = Objects.requireNonNull(direction);
        this.spacing = positive(spacing, "spacing");
        this.columnSpacing = positive(columnSpacing, "columnSpacing");
    }

    @Override
    protected Map<U, Pair<Double>> generate(IndexedGraphSnapshot<U> graph, LayoutRequest<U> request)
    {
        int count = graph.getNodes().size();
        int start = root == null ? 0 : graph.indexOf(root);
        if (start < 0 || (root != null && count == 0)) throw new IllegalArgumentException("Radial root must exist in the graph");
        Map<U, Pair<Double>> positions = new LinkedHashMap<>();
        if (count == 0) return positions;
        int[][] adjacent = Topology.neighbours(graph, direction);
        int[] depth = new int[count];
        Arrays.fill(depth, -1);
        depth[start] = 0;
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(start);
        int maximum = 0;
        while (!queue.isEmpty())
        {
            int node = queue.remove();
            for (int neighbour : adjacent[node])
                if (depth[neighbour] < 0)
                {
                    depth[neighbour] = depth[node] + 1;
                    maximum = Math.max(maximum, depth[neighbour]);
                    queue.add(neighbour);
                }
        }
        Map<Integer, List<U>> rings = new TreeMap<>();
        for (int i = 0; i < count; i++)
            rings.computeIfAbsent(depth[i] < 0 ? maximum + 1 : depth[i], key -> new ArrayList<>()).add(graph.getNodes().get(i));
        rings.forEach((level, nodes) -> {
            for (int i = 0; i < nodes.size(); i++)
                positions.put(nodes.get(i), new Pair<>(level * columnSpacing, (i - (nodes.size() - 1) / 2.0) * spacing));
        });
        return positions;
    }
}
