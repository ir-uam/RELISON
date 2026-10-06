/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz;


import es.uam.eps.ir.relison.graph.Graph;

/**
 * A coordinate-processing stage that must preserve node coverage and pins.
 * @param <U> node type
 */
@FunctionalInterface
public interface LayoutPostProcessor<U>
{
    /**
     * @param result input coordinates
     * @param request original constraints
     * @return processed coordinates
     */
    LayoutResult<U> process(LayoutResult<U> result, LayoutRequest<U> request);

    /**
     * Graph-aware processing hook. Coordinate-only processors retain their existing behaviour.
     * The graph must not be modified during processing.
     * @param graph input graph
     * @param result input coordinates
     * @param request original constraints
     * @return processed coordinates
     */
    default LayoutResult<U> process(Graph<U> graph, LayoutResult<U> result, LayoutRequest<U> request)
    {
        return process(result, request);
    }
}
