/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.community.detection.modularity;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.socialranksys.community.graph.SimpleCommunityGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeWeight;
import es.uam.eps.ir.socialranksys.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.socialranksys.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.gephi.appearance.api.*;
import org.gephi.appearance.plugin.PartitionElementColorTransformer;
import org.gephi.graph.api.*;
import org.gephi.project.api.ProjectController;
import org.gephi.statistics.plugin.Modularity;
import org.openide.util.Lookup;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Class for computing the Louvain community detection algorithm.
 * <p>
 * <b>Reference:</b>  V. Blondel, J. Guillaume, R. Lambiotte, E. Lefebvre, Fast unfolding of communities in large networks. Journal of Statistical Mechanics 10 (2008)
 * </p>
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
/*public class OwnLouvain<U extends Serializable> implements CommunityDetectionAlgorithm<U>
{
    @Override
    public Communities<U> detectCommunities(Graph<U> graph)
    {
        // First, we assign each node to a single community
        Object2IntMap<U> map = new Object2IntOpenHashMap<>();
        graph.getAllNodes().forEach(u -> map.put(u, map.size()));

        double modularity =












        Communities<U> communities = new Communities<>();
        // First, initialize a project and get a workspace
        ProjectController pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();

        // Get all the necessary controllers and models
        GraphModel graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel();
        AppearanceController appearanceController = Lookup.getDefault().lookup(AppearanceController.class);
        AppearanceModel appearanceModel = appearanceController.getModel();

        // Load the graph into the Gephi workspace
        Map<U, Node> nodes = new HashMap<>();
        Map<String, U> nodeIds = new HashMap<>();

        // Get the graph as undirected

        graph.getAllNodes().forEach(u ->
        {
            Node node = graphModel.factory().newNode(u.toString());
            node.setLabel(u.toString());
            graphModel.getUndirectedGraph().addNode(node);
            nodes.put(u, node);
            nodeIds.put(u.toString(), u);
        });

        Set<U> neighbors = new HashSet<>();
        graph.getAllNodes().forEach(u ->
        {
            Node uNode = nodes.get(u);
            graph.getNeighbourNodes(u).forEach(v ->
            {
                if (neighbors.contains(v))
                {
                    Node vNode = nodes.get(v);
                    if (graph.isDirected())
                    {
                        double a = graph.getEdgeWeight(u, v);
                        double b = graph.getEdgeWeight(v, u);
                        double weight = 0.0;
                        if (!EdgeWeight.isErrorValue(a))
                        {
                            weight += a;
                        }
                        if (!EdgeWeight.isErrorValue(b))
                        {
                            weight += b;
                        }

                        Edge e = graphModel.factory().newEdge(uNode, vNode, 1, weight, false);
                        graphModel.getUndirectedGraph().addEdge(e);
                    }
                    else
                    {
                        double a = graph.getEdgeWeight(u, v);
                        Edge e = graphModel.factory().newEdge(uNode, vNode, 1, a, true);
                        graphModel.getUndirectedGraph().addEdge(e);
                    }
                }
            });
            neighbors.add(u);
        });

        // Compute the modularity (and the partition using the Louvain algorithm)

        Modularity mod = new Modularity();
        mod.setRandom(true);
        mod.execute(graphModel);

        // Get the column for the node table including the communities data.
        Column modColumn = graphModel.getNodeTable().getColumn(Modularity.MODULARITY_CLASS);
        Function function = appearanceModel.getNodeFunction(graphModel.getGraph(), modColumn, PartitionElementColorTransformer.class);
        Partition partition = ((PartitionFunction) function).getPartition();

        // Create the partition
        int numPartitions = partition.size();
        for (int i = 0; i < numPartitions; ++i)
        {
            communities.addCommunity();
        }

        // Generate the communities

        graphModel.getUndirectedGraph().getNodes().forEach(node ->
        {
            String uName = node.getLabel();
            U u = nodeIds.get(uName);
            int comm = (int) node.getAttribute(modColumn);
            communities.add(u, comm);
        });

        return communities;
    }

    @Override
    public Communities<U> detectCommunities(Graph<U> graph, List<Pair<U>> newLinks, List<Pair<U>> disapLinks, Communities<U> previous)
    {
        try
        {
            // First, we detect nodes with new links or links which have disappeared.
            Set<U> newLinkNodes = new HashSet<>();

            // Add nodes with new links
            newLinks.forEach(p ->
            {
                newLinkNodes.add(p.v1());
               newLinkNodes.add(p.v2());
            });

            // Add nodes with disappeared links
            disapLinks.forEach(p ->
            {
                newLinkNodes.add(p.v1());
                newLinkNodes.add(p.v2());
            });

            // First, we build a new community partition, where each of the users
            // with newly added or removed links for its own community. The rest of users
            // remain in the same communities.           
            Communities<U> comms = new Communities<>();
            for (U user : newLinkNodes)
            {
                comms.addCommunity();
                comms.add(user, comms.getNumCommunities() - 1);
            }

            previous.getCommunities().forEach(comm ->
            {
                int i = comms.getNumCommunities();
                Set<U> cUsers = previous.getUsers(comm).filter(u -> !newLinkNodes.contains(u)).collect(Collectors.toSet());
                if (!cUsers.isEmpty())
                {
                    comms.addCommunity();
                    cUsers.forEach(u -> comms.add(u, i));
                }
            });

            // Generate a small graph: each community is a node, and links between communities have
            // weight equal to the sum of weights between communities.
            SimpleCommunityGraphGenerator<U> ggen = new SimpleCommunityGraphGenerator<>();
            ggen.configure(graph, comms, false);
            Graph<Integer> smallGraph = ggen.generate();

            // Find the Louvain communities for the small graph
            OwnLouvain<Integer> louvain = new OwnLouvain<>();
            Communities<Integer> smallgraphComms = louvain.detectCommunities(smallGraph);

            // Determine the new communities:
            Communities<U> defComms = new Communities<>();

            IntStream.range(0, smallgraphComms.getNumCommunities()).forEach(c ->
            {
                defComms.addCommunity();
                smallgraphComms.getUsers(c).forEach(userComm -> comms.getUsers(userComm).forEach(u -> defComms.add(u, c)));
            });

            return defComms;
        }
        catch (GeneratorNotConfiguredException | GeneratorBadConfiguredException ex) // In case this fails.
        {
            return null;
        }
    }

}
*/