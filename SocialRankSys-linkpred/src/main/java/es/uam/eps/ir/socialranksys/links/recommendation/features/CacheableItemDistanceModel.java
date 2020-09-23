package es.uam.eps.ir.socialranksys.links.recommendation.features;

import es.uam.eps.ir.ranksys.novdiv.distance.ItemDistanceModel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.ToDoubleFunction;

public class CacheableItemDistanceModel<I> implements ItemDistanceModel<I>
{
    private final Map<I, Map<I, Double>> cache;
    private final ItemDistanceModel<I> model;
    private final boolean symmetric;

    public CacheableItemDistanceModel(ItemDistanceModel<I> model, boolean symmetric)
    {
        this.model = model;
        cache = new HashMap<>();
        this.symmetric = symmetric;
    }


    @Override
    public ToDoubleFunction<I> dist(I i)
    {
        return (I j) -> dist(i,j);
    }

    @Override
    public double dist(I i, I j)
    {
        if(cache.containsKey(i) && cache.get(i).containsKey(j))
        {
            return cache.get(i).get(j);
        }
        else
        {
            double dist = model.dist(i,j);
            if(!cache.containsKey(i))
            {
                cache.put(i, new HashMap<>());
            }
            cache.get(i).put(j,dist);

            if(symmetric)
            {
                if(!cache.containsKey(j))
                {
                    cache.put(j, new HashMap<>());
                }
                cache.get(j).put(i,dist);
            }
        }
        return 0;
    }
}
