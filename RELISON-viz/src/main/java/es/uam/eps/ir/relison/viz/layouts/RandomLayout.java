/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;

import es.uam.eps.ir.relison.utils.datatypes.Pair;

import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.*;
import java.util.*;

/**
 * Seeded uniform placement in a rectangle centred at the origin.
 * @param <U> node type
 */
public final class RandomLayout<U> extends AbstractStaticLayout<U>
{
    private final double width, height;
    /** Creates a layout in a unit square. */
    public RandomLayout() { this(1, 1); }
    /**
     * @param width rectangle width
     * @param height rectangle height
     */
    public RandomLayout(double width, double height)
    {
        super("random", "Random");
        this.width = positive(width, "width");
        this.height = positive(height, "height");
    }

    @Override
    protected Map<U, Pair<Double>> generate(IndexedGraphSnapshot<U> graph, LayoutRequest<U> request)
    {
        Random random = new Random(request.getSeed());
        Map<U, Pair<Double>> positions = new LinkedHashMap<>();
        for (U node : graph.getNodes())
            positions.put(node, new Pair<>((random.nextDouble() - 0.5) * width, (random.nextDouble() - 0.5) * height));
        return positions;
    }
}
