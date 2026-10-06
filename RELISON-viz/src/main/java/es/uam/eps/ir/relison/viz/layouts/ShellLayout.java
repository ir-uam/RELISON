/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;

import es.uam.eps.ir.relison.utils.datatypes.Pair;

import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.*;
import java.util.*;

/**
 * User-defined shells, ordered from inside to outside. Shells must partition
 * every graph node exactly once; node order within shells follows the request.
 * A single node in the innermost shell is centred at the origin.
 * @param <U> node type
 */
public final class ShellLayout<U> extends AbstractStaticLayout<U>
{
    private final List<List<U>> shells;
    private final double spacing;

    /**
     * @param shells node groups from inside to outside
     * @param spacing radial separation
     */
    public ShellLayout(List<? extends Collection<U>> shells, double spacing)
    {
        super("shell", "Shell");
        this.spacing = positive(spacing, "spacing");
        List<List<U>> copy = new ArrayList<>();
        Set<U> seen = new HashSet<>();
        for (Collection<U> shell : shells)
        {
            if (shell.isEmpty()) throw new IllegalArgumentException("Shells must not be empty");
            for (U node : shell)
                if (node == null || !seen.add(node)) throw new IllegalArgumentException("Shell nodes must be non-null and unique");
            copy.add(Collections.unmodifiableList(new ArrayList<>(shell)));
        }
        this.shells = Collections.unmodifiableList(copy);
    }

    @Override
    protected Map<U, Pair<Double>> generate(IndexedGraphSnapshot<U> graph, LayoutRequest<U> request)
    {
        Map<U, Pair<Double>> positions = new LinkedHashMap<>();
        Set<U> all = new HashSet<>();
        shells.forEach(all::addAll);
        if (!all.equals(new HashSet<>(graph.getNodes())))
            throw new IllegalArgumentException("Shells must contain exactly the graph nodes");
        Map<U, Integer> membership = new HashMap<>();
        List<List<U>> ordered = new ArrayList<>();
        for (int i = 0; i < shells.size(); i++)
        {
            ordered.add(new ArrayList<>());
            for (U node : shells.get(i)) membership.put(node, i);
        }
        for (U node : graph.getNodes()) ordered.get(membership.get(node)).add(node);
        boolean centred = !shells.isEmpty() && shells.get(0).size() == 1;
        for (int i = 0; i < ordered.size(); i++) ring(ordered.get(i), (centred ? i : i + 1.0) * spacing, 0, positions);
        return positions;
    }
}
