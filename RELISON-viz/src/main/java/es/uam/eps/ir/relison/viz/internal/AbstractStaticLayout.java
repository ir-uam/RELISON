/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.internal;

import es.uam.eps.ir.relison.utils.datatypes.Pair;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.viz.*;
import java.util.*;

/**
 * Shared snapshot, output validation, and exact pin handling.
 * @param <U> node type
 */
public abstract class AbstractStaticLayout<U> implements Layout<U>
{
    private final LayoutDescriptor descriptor;

    /**
     * @param id stable identity
     * @param name display name
     */
    protected AbstractStaticLayout(String id, String name) { descriptor = new LayoutDescriptor(id, name); }

    @Override
    public final LayoutDescriptor getDescriptor() { return descriptor; }

    @Override
    public final LayoutResult<U> compute(Graph<U> graph, LayoutRequest<U> request)
    {
        IndexedGraphSnapshot<U> snapshot = IndexedGraphSnapshot.capture(graph, request);
        Map<U, Pair<Double>> generated = generate(snapshot, request);
        if (!generated.keySet().equals(new HashSet<>(snapshot.getNodes())))
            throw new IllegalStateException("Layout must place every graph node exactly once");
        Map<U, Pair<Double>> positions = new LinkedHashMap<>();
        for (U node : snapshot.getNodes())
            positions.put(node, request.getPinnedNodes().contains(node)
                ? request.getInitialPositions().get(node) : Objects.requireNonNull(generated.get(node)));
        return new LayoutResult<>(positions, new LayoutDiagnostics(descriptor.getId(), LayoutDiagnostics.Termination.COMPLETED));
    }

    /**
     * @param graph indexed snapshot
     * @param request common inputs
     * @return generated coordinates
     */
    protected abstract Map<U, Pair<Double>> generate(IndexedGraphSnapshot<U> graph, LayoutRequest<U> request);

    /**
     * @param value parameter
     * @param name parameter name
     * @return validated positive finite value
     */
    public static double positive(double value, String name)
    {
        if (!Double.isFinite(value) || value <= 0) throw new IllegalArgumentException(name + " must be finite and positive");
        return value;
    }

    /**
     * @param nodes ordered nodes
     * @param radius radius
     * @param startAngle angle in radians
     * @param positions destination
     * @param <U> node type
     */
    public static <U> void ring(List<U> nodes, double radius, double startAngle, Map<U, Pair<Double>> positions)
    {
        for (int i = 0; i < nodes.size(); i++)
        {
            double angle = startAngle + 2 * Math.PI * i / nodes.size();
            positions.put(nodes.get(i), new Pair<>(radius * Math.cos(angle), radius * Math.sin(angle)));
        }
    }
}
