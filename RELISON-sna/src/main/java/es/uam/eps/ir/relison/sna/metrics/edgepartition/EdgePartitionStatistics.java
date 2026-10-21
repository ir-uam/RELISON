package es.uam.eps.ir.relison.sna.metrics.edgepartition;

import es.uam.eps.ir.relison.utils.ComputationMonitor;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;
import es.uam.eps.ir.relison.sna.edge.Edge;
import es.uam.eps.ir.relison.sna.edge.EdgePartition;
import es.uam.eps.ir.relison.sna.edge.detection.EdgePartitionSupport;
import java.util.*;

/**
 * Snapshot of counts or group membership in the supplied graph. Edge snapshots
 * accept only live edges and resolve stable multigraph ids to their current index.
 * Node membership ignores direction and counts a self-loop's endpoint once.
 */
public final class EdgePartitionStatistics<U>
{
    /** Build only the data required by the metric, in increasing levels of detail. */
    public enum Detail { COUNTS, EDGES, NODES, MEMBERSHIPS }

    public final Graph<U> graph;
    public final int[] groupSizes;
    public final List<List<Edge<U>>> edges = new ArrayList<>();
    public final List<Set<U>> nodes = new ArrayList<>();
    /** Includes every nonisolated graph node, even when none of its edges is assigned. */
    public final Map<U, Set<Integer>> nodeGroups = new HashMap<>();
    public long graphEdgeCount;
    public long assignedEdgeCount;

    public EdgePartitionStatistics(Graph<U> graph, EdgePartition<U> partition)
    {
        this(graph, partition, Detail.MEMBERSHIPS);
    }

    /** Count-only metrics use the partition's stored sizes and the graph's edge counter.
     * As with community partitions, callers must supply a partition of the current graph.
     * Higher detail levels also validate membership against the live graph.
     */
    public EdgePartitionStatistics(Graph<U> graph, EdgePartition<U> partition, Detail detail)
    {
        ComputationMonitor.current().phase("Preparing partition statistics");
        this.graph = Objects.requireNonNull(graph);
        Objects.requireNonNull(partition);
        Objects.requireNonNull(detail);
        groupSizes = new int[partition.getNumPartitions()];
        graphEdgeCount = graph.getEdgeCount();
        partition.getPartitions().forEach(group -> {
            checkInterrupted();
            groupSizes[group] = partition.getPartitionSize(group);
            assignedEdgeCount += groupSizes[group];
        });
        if (detail == Detail.COUNTS) return;
        boolean needNodes = detail == Detail.NODES || detail == Detail.MEMBERSHIPS;
        boolean needMemberships = detail == Detail.MEMBERSHIPS;
        partition.getPartitions().forEach(group -> {
            edges.add(new ArrayList<>());
            if (needNodes) nodes.add(new HashSet<>());
        });
        EdgePartitionSupport.forEachEdge(graph, edge -> {
            checkInterrupted();
            if (needMemberships)
            {
                nodeGroups.computeIfAbsent(edge.getSource(), node -> new HashSet<>());
                nodeGroups.computeIfAbsent(edge.getTarget(), node -> new HashSet<>());
            }
            int group = partition.getPartition(edge);
            if (group < 0) return;
            edges.get(group).add(edge);
            if (needNodes)
            {
                nodes.get(group).add(edge.getSource());
                nodes.get(group).add(edge.getTarget());
            }
            if (needMemberships)
            {
                nodeGroups.get(edge.getSource()).add(group);
                nodeGroups.get(edge.getTarget()).add(group);
            }
        });
        partition.getPartitions().forEach(group -> {
            if (edges.get(group).size() != partition.getPartitionSize(group))
                throw new IllegalArgumentException("Edge partition contains edges absent from the supplied graph.");
        });
    }

    private static void checkInterrupted()
    {
        if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
    }

    public double weight(Edge<U> edge)
    {
        double value = graph instanceof MultiGraph<?>
                ? ((MultiGraph<U>) graph).getEdgeWeights(edge.getSource(), edge.getTarget()).get(edge.getIndex())
                : graph.getEdgeWeight(edge.getSource(), edge.getTarget());
        if (!Double.isFinite(value))
            throw new IllegalArgumentException("Edge weight must be finite.");
        return value;
    }

    public double totalWeight(int group)
    {
        double sum = edges.get(group).stream().mapToDouble(this::weight).sum();
        if (!Double.isFinite(sum)) throw new IllegalArgumentException("Group weight exceeds the finite numeric range.");
        return sum;
    }

    public long sharedNodes(int group)
    {
        return nodes.get(group).stream().filter(node -> nodeGroups.get(node).size() > 1).count();
    }

    public int groupDegree(int group)
    {
        Set<Integer> neighbours = new HashSet<>();
        nodes.get(group).forEach(node -> neighbours.addAll(nodeGroups.get(node)));
        neighbours.remove(group);
        return neighbours.size();
    }

    /** Weakly connected component sizes, computed without recursion, using only this group's edges. */
    public List<Integer> componentSizes(int group)
    {
        Map<U, Set<U>> adjacent = new HashMap<>();
        nodes.get(group).forEach(node -> adjacent.put(node, new HashSet<>()));
        edges.get(group).forEach(edge -> {
            adjacent.get(edge.getSource()).add(edge.getTarget());
            adjacent.get(edge.getTarget()).add(edge.getSource());
        });
        Set<U> seen = new HashSet<>();
        List<Integer> sizes = new ArrayList<>();
        for (U node : nodes.get(group))
        {
            if (!seen.add(node)) continue;
            Deque<U> queue = new ArrayDeque<>();
            queue.add(node);
            int size = 0;
            while (!queue.isEmpty())
            {
                if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
                U current = queue.removeFirst();
                size++;
                for (U neighbour : adjacent.get(current))
                    if (seen.add(neighbour)) queue.addLast(neighbour);
            }
            sizes.add(size);
        }
        return sizes;
    }
}
