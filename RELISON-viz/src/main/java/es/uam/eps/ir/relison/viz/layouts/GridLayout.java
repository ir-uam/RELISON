/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;

import es.uam.eps.ir.relison.utils.datatypes.Pair;

import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.*;
import java.util.*;

/**
 * Row-major grid centred at the origin.
 * @param <U> node type
 */
public final class GridLayout<U> extends AbstractStaticLayout<U>
{
    private final int columns;
    private final double spacing;
    /** Creates an approximately square grid with unit spacing. */
    public GridLayout() { this(0, 1); }
    /**
     * @param columns column count, or zero for automatic
     * @param spacing row and column spacing
     */
    public GridLayout(int columns, double spacing)
    {
        super("grid", "Grid");
        if (columns < 0) throw new IllegalArgumentException("columns must be non-negative");
        this.columns = columns;
        this.spacing = positive(spacing, "spacing");
    }

    @Override
    protected Map<U, Pair<Double>> generate(IndexedGraphSnapshot<U> graph, LayoutRequest<U> request)
    {
        Map<U, Pair<Double>> positions = new LinkedHashMap<>();
        int count = graph.getNodes().size();
        if (count == 0) return positions;
        int cols = columns == 0 ? (int) Math.ceil(Math.sqrt(count)) : Math.min(columns, count);
        int rows = (count - 1) / cols + 1;
        for (int i = 0; i < count; i++)
            positions.put(graph.getNodes().get(i), new Pair<>((i % cols - (cols - 1) / 2.0) * spacing, (i / cols - (rows - 1) / 2.0) * spacing));
        return positions;
    }
}
