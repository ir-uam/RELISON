/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz;


import es.uam.eps.ir.relison.graph.Graph;

/**
 * Headless layout computation. Implementations must not mutate the input graph.
 * The graph must not be modified while its snapshot is being captured.
 * @param <U> node type
 */
public interface Layout<U>
{
    /**
     * @param graph input graph
     * @param request common inputs
     * @return layout result
     */
    LayoutResult<U> compute(Graph<U> graph, LayoutRequest<U> request);
    /**
     * @param graph input graph
     * @return layout with default inputs
     */
    default LayoutResult<U> compute(Graph<U> graph) { return compute(graph, LayoutRequest.defaults()); }
    /** @return algorithm identity */
    LayoutDescriptor getDescriptor();
}
