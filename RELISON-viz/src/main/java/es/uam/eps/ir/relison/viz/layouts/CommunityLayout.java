/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.FastUndirectedWeightedGraph;
import es.uam.eps.ir.relison.graph.multigraph.fast.FastDirectedWeightedMultiGraph;
import es.uam.eps.ir.relison.graph.multigraph.fast.FastUndirectedWeightedMultiGraph;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.IndexedGraphSnapshot;
import es.uam.eps.ir.relison.viz.transforms.OverlapRemoval;
import java.util.*;

/**
 * Composition of published layouts on induced communities and their quotient.
 * This is a composition utility, not a reproduction of an additional named
 * community-layout paper. Its default children are Fruchterman-Reingold (1991)
 * and Hu (2005). Community detection is external; membership must partition nodes.
 * Quotient edge weights count original crossing edges, regardless of attributes.
 * Circular community envelopes are separated after quotient layout. A community
 * containing pins remains at its original center, preserving all pinned nodes.
 * Iteration budgets apply independently to each child layout.
 * @param <U> node type
 */
public final class CommunityLayout<U> implements Layout<U>
{
    private final List<List<U>> communities;
    private final Layout<U> inner;
    private final Layout<Integer> outer;
    private final double gap;
    /**
     * @param communities disjoint communities covering the graph
     * @param gap gap between community envelopes
     */
    public CommunityLayout(List<? extends List<U>> communities,double gap)
    { this(communities,new FruchtermanReingoldLayout<>(),new MultilevelForceLayout<>(
        new MultilevelForceConfig(50,.8,.2,.9,.001,100,true)),gap); }
    /**
     * @param communities disjoint communities covering the graph
     * @param inner layout within each community
     * @param outer layout of the quotient graph (Integer community IDs)
     * @param gap gap between community envelopes
     */
    public CommunityLayout(List<? extends List<U>> communities,Layout<U> inner,Layout<Integer> outer,double gap)
    {
        if(!Double.isFinite(gap) || gap<0) throw new IllegalArgumentException("Community gap must be finite and nonnegative");
        this.communities=new ArrayList<>();
        for(List<U> group:Objects.requireNonNull(communities)) this.communities.add(Collections.unmodifiableList(new ArrayList<>(group)));
        this.inner=Objects.requireNonNull(inner);this.outer=Objects.requireNonNull(outer);this.gap=gap;
    }
    @Override public LayoutDescriptor getDescriptor() { return new LayoutDescriptor("community","Community composition"); }
    @Override public LayoutResult<U> compute(Graph<U> input,LayoutRequest<U> request)
    {
        IndexedGraphSnapshot<U> snapshot=IndexedGraphSnapshot.capture(input,request);
        Map<U,Integer> owner=new HashMap<>();int count=communities.size();
        List<Graph<U>> subgraphs=new ArrayList<>();
        Graph<Integer> quotient=new FastUndirectedWeightedGraph<>();
        for(int c=0;c<count;c++)
        {
            if(communities.get(c).isEmpty()) throw new IllegalArgumentException("Community cannot be empty");
            Graph<U> sub=snapshot.isDirected()?new FastDirectedWeightedMultiGraph<>():new FastUndirectedWeightedMultiGraph<>();
            for(U node:communities.get(c))
            {
                if(snapshot.indexOf(node)<0 || owner.put(node,c)!=null) throw new IllegalArgumentException("Invalid or duplicate community member");
                sub.addNode(node);
            }
            subgraphs.add(sub);quotient.addNode(c);
        }
        if(owner.size()!=snapshot.getNodes().size()) throw new IllegalArgumentException("Communities must cover every graph node");
        Map<Long,Double> crossing=new TreeMap<>();
        for(int i=0;i<snapshot.getNodes().size();i++)
        {
            int[] targets=snapshot.getTargets(i);double[] weights=snapshot.getWeights(i);U source=snapshot.getNodes().get(i);
            for(int a=0;a<targets.length;a++)
            {
                int j=targets[a];if(!snapshot.isDirected() && i>j) continue;
                U target=snapshot.getNodes().get(j);int c=owner.get(source),d=owner.get(target);
                if(c==d) subgraphs.get(c).addEdge(source,target,weights[a]);
                else { long key=((long)Math.min(c,d)<<32)|Math.max(c,d);crossing.merge(key,1.0,Double::sum); }
            }
        }
        crossing.forEach((key,weight)->quotient.addEdge((int)(key>>>32),(int)(long)key,weight));
        Map<U,Pair<Double>> fallback=new LinkedHashMap<>();
        for(U node:snapshot.getNodes()) fallback.put(node,request.getInitialPositions().getOrDefault(node,new Pair<>(0.0,0.0)));
        if(count==0) return result(fallback,Collections.emptyList(),LayoutDiagnostics.Termination.COMPLETED,0,0);
        List<LayoutResult<U>> local=new ArrayList<>();
        Map<Integer,Pair<Double>> centers=new LinkedHashMap<>();Map<Integer,Double> radii=new LinkedHashMap<>();
        Set<Integer> pinnedGroups=new LinkedHashSet<>();List<Integer> communityOrder=new ArrayList<>();
        int iterations=0;double movement=0;boolean limit=false;
        for(int c=0;c<count;c++)
        {
            if(cancelled(request)) return result(fallback,Collections.emptyList(),LayoutDiagnostics.Termination.CANCELLED,iterations,movement);
            List<U> order=new ArrayList<>();Map<U,Pair<Double>> positions=new LinkedHashMap<>();
            Map<U,Double> sizes=new LinkedHashMap<>();Set<U> pins=new LinkedHashSet<>();
            for(U node:snapshot.getNodes()) if(owner.get(node)==c)
            {
                order.add(node);
                if(request.getInitialPositions().containsKey(node)) positions.put(node,request.getInitialPositions().get(node));
                if(request.getNodeSizes().containsKey(node)) sizes.put(node,request.getNodeSizes().get(node));
                if(request.getPinnedNodes().contains(node)) pins.add(node);
            }
            LayoutRequest<U> child=LayoutRequest.<U>builder().nodeOrder(order).initialPositions(positions).pinnedNodes(pins)
                .nodeSizes(sizes).seed(request.getSeed()+c).maxIterations(request.getMaxIterations())
                .timeLimitMillis(request.getTimeLimitMillis()).cancellation(request.getCancellation()).build();
            LayoutResult<U> layout=inner.compute(subgraphs.get(c),child);
            if(!layout.getPositions().keySet().equals(new HashSet<>(order))) throw new IllegalStateException("Child layout changed node coverage");
            for(U pin:pins) if(!Objects.equals(layout.getPositions().get(pin),positions.get(pin))) throw new IllegalStateException("Child layout moved a pin");
            local.add(layout);fallback.putAll(layout.getPositions());
            iterations=Math.addExact(iterations,layout.getDiagnostics().getIterations());
            movement=Math.max(movement,layout.getDiagnostics().getMaximumDisplacement());
            if(layout.getDiagnostics().getTermination()==LayoutDiagnostics.Termination.CANCELLED)
                return result(fallback,Collections.emptyList(),LayoutDiagnostics.Termination.CANCELLED,iterations,movement);
            limit|=layout.getDiagnostics().getTermination()==LayoutDiagnostics.Termination.LIMIT_REACHED;
            Bounds2D bounds=layout.getBounds();double cx=bounds.getMinX()/2+bounds.getMaxX()/2,cy=bounds.getMinY()/2+bounds.getMaxY()/2;
            centers.put(c,new Pair<>(cx,cy));double radius=0;
            for(U node:order)
            { Pair<Double> p=layout.getPositions().get(node);radius=Math.max(radius,Math.hypot(p.v1()-cx,p.v2()-cy)+sizes.getOrDefault(node,0.0)); }
            radii.put(c,radius);if(!pins.isEmpty()) pinnedGroups.add(c);communityOrder.add(c);
        }
        // Only constrained centers seed the quotient; unconstrained child frames often share identical centers.
        Map<Integer,Pair<Double>> fixed=new LinkedHashMap<>();for(int c:pinnedGroups) fixed.put(c,centers.get(c));
        LayoutRequest<Integer> outerRequest=LayoutRequest.<Integer>builder().nodeOrder(communityOrder).initialPositions(fixed)
            .pinnedNodes(pinnedGroups).nodeSizes(radii).seed(request.getSeed()).maxIterations(request.getMaxIterations())
            .timeLimitMillis(request.getTimeLimitMillis()).cancellation(request.getCancellation()).build();
        LayoutResult<Integer> global=outer.compute(quotient,outerRequest);
        if(!global.getPositions().keySet().equals(new HashSet<>(communityOrder))) throw new IllegalStateException("Quotient layout changed node coverage");
        for(int c:pinnedGroups) if(!Objects.equals(global.getPositions().get(c),fixed.get(c))) throw new IllegalStateException("Quotient layout moved a pinned community");
        if(global.getDiagnostics().getTermination()!=LayoutDiagnostics.Termination.CANCELLED)
            global=new OverlapRemoval<Integer>(gap,1000).process(global,outerRequest);
        iterations=Math.addExact(iterations,global.getDiagnostics().getIterations());movement=Math.max(movement,global.getDiagnostics().getMaximumDisplacement());
        limit|=global.getDiagnostics().getTermination()==LayoutDiagnostics.Termination.LIMIT_REACHED;
        Map<U,Pair<Double>> output=new LinkedHashMap<>();List<EdgeRoute<U>> routes=new ArrayList<>();
        for(int c=0;c<count;c++)
        {
            Pair<Double> center=centers.get(c),target=global.getPositions().get(c);
            double dx=pinnedGroups.contains(c)?0:target.v1()-center.v1(),dy=pinnedGroups.contains(c)?0:target.v2()-center.v2();
            for(Map.Entry<U,Pair<Double>> e:local.get(c).getPositions().entrySet())
                output.put(e.getKey(),new Pair<>(e.getValue().v1()+dx,e.getValue().v2()+dy));
            for(EdgeRoute<U> route:local.get(c).getEdgeRoutes())
            {
                List<Pair<Double>> points=new ArrayList<>();for(Pair<Double> p:route.getPoints()) points.add(new Pair<>(p.v1()+dx,p.v2()+dy));
                routes.add(new EdgeRoute<>(route.getSource(),route.getTarget(),route.getOccurrence(),points));
            }
        }
        Map<U,Pair<Double>> ordered=new LinkedHashMap<>();for(U node:snapshot.getNodes()) ordered.put(node,output.get(node));
        return result(ordered,routes,global.getDiagnostics().getTermination()==LayoutDiagnostics.Termination.CANCELLED
            ?LayoutDiagnostics.Termination.CANCELLED:limit?LayoutDiagnostics.Termination.LIMIT_REACHED:LayoutDiagnostics.Termination.COMPLETED,iterations,movement);
    }
    private boolean cancelled(LayoutRequest<U> request) { return Thread.currentThread().isInterrupted() || request.getCancellation().getAsBoolean(); }
    private LayoutResult<U> result(Map<U,Pair<Double>> positions,List<EdgeRoute<U>> routes,LayoutDiagnostics.Termination status,int iterations,double movement)
    { return new LayoutResult<>(positions,new LayoutDiagnostics("community",status,iterations,movement),routes); }
}
