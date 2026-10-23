/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.internal;

import java.util.*;
import java.util.function.BooleanSupplier;

/** Barnes-Hut mass tree rebuilt from immutable iteration coordinates. */
public final class QuadTree {
    private final double[] x, y, mass;
    private final BooleanSupplier stop;
    private final Cell root;
    private final boolean regularize;
    private static final class Cell {
        double minX, minY, side, mass, cx, cy;
        int[] members;
        Cell[] children;
    }
    /**
     * @param x x coordinates
     * @param y y coordinates
     * @param mass positive node masses
     * @param stop cancellation/time-limit signal
     */
    public QuadTree(double[] x, double[] y, double[] mass, BooleanSupplier stop) {
        this(x,y,mass,stop,true);
    }
    /**
     * @param x x coordinates
     * @param y y coordinates
     * @param mass positive node masses
     * @param stop cancellation signal
     * @param regularize whether to regularize close pairs for FR; false uses ForceAtlas2 forces
     */
    public QuadTree(double[] x,double[] y,double[] mass,BooleanSupplier stop,boolean regularize) {
        this.x=x; this.y=y; this.mass=mass; this.stop=stop; this.regularize=regularize;
        double minX=0,minY=0,maxX=0,maxY=0;
        if(x.length>0) { minX=maxX=x[0]; minY=maxY=y[0]; }
        int[] members=new int[x.length];
        for(int i=0;i<x.length;i++) {
            members[i]=i; minX=Math.min(minX,x[i]); maxX=Math.max(maxX,x[i]);
            minY=Math.min(minY,y[i]); maxY=Math.max(maxY,y[i]);
        }
        double side=Math.max(1,Math.max(maxX-minX,maxY-minY));
        if(!Double.isFinite(side)) throw new IllegalArgumentException("Coordinate range is too large for force evaluation");
        root=build(members,minX,minY,side,0);
    }
    private Cell build(int[] members,double minX,double minY,double side,int depth) {
        Cell c=new Cell(); c.minX=minX; c.minY=minY; c.side=side; c.members=members;
        for(int i:members) {
            if(stop.getAsBoolean()) return c;
            double total=c.mass+mass[i];
            c.cx+=(x[i]-c.cx)*(mass[i]/total); c.cy+=(y[i]-c.cy)*(mass[i]/total); c.mass=total;
        }
        if(members.length<=1 || depth>=32 || side<=1e-9) return c;
        List<List<Integer>> groups=new ArrayList<>();
        for(int i=0;i<4;i++) groups.add(new ArrayList<>());
        double half=side/2;
        for(int i:members) {
            if(stop.getAsBoolean()) return c;
            int q=(x[i]>=minX+half?1:0)+(y[i]>=minY+half?2:0);
            groups.get(q).add(i);
        }
        c.children=new Cell[4]; c.members=null;
        for(int q=0;q<4;q++) if(!groups.get(q).isEmpty())
            c.children[q]=build(groups.get(q).stream().mapToInt(Integer::intValue).toArray(),
                minX+(q%2)*half,minY+(q/2)*half,half,depth+1);
        return c;
    }
    /**
     * Adds inverse-distance repulsion for one node. Theta zero visits exact leaves.
     * @param node target index
     * @param theta opening ratio
     * @param coefficient repulsion scale
     * @param dx output x forces
     * @param dy output y forces
     */
    public void accumulate(int node,double theta,double coefficient,double[] dx,double[] dy) {
        visit(root,node,theta,coefficient,dx,dy);
    }
    private void visit(Cell c,int node,double theta,double coefficient,double[] dx,double[] dy) {
        if(c==null || stop.getAsBoolean()) return;
        if(c.members!=null) {
            for(int other:c.members) {
                if(stop.getAsBoolean()) return;
                if(other!=node) {
                    if(regularize) pair(node,other,x,y,mass,coefficient,dx,dy);
                    else forceAtlasPair(node,other,x,y,mass,coefficient,dx,dy);
                }
            }
            return;
        }
        double distance=Math.hypot(x[node]-c.cx,y[node]-c.cy);
        boolean contains=x[node]>=c.minX && x[node]<=c.minX+c.side && y[node]>=c.minY && y[node]<=c.minY+c.side;
        if(!contains && distance>0 && c.side/distance<theta) {
            double f=coefficient*mass[node]*c.mass/(distance*distance);
            dx[node]+=(x[node]-c.cx)*f; dy[node]+=(y[node]-c.cy)*f;
        } else for(Cell child:c.children) visit(child,node,theta,coefficient,dx,dy);
    }
    /**
     * Adds unregularized ForceAtlas2 repulsion. Coincident points exert no force.
     * @param node target index
     * @param other source index
     * @param x x coordinates
     * @param y y coordinates
     * @param mass node masses
     * @param coefficient repulsion coefficient
     * @param dx x forces
     * @param dy y forces
     */
    public static void forceAtlasPair(int node,int other,double[] x,double[] y,double[] mass,
            double coefficient,double[] dx,double[] dy) {
        double vx=x[node]-x[other],vy=y[node]-y[other],distance=Math.hypot(vx,vy);
        if(distance==0) return;
        double force=coefficient*mass[node]*mass[other]/distance;
        dx[node]+=(vx/distance)*force; dy[node]+=(vy/distance)*force;
    }
    /**
     * Adds exact repulsion for a single ordered pair; coincidences use a deterministic direction.
     * @param node target index
     * @param other source index
     * @param x x coordinates
     * @param y y coordinates
     * @param mass node masses
     * @param coefficient repulsion coefficient
     * @param dx x forces
     * @param dy y forces
     */
    public static void pair(int node,int other,double[] x,double[] y,double[] mass,double coefficient,double[] dx,double[] dy) {
        double vx=x[node]-x[other],vy=y[node]-y[other];
        double distance=Math.hypot(vx,vy);
        if(distance==0) {
            int low=Math.min(node,other),high=Math.max(node,other);
            double angle=((low*73856093L ^ high*19349663L)&0xffff)*2*Math.PI/65536;
            double sign=node<other?1:-1; vx=sign*Math.cos(angle)*1e-6; vy=sign*Math.sin(angle)*1e-6; distance=1e-6;
        }
        double f=coefficient*mass[node]*mass[other]/(distance*distance);
        dx[node]+=vx*f; dy[node]+=vy*f;
    }
}
