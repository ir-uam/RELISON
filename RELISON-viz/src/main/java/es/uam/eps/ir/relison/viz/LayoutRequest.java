/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz;

import es.uam.eps.ir.relison.utils.datatypes.Pair;

import java.util.*;

/**
 * Immutable common layout inputs. Every pinned node must have an initial position.
 * An explicit node order must be an exact permutation of the graph's nodes.
 * Node objects themselves are not copied and must retain stable equality/hash codes.
 * @param <U> node type
 */
public final class LayoutRequest<U>
{
    private final Map<U, Pair<Double>> initialPositions;
    private final Set<U> pinnedNodes;
    private final List<U> nodeOrder;
    private final long seed;

    private LayoutRequest(Builder<U> builder)
    {
        initialPositions = Collections.unmodifiableMap(new LinkedHashMap<>(builder.positions));
        pinnedNodes = Collections.unmodifiableSet(new LinkedHashSet<>(builder.pins));
        nodeOrder = builder.order == null ? null : Collections.unmodifiableList(new ArrayList<>(builder.order));
        seed = builder.seed;
        initialPositions.forEach((node, point) -> { Objects.requireNonNull(node); Objects.requireNonNull(point); });
        Bounds2D.of(initialPositions.values());
        if (!initialPositions.keySet().containsAll(pinnedNodes))
            throw new IllegalArgumentException("Every pinned node requires an initial position");
        if (nodeOrder != null && (nodeOrder.contains(null) || new HashSet<>(nodeOrder).size() != nodeOrder.size()))
            throw new IllegalArgumentException("Node order cannot contain nulls or duplicates");
    }

    /**
     * @param <U> node type
     * @return builder with seed zero and no constraints
     */
    public static <U> Builder<U> builder() { return new Builder<>(); }
    /**
     * @param <U> node type
     * @return default request
     */
    public static <U> LayoutRequest<U> defaults() { return LayoutRequest.<U>builder().build(); }
    /** @return immutable initial positions */
    public Map<U, Pair<Double>> getInitialPositions() { return initialPositions; }
    /** @return immutable pinned nodes */
    public Set<U> getPinnedNodes() { return pinnedNodes; }
    /** @return explicit order, or empty if graph encounter order should be used */
    public Optional<List<U>> getNodeOrder() { return Optional.ofNullable(nodeOrder); }
    /** @return random seed */
    public long getSeed() { return seed; }

    /**
     * Mutable request builder.
     * @param <U> node type
     */
    public static final class Builder<U>
    {
        private Map<U, Pair<Double>> positions = Collections.emptyMap();
        private Set<U> pins = Collections.emptySet();
        private List<U> order;
        private long seed;

        private Builder() { }
        /**
         * @param value coordinates
         * @return this builder
         */
        public Builder<U> initialPositions(Map<U, Pair<Double>> value) { positions = new LinkedHashMap<>(value); return this; }
        /**
         * @param value pinned nodes
         * @return this builder
         */
        public Builder<U> pinnedNodes(Set<U> value) { pins = new LinkedHashSet<>(value); return this; }
        /**
         * @param value exact node order
         * @return this builder
         */
        public Builder<U> nodeOrder(List<U> value) { order = new ArrayList<>(value); return this; }
        /**
         * @param value seed
         * @return this builder
         */
        public Builder<U> seed(long value) { seed = value; return this; }
        /** @return immutable request */
        public LayoutRequest<U> build() { return new LayoutRequest<>(this); }
    }
}
