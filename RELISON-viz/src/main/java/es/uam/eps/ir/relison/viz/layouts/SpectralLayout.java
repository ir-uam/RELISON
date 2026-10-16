/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.IndexedGraphSnapshot;
import es.uam.eps.ir.relison.viz.internal.SpectralSolver;
import java.util.*;

/**
 * Spectral eigenprojection in two or three dimensions, following Koren (2003),
 * Sections 3.1 and 4. Classical mode solves Lx=lambda*x; degree-normalized
 * mode solves Lx=lambda*D*x with D-orthonormal coordinates. Components are
 * solved independently, then translated along x for display. Arbitrary pins
 * are rejected because they change the eigenproblem.
 * @param <U> node type
 */
public final class SpectralLayout<U> implements Layout<U> {
    private final int dimensions;
    private final double scale,tolerance;
    private final boolean normalized,weighted;
    /** Creates a 2D degree-normalized layout with scale 100. */
    public SpectralLayout() { this(2); }
    /** Creates a degree-normalized layout.
     * @param dimensions 2 or 3 */
    public SpectralLayout(int dimensions) { this(dimensions,100,true,false,1e-8); }
    /** Creates a spectral eigenprojection.
     * @param dimensions 2 or 3
     * @param scale positive coordinate multiplier and component-spacing unit
     * @param normalized whether to use Koren's degree-normalized formulation
     * @param weighted whether edges carry nonnegative similarity strengths
     * @param tolerance positive scaled eigen-residual threshold
     */
    public SpectralLayout(int dimensions,double scale,boolean normalized,boolean weighted,double tolerance) {
        if((dimensions!=2&&dimensions!=3)||!Double.isFinite(scale)||scale<=0||!Double.isFinite(tolerance)||tolerance<=0)
            throw new IllegalArgumentException("Invalid spectral dimensions, scale or tolerance");
        this.dimensions=dimensions;this.scale=scale;this.normalized=normalized;this.weighted=weighted;this.tolerance=tolerance;
    }
    @Override public LayoutDescriptor getDescriptor() { return new LayoutDescriptor(dimensions==3?"spectral-3d":"spectral","Spectral "+dimensions+"D",dimensions); }
    @Override public LayoutResult<U> compute(Graph<U> input,LayoutRequest<U> request) {
        IndexedGraphSnapshot<U> graph=IndexedGraphSnapshot.capture(input,request);
        if(!request.getPinnedNodes().isEmpty())throw new IllegalArgumentException("Spectral layout does not support arbitrary pins");
        int n=graph.getNodes().size();List<Map<Integer,Double>> adjacent=new ArrayList<>();
        for(int i=0;i<n;i++)adjacent.add(new TreeMap<>());
        Runnable check=()->{if(Thread.currentThread().isInterrupted()||request.getCancellation().getAsBoolean())throw new IllegalArgumentException("Spectral layout cancelled");};
        for(int i=0;i<n;i++) { check.run();int[] targets=graph.getTargets(i);double[] weights=graph.getWeights(i);
            for(int e=0;e<targets.length;e++) { int j=targets[e];if(i==j||(!graph.isDirected()&&j<i))continue;
                double weight=weighted?weights[e]:1;if(!Double.isFinite(weight)||weight<0)throw new IllegalArgumentException("Spectral strengths must be nonnegative and finite");
                if(weight>0) { adjacent.get(i).merge(j,weight,Double::sum);adjacent.get(j).merge(i,weight,Double::sum); }
            }
        }
        List<List<Integer>> components=new ArrayList<>();boolean[] seen=new boolean[n];
        for(int i=0;i<n;i++)if(!seen[i]) { List<Integer> component=new ArrayList<>();ArrayDeque<Integer> queue=new ArrayDeque<>();queue.add(i);seen[i]=true;
            while(!queue.isEmpty()) { check.run();int u=queue.remove();component.add(u);for(int v:adjacent.get(u).keySet())if(!seen[v]){seen[v]=true;queue.add(v);} }
            Collections.sort(component);components.add(component);
        }
        double[][] coordinates=new double[n][dimensions];double cursor=0;int iterations=0;boolean converged=true;Random random=new Random(request.getSeed());
        int[] local=new int[n];
        for(List<Integer> component:components) {
            int count=component.size();for(int i=0;i<count;i++)local[component.get(i)]=i;
            int[][] targets=new int[count][];double[][] strengths=new double[count][];double[] degree=new double[count];
            for(int i=0;i<count;i++) { check.run();Map<Integer,Double> neighbours=adjacent.get(component.get(i));targets[i]=new int[neighbours.size()];strengths[i]=new double[neighbours.size()];int e=0;
                for(Map.Entry<Integer,Double> edge:neighbours.entrySet()){targets[i][e]=local[edge.getKey()];strengths[i][e]=edge.getValue();degree[i]+=edge.getValue();e++;}
                if(!Double.isFinite(degree[i]))throw new IllegalArgumentException("Spectral degree overflow");
            }
            SpectralSolver.Result result=SpectralSolver.solve(targets,strengths,degree,dimensions,normalized,request.getMaxIterations(),tolerance,random,check);
            iterations=Math.max(iterations,result.iterations);converged&=result.converged;
            double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;
            for(int i=0;i<count;i++) { int node=component.get(i);for(int axis=0;axis<result.vectors.length;axis++)
                coordinates[node][axis]=scale*result.vectors[axis][i]/(normalized?Math.sqrt(degree[i]):1);
                min=Math.min(min,coordinates[node][0]);max=Math.max(max,coordinates[node][0]); }
            if(components.size()>1) { for(int node:component)coordinates[node][0]+=cursor-min;cursor+=max-min+scale; }
        }
        if(components.size()>1) { double center=(cursor-scale)/2;for(double[] p:coordinates)p[0]-=center; }
        LayoutDiagnostics diagnostics=new LayoutDiagnostics(getDescriptor().getId(),converged?LayoutDiagnostics.Termination.CONVERGED:LayoutDiagnostics.Termination.LIMIT_REACHED,iterations,0);
        if(dimensions==3) { Map<U,Point3D> result=new LinkedHashMap<>();for(int i=0;i<n;i++)result.put(graph.getNodes().get(i),new Point3D(coordinates[i][0],coordinates[i][1],coordinates[i][2]));return LayoutResult.spatial(result,diagnostics); }
        Map<U,Pair<Double>> result=new LinkedHashMap<>();for(int i=0;i<n;i++)result.put(graph.getNodes().get(i),new Pair<>(coordinates[i][0],coordinates[i][1]));return new LayoutResult<>(result,diagnostics);
    }
}
