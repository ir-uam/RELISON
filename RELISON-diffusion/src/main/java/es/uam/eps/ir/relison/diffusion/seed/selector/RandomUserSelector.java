/*
 *  Copyright (C) 2024 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.seed.selector;

import es.uam.eps.ir.relison.graph.Graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Selects a random subset of the users of the network — either a fraction of the nodes (a random {@code %}) or an
 * absolute number of them, chosen uniformly at random without replacement. Provide a seed for reproducibility.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 *
 * @param <U> type of the users.
 */
public class RandomUserSelector<U> implements UserSelector<U>
{
    /**
     * Fraction of the nodes to select (in {@code [0, 1]}); ignored (negative) when a fixed count is used instead.
     */
    private final double fraction;
    /**
     * Absolute number of nodes to select; negative when a fraction is used instead.
     */
    private final int count;
    /**
     * Random number generator driving the selection.
     */
    private final Random rng;

    /**
     * Selects a fraction of the nodes with a non-reproducible random generator.
     * @param fraction the fraction of nodes to select (clamped to {@code [0, 1]}).
     */
    public RandomUserSelector(double fraction)
    {
        this(fraction, new Random());
    }

    /**
     * Selects a fraction of the nodes with a reproducible (seeded) random generator.
     * @param fraction the fraction of nodes to select (clamped to {@code [0, 1]}).
     * @param seed     the seed for the random number generator.
     */
    public RandomUserSelector(double fraction, long seed)
    {
        this(fraction, new Random(seed));
    }

    /**
     * Selects a fraction of the nodes with a given random generator.
     * @param fraction the fraction of nodes to select (clamped to {@code [0, 1]}).
     * @param rng      the random number generator.
     */
    public RandomUserSelector(double fraction, Random rng)
    {
        this.fraction = Math.max(0.0, Math.min(1.0, fraction));
        this.count = -1;
        this.rng = rng;
    }

    private RandomUserSelector(int count, Random rng)
    {
        this.fraction = -1.0;
        this.count = Math.max(0, count);
        this.rng = rng;
    }

    /**
     * Creates a selector that picks a fixed number of nodes at random (non-reproducible).
     * @param count the number of nodes to select.
     * @param <U>   type of the users.
     * @return the selector.
     */
    public static <U> RandomUserSelector<U> byCount(int count)
    {
        return new RandomUserSelector<>(count, new Random());
    }

    /**
     * Creates a selector that picks a fixed number of nodes at random with a reproducible (seeded) generator.
     * @param count the number of nodes to select.
     * @param seed  the seed for the random number generator.
     * @param <U>   type of the users.
     * @return the selector.
     */
    public static <U> RandomUserSelector<U> byCount(int count, long seed)
    {
        return new RandomUserSelector<>(count, new Random(seed));
    }

    @Override
    public Set<U> select(Graph<U> graph)
    {
        List<U> nodes = new ArrayList<>();
        graph.getAllNodes().forEach(nodes::add);

        int n = (this.count >= 0) ? this.count : (int) Math.round(this.fraction * nodes.size());
        n = Math.max(0, Math.min(n, nodes.size()));

        Collections.shuffle(nodes, this.rng);
        return new LinkedHashSet<>(nodes.subList(0, n));
    }
}
