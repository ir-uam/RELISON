package es.uam.eps.ir.relison.sna.edge.graph;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.generator.*;
import es.uam.eps.ir.relison.graph.generator.exception.*;
import es.uam.eps.ir.relison.graph.multigraph.MultiGraph;
import es.uam.eps.ir.relison.sna.edge.Edge;
import es.uam.eps.ir.relison.sna.edge.EdgePartition;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates the subgraph consisting of a group's edges and their endpoints.
 * Direction, multiplicity, loops, weights, edge types and available attributes
 * are preserved. Other edges between covered nodes are deliberately excluded.
 */
public class EdgePartitionSubgraphGenerator<U> implements GraphGenerator<U>
{
    private Graph<U> graph;
    private EdgePartition<U> partition;
    private Collection<Edge<U>> selected;
    private int group;
    private boolean configured;

    @Override
    @SuppressWarnings("unchecked")
    public void configure(Object... configuration)
    {
        configured = false;
        if (configuration != null && configuration.length == 3)
            configure((Graph<U>) configuration[0], (EdgePartition<U>) configuration[1], (Integer) configuration[2]);
    }

    public void configure(Graph<U> graph, EdgePartition<U> partition, int group)
    {
        this.graph = graph;
        this.partition = partition;
        this.group = group;
        this.selected = null;
        configured = true;
    }

    /** Configures a preselected collection, useful when processing many groups from one membership snapshot. */
    public void configure(Graph<U> graph, Collection<Edge<U>> selected)
    {
        this.graph = graph;
        this.selected = selected;
        this.partition = null;
        configured = true;
    }

    @Override
    public Graph<U> generate() throws GeneratorNotConfiguredException, GeneratorBadConfiguredException
    {
        if (!configured) throw new GeneratorNotConfiguredException("Edge subgraph generator is not configured.");
        if (graph == null || (partition == null && selected == null)
                || (partition != null && (group < 0 || group >= partition.getNumPartitions())))
            throw new GeneratorBadConfiguredException("Invalid graph or edge group.");
        Collection<Edge<U>> edges = partition == null ? selected
                : partition.getEdges(group).collect(Collectors.toList());
        GraphGenerator<U> generator = graph.isMultigraph() ? new EmptyMultiGraphGenerator<>() : new EmptyGraphGenerator<>();
        generator.configure(graph.isDirected(), graph.isWeighted());
        Graph<U> result = generator.generate();
        List<String> nodeAttributes = names(true), edgeAttributes = names(false);
        nodeAttributes.forEach(name -> result.defineNodeAttribute(name, graph.getNodeAttributeType(name)));
        edgeAttributes.forEach(name -> result.defineEdgeAttribute(name, graph.getEdgeAttributeType(name)));
        for (Edge<U> edge : edges)
        {
            if (Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            U source = edge.getSource(), target = edge.getTarget();
            if (edge.isDirected() != graph.isDirected() || !graph.containsEdge(source, target))
                throw new GeneratorBadConfiguredException("Selected edge is absent from the graph.");
            int index = edge.getIndex();
            double weight;
            int type;
            MultiGraph<U> multiGraph = graph instanceof MultiGraph<?> ? (MultiGraph<U>) graph : null;
            if (multiGraph != null)
            {
                if (edge.getId() != null)
                {
                    List<Long> ids = multiGraph.getEdgeIds(source, target);
                    if (index < 0 || index >= ids.size() || !edge.getId().equals(ids.get(index)))
                        index = ids.indexOf(edge.getId());
                }
                if (index < 0 || index >= multiGraph.getNumEdges(source, target))
                    throw new GeneratorBadConfiguredException("Selected parallel edge is absent from the graph.");
                weight = multiGraph.getEdgeWeights(source, target).get(index);
                type = multiGraph.getEdgeTypes(source, target).get(index);
            }
            else
            {
                weight = graph.getEdgeWeight(source, target);
                type = graph.getEdgeType(source, target);
            }
            if (!Double.isFinite(weight)) throw new GeneratorBadConfiguredException("Edge weight must be finite.");
            if (!result.addEdge(source, target, weight, type))
                throw new GeneratorBadConfiguredException("Could not copy selected edge.");
            for (String name : nodeAttributes)
            {
                result.setNodeAttribute(source, name, graph.getNodeAttribute(source, name));
                result.setNodeAttribute(target, name, graph.getNodeAttribute(target, name));
            }
            for (String name : edgeAttributes)
            {
                if (multiGraph == null)
                    result.setEdgeAttribute(source, target, name, graph.getEdgeAttribute(source, target, name));
                else
                {
                    MultiGraph<U> output = (MultiGraph<U>) result;
                    output.setEdgeAttribute(source, target, output.getNumEdges(source, target) - 1, name,
                            multiGraph.getEdgeAttribute(source, target, index, name));
                }
            }
        }
        return result;
    }

    private List<String> names(boolean nodes)
    {
        try
        {
            Stream<String> names = nodes ? graph.getNodeAttributeNames() : graph.getEdgeAttributeNames();
            return names.collect(Collectors.toList());
        }
        catch (UnsupportedOperationException e) { return Collections.emptyList(); }
    }
}
