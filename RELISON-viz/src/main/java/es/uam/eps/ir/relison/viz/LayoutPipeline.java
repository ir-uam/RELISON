/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz;


import es.uam.eps.ir.relison.graph.Graph;
import java.util.*;

/**
 * Applies ordered processors to a base layout, checking coverage and pins.
 * @param <U> node type
 */
public final class LayoutPipeline<U> implements Layout<U>
{
    private final Layout<U> base;
    private final List<LayoutPostProcessor<U>> processors;

    /**
     * @param base base algorithm
     * @param processors ordered coordinate processors
     */
    public LayoutPipeline(Layout<U> base, List<? extends LayoutPostProcessor<U>> processors)
    {
        this.base = Objects.requireNonNull(base);
        List<LayoutPostProcessor<U>> copy = new ArrayList<>(processors);
        copy.forEach(Objects::requireNonNull);
        this.processors = Collections.unmodifiableList(copy);
    }

    @Override
    public LayoutDescriptor getDescriptor() { return base.getDescriptor(); }

    @Override
    public LayoutResult<U> compute(Graph<U> graph, LayoutRequest<U> request)
    {
        LayoutResult<U> result = base.compute(graph, request);
        Set<U> nodes = result.getPositions().keySet();
        for (LayoutPostProcessor<U> processor : processors)
        {
            result = Objects.requireNonNull(processor.process(graph, result, request));
            if (!result.getPositions().keySet().equals(nodes))
                throw new IllegalStateException("Processor changed node coverage");
            for (U node : request.getPinnedNodes())
                if (!Objects.equals(result.getPositions().get(node), request.getInitialPositions().get(node)))
                    throw new IllegalStateException("Processor moved a pinned node");
        }
        return result;
    }
}
