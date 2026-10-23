/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.internal;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.layouts.MultilevelForceConfig;
import java.util.*;

/**
 * Hu's EC hierarchy and Algorithm 1. Each step is one sequential force sweep.
 * The adaptive energy proxy is the sum of squared forces, as in the paper.
 * @param <U> node type
 */
public final class MultilevelForceSession<U> extends NumericalLayoutSession<U>
{
    private final MultilevelForceConfig config;
    private List<Level> levels;
    private int current, sweeps, progress;
    private double step, energy=Double.POSITIVE_INFINITY;
    private boolean truncated;
    /**
     * @param input graph
     * @param request common constraints
     * @param config multilevel model
     */
    public MultilevelForceSession(Graph<U> input,LayoutRequest<U> request,MultilevelForceConfig config)
    { super(input,request,"multilevel-force",config.initialScale);this.config=config; }

    private void prepare()
    {
        List<Map<Integer,Double>> edges=maps(x.length);
        for(int i=0;i<x.length;i++)
        {
            checkpoint(); int[] targets=graph.getTargets(i);double[] weights=graph.getWeights(i);
            for(int a=0;a<targets.length;a++)
            {
                int j=targets[a];if(i==j || (!graph.isDirected() && i>j)) continue;
                double w=config.weighted?weights[a]:1;
                if(!Double.isFinite(w) || w<=0) throw new IllegalArgumentException("Hu attraction weights must be positive and finite");
                merge(edges,i,j,w);
            }
        }
        List<Level> hierarchy=new ArrayList<>();
        Level fine=new Level(edges,x.clone(),y.clone(),pins.clone());hierarchy.add(fine);
        while(fine.x.length>2)
        {
            checkpoint();int n=fine.x.length;
            int[] order=new int[n],parent=new int[n];Arrays.fill(parent,-1);
            for(int i=0;i<n;i++) order[i]=i;
            for(int i=n-1;i>0;i--) { int j=random.nextInt(i+1),t=order[i];order[i]=order[j];order[j]=t; }
            int count=0;
            for(int i:order) if(parent[i]<0)
            {
                checkpoint();int partner=-1;double strongest=-1;
                if(!fine.pins[i]) for(Map.Entry<Integer,Double> edge:fine.edges.get(i).entrySet())
                    if(parent[edge.getKey()]<0 && !fine.pins[edge.getKey()] && edge.getValue()>strongest)
                    { partner=edge.getKey();strongest=edge.getValue(); }
                parent[i]=count;if(partner>=0) parent[partner]=count;count++;
            }
            if(count>n*.75) break;
            List<Map<Integer,Double>> coarseEdges=maps(count);
            double[] cx=new double[count],cy=new double[count];int[] sizes=new int[count];boolean[] cp=new boolean[count];
            for(int i=0;i<n;i++)
            {
                int p=parent[i];cx[p]+=fine.x[i];cy[p]+=fine.y[i];sizes[p]++;cp[p]|=fine.pins[i];
                for(Map.Entry<Integer,Double> e:fine.edges.get(i).entrySet())
                    if(i<e.getKey() && p!=parent[e.getKey()]) merge(coarseEdges,p,parent[e.getKey()],e.getValue());
            }
            for(int i=0;i<count;i++) { cx[i]/=sizes[i];cy[i]/=sizes[i]; }
            fine.parent=parent;fine=new Level(coarseEdges,cx,cy,cp);hierarchy.add(fine);
        }
        Level coarse=hierarchy.get(hierarchy.size()-1);
        if(request.getInitialPositions().isEmpty())
        {
            double side=config.initialScale*Math.sqrt(Math.max(1,coarse.x.length));
            for(int i=0;i<coarse.x.length;i++)
            { coarse.x[i]=(random.nextDouble()-.5)*side;coarse.y[i]=(random.nextDouble()-.5)*side; }
        }
        double average=0;int edgeCount=0;
        for(int i=0;i<coarse.x.length;i++) for(int j:coarse.edges.get(i).keySet()) if(i<j)
        { average+=Math.hypot(coarse.x[i]-coarse.x[j],coarse.y[i]-coarse.y[j]);edgeCount++; }
        coarse.k=edgeCount==0 || average==0?config.initialScale:average/edgeCount;
        if(!Double.isFinite(coarse.k) || coarse.k<=0) throw new IllegalArgumentException("Nominal edge length overflow");
        levels=hierarchy;current=levels.size()-1;step=coarse.k;
    }
    private static List<Map<Integer,Double>> maps(int n)
    { List<Map<Integer,Double>> result=new ArrayList<>();for(int i=0;i<n;i++) result.add(new TreeMap<>());return result; }
    private static void merge(List<Map<Integer,Double>> edges,int i,int j,double w)
    {
        double total=edges.get(i).getOrDefault(j,0.0)+w;
        if(!Double.isFinite(total)) throw new IllegalArgumentException("Coarse edge weight overflow");
        edges.get(i).put(j,total);edges.get(j).put(i,total);
    }
    @Override protected void advance()
    {
        if(levels==null) prepare();
        Level level=levels.get(current);
        double[] nx=level.x.clone(),ny=level.y.clone();
        MovingQuadTree tree=config.theta==0?null:new MovingQuadTree(nx,ny,step);
        double nextEnergy=0,movement=0,coefficient=config.repulsion*level.k*level.k;
        if(!Double.isFinite(coefficient)) throw new IllegalArgumentException("Repulsion coefficient overflow");
        for(int i=0;i<nx.length;i++)
        {
            checkpoint();if(level.pins[i]) continue;
            double[] force=tree==null?new double[2]:tree.force(i,config.theta,coefficient);
            if(tree==null) for(int j=0;j<nx.length;j++)
            { if((j&255)==0) checkpoint();if(i!=j) MovingQuadTree.add(i,nx[j],ny[j],1,coefficient,j,force,nx,ny); }
            for(Map.Entry<Integer,Double> edge:level.edges.get(i).entrySet())
            {
                int j=edge.getKey();double dx=nx[j]-nx[i],dy=ny[j]-ny[i];
                double factor=edge.getValue()*Math.hypot(dx,dy)/level.k;
                force[0]+=factor*dx;force[1]+=factor*dy;
            }
            double length=Math.hypot(force[0],force[1]);
            if(!Double.isFinite(length)) throw new IllegalArgumentException("Force overflow; rescale input coordinates or weights");
            nextEnergy+=length*length;
            if(length==0) continue;
            double px=nx[i]+step*(force[0]/length),py=ny[i]+step*(force[1]/length);
            if(!Double.isFinite(px) || !Double.isFinite(py)) throw new IllegalArgumentException("Coordinate overflow");
            movement=Math.hypot(movement,Math.hypot(px-nx[i],py-ny[i]));
            if(tree==null) { nx[i]=px;ny[i]=py; } else tree.move(i,px,py);
        }
        if(!Double.isFinite(nextEnergy)) throw new IllegalArgumentException("Force energy overflow");
        double[] fx=new double[x.length],fy=new double[y.length];
        for(int i=0;i<x.length;i++)
        {
            int ancestor=i;for(int l=0;l<current;l++) ancestor=levels.get(l).parent[ancestor];
            fx[i]=pins[i]?x[i]:nx[ancestor];fy[i]=pins[i]?y[i]:ny[ancestor];
        }
        commit(fx,fy);level.x=nx;level.y=ny;sweeps++;
        if(current==levels.size()-1)
        {
            if(nextEnergy<energy) { if(++progress>=5) { progress=0;step/=config.cooling; } }
            else { progress=0;step*=config.cooling; }
        }
        else step*=config.cooling;
        energy=nextEnergy;
        boolean converged=movement<config.tolerance*level.k;
        if(converged || sweeps>=config.levelIterations)
        {
            truncated|=!converged;
            if(current==0) status=truncated?LayoutDiagnostics.Termination.LIMIT_REACHED:LayoutDiagnostics.Termination.CONVERGED;
            else refine(level);
        }
    }
    private void refine(Level coarse)
    {
        Level fine=levels.get(current-1);
        int fineDiameter=pseudoDiameter(fine),coarseDiameter=pseudoDiameter(coarse);
        double gamma=coarseDiameter==0?1:(double)Math.max(1,fineDiameter)/coarseDiameter;
        fine.k=coarse.k/gamma;
        double perturb=fine.k*.01;
        for(int i=0;i<fine.x.length;i++) if(!fine.pins[i])
        {
            int parent=fine.parent[i];double angle=random.nextDouble()*2*Math.PI;
            fine.x[i]=coarse.x[parent]+perturb*Math.cos(angle);fine.y[i]=coarse.y[parent]+perturb*Math.sin(angle);
        }
        current--;sweeps=0;progress=0;energy=Double.POSITIVE_INFINITY;step=fine.k;
    }
    private int pseudoDiameter(Level level)
    {
        boolean[] visited=new boolean[level.x.length];int diameter=0;
        int[] distances=new int[level.x.length];Arrays.fill(distances,-1);
        for(int i=0;i<visited.length;i++) if(!visited[i])
        {
            int[] first=bfs(level,i,visited,distances);int[] second=bfs(level,first[0],null,distances);
            diameter=Math.max(diameter,second[1]);
        }
        return diameter;
    }
    private int[] bfs(Level level,int start,boolean[] visited,int[] distances)
    {
        distances[start]=0;
        ArrayDeque<Integer> queue=new ArrayDeque<>();queue.add(start);int farthest=start;List<Integer> reached=new ArrayList<>();
        while(!queue.isEmpty())
        {
            checkpoint();int i=queue.remove();if(visited!=null) visited[i]=true;
            reached.add(i);
            if(distances[i]>distances[farthest]) farthest=i;
            for(int j:level.edges.get(i).keySet()) if(distances[j]<0) { distances[j]=distances[i]+1;queue.add(j); }
        }
        int length=distances[farthest];for(int i:reached) distances[i]=-1;
        return new int[]{farthest,length};
    }
    private static final class Level
    {
        final List<Map<Integer,Double>> edges;final boolean[] pins;
        double[] x,y;int[] parent;double k;
        Level(List<Map<Integer,Double>> edges,double[] x,double[] y,boolean[] pins)
        { this.edges=edges;this.x=x;this.y=y;this.pins=pins; }
    }
}
