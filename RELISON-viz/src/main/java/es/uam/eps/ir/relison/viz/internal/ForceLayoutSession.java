/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.internal;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.layouts.*;
import java.util.*;

/**
 * Shared numerical state for FR and ForceAtlas2. Edge directions are projected
 * structurally; directed reciprocal arcs accumulate independently, undirected
 * edges are counted once. Self-loops exert no force, but contribute two incidences
 * to ForceAtlas2 degree mass.
 * @param <U> node type
 */
public final class ForceLayoutSession<U> implements LayoutSession<U> {
    private final IndexedGraphSnapshot<U> graph;
    private final LayoutRequest<U> request;
    private final String id;
    private final FruchtermanReingoldConfig fr;
    private final ForceAtlas2Config fa;
    private final double[] x,y,mass,oldX,oldY;
    private final boolean[] pins;
    private final List<Edge> edges=new ArrayList<>();
    private volatile boolean cancelled;
    private LayoutDiagnostics.Termination status=LayoutDiagnostics.Termination.RUNNING;
    private int iterations, stable;
    private final double idealLength;
    private double maximumDisplacement, speed=1, speedEfficiency=1, temperature;
    private long activeNanos, stepStarted;
    private static final class Edge {
        final int source,target; double weight;
        Edge(int source,int target,double weight) { this.source=source; this.target=target; this.weight=weight; }
    }
    /**
     * @param input graph to capture
     * @param request constraints and limits
     * @param id algorithm identifier
     * @param fr FR settings, or null for ForceAtlas2
     * @param fa ForceAtlas2 settings, or null for FR
     */
    public ForceLayoutSession(Graph<U> input,LayoutRequest<U> request,String id,FruchtermanReingoldConfig fr,ForceAtlas2Config fa) {
        this.graph=IndexedGraphSnapshot.capture(input,request); this.request=request; this.id=id; this.fr=fr; this.fa=fa;
        if((fr==null)==(fa==null)) throw new IllegalArgumentException("Exactly one force model is required");
        if(!new HashSet<>(graph.getNodes()).containsAll(request.getNodeSizes().keySet()))
            throw new IllegalArgumentException("Node sizes refer to nodes outside the graph");
        int n=graph.getNodes().size();
        x=new double[n]; y=new double[n]; mass=new double[n]; oldX=new double[n]; oldY=new double[n]; pins=new boolean[n];
        Arrays.fill(mass,1);
        Random random=new Random(request.getSeed());
        idealLength=fr==null?0:fr.bounded?Math.sqrt(fr.width*fr.height/Math.max(1,n)):fr.idealLength;
        double scale=fr!=null?idealLength:Math.sqrt(fa.scaling);
        double side=scale*Math.max(1,Math.sqrt(n));
        if(!Double.isFinite(side)) throw new IllegalArgumentException("Initialization scale is too large");
        temperature=fr!=null?fr.initialTemperature:scale;
        double initialWidth=fr!=null && fr.bounded?fr.width:side;
        double initialHeight=fr!=null && fr.bounded?fr.height:side;
        if(fr!=null && idealLength==0) throw new IllegalArgumentException("FR edge scale underflow");
        for(int i=0;i<n;i++) {
            U node=graph.getNodes().get(i);
            Pair<Double> initial=request.getInitialPositions().get(node);
            x[i]=initial==null?(random.nextDouble()-0.5)*initialWidth:initial.v1();
            y[i]=initial==null?(random.nextDouble()-0.5)*initialHeight:initial.v2();
            pins[i]=request.getPinnedNodes().contains(node);
            if(fr!=null && fr.bounded) {
                if(pins[i] && (Math.abs(x[i])>fr.width/2 || Math.abs(y[i])>fr.height/2))
                    throw new IllegalArgumentException("Pinned node is outside the FR drawing frame");
                x[i]=Math.max(-fr.width/2,Math.min(fr.width/2,x[i]));
                y[i]=Math.max(-fr.height/2,Math.min(fr.height/2,y[i]));
            }
            int[] targets=graph.getTargets(i); double[] weights=graph.getWeights(i);
            for(int j=0;j<targets.length;j++) {
                int target=targets[j];
                if(!graph.isDirected() && target<i) continue;
                if(fa!=null) { mass[i]++; mass[target]++; }
                if(target==i && fa==null) continue;
                double influence=fr!=null?(fr.weighted?1:0):fa.weightInfluence;
                double weight=1;
                if(influence>0) {
                    if(!Double.isFinite(weights[j])||weights[j]<0) throw new IllegalArgumentException("Force attraction requires finite non-negative weights");
                    weight=fa!=null && fa.invertWeights && weights[j]!=0?1/weights[j]:weights[j];
                    if(fa==null || !fa.normalizeWeights) weight=Math.pow(weight,influence);
                    if(!Double.isFinite(weight)) throw new IllegalArgumentException("Edge weight influence overflow");
                }
                edges.add(new Edge(i,target,weight));
            }
        }
        if(fa!=null && fa.normalizeWeights && fa.weightInfluence>0 && !edges.isEmpty()) {
            double min=edges.stream().mapToDouble(edge->edge.weight).min().orElse(0);
            double max=edges.stream().mapToDouble(edge->edge.weight).max().orElse(0);
            for(Edge edge:edges) edge.weight=min==max?1:Math.pow((edge.weight-min)/(max-min),fa.weightInfluence);
        }
        if(n==0 || allPinned()) status=LayoutDiagnostics.Termination.CONVERGED;
        else if(request.getMaxIterations()==0) status=LayoutDiagnostics.Termination.LIMIT_REACHED;
        checkStop();
    }
    private boolean allPinned() { for(boolean pin:pins) if(!pin) return false; return true; }
    private boolean checkStop() {
        if(status!=LayoutDiagnostics.Termination.RUNNING) return true;
        if(cancelled || Thread.currentThread().isInterrupted() || request.getCancellation().getAsBoolean()) {
            status=LayoutDiagnostics.Termination.CANCELLED; return true;
        }
        long elapsed=activeNanos+(stepStarted==0?0:System.nanoTime()-stepStarted);
        if(request.getTimeLimitMillis()>0 && elapsed/1000000>=request.getTimeLimitMillis()) {
            status=LayoutDiagnostics.Termination.LIMIT_REACHED; return true;
        }
        return false;
    }
    @Override
    public void step(int count) {
        if(count<0) throw new IllegalArgumentException("Iteration count must be non-negative");
        if(checkStop()) return;
        stepStarted=System.nanoTime();
        try {
            for(int run=0;run<count && !checkStop();run++) {
                iterate();
                if(checkStop()) break;
                iterations++;
                double tolerance=fr!=null?fr.tolerance:fa.tolerance;
                stable=maximumDisplacement<=tolerance?stable+1:0;
                if(stable>=5) status=LayoutDiagnostics.Termination.CONVERGED;
                else if(iterations>=request.getMaxIterations()) status=LayoutDiagnostics.Termination.LIMIT_REACHED;
            }
        } finally { activeNanos+=System.nanoTime()-stepStarted; stepStarted=0; }
    }
    private void iterate() {
        int n=x.length;
        double[] dx=new double[n],dy=new double[n];
        double theta=fr!=null?fr.theta:fa.theta,coefficient=fr!=null?idealLength*idealLength:fa.scaling;
        if(!Double.isFinite(coefficient)) throw new IllegalArgumentException("Repulsion scale overflow");
        if(theta>0 && (fa==null || !fa.adjustSizes)) {
            QuadTree tree=new QuadTree(x,y,mass,this::checkStop,fr!=null);
            for(int i=0;i<n && !checkStop();i++) tree.accumulate(i,theta,coefficient,dx,dy);
        } else {
            for(int i=0;i<n && !checkStop();i++)
                for(int j=0;j<n;j++) {
                    if(checkStop()) return;
                    if(i!=j) {
                        if(fr!=null) QuadTree.pair(i,j,x,y,mass,coefficient,dx,dy);
                        else if(fa.adjustSizes) collisionRepulsion(i,j,dx,dy);
                        else QuadTree.forceAtlasPair(i,j,x,y,mass,coefficient,dx,dy);
                    }
                }
        }
        double meanMass=0;
        if(fa!=null && fa.outboundAttractionDistribution) {
            for(double value:mass) meanMass+=value;
            meanMass/=n;
        }
        for(Edge edge:edges) {
            if(checkStop()) return;
            int a=edge.source,b=edge.target;
            double vx=x[a]-x[b],vy=y[a]-y[b],distance=Math.hypot(vx,vy);
            if(distance==0) continue;
            double attractionDistance=distance;
            if(fa!=null && fa.adjustSizes) {
                attractionDistance-=radius(a)+radius(b);
                if(attractionDistance<=0) continue;
            }
            double f=edge.weight*(fr!=null?distance/idealLength:(fa.linLog?Math.log1p(attractionDistance)/attractionDistance:1));
            if(fa!=null && fa.outboundAttractionDistribution) f*=meanMass/mass[a];
            dx[a]-=vx*f; dy[a]-=vy*f; dx[b]+=vx*f; dy[b]+=vy*f;
        }
        if(fa!=null) {
            double swinging=0,traction=0;
            for(int i=0;i<n;i++) {
                if(checkStop()) return;
                double distance=Math.hypot(x[i],y[i]);
                if(distance>0) {
                    double g=fa.gravity*mass[i]*(fa.strongGravity?1:1/distance);
                    dx[i]-=x[i]*g; dy[i]-=y[i]*g;
                }
                if(!pins[i]) {
                    swinging+=mass[i]*Math.hypot(dx[i]-oldX[i],dy[i]-oldY[i]);
                    traction+=mass[i]*Math.hypot(dx[i]+oldX[i],dy[i]+oldY[i])/2;
                }
            }
            adaptForceAtlasSpeed(swinging,traction,n);
        }
        double[] nextX=x.clone(),nextY=y.clone();
        double movement=0;
        for(int i=0;i<n;i++) {
            if(checkStop()) return;
            if(!Double.isFinite(dx[i])||!Double.isFinite(dy[i])) throw new IllegalArgumentException("Force evaluation overflow; reduce scales or coordinate range");
            if(pins[i]) continue;
            double force=Math.hypot(dx[i],dy[i]);
            double factor;
            if(fr!=null) factor=force==0?0:Math.min(temperature,force)/force;
            else {
                double swing=mass[i]*Math.hypot(dx[i]-oldX[i],dy[i]-oldY[i]);
                factor=speed/(1+Math.sqrt(speed*swing));
                if(fa.adjustSizes) factor=force==0?0:Math.min(0.1*factor*force,10)/force;
            }
            double mx=dx[i]*factor,my=dy[i]*factor;
            nextX[i]+=mx; nextY[i]+=my;
            if(!Double.isFinite(nextX[i])||!Double.isFinite(nextY[i])) throw new IllegalArgumentException("Position update overflow");
            if(fr!=null && fr.bounded) {
                nextX[i]=Math.max(-fr.width/2,Math.min(fr.width/2,nextX[i]));
                nextY[i]=Math.max(-fr.height/2,Math.min(fr.height/2,nextY[i]));
            }
            movement=Math.max(movement,Math.hypot(nextX[i]-x[i],nextY[i]-y[i]));
        }
        if(checkStop()) return;
        System.arraycopy(nextX,0,x,0,n); System.arraycopy(nextY,0,y,0,n);
        System.arraycopy(dx,0,oldX,0,n); System.arraycopy(dy,0,oldY,0,n);
        maximumDisplacement=movement;
        if(fr!=null) temperature=fr.bounded
            ?fr.initialTemperature*Math.max(0,1-(iterations+1.0)/Math.max(1,request.getMaxIterations()))
            :temperature*fr.cooling;
    }

    private double radius(int node) { return request.getNodeSizes().getOrDefault(graph.getNodes().get(node),0.0); }
    private void collisionRepulsion(int a,int b,double[] dx,double[] dy) {
        double vx=x[a]-x[b],vy=y[a]-y[b];
        double separation=Math.hypot(vx,vy)-radius(a)-radius(b);
        double coefficient=fa.scaling*mass[a]*mass[b];
        double factor=separation>0?coefficient/(separation*separation):separation<0?100*coefficient:0;
        dx[a]+=vx*factor; dy[a]+=vy*factor;
    }
    /*
     * ForceAtlas2 feedback controller. Efficiency is persistent between steps.
     * Zero total force is an equilibrium: avoid the undefined zero/zero ratio.
     * Zero swinging with positive traction allows the usual 50% speed increase.
     */
    private void adaptForceAtlasSpeed(double swinging,double traction,int nodeCount) {
        if(!Double.isFinite(swinging)||!Double.isFinite(traction))
            throw new IllegalArgumentException("ForceAtlas2 feedback overflow");
        if(swinging==0 && traction==0) return;
        double estimate=0.05*Math.sqrt(nodeCount);
        double jitter=fa.jitterTolerance*Math.max(Math.sqrt(estimate),
            Math.min(10,estimate*traction/((double)nodeCount*nodeCount)));
        if(swinging>2*traction) {
            if(speedEfficiency>0.05) speedEfficiency*=0.5;
            jitter=Math.max(jitter,fa.jitterTolerance);
        }
        double target=swinging==0?Double.POSITIVE_INFINITY:jitter*speedEfficiency*traction/swinging;
        if(swinging>jitter*traction) {
            if(speedEfficiency>0.05) speedEfficiency*=0.7;
        } else if(speed<1000) speedEfficiency*=1.3;
        speed+=Math.min(target-speed,0.5*speed);
        if(!Double.isFinite(speed))
            throw new IllegalArgumentException("ForceAtlas2 speed overflow");
    }
    @Override
    public LayoutResult<U> snapshot() {
        checkStop();
        Map<U,Pair<Double>> positions=new LinkedHashMap<>();
        for(int i=0;i<x.length;i++) positions.put(graph.getNodes().get(i),new Pair<>(x[i],y[i]));
        return new LayoutResult<>(positions,new LayoutDiagnostics(id,status,iterations,maximumDisplacement));
    }
    @Override
    public void cancel() { cancelled=true; }
    @Override
    public boolean isFinished() { return checkStop(); }
}
