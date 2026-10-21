package es.uam.eps.ir.relison.metrics;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.fast.FastDirectedUnweightedGraph;
import es.uam.eps.ir.relison.graph.fast.FastUndirectedUnweightedGraph;
import es.uam.eps.ir.relison.sna.metrics.vertex.KatzCentrality;
import es.uam.eps.ir.relison.utils.matrix.MatrixLibrary;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class KatzCentralityTest
{
    private final MatrixLibrary library;

    public KatzCentralityTest(MatrixLibrary library)
    {
        this.library = library;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> libraries()
    {
        return Arrays.asList(new Object[][] {
                {MatrixLibrary.JBLAS}, {MatrixLibrary.MTJ}, {MatrixLibrary.COLT}
        });
    }

    @Test
    public void incomingPathsIncludeLongerWalks()
    {
        Map<Integer, Double> scores = new KatzCentrality<Integer>(library, 0.2).compute(chain());
        assertScores(scores, 0.0, 0.2, 0.24, 0.0);
    }

    @Test
    public void outgoingPathsReverseTheScores()
    {
        Map<Integer, Double> scores = new KatzCentrality<Integer>(library, EdgeOrientation.OUT, 0.2)
                .compute(chain());
        assertScores(scores, 0.24, 0.2, 0.0, 0.0);
    }

    @Test
    public void zeroAlphaGivesZeroScores()
    {
        assertScores(new KatzCentrality<Integer>(library, 0.0).compute(chain()), 0.0, 0.0, 0.0, 0.0);
    }

    @Test
    public void isolatedNodesHaveZeroScores()
    {
        Graph<Integer> graph = new FastDirectedUnweightedGraph<>();
        graph.addNode(10);
        graph.addNode(20);
        Map<Integer, Double> scores = new KatzCentrality<Integer>(library, 0.2).compute(graph);
        assertEquals(2, scores.size());
        assertEquals(0.0, scores.get(10), 1e-10);
        assertEquals(0.0, scores.get(20), 1e-10);
    }

    @Test
    public void undirectedEdgeIncludesRepeatedWalks()
    {
        Graph<Integer> graph = new FastUndirectedUnweightedGraph<>();
        graph.addEdge(10, 20);
        Map<Integer, Double> scores = new KatzCentrality<Integer>(library, 0.2).compute(graph);
        assertEquals(2, scores.size());
        assertEquals(0.25, scores.get(10), 1e-10);
        assertEquals(0.25, scores.get(20), 1e-10);
    }

    private Graph<Integer> chain()
    {
        Graph<Integer> graph = new FastDirectedUnweightedGraph<>();
        for (int node : new int[] {10, 20, 30, 40}) graph.addNode(node);
        graph.addEdge(10, 20);
        graph.addEdge(20, 30);
        return graph;
    }

    private void assertScores(Map<Integer, Double> scores, double... expected)
    {
        assertEquals(expected.length, scores.size());
        for (int i = 0; i < expected.length; ++i)
            assertEquals(expected[i], scores.get((i + 1) * 10), 1e-10);
    }
}
