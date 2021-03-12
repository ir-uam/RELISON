package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.pathbased;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import org.jblas.DoubleMatrix;
import org.jblas.Eigen;
import org.jblas.Solve;

public class GlobalLHNIndex<U> extends MatrixBasedRecommender<U>
{
    private final double phi;
    /**
     * Constructor.
     *
     * @param graph A fast graph representing the social network.
     */
    public GlobalLHNIndex(FastGraph<U> graph, double phi)
    {
        super(graph);
        this.phi = phi;
        this.matrix = this.getMatrix();
    }

    @Override
    protected DoubleMatrix getMatrix()
    {
        DoubleMatrix adj = graph.getJBLASAdjacencyMatrix(EdgeOrientation.UND);
        double edgeCount = adj.sum();
        DoubleMatrix eigen = Eigen.symmetricEigenvalues(adj);
        int[] largest = eigen.sortingPermutation();

        // The largest eigenvalue.
        double lambda = eigen.get(largest[largest.length-1]);

        DoubleMatrix sum = adj.rowSums();
        DoubleMatrix D = DoubleMatrix.zeros(numUsers());
        for(int i = 0; i < numUsers(); ++i)
        {
            D.put(i, i, 1.0/sum.get(i));
        }

        DoubleMatrix X = DoubleMatrix.eye(numUsers());
        adj.muli(phi/lambda);

        X.addi(adj.mul(-1.0));

        DoubleMatrix Z = Solve.solve(X, DoubleMatrix.eye(numUsers()));

        DoubleMatrix lhn = D.mmul(Z).mmul(D).mul(edgeCount*lambda);
        return lhn;
    }
}
