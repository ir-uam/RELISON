/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.internal;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;


import java.util.*;

/** Structural projections shared by Stage B algorithms. Weights do not affect these projections. */
public final class Topology
{
    private Topology() { }

    /**
     * Builds sorted unique neighbours; parallel arcs are collapsed and loops retained.
     * @param graph indexed graph
     * @param direction traversal direction
     * @return neighbour arrays in snapshot order
     */
    public static int[][] neighbours(IndexedGraphSnapshot<?> graph, EdgeOrientation direction)
    {
        Objects.requireNonNull(direction);
        int size = graph.getNodes().size();
        List<Set<Integer>> adjacent = new ArrayList<>(size);
        for (int i = 0; i < size; i++) adjacent.add(new HashSet<>());
        List<Set<Integer>> outgoing = new ArrayList<>(size);
        for (int i = 0; i < size; i++) outgoing.add(new HashSet<>());
        for (int source = 0; source < size; source++)
            for (int target : graph.getTargets(source)) outgoing.get(source).add(target);
        for (int source = 0; source < size; source++)
            for (int target : graph.getTargets(source))
            {
                if (direction == EdgeOrientation.MUTUAL)
                {
                    if (outgoing.get(target).contains(source)) adjacent.get(source).add(target);
                    continue;
                }
                if (direction != EdgeOrientation.IN) adjacent.get(source).add(target);
                if (direction != EdgeOrientation.OUT) adjacent.get(target).add(source);
            }
        int[][] result = new int[size][];
        for (int i = 0; i < size; i++) result[i] = adjacent.get(i).stream().mapToInt(Integer::intValue).sorted().toArray();
        return result;
    }

    /**
     * Finds weak components in snapshot order, with sorted node indices in each.
     * @param graph indexed graph
     * @return component node indices
     */
    public static List<int[]> components(IndexedGraphSnapshot<?> graph)
    {
        int[][] adjacent = neighbours(graph, EdgeOrientation.UND);
        boolean[] visited = new boolean[adjacent.length];
        List<int[]> result = new ArrayList<>();
        Deque<Integer> queue = new ArrayDeque<>();
        for (int start = 0; start < adjacent.length; start++)
        {
            if (visited[start]) continue;
            List<Integer> component = new ArrayList<>();
            visited[start] = true;
            queue.add(start);
            while (!queue.isEmpty())
            {
                int node = queue.remove();
                component.add(node);
                for (int neighbour : adjacent[node])
                    if (!visited[neighbour]) { visited[neighbour] = true; queue.add(neighbour); }
            }
            result.add(component.stream().mapToInt(Integer::intValue).sorted().toArray());
        }
        return result;
    }
}
