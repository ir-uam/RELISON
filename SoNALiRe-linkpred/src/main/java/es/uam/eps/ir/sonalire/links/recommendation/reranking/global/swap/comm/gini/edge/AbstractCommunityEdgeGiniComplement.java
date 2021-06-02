package es.uam.eps.ir.sonalire.links.recommendation.reranking.global.swap.comm.gini.edge;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.sonalire.community.Communities;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.global.swap.comm.CommunityReranker;
import es.uam.eps.ir.sonalire.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.sonalire.utils.indexes.FastGiniIndex;
import org.ranksys.core.util.tuples.Tuple2od;

import java.util.Map;
import java.util.function.Supplier;
/**
 * Swap reranker for promoting the balance in the distribution of the number of links
 * between pairs of communities.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 */
public abstract class AbstractCommunityEdgeGiniComplement<U> extends CommunityReranker<U>
{
    /**
     * Fast implementation of the Gini index.
     */
    private FastGiniIndex gini;
    /**
     * True if we want to force links to go outside communities, false otherwise.
     */
    private final boolean outer;

    /**
     * Constructor.
     * @param lambda        establishes the trade-off between the value of the Gini Index and the original score
     * @param cutoff        the maximum number of contacts in the definitive rankings.
     * @param norm          the normalization scheme.
     * @param graph         the original graph.
     * @param communities   the relation between communities and users in the graph.
     * @param selfloops     true if selfloops are allowed, false if they are not.
     * @param outer         true if we want to force links to go outside communities.
     */
    public AbstractCommunityEdgeGiniComplement(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities, boolean selfloops, boolean outer)
    {
        super(lambda, cutoff, norm, graph, communities, selfloops);
        this.outer = outer;
    }

    @Override
    protected double novAddDelete(U u, Tuple2od<U> newValue, Tuple2od<U> oldValue)
    {
        U recomm = newValue.v1;
        U del = oldValue.v1;

        int userComm = communities.getCommunity(u);
        int recComm = communities.getCommunity(recomm);
        int delComm = communities.getCommunity(del);

        if(outer && userComm == recComm)
        {
            return -1.0;
        }

        int addIndex = this.findIndex(userComm, recComm);
        int delIndex = this.findIndex(userComm, delComm);

        if(addIndex != -1) gini.increaseFrequency(addIndex);
        if(delIndex != -1) gini.decreaseFrequency(delIndex);

        double value = gini.getValue();

        if(addIndex != -1) gini.decreaseFrequency(addIndex);
        if(delIndex != -1) gini.increaseFrequency(delIndex);

        return value;
    }

    @Override
    protected double novAdd(U u, Tuple2od<U> newValue, Tuple2od<U> oldValue)
    {
        U recomm = newValue.v1;

        int userComm = communities.getCommunity(u);
        int recComm = communities.getCommunity(recomm);

        if(outer && userComm == recComm)
        {
            return -1.0;
        }

        int addIndex = this.findIndex(userComm, recComm);

        if(addIndex != -1) gini.increaseFrequency(addIndex);
        double value = gini.getValue();
        if(addIndex != -1) gini.decreaseFrequency(addIndex);

        return value;
    }

    @Override
    protected double novDelete(U u, Tuple2od<U> newValue, Tuple2od<U> oldValue)
    {
        U del = oldValue.v1;
        U recomm = newValue.v1;

        int userComm = communities.getCommunity(u);
        int recComm = communities.getCommunity(recomm);
        int delComm = communities.getCommunity(del);

        if(outer && userComm == recComm)
        {
            return -1.0;
        }

        int delIndex = this.findIndex(userComm, delComm);
        if(delIndex != -1) gini.decreaseFrequency(delIndex);
        double value = gini.getValue();
        if(delIndex != -1) gini.increaseFrequency(delIndex);

        return value;
    }

    @Override
    protected void innerUpdate(U user, Tuple2od<U> updated, Tuple2od<U> old)
    {
        U recomm = updated.v1;
        U del = old.v1;

        int userComm = communities.getCommunity(user);
        int recComm = communities.getCommunity(recomm);
        int delComm = communities.getCommunity(del);

        int addIndex = (communityGraph.isDirected() || !recs.get(del).contains(user)) ? this.findIndex(userComm, recComm) : -1;
        int delIndex = (communityGraph.isDirected() || !recs.get(recomm).contains(user)) ? this.findIndex(userComm, delComm) : -1;

        if(addIndex != -1) gini.increaseFrequency(addIndex);
        if(delIndex != -1) gini.decreaseFrequency(delIndex);
    }

    @Override
    protected void update(Recommendation<U, U> reranked)
    {

    }

    @Override
    public void computeGlobalValue()
    {
        super.computeGlobalValue();

        if(this.gini == null)
        {
            Map<Integer, Long> frequencies = this.computeInitialFrequencies();
            this.gini = new FastGiniIndex(frequencies.size(), frequencies);
        }

        this.globalvalue = 1.0 - this.gini.getValue();
    }

    /**
     * Finds the index of a pair of communities in the Gini index calculation.
     * @param userComm      the community of the target user.
     * @param recommComm    the community of the candidate user.
     * @return the index if it exists, -1 otherwise.
     */
    protected abstract int findIndex(int userComm, int recommComm);

    /**
     * Given the initial graph, computes the initial number of edges between the
     * different pairs of communities.
     * @return the number of edges between the different pairs of communities.
     */
    protected abstract Map<Integer, Long> computeInitialFrequencies();
}
