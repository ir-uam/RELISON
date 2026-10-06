/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;

import es.uam.eps.ir.relison.utils.datatypes.Pair;

import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.*;
import java.util.*;
import java.util.function.ToDoubleFunction;

/**
 * Equal scores share a ring, with higher scores nearer the centre. Ties retain
 * request order. Scores are evaluated once per node and must be finite.
 * Supply precomputed values when scores originate from mutable analysis state.
 * @param <U> node type
 */
public final class ConcentricLayout<U> extends AbstractStaticLayout<U>
{
    private final ToDoubleFunction<U> score;
    private final double spacing;
    /** @param score node scoring function */
    public ConcentricLayout(ToDoubleFunction<U> score) { this(score, 1); }
    /**
     * @param score node scoring function
     * @param spacing radial separation
     */
    public ConcentricLayout(ToDoubleFunction<U> score, double spacing)
    {
        super("concentric", "Concentric by score");
        this.score = Objects.requireNonNull(score);
        this.spacing = positive(spacing, "spacing");
    }

    @Override
    protected Map<U, Pair<Double>> generate(IndexedGraphSnapshot<U> graph, LayoutRequest<U> request)
    {
        Map<Double, List<U>> groups = new TreeMap<>(Comparator.reverseOrder());
        for (U node : graph.getNodes())
        {
            double value = score.applyAsDouble(node);
            if (!Double.isFinite(value)) throw new IllegalArgumentException("Scores must be finite");
            groups.computeIfAbsent(value == 0 ? 0.0 : value, key -> new ArrayList<>()).add(node);
        }
        Map<U, Pair<Double>> positions = new LinkedHashMap<>();
        int index = !groups.isEmpty() && groups.values().iterator().next().size() == 1 ? 0 : 1;
        for (List<U> group : groups.values()) ring(group, index++ * spacing, 0, positions);
        return positions;
    }
}
