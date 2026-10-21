package es.uam.eps.ir.relison.sna.metrics.communities.indiv;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.utils.ComputationMonitor;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.edge.detection.EdgePartitionSupport;
import java.util.*;

/**
 * Per-computation snapshot, scanning each physical edge once. Parallel edges retain
 * their own mass. Undirected edges contribute two degree incidences, including loops.
 * Unassigned nodes lie outside every community, but are not a neighboring community.
 */
final class CommunityBoundaryStatistics<U>
{
    final boolean directed;
    final long[] members;
    final double[] internal, outVolume, inVolume, outCut, inCut;
    final List<Set<U>> outBoundary = new ArrayList<>(), inBoundary = new ArrayList<>();
    final List<Set<Integer>> outNeighbours = new ArrayList<>(), inNeighbours = new ArrayList<>();
    double totalMass;
    boolean complete = true;

    CommunityBoundaryStatistics(Graph<U> graph, Communities<U> communities, boolean weighted)
    {
        ComputationMonitor.current().phase("Preparing community statistics");
        Objects.requireNonNull(graph);
        Objects.requireNonNull(communities);
        directed = graph.isDirected();
        int count = communities.getNumCommunities();
        members = new long[count];
        internal = new double[count];
        outVolume = new double[count];
        inVolume = new double[count];
        outCut = new double[count];
        inCut = new double[count];
        for (int group = 0; group < count; group++)
        {
            outBoundary.add(new HashSet<>());
            inBoundary.add(new HashSet<>());
            outNeighbours.add(new HashSet<>());
            inNeighbours.add(new HashSet<>());
        }
        graph.getAllNodes().forEach(node -> {
            int group = communities.getCommunity(node);
            if (group >= 0) members[group]++;
            else complete = false;
        });
        communities.getCommunities().forEach(group -> {
            if (members[group] != communities.getCommunitySize(group))
                throw new IllegalArgumentException("Community contains nodes absent from the supplied graph.");
        });
        EdgePartitionSupport.forEachEdge(graph, edge -> {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            double mass = 1.0;
            if (weighted)
            {
                mass = graph instanceof MultiGraph<?>
                        ? ((MultiGraph<U>) graph).getEdgeWeights(edge.getSource(), edge.getTarget()).get(edge.getIndex())
                        : graph.getEdgeWeight(edge.getSource(), edge.getTarget());
                if (!Double.isFinite(mass) || mass < 0.0)
                    throw new IllegalArgumentException("Weighted community boundary metrics require finite nonnegative edge weights.");
            }
            int source = communities.getCommunity(edge.getSource()), target = communities.getCommunity(edge.getTarget());
            totalMass += mass;
            if (source >= 0 && source == target) internal[source] += mass;
            incidence(edge.getSource(), edge.getTarget(), source, target, mass);
            if (!directed) incidence(edge.getTarget(), edge.getSource(), target, source, mass);
        });
        if (!Double.isFinite(totalMass) || !Double.isFinite(2.0 * totalMass))
            throw new IllegalArgumentException("Graph edge mass exceeds the supported finite numeric range.");
    }

    private void incidence(U sourceNode, U targetNode, int source, int target, double mass)
    {
        if (source >= 0) outVolume[source] += mass;
        if (target >= 0) inVolume[target] += mass;
        if (source == target) return;
        if (source >= 0)
        {
            outCut[source] += mass;
            outBoundary.get(source).add(sourceNode);
            if (target >= 0) outNeighbours.get(source).add(target);
        }
        if (target >= 0)
        {
            inCut[target] += mass;
            inBoundary.get(target).add(targetNode);
            if (source >= 0) inNeighbours.get(target).add(source);
        }
    }

    double volume(int group, EdgeOrientation orientation)
    {
        if (!directed || orientation == EdgeOrientation.OUT) return outVolume[group];
        if (orientation == EdgeOrientation.IN) return inVolume[group];
        return outVolume[group] + inVolume[group];
    }

    double cut(int group, EdgeOrientation orientation)
    {
        if (!directed || orientation == EdgeOrientation.OUT) return outCut[group];
        if (orientation == EdgeOrientation.IN) return inCut[group];
        return outCut[group] + inCut[group];
    }

    long boundaryNodes(int group, EdgeOrientation orientation)
    {
        if (!directed || orientation == EdgeOrientation.OUT) return outBoundary.get(group).size();
        if (orientation == EdgeOrientation.IN) return inBoundary.get(group).size();
        Set<U> union = new HashSet<>(outBoundary.get(group));
        union.addAll(inBoundary.get(group));
        return union.size();
    }

    long neighboringCommunities(int group, EdgeOrientation orientation)
    {
        if (!directed || orientation == EdgeOrientation.OUT) return outNeighbours.get(group).size();
        if (orientation == EdgeOrientation.IN) return inNeighbours.get(group).size();
        Set<Integer> union = new HashSet<>(outNeighbours.get(group));
        union.addAll(inNeighbours.get(group));
        return union.size();
    }
}
