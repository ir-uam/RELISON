package es.uam.eps.ir.relison.sna.edge.detection.community;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.edge.Edge;
import es.uam.eps.ir.relison.sna.edge.EdgePartition;
import es.uam.eps.ir.relison.sna.edge.detection.EdgePartitionDetector;
import es.uam.eps.ir.relison.sna.edge.detection.EdgePartitionSupport;

/** Partitions edges according to whether they are within or between communities. */
public class CommunityInternalExternalEdgePartitionDetector<U> implements EdgePartitionDetector<U>
{
    public static final int INTRACOMMUNITY = 0;
    public static final int INTERCOMMUNITY = 1;
    public static final int UNASSIGNED = 2;

    private final Communities<U> communities;

    public CommunityInternalExternalEdgePartitionDetector(Communities<U> communities)
    {
        if (communities == null) throw new IllegalArgumentException("Communities cannot be null");
        this.communities = communities;
    }

    @Override
    public EdgePartition<U> detectEdgePartition(Graph<U> graph)
    {
        if (graph == null) return null;
        EdgePartition<U> partition = new EdgePartition<>();
        partition.addPartition();
        partition.addPartition();
        partition.addPartition();
        EdgePartitionSupport.forEachEdge(graph, edge ->
        {
            int sourceCommunity = communities.getCommunity(edge.getSource());
            int targetCommunity = communities.getCommunity(edge.getTarget());
            int group = sourceCommunity < 0 || targetCommunity < 0 ? UNASSIGNED
                    : sourceCommunity == targetCommunity ? INTRACOMMUNITY : INTERCOMMUNITY;
            partition.add(edge, group);
        });
        return partition;
    }
}
