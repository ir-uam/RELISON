/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.transforms;

import es.uam.eps.ir.relison.utils.datatypes.Pair;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.*;
import java.util.*;

/**
 * Shelf-packs the bounding boxes of weak components using rigid translations.
 * A component containing any pinned node is anchored in its entirety. Movable
 * components are packed to the right of all anchors. Overlapping anchored boxes,
 * including the requested gap, are rejected as an unsatisfiable constraint.
 * Bounds describe node centres, not glyph sizes. Empty/single-component layouts
 * are unchanged. Use through LayoutPipeline or the graph-aware process overload.
 * @param <U> node type
 */
public final class ComponentPacking<U> implements LayoutPostProcessor<U>
{
    private final double gap;

    /** Uses unit separation between component bounding boxes. */
    public ComponentPacking() { this(1); }

    /**
     * @param gap finite positive separation between component bounding boxes
     */
    public ComponentPacking(double gap) { this.gap = AbstractStaticLayout.positive(gap, "gap"); }

    @Override
    public LayoutResult<U> process(LayoutResult<U> result, LayoutRequest<U> request)
    {
        throw new IllegalArgumentException("Component packing requires a graph; use LayoutPipeline or the graph-aware overload");
    }

    @Override
    public LayoutResult<U> process(Graph<U> graph, LayoutResult<U> result, LayoutRequest<U> request)
    {
        Objects.requireNonNull(result);
        IndexedGraphSnapshot<U> snapshot = IndexedGraphSnapshot.capture(graph, request);
        if (!result.getPositions().keySet().equals(new HashSet<>(snapshot.getNodes())))
            throw new IllegalArgumentException("Result must contain exactly the graph nodes");
        for (U pin : request.getPinnedNodes())
            if (!request.getInitialPositions().get(pin).equals(result.getPositions().get(pin)))
                throw new IllegalArgumentException("Input result does not preserve its pins");
        List<int[]> components = Topology.components(snapshot);
        if (components.size() < 2) return result;
        List<Box<U>> movable = new ArrayList<>(), anchored = new ArrayList<>();
        for (int[] component : components)
        {
            List<U> nodes = new ArrayList<>();
            List<Pair<Double>> points = new ArrayList<>();
            boolean pinned = false;
            for (int index : component)
            {
                U node = snapshot.getNodes().get(index);
                nodes.add(node);
                points.add(result.getPositions().get(node));
                pinned |= request.getPinnedNodes().contains(node);
            }
            Box<U> box = new Box<>(nodes, Bounds2D.of(points), gap);
            (pinned ? anchored : movable).add(box);
        }
        double originX = 0;
        for (int i = 0; i < anchored.size(); i++)
        {
            Box<U> box = anchored.get(i);
            originX = Math.max(originX, box.bounds.getMaxX() + gap);
            for (int j = 0; j < i; j++)
                if (overlap(box.bounds, anchored.get(j).bounds))
                    throw new IllegalArgumentException("Pinned component bounding boxes overlap or cannot meet the requested gap");
        }
        if (movable.isEmpty()) return result;
        double area = 0, widest = 0;
        for (Box<U> box : movable) { area += box.width * box.height; widest = Math.max(widest, box.width); }
        if (!Double.isFinite(area) || !Double.isFinite(originX)) throw new IllegalArgumentException("Component extents are too large to pack");
        double shelfWidth = Math.max(widest, Math.sqrt(area));
        // Stable sort retains snapshot component order when heights tie.
        movable.sort(Comparator.comparingDouble((Box<U> box) -> box.height).reversed());
        double x = 0, y = 0, shelfHeight = 0;
        Map<U, Pair<Double>> positions = new LinkedHashMap<>(result.getPositions());
        for (Box<U> box : movable)
        {
            if (x > 0 && box.width > shelfWidth - x) { y += shelfHeight; x = 0; shelfHeight = 0; }
            double dx = originX + x + gap / 2 - box.bounds.getMinX();
            double dy = y + gap / 2 - box.bounds.getMinY();
            for (U node : box.nodes)
            {
                Pair<Double> point = result.getPositions().get(node);
                positions.put(node, new Pair<>(point.v1() + dx, point.v2() + dy));
            }
            x += box.width;
            shelfHeight = Math.max(shelfHeight, box.height);
        }
        return new LayoutResult<>(positions, result.getDiagnostics());
    }

    private boolean overlap(Bounds2D first, Bounds2D second)
    {
        return first.getMinX() - gap / 2 < second.getMaxX() + gap / 2
            && first.getMaxX() + gap / 2 > second.getMinX() - gap / 2
            && first.getMinY() - gap / 2 < second.getMaxY() + gap / 2
            && first.getMaxY() + gap / 2 > second.getMinY() - gap / 2;
    }

    private static final class Box<U>
    {
        final List<U> nodes;
        final Bounds2D bounds;
        final double width, height;
        Box(List<U> nodes, Bounds2D bounds, double gap)
        {
            this.nodes = nodes;
            this.bounds = bounds;
            width = bounds.getMaxX() - bounds.getMinX() + gap;
            height = bounds.getMaxY() - bounds.getMinY() + gap;
            if (!Double.isFinite(width) || !Double.isFinite(height)) throw new IllegalArgumentException("Component extents are too large to pack");
        }
    }
}
