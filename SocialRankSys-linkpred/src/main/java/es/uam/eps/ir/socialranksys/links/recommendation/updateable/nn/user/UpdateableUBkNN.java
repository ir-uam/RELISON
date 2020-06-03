package es.uam.eps.ir.socialranksys.links.recommendation.updateable.nn.user;

import es.uam.eps.ir.ranksys.nn.neighborhood.TopKNeighborhood;
import es.uam.eps.ir.socialranksys.links.data.updateable.preference.fast.updateable.FastUpdateablePreferenceData;
import es.uam.eps.ir.socialranksys.links.recommendation.updateable.fast.FastUpdateableRankingRecommender;
import es.uam.eps.ir.socialranksys.links.recommendation.updateable.nn.sim.UpdateableGraphSimilarity;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.jooq.lambda.tuple.Tuple3;

import java.util.stream.Stream;

public class UpdateableUBkNN<U> extends FastUpdateableRankingRecommender<U,U>
{
    private final UpdateableGraphSimilarity sim;
    private final int k;

    public UpdateableUBkNN(FastUpdateablePreferenceData<U,U> prefData, UpdateableGraphSimilarity similarity, int k)
    {
        super(prefData);
        this.k = k;
        this.sim = similarity;
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx)
    {
        Int2DoubleOpenHashMap scores = new Int2DoubleOpenHashMap();
        scores.defaultReturnValue(0.0);
        TopKNeighborhood neighborhood = new TopKNeighborhood(this.sim, this.k);
        neighborhood.getNeighbors(uidx).forEach(neighbor ->
        {
            int widx = neighbor.v1;
            double sim = neighbor.v2;

            prefData.getUidxPreferences(widx).forEach(vW -> scores.addTo(vW.v1, vW.v2 * sim));
        });
        return scores;
    }


    @Override
    public void update(Stream<Tuple3<U, U, Double>> tuples)
    {
        tuples.forEach(t -> this.update(t.v1, t.v2, t.v3));
    }

    @Override
    public void updateDelete(Stream<Tuple3<U, U, Double>> tuples)
    {

    }

    @Override
    public void updateAddUser(U u)
    {
        this.sim.updateAddElement();
        this.prefData.updateAddUser(u);

    }

    @Override
    public void updateAddItem(U u)
    {
        this.updateAddUser(u);
    }

    @Override
    public void update(U u, U v, double val)
    {
        this.sim.updateAdd(this.user2uidx(u), this.user2uidx(v), val);
        this.prefData.update(u, v, val);
    }

    @Override
    public void updateDelete(U u, U u2, double val)
    {

    }
}
