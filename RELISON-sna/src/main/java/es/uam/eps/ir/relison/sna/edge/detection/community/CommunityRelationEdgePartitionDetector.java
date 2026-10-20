package es.uam.eps.ir.relison.sna.edge.detection.community;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.edge.Edge;
import es.uam.eps.ir.relison.sna.edge.EdgePartition;
import es.uam.eps.ir.relison.sna.edge.detection.EdgePartitionDetector;
import es.uam.eps.ir.relison.sna.edge.detection.EdgePartitionSupport;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Groups edges by the ordered (directed) or unordered (undirected) pair of endpoint communities. */
public class CommunityRelationEdgePartitionDetector<U> implements EdgePartitionDetector<U>
{
    private final Communities<U> communities;

    public CommunityRelationEdgePartitionDetector(Communities<U> communities)
    {
        if (communities == null) throw new IllegalArgumentException("Communities cannot be null");
        this.communities = communities;
    }

    @Override
    public EdgePartition<U> detectEdgePartition(Graph<U> graph)
    {
        if (graph == null) return null;
        EdgePartition<U> partition = new EdgePartition<>();
        Map<CommunityPair, Integer> groups = new LinkedHashMap<>();
        EdgePartitionSupport.forEachEdge(graph, edge ->
        {
            int source = communities.getCommunity(edge.getSource());
            int target = communities.getCommunity(edge.getTarget());
            CommunityPair pair = new CommunityPair(source, target, graph.isDirected());
            Integer group = groups.get(pair);
            if (group == null)
            {
                group = groups.size();
                groups.put(pair, group);
                partition.addPartition();
            }
            partition.add(edge, group);
        });
        return partition;
    }

    private static final class CommunityPair
    {
        private final int source;
        private final int target;

        private CommunityPair(int source, int target, boolean directed)
        {
            this.source = !directed && source > target ? target : source;
            this.target = !directed && source > target ? source : target;
        }

        @Override
        public boolean equals(Object other)
        {
            if (this == other) return true;
            if (!(other instanceof CommunityPair)) return false;
            CommunityPair pair = (CommunityPair) other;
            return source == pair.source && target == pair.target;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(source, target);
        }
    }
}
