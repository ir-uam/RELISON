/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;

import es.uam.eps.ir.relison.utils.datatypes.Pair;

import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.*;
import java.util.*;

/**
 * Restores supplied coordinates; every graph node must have a position.
 * @param <U> node type
 */
public final class PresetLayout<U> extends AbstractStaticLayout<U>
{
    /** Creates a preset layout. */
    public PresetLayout() { super("preset", "Saved / manual"); }

    @Override
    protected Map<U, Pair<Double>> generate(IndexedGraphSnapshot<U> graph, LayoutRequest<U> request)
    {
        if (request.getInitialPositions().size() != graph.getNodes().size())
            throw new IllegalArgumentException("Preset layout requires a position for every node");
        return request.getInitialPositions();
    }
}
