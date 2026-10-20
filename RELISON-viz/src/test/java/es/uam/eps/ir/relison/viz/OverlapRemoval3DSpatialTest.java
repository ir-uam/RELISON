package es.uam.eps.ir.relison.viz;

import es.uam.eps.ir.relison.viz.internal.SphereSpatialIndex;
import es.uam.eps.ir.relison.viz.transforms.OverlapRemoval3D;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class OverlapRemoval3DSpatialTest {
    @Test public void matchesExhaustivePairOrderIncludingCoincidenceAndPins() {
        for(int seed=0;seed<30;seed++) {
            int n=seed==0?132:24;
            Random random=new Random(seed);double[][] p=new double[n][3];
            double[] radii=new double[n];Map<Integer,Point3D> positions=new LinkedHashMap<>();Map<Integer,Double> sizes=new LinkedHashMap<>();
            for(int i=0;i<n;i++) {
                for(int axis=0;axis<3;axis++)p[i][axis]=seed==0?0:random.nextDouble()*20-10;
                radii[i]=.5+random.nextDouble();sizes.put(i,radii[i]);positions.put(i,new Point3D(p[i][0],p[i][1],p[i][2]));
            }
            LayoutRequest<Integer> request=LayoutRequest.<Integer>builder().seed(seed).initialPositions3D(positions).pinnedNodes(Set.of(0)).nodeSizes(sizes).build();
            boolean separated=exhaustive(p,radii,seed,300);
            LayoutResult<Integer> input=LayoutResult.spatial(positions,new LayoutDiagnostics("test",LayoutDiagnostics.Termination.COMPLETED));
            if(!separated)assertThrows(IllegalArgumentException.class,()->new OverlapRemoval3D<Integer>(1,300).process(input,request));
            else {
                Map<Integer,Point3D> actual=new OverlapRemoval3D<Integer>(1,300).process(input,request).getPositions3D();
                for(int i=0;i<n;i++)assertEquals(new Point3D(p[i][0],p[i][1],p[i][2]),actual.get(i));
            }
        }
    }
    private boolean exhaustive(double[][] p,double[] radii,long seed,int passes) {
        Random random=new Random(seed);
        for(int pass=0;pass<passes;pass++) {
            boolean moved=false;
            for(int i=0;i<p.length;i++)for(int j=i+1;j<p.length;j++) {
                double dx=p[j][0]-p[i][0],dy=p[j][1]-p[i][1],dz=p[j][2]-p[i][2];
                double distance=Math.hypot(Math.hypot(dx,dy),dz),required=radii[i]+radii[j]+1;
                if(distance+1e-8>=required)continue;
                if(distance<1e-12){double latitude=2*random.nextDouble()-1,angle=random.nextDouble()*2*Math.PI,radial=Math.sqrt(1-latitude*latitude);dx=radial*Math.cos(angle);dy=latitude;dz=radial*Math.sin(angle);}
                else{dx/=distance;dy/=distance;dz/=distance;}
                double push=required-distance+1e-8,a=i==0?0:.5,b=i==0?1:.5;
                p[i][0]-=dx*push*a;p[i][1]-=dy*push*a;p[i][2]-=dz*push*a;
                p[j][0]+=dx*push*b;p[j][1]+=dy*push*b;p[j][2]+=dz*push*b;moved=true;
            }
            if(!moved)return true;
        }
        for(int i=0;i<p.length;i++)for(int j=i+1;j<p.length;j++)
            if(Math.hypot(Math.hypot(p[i][0]-p[j][0],p[i][1]-p[j][1]),p[i][2]-p[j][2])+1e-8<radii[i]+radii[j]+1)return false;
        return true;
    }
    @Test public void movingIndexNeverMissesNearbyPairs() {
        Random random=new Random(42);int n=150;double[] x=new double[n],y=new double[n],z=new double[n];
        SphereSpatialIndex index=new SphereSpatialIndex(x,y,z,3);
        for(int step=0;step<1000;step++) {
            int node=random.nextInt(n);x[node]=random.nextDouble()*50-25;y[node]=random.nextDouble()*50-25;z[node]=random.nextDouble()*50-25;
            index.move(node,x[node],y[node],z[node]);Set<Integer> candidates=index.candidates(node,-1);
            for(int j=0;j<n;j++)if(Math.hypot(Math.hypot(x[node]-x[j],y[node]-y[j]),z[node]-z[j])<3)assertTrue(candidates.contains(j));
        }
    }
    @Test public void sparseSceneAvoidsAllPairCandidates() {
        int n=10000;double[] x=new double[n],y=new double[n],z=new double[n];for(int i=0;i<n;i++)x[i]=10.0*i;
        SphereSpatialIndex index=new SphereSpatialIndex(x,y,z,3);
        long candidates=0;for(int i=0;i<n;i++)candidates+=index.candidates(i,i).size();
        assertEquals(0,candidates);
    }
}
