/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz;
import es.uam.eps.ir.relison.graph.Graph;
/**
 * Independent sessions with a compatible batch API.
 * @param <U> node type
 */
public interface IterativeLayout<U> extends Layout<U> {
    /**
     * @param graph input graph
     * @param request inputs and limits
     * @return new independent session
     */
    LayoutSession<U> initialize(Graph<U> graph, LayoutRequest<U> request);
    @Override
    default LayoutResult<U> compute(Graph<U> graph, LayoutRequest<U> request) {
        LayoutSession<U> session = initialize(graph, request);
        while (!session.isFinished()) session.step(1);
        return session.snapshot();
    }
}
