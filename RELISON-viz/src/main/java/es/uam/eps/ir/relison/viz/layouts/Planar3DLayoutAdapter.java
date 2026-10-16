/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.transforms.OverlapRemoval;
import java.util.*;

/** Adapts an existing 2D layout to a flat 3D plane by assigning z = 0. */
public final class Planar3DLayoutAdapter<U> implements Layout<U> {
    private final Layout<U> layout2D;
    private final LayoutPostProcessor<U> processor2D;

    /** @param layout2D layout to compute x/y coordinates */
    public Planar3DLayoutAdapter(Layout<U> layout2D) { this(layout2D, null); }

    /**
     * @param layout2D layout to compute x/y coordinates
     * @param processor2D optional 2D postprocessor applied before lifting points
     */
    public Planar3DLayoutAdapter(Layout<U> layout2D, LayoutPostProcessor<U> processor2D) {
        this.layout2D=Objects.requireNonNull(layout2D);
        this.processor2D=processor2D;
        if(layout2D.getDescriptor().getSpaceDimensions()!=2)
            throw new IllegalArgumentException("The wrapped layout must be two-dimensional");
    }

    @Override public LayoutDescriptor getDescriptor() {
        LayoutDescriptor base=layout2D.getDescriptor();
        return new LayoutDescriptor(base.getId()+"-flat-3d",base.getName()+" (flat in 3D)",3);
    }

    @Override public LayoutResult<U> compute(Graph<U> graph,LayoutRequest<U> request) {
        for(U pin:request.getPinnedNodes()) {
            Point3D point=request.getInitialPositions3D().get(pin);
            if(point!=null&&point.getZ()!=0)
                throw new IllegalArgumentException("A flat 2D layout cannot preserve a pinned node outside z = 0");
        }
        LayoutRequest.Builder<U> builder=LayoutRequest.<U>builder()
            .initialPositions(request.getInitialPositions())
            .pinnedNodes(request.getPinnedNodes())
            .seed(request.getSeed())
            .maxIterations(request.getMaxIterations())
            .timeLimitMillis(request.getTimeLimitMillis())
            .nodeSizes(request.getNodeSizes())
            .cancellation(request.getCancellation());
        request.getNodeOrder().ifPresent(builder::nodeOrder);
        LayoutRequest<U> request2D=builder.build();
        LayoutResult<U> result=layout2D.compute(graph,request2D);
        if(processor2D!=null) result=processor2D.process(graph,result,request2D);
        Map<U,Point3D> points=new LinkedHashMap<>();
        for(Map.Entry<U,Pair<Double>> entry:result.getPositions().entrySet())
            points.put(entry.getKey(),new Point3D(entry.getValue().v1(),entry.getValue().v2(),0));
        LayoutDiagnostics diagnostics=result.getDiagnostics();
        return LayoutResult.spatial(points,new LayoutDiagnostics(getDescriptor().getId(),diagnostics.getTermination(),
            diagnostics.getIterations(),diagnostics.getMaximumDisplacement()));
    }
}
