package es.uam.eps.ir.relison.viz;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.*;
import es.uam.eps.ir.relison.viz.layouts.SpectralLayout;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SpectralLayoutTest {
    private Graph<Integer> path(int n,boolean weighted) {
        Graph<Integer> g=weighted?new FastUndirectedWeightedGraph<>():new FastUndirectedUnweightedGraph<>();
        for(int i=0;i<n;i++)g.addNode(i);for(int i=1;i<n;i++)g.addEdge(i-1,i,weighted?(double)i:1.0);return g;
    }
    private LayoutRequest<Integer> request(int n,int budget) {
        List<Integer> order=new ArrayList<>();for(int i=0;i<n;i++)order.add(i);
        return LayoutRequest.<Integer>builder().nodeOrder(order).seed(42).maxIterations(budget).build();
    }
    private double[][] axes(LayoutResult<Integer> result,int n,int dimensions) {
        double[][] axes=new double[dimensions][n];for(int i=0;i<n;i++) { if(dimensions==2) {axes[0][i]=result.getPositions().get(i).v1();axes[1][i]=result.getPositions().get(i).v2();}
            else {Point3D p=result.getPositions3D().get(i);axes[0][i]=p.getX();axes[1][i]=p.getY();axes[2][i]=p.getZ();} }return axes;
    }
    private double checkEigen(Graph<Integer> g,double[] vector,boolean normalized) {
        double[] degree=new double[vector.length],lv=new double[vector.length];
        for(int i=0;i<vector.length;i++)for(int j=0;j<vector.length;j++)if(i!=j&&g.containsEdge(i,j)) {double w=g.getEdgeWeight(i,j);degree[i]+=w;lv[i]+=w*(vector[i]-vector[j]);}
        double numerator=0,denominator=0,mean=0;
        for(int i=0;i<vector.length;i++){numerator+=vector[i]*lv[i];double m=normalized?degree[i]:1;denominator+=m*vector[i]*vector[i];mean+=m*vector[i];}
        assertEquals(0,mean,1e-7);assertEquals(1,denominator,1e-7);double lambda=numerator/denominator;
        for(int i=0;i<vector.length;i++)assertEquals(lambda*(normalized?degree[i]:1)*vector[i],lv[i],1e-6);
        return lambda;
    }
    @Test public void classicalPathRecoversKnownLowestEigenvalues() {
        Graph<Integer> g=path(12,false);var result=new SpectralLayout<Integer>(2,1,false,false,1e-10).compute(g,request(12,3000));
        assertEquals(LayoutDiagnostics.Termination.CONVERGED,result.getDiagnostics().getTermination());double[][] axes=axes(result,12,2);
        for(int k=0;k<2;k++)assertEquals(2-2*Math.cos((k+1)*Math.PI/12),checkEigen(g,axes[k],false),1e-8);
        double dot=0;for(int i=0;i<12;i++)dot+=axes[0][i]*axes[1][i];assertEquals(0,dot,1e-8);
    }
    @Test public void weightedNormalizedCoordinatesSolveGeneralizedEigenproblemIn3D() {
        Graph<Integer> g=path(15,true);var layout=new SpectralLayout<Integer>(3,1,true,true,1e-10);var result=layout.compute(g,request(15,3000));
        assertEquals(3,result.getSpaceDimensions());assertEquals(LayoutDiagnostics.Termination.CONVERGED,result.getDiagnostics().getTermination());
        double[][] axes=axes(result,15,3);for(double[] v:axes)checkEigen(g,v,true);
        for(int a=0;a<3;a++)for(int b=0;b<a;b++){double dot=0;for(int i=0;i<15;i++){double d=(i==0?0:i)+(i==14?0:i+1);dot+=d*axes[a][i]*axes[b][i];}assertEquals(0,dot,1e-8);}
        assertEquals(result.getPositions3D(),layout.compute(g,request(15,3000)).getPositions3D());
    }
    @Test public void repeatedEigenvaluesStillProduceOrthogonalCoordinates() {
        Graph<Integer> g=path(12,false);g.addEdge(0,11);
        var r=new SpectralLayout<Integer>(3,1,false,false,1e-10).compute(g,request(12,3000));double[][] a=axes(r,12,3);
        assertEquals(2-2*Math.cos(2*Math.PI/12),checkEigen(g,a[0],false),1e-8);
        assertEquals(2-2*Math.cos(2*Math.PI/12),checkEigen(g,a[1],false),1e-8);
        assertEquals(2-2*Math.cos(4*Math.PI/12),checkEigen(g,a[2],false),1e-8);
    }
    @Test public void disconnectedAndSmallGraphsAreFiniteAndSeparated() {
        for(int dimensions:new int[]{2,3})for(int n=0;n<4;n++) {
            Graph<Integer> g=new FastUndirectedUnweightedGraph<>();for(int i=0;i<n;i++)g.addNode(i);
            var r=new SpectralLayout<Integer>(dimensions).compute(g,request(n,500));assertEquals(dimensions,r.getSpaceDimensions());
            Set<Double> x=new HashSet<>();for(Pair<Double> p:r.getPositions().values()) {assertTrue(Double.isFinite(p.v1()));x.add(p.v1());}
            assertEquals(n,x.size());
        }
        var g=path(5,false);g.addNode(5);g.addNode(6);g.addEdge(5,6);
        var r=new SpectralLayout<Integer>().compute(g,request(7,500));double max=-Double.MAX_VALUE;for(int i=0;i<5;i++)max=Math.max(max,r.getPositions().get(i).v1());
        assertTrue(Math.min(r.getPositions().get(5).v1(),r.getPositions().get(6).v1())-max>=99.999999);
    }
    @Test public void budgetsWeightsPinsAndDirectedProjectionAreExplicit() {
        var g=path(20,false);var r=new SpectralLayout<Integer>().compute(g,request(20,0));assertEquals(LayoutDiagnostics.Termination.LIMIT_REACHED,r.getDiagnostics().getTermination());
        assertThrows(IllegalArgumentException.class,()->new SpectralLayout<Integer>().compute(g,LayoutRequest.<Integer>builder().initialPositions(Map.of(0,new Pair<>(0.,0.))).pinnedNodes(Set.of(0)).build()));
        var negative=path(3,true);negative.updateEdgeWeight(0,1,-1);
        assertThrows(IllegalArgumentException.class,()->new SpectralLayout<Integer>(2,1,true,true,1e-8).compute(negative,request(3,500)));
        Graph<Integer> directed=new FastDirectedUnweightedGraph<>();directed.addEdge(0,1);directed.addEdge(1,2);
        assertEquals(new SpectralLayout<Integer>().compute(path(3,false),request(3,500)).getPositions(),new SpectralLayout<Integer>().compute(directed,request(3,500)).getPositions());
    }
}
