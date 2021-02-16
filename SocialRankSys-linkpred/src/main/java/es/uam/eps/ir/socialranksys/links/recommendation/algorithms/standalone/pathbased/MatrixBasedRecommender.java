package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.pathbased;

import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.jblas.DoubleMatrix;

public abstract class MatrixBasedRecommender<U> extends UserFastRankingRecommender<U>
{
    protected DoubleMatrix matrix;

    /**
     * Constructor.
     *
     * @param graph A fast graph representing the social network.
     */
    public MatrixBasedRecommender(FastGraph<U> graph)
    {
        super(graph);
        matrix = null;
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx)
    {
        if(matrix == null) this.matrix = this.getMatrix();

        Int2DoubleMap scoresMap = new Int2DoubleOpenHashMap();
        for(int vidx = 0; vidx < matrix.columns; ++vidx)
        {
            scoresMap.put(vidx, matrix.get(uidx, vidx));
        }

        return scoresMap;
    }

    protected abstract DoubleMatrix getMatrix();
}
