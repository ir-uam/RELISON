/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;

import es.uam.eps.ir.relison.utils.datatypes.Pair;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;

import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.*;
import java.util.*;

/**
 * Feature-value groups placed in columns. Edges within a group are allowed.
 * Optional alternating barycentre sweeps provide heuristic crossing reduction;
 * zero sweeps retain request order within columns. Direction, weights, and
 * parallel multiplicity do not affect ordering. Empty columns are allowed.
 * @param <U> node type
 */
public final class FeatureGridLayout<U> extends AbstractStaticLayout<U>
{
    private final List<List<U>> partitions;
    private final double columnSpacing, rowSpacing;
    private final int sweeps;

    /**
     * @param partitions ordered node groups; empty groups are allowed
     */
    public FeatureGridLayout(List<? extends Collection<U>> partitions) { this(partitions, 1, 1, 4); }

    /**
     * @param partitions ordered node partitions, at least two
     * @param columnSpacing separation between columns
     * @param rowSpacing separation within columns
     * @param sweeps non-negative number of forward/backward ordering passes
     */
    public FeatureGridLayout(List<? extends Collection<U>> partitions, double columnSpacing, double rowSpacing, int sweeps)
    {
        super("feature-grid", "Grid by feature");
        if (sweeps < 0) throw new IllegalArgumentException("sweeps must be non-negative");
        this.columnSpacing = positive(columnSpacing, "columnSpacing");
        this.rowSpacing = positive(rowSpacing, "rowSpacing");
        this.sweeps = sweeps;
        Set<U> seen = new HashSet<>();
        List<List<U>> copy = new ArrayList<>();
        for (Collection<U> partition : partitions)
        {
            for (U node : partition)
                if (node == null || !seen.add(node)) throw new IllegalArgumentException("Partition nodes must be non-null and unique");
            copy.add(Collections.unmodifiableList(new ArrayList<>(partition)));
        }
        this.partitions = Collections.unmodifiableList(copy);
    }

    @Override
    protected Map<U, Pair<Double>> generate(IndexedGraphSnapshot<U> graph, LayoutRequest<U> request)
    {
        Map<U, Integer> membership = new HashMap<>();
        List<List<Integer>> columns = new ArrayList<>();
        for (int i = 0; i < partitions.size(); i++)
        {
            columns.add(new ArrayList<>());
            for (U node : partitions.get(i)) membership.put(node, i);
        }
        if (!membership.keySet().equals(new HashSet<>(graph.getNodes())))
            throw new IllegalArgumentException("Partitions must contain exactly the graph nodes");
        int[] column = new int[graph.getNodes().size()];
        for (int i = 0; i < column.length; i++)
        {
            column[i] = membership.get(graph.getNodes().get(i));
            columns.get(column[i]).add(i);
        }
        int[][] adjacent = Topology.neighbours(graph, EdgeOrientation.UND);


        double[] rank = new double[column.length];
        for (List<Integer> nodes : columns) updateRanks(nodes, rank);
        for (int sweep = 0; sweep < sweeps; sweep++)
            for (int offset = 0; offset < columns.size(); offset++)
            {
                int index = sweep % 2 == 0 ? offset : columns.size() - 1 - offset;
                List<Integer> nodes = columns.get(index);
                Map<Integer, Double> barycentre = new HashMap<>();
                for (int node : nodes)
                {
                    double sum = 0;
                    for (int neighbour : adjacent[node]) sum += rank[neighbour];
                    barycentre.put(node, adjacent[node].length == 0 ? rank[node] : sum / adjacent[node].length);
                }
                // List.sort is stable, so tied values retain their current ordering.
                nodes.sort(Comparator.comparingDouble(barycentre::get));
                updateRanks(nodes, rank);
            }
        Map<U, Pair<Double>> positions = new LinkedHashMap<>();
        for (int i = 0; i < columns.size(); i++)
            for (int node : columns.get(i))
                positions.put(graph.getNodes().get(node), new Pair<>((i - (columns.size() - 1) / 2.0) * columnSpacing, rank[node] * rowSpacing));
        return positions;
    }

    private static void updateRanks(List<Integer> nodes, double[] rank)
    {
        for (int i = 0; i < nodes.size(); i++) rank[nodes.get(i)] = i - (nodes.size() - 1) / 2.0;
    }
}
