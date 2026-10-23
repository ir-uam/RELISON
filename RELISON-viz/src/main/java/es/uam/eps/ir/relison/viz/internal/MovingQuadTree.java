/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.internal;
import java.util.*;

/** Barnes-Hut quadtree updated after each sequential node movement. */
final class MovingQuadTree
{
    private final double[] x,y;
    private final Cell root;
    MovingQuadTree(double[] x, double[] y, double step)
    {
        this.x=x; this.y=y;
        double minX=Arrays.stream(x).min().orElse(0), maxX=Arrays.stream(x).max().orElse(0);
        double minY=Arrays.stream(y).min().orElse(0), maxY=Arrays.stream(y).max().orElse(0);
        double size=Math.max(maxX-minX,maxY-minY)+2*step+1;
        if (!Double.isFinite(size)) throw new IllegalArgumentException("Quadtree bounds overflow");
        root=new Cell(minX-step-.5,minY-step-.5,size,0);
        for(int i=0;i<x.length;i++) root.insert(i);
    }
    void move(int i,double nx,double ny)
    { root.remove(i,x[i],y[i]); x[i]=nx; y[i]=ny; root.insert(i); }
    double[] force(int i,double theta,double coefficient)
    { double[] f=new double[2]; root.force(i,theta,coefficient,f); return f; }
    private final class Cell
    {
        final double left,bottom,size; final int depth;
        int count; double sumX,sumY;
        List<Integer> members=new ArrayList<>(); Cell[] children;
        Cell(double left,double bottom,double size,int depth)
        { this.left=left;this.bottom=bottom;this.size=size;this.depth=depth; }
        int quadrant(double px,double py) { return (px>=left+size/2?1:0)+(py>=bottom+size/2?2:0); }
        boolean contains(double px,double py) { return px>=left && px<=left+size && py>=bottom && py<=bottom+size; }
        void insert(int i)
        {
            count++;sumX+=x[i];sumY+=y[i];
            if(children!=null) { children[quadrant(x[i],y[i])].insert(i); return; }
            members.add(i);
            if(members.size()>1 && depth<32 && size>1e-12)
            {
                children=new MovingQuadTree.Cell[4];
                for(int q=0;q<4;q++) children[q]=new Cell(left+(q%2)*size/2,bottom+(q/2)*size/2,size/2,depth+1);
                for(int node:members) children[quadrant(x[node],y[node])].insert(node);
                members=null;
            }
        }
        void remove(int i,double ox,double oy)
        {
            count--;sumX-=ox;sumY-=oy;
            if(children!=null) children[quadrant(ox,oy)].remove(i,ox,oy);
            else members.remove(Integer.valueOf(i));
            if(count==0) { sumX=0;sumY=0;children=null;members=new ArrayList<>(); }
        }
        void force(int i,double theta,double coefficient,double[] f)
        {
            if(count==0) return;
            if(children==null)
            {
                for(int j:members) if(i!=j) add(i,x[j],y[j],1,coefficient,j,f);
                return;
            }
            double cx=sumX/count,cy=sumY/count,r=Math.hypot(x[i]-cx,y[i]-cy);
            if(!contains(x[i],y[i]) && size/r<theta) add(i,cx,cy,count,coefficient,-1,f);
            else for(Cell child:children) child.force(i,theta,coefficient,f);
        }
    }
    static void add(int i,double px,double py,int mass,double coefficient,int j,double[] f,double[] x,double[] y)
    {
        double dx=x[i]-px,dy=y[i]-py,r=Math.hypot(dx,dy);
        if(r==0)
        {
            double angle=((Math.min(i,j)*31L+Math.max(i,j)*17L)%1009)*2*Math.PI/1009;
            dx=Math.cos(angle)*(i<j?1:-1)*1e-9;dy=Math.sin(angle)*(i<j?1:-1)*1e-9;r=1e-9;
        }
        double factor=coefficient*mass/r;
        f[0]+=factor*(dx/r);f[1]+=factor*(dy/r);
    }
    private void add(int i,double px,double py,int mass,double coefficient,int j,double[] f)
    { add(i,px,py,mass,coefficient,j,f,x,y); }
}
