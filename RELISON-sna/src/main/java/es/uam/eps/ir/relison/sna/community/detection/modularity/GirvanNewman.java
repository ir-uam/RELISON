package es.uam.eps.ir.relison.sna.community.detection.modularity;

import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.relison.sna.community.detection.DendogramCommunityDetectionAlgorithm;
import es.uam.eps.ir.relison.sna.community.detection.connectedness.WeaklyConnectedComponents;
import es.uam.eps.ir.relison.sna.metrics.distance.pair.EdgeBetweenness;
import es.uam.eps.ir.relison.sna.community.Dendogram;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.generator.GraphCloneGenerator;
import es.uam.eps.ir.relison.graph.generator.SubGraphGenerator;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.relison.index.fast.FastIndex;
import es.uam.eps.ir.relison.sna.metrics.communities.graph.Modularity;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import org.jooq.lambda.tuple.Tuple2;
import org.jooq.lambda.tuple.Tuple3;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the Girvan-Newman community detection algorithm, based on removing edges with the highest
 * betweenness value.
 *
 * <p><b>Reference: </b>M. Girvan, M.E.J. Newman. Community structure in social and biological networks, Proc. Natl. Acad. Sci. USA 99, 7821–7826 (2002)</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 */
public class GirvanNewman<U> implements CommunityDetectionAlgorithm<U>, DendogramCommunityDetectionAlgorithm<U>
{
    /**
     * The optimal number of communities.
     */
    private int optimalNumComms;

    @Override
    public Communities<U> detectCommunities(Graph<U> graph)
    {
        Dendogram<U> dendogram = this.detectCommunityDendogram(graph);
        return dendogram.getCommunitiesByNumber(this.optimalNumComms);
    }

    @Override
    public Dendogram<U> detectCommunityDendogram(Graph<U> graph)
    {
        Graph<U> clone;
        try
        {
            GraphCloneGenerator<U> ggen = new GraphCloneGenerator<>();
            ggen.configure(graph);
            clone = ggen.generate();
        }
        catch (GeneratorNotConfiguredException | GeneratorBadConfiguredException e)
        {
            return null; // Although this might never happen.
        }

        // We have to build the dendogram. The dendogram has, as leaves, the different nodes in the
        // graph. So:
        FastIndex<U> fastIndex = new FastIndex<>();

        List<Tuple3<Integer, Integer, Integer>> triplets = new ArrayList<>();

        Communities<U> aux;
        Map<Integer, Integer> clusterMap = new HashMap<>();

        graph.getAllNodes().forEach(fastIndex::addObject);

        int minCluster = 2*fastIndex.numObjects()-2;
        clusterMap.put(0, minCluster);

        // We first compute the edge betweenness of the edges in the original network:
        EdgeBetweenness<U> edgeBetweenness = new EdgeBetweenness<>(false);
        Map<Pair<U>, Double> betweenness = edgeBetweenness.compute(graph);

        Comparator<Tuple2<Pair<U>, Double>> comparator = (o1, o2) -> Double.compare(o2.v2, o1.v2);

        // As a first step, we find the connected components of the network.
        CommunityDetectionAlgorithm<U> connectedness = new WeaklyConnectedComponents<>();
        aux = connectedness.detectCommunities(graph);

        // As a first step, we find the dendogram for the wcc:
        if(aux.getNumCommunities() > 1)
        {
            for(int i = 0; i < aux.getNumCommunities() - 2; ++i)
            {
                int parent = minCluster;
                int leftChild;
                if(aux.getCommunitySize(0) == 1)
                {
                    U u = aux.getUsers(i).collect(Collectors.toList()).get(0);
                    leftChild = fastIndex.object2idx(u);
                }
                else
                {
                    leftChild = --minCluster;
                }

                int rightChild = --minCluster;

                clusterMap.put(i, leftChild);
                triplets.add(new Tuple3<>(leftChild, rightChild, parent));
            }

            // Now we test the final two:
            int parent = minCluster;
            int leftChild;
            int index = aux.getNumCommunities()-2;
            if(aux.getCommunitySize(index) == 1)
            {
                U u = aux.getUsers(index).collect(Collectors.toList()).get(0);
                leftChild = fastIndex.object2idx(u);
            }
            else
            {
                leftChild = --minCluster;
            }

            clusterMap.put(index, leftChild);

            int rightChild;
            ++index;
            if(aux.getCommunitySize(index) == 1)
            {
                U u = aux.getUsers(index).collect(Collectors.toList()).get(0);
                rightChild = fastIndex.object2idx(u);
            }
            else
            {
                rightChild = --minCluster;
            }

            clusterMap.put(index, rightChild);

            triplets.add(new Tuple3<>(leftChild, rightChild, parent));
        }

        Modularity<U> mod = new Modularity<>();
        double maxq = mod.compute(graph, aux);

        while(aux.getNumCommunities() < graph.getVertexCount())
        {
            try
            {
                // First step: we sort the links by betweenness value:
                List<Tuple2<Pair<U>, Double>> list = new ArrayList<>();
                betweenness.forEach((key, value) -> list.add(new Tuple2<>(key, value)));
                list.sort(comparator);

                // We take the edge with the highest betweenness in the network:
                Pair<U> p = list.get(0).v1;

                // Then
                // a) we remove the edge
                clone.removeEdge(p.v1(), p.v2());

                // b) we find a subgraph for the connected component the edge belonged to:
                int comm = aux.getCommunity(p.v1());
                SubGraphGenerator<U> subGraphGenerator = new SubGraphGenerator<>();
                subGraphGenerator.configure(clone, aux.getUsers(aux.getCommunity(p.v1())).collect(Collectors.toCollection(HashSet::new)));
                Graph<U> subgraph = subGraphGenerator.generate();

                // c) we update the edge betweenness values for such subgraph
                EdgeBetweenness<U> betw = new EdgeBetweenness<>(false);
                Map<Pair<U>, Double> newBetw = betw.compute(subgraph);
                newBetw.forEach(betweenness::put);
                betweenness.remove(p);

                // d) we check whether removing that edge has lead us to a new community division.
                Communities<U> scc = connectedness.detectCommunities(subgraph);

                // if the component is divided in two:
                if (scc.getNumCommunities() > 1)
                {
                    // We first obtain the new community partition
                    Communities<U> aux2 = new Communities<>();
                    for (int i = 0; i < aux.getNumCommunities(); ++i)
                    {
                        int j = i;
                        aux2.addCommunity();
                        if (i == comm)
                        {
                            scc.getUsers(0).forEach(u -> aux2.add(u, j));
                        }
                        else
                        {
                            aux.getUsers(i).forEach(u -> aux2.add(u, j));
                        }
                    }

                    aux2.addCommunity();
                    Communities<U> finalAux = aux;
                    scc.getUsers(1).forEach(u -> aux2.add(u, finalAux.getNumCommunities()));

                    // We find the corresponding modularity, to see whether the division is the optimal one:
                    double q = mod.compute(graph, aux2);
                    if (q > maxq)
                    {
                        this.optimalNumComms = aux2.getNumCommunities();
                        maxq = q;
                    }

                    // Then, we find the new edges for the dendogram:
                    int parent = clusterMap.get(comm);
                    int childrenA;
                    if (scc.getCommunitySize(0) == 1)
                    {
                        U u = scc.getCommunity(p.v1()) == 0 ? p.v1() : p.v2();
                        childrenA = fastIndex.object2idx(u);
                    }
                    else
                    {
                        childrenA = --minCluster;
                    }

                    int childrenB;
                    if (scc.getCommunitySize(1) == 1)
                    {
                        U u = scc.getCommunity(p.v1()) == 1 ? p.v1() : p.v2();
                        childrenB = fastIndex.object2idx(u);
                    }
                    else
                    {
                        childrenB = --minCluster;
                    }

                    triplets.add(new Tuple3<>(childrenA, childrenB, parent));
                    clusterMap.put(parent, childrenA);
                    clusterMap.put(aux2.getNumCommunities() - 1, childrenB);
                    aux = aux2;
                }
            }
            catch (GeneratorNotConfiguredException | GeneratorBadConfiguredException e)
            {
                e.printStackTrace(); // This should never happen
            }
        }

        return new Dendogram<>(fastIndex, graph, triplets.stream());
    }
}