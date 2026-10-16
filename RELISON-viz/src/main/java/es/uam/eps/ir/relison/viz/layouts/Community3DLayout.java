/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import es.uam.eps.ir.relison.viz.*;
import java.util.*;

/** Keeps 2D community composition intact and separates its groups onto depth layers. */
public final class Community3DLayout<U> implements Layout<U> {
    private final List<List<U>> groups;
    private final Layout<U> inner;
    private final Layout<Integer> outer;
    private final double gap;
    public Community3DLayout(List<? extends List<U>> groups,Layout<U> inner,Layout<Integer> outer,double gap) {
        this.groups=new ArrayList<>();for(List<U> group:groups)this.groups.add(new ArrayList<>(group));
        this.inner=Objects.requireNonNull(inner);this.outer=Objects.requireNonNull(outer);
        if(!Double.isFinite(gap)||gap<0)throw new IllegalArgumentException("Community gap must be finite and nonnegative");this.gap=gap;
    }
    @Override public LayoutDescriptor getDescriptor() { return new LayoutDescriptor("community-3d","Community composition 3D (depth layers)",3); }
    @Override public LayoutResult<U> compute(Graph<U> graph,LayoutRequest<U> request) {
        LayoutResult<U> flat=new CommunityLayout<>(groups,inner,outer,gap).compute(graph,request);
        Map<U,Integer> membership=new HashMap<>();for(int i=0;i<groups.size();i++)for(U node:groups.get(i))membership.put(node,i);
        double[] radius=new double[groups.size()],center=new double[groups.size()];
        for(int i=0;i<groups.size();i++) {
            double cx=0,cy=0;for(U node:groups.get(i)){Pair<Double> p=flat.getPositions().get(node);cx+=p.v1();cy+=p.v2();}
            cx/=Math.max(1,groups.get(i).size());cy/=Math.max(1,groups.get(i).size());
            for(U node:groups.get(i)){Pair<Double> p=flat.getPositions().get(node);radius[i]=Math.max(radius[i],Math.hypot(p.v1()-cx,p.v2()-cy)+request.getNodeSizes().getOrDefault(node,0.0));}
        }
        double total=0;for(int i=1;i<radius.length;i++)total+=radius[i-1]+radius[i]+gap;
        double cursor=-total/2;for(int i=0;i<center.length;i++){if(i>0)cursor+=radius[i-1]+radius[i]+gap;center[i]=cursor;}
        Map<U,Point3D> points=new LinkedHashMap<>();flat.getPositions().forEach((node,p)->points.put(node,new Point3D(p.v1(),p.v2(),center[membership.get(node)])));
        for(U pin:request.getPinnedNodes()) {
            Point3D initial=request.getInitialPositions3D().get(pin);
            if(initial!=null)points.put(pin,initial);
        }
        return LayoutResult.spatial(points,new LayoutDiagnostics(getDescriptor().getId(),flat.getDiagnostics().getTermination(),
            flat.getDiagnostics().getIterations(),flat.getDiagnostics().getMaximumDisplacement()));
    }
}
