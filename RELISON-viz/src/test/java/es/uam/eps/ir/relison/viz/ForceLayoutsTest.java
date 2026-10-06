/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.fast.*;
import es.uam.eps.ir.relison.graph.multigraph.fast.FastDirectedWeightedMultiGraph;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import es.uam.eps.ir.relison.viz.layouts.*;
import es.uam.eps.ir.relison.viz.transforms.*;
import es.uam.eps.ir.relison.viz.internal.QuadTree;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import static org.junit.Assert.*;

public class ForceLayoutsTest {
    private Graph<Integer> graph(int n) {
        Graph<Integer> graph=new FastDirectedUnweightedGraph<>();
        for(int i=0;i<n;i++) graph.addNode(i);
        return graph;
    }
    private List<IterativeLayout<Integer>> layouts() {
        return Arrays.asList(new FruchtermanReingoldLayout<>(),new ForceAtlas2Layout<>());
    }
    @Test
    public void steppingMatchesBatchAndSessionsAreIndependent() {
        Graph<Integer> graph=graph(5); graph.addEdge(0,1); graph.addEdge(1,2);
        LayoutRequest<Integer> request=LayoutRequest.<Integer>builder().seed(42).maxIterations(12).build();
        for(IterativeLayout<Integer> layout:layouts()) {
            LayoutResult<Integer> expected=layout.compute(graph,request);
            LayoutSession<Integer> a=layout.initialize(graph,request),b=layout.initialize(graph,request);
            LayoutResult<Integer> before=b.snapshot();
            a.step(4); a.step(8);
            assertEquals(expected.getPositions(),a.snapshot().getPositions());
            assertEquals(before.getPositions(),b.snapshot().getPositions());
            assertEquals(12,a.snapshot().getDiagnostics().getIterations());
            assertEquals(LayoutDiagnostics.Termination.LIMIT_REACHED,a.snapshot().getDiagnostics().getTermination());
            assertEquals(LayoutDiagnostics.Termination.RUNNING,b.snapshot().getDiagnostics().getTermination());
            assertThrows(IllegalArgumentException.class,()->a.step(-1));
            assertThrows(UnsupportedOperationException.class,()->before.getPositions().clear());
        }
    }
    @Test
    public void pinsAndWarmStartsSurviveSnapshotAndGraphChanges() {
        Graph<Integer> graph=graph(3); graph.addEdge(0,1);
        Pair<Double> pin=new Pair<>(100.0,-20.0);
        LayoutRequest<Integer> request=LayoutRequest.<Integer>builder()
            .initialPositions(Map.of(0,pin,1,new Pair<>(0.0,0.0))).pinnedNodes(Collections.singleton(0))
            .maxIterations(20).build();
        for(IterativeLayout<Integer> layout:layouts()) {
            LayoutSession<Integer> session=layout.initialize(graph,request);
            assertEquals(new Pair<>(0.0,0.0),session.snapshot().getPositions().get(1));
            session.step(20);
            assertEquals(pin,session.snapshot().getPositions().get(0));
            session.snapshot().getPositions().values().forEach(point->{
                assertTrue(Double.isFinite(point.v1())); assertTrue(Double.isFinite(point.v2()));
            });
        }
        LayoutSession<Integer> session=new FruchtermanReingoldLayout<Integer>().initialize(graph,request);
        graph.addNode(9); session.step(20);
        assertFalse(session.snapshot().getPositions().containsKey(9));
    }
    @Test
    public void cancellationAndZeroBudgetPreserveCurrentCoordinates() {
        AtomicBoolean cancelled=new AtomicBoolean(false);
        LayoutRequest<Integer> request=LayoutRequest.<Integer>builder().cancellation(cancelled::get).build();
        for(IterativeLayout<Integer> layout:layouts()) {
            LayoutSession<Integer> session=layout.initialize(graph(4),request);
            Map<Integer,Pair<Double>> before=session.snapshot().getPositions();
            session.cancel(); session.step(100);
            assertEquals(before,session.snapshot().getPositions());
            assertEquals(LayoutDiagnostics.Termination.CANCELLED,session.snapshot().getDiagnostics().getTermination());
        }
        cancelled.set(true);
        assertEquals(LayoutDiagnostics.Termination.CANCELLED,new ForceAtlas2Layout<Integer>().compute(graph(4),request).getDiagnostics().getTermination());
        for(IterativeLayout<Integer> layout:layouts()) {
            assertEquals(LayoutDiagnostics.Termination.LIMIT_REACHED,layout.compute(graph(2),
                LayoutRequest.<Integer>builder().maxIterations(0).build()).getDiagnostics().getTermination());
            assertEquals(LayoutDiagnostics.Termination.CONVERGED,layout.compute(graph(0)).getDiagnostics().getTermination());
        }
    }
    @Test
    public void frAndForceAtlasHaveReferenceEquilibria() {
        Graph<Integer> graph=graph(2); graph.addEdge(0,1);
        LayoutRequest<Integer> request=LayoutRequest.<Integer>builder().maxIterations(1)
            .initialPositions(Map.of(0,new Pair<>(-5.0,0.0),1,new Pair<>(5.0,0.0))).build();
        LayoutResult<Integer> result=new FruchtermanReingoldLayout<Integer>(
            new FruchtermanReingoldConfig(10,0.95,0,0,false)).compute(graph,request);
        assertEquals(request.getInitialPositions(),result.getPositions());
        request=LayoutRequest.<Integer>builder().maxIterations(1)
            .initialPositions(Map.of(0,new Pair<>(-10.0,0.0),1,new Pair<>(10.0,0.0))).build();
        result=new ForceAtlas2Layout<Integer>(new ForceAtlas2Config(100,0,1,0,0,0,false,false)).compute(graph,request);
        assertEquals(request.getInitialPositions(),result.getPositions());
    }
    @Test
    public void barnesHutMatchesExactWhenThetaIsZeroAndHandlesCoincidences() {
        double[] x={-3,0,2,5},y={1,-1,4,0},mass={1,2,3,4};
        double[] dx=new double[4],dy=new double[4],exactX=new double[4],exactY=new double[4];
        QuadTree tree=new QuadTree(x,y,mass,()->false);
        for(int i=0;i<4;i++) {
            tree.accumulate(i,0,2,dx,dy);
            for(int j=0;j<4;j++) if(i!=j) QuadTree.pair(i,j,x,y,mass,2,exactX,exactY);
        }
        assertArrayEquals(exactX,dx,1e-12); assertArrayEquals(exactY,dy,1e-12);
        Graph<Integer> graph=graph(10); Map<Integer,Pair<Double>> initial=new LinkedHashMap<>();
        for(int i=0;i<10;i++) initial.put(i,new Pair<>(0.0,0.0));
        LayoutRequest<Integer> request=LayoutRequest.<Integer>builder().initialPositions(initial).maxIterations(20).build();
        LayoutResult<Integer> result=new FruchtermanReingoldLayout<Integer>(
            new FruchtermanReingoldConfig(10,0.95,0.8,0,false)).compute(graph,request);
        assertTrue(new HashSet<>(result.getPositions().values()).size()>1);
        assertEquals(result.getPositions(),new FruchtermanReingoldLayout<Integer>(
            new FruchtermanReingoldConfig(10,0.95,0.8,0,false)).compute(graph,request).getPositions());
    }
    @Test
    public void undirectedEdgesAreCountedOnceAndParallelForcesAccumulate() {
        Graph<Integer> directed=graph(2); directed.addEdge(0,1);
        Graph<Integer> undirected=new FastUndirectedUnweightedGraph<>(); undirected.addEdge(0,1);
        LayoutRequest<Integer> request=LayoutRequest.<Integer>builder().nodeOrder(Arrays.asList(0,1)).maxIterations(1)
            .initialPositions(Map.of(0,new Pair<>(-10.0,0.0),1,new Pair<>(10.0,0.0))).build();
        for(IterativeLayout<Integer> layout:layouts())
            assertEquals(layout.compute(directed,request).getPositions(),layout.compute(undirected,request).getPositions());
        Graph<Integer> multi=new FastDirectedWeightedMultiGraph<>(); multi.addEdge(0,1,1.0); multi.addEdge(0,1,1.0);
        FruchtermanReingoldLayout<Integer> reference=new FruchtermanReingoldLayout<>(new FruchtermanReingoldConfig(20,0.95,0,0,false));
        assertNotEquals(reference.compute(directed,request).getPositions(), reference.compute(multi,request).getPositions());
        directed.addEdge(0,0);
        assertNotEquals(new ForceAtlas2Layout<Integer>().compute(undirected,request).getPositions(),
            new ForceAtlas2Layout<Integer>().compute(directed,request).getPositions());
    }
    @Test
    public void overlapRemovalRespectsRadiiAndPins() {
        Map<Integer,Pair<Double>> points=Map.of(0,new Pair<>(0.0,0.0),1,new Pair<>(0.0,0.0));
        LayoutRequest<Integer> request=LayoutRequest.<Integer>builder().initialPositions(points)
            .pinnedNodes(Collections.singleton(0)).nodeSizes(Map.of(0,2.0,1,3.0)).build();
        LayoutResult<Integer> before=new LayoutResult<>(points,new LayoutDiagnostics("test",LayoutDiagnostics.Termination.COMPLETED));
        LayoutResult<Integer> after=new OverlapRemoval<Integer>(1,200).process(before,request);
        assertEquals(points.get(0),after.getPositions().get(0));
        assertTrue(Math.hypot(after.getPositions().get(1).v1(),after.getPositions().get(1).v2())>=6);
        LayoutRequest<Integer> pinned=LayoutRequest.<Integer>builder().initialPositions(points)
            .pinnedNodes(new HashSet<>(Arrays.asList(0,1))).build();
        assertThrows(IllegalArgumentException.class,()->new OverlapRemoval<Integer>().process(before,pinned));
    }
    @Test
    public void settingsAndSizeConstraintsAreValidated() {
        assertThrows(IllegalArgumentException.class,()->new FruchtermanReingoldConfig(1,1,0,0,false));
        assertThrows(IllegalArgumentException.class,()->new ForceAtlas2Config(0,1,1,0,0,0,false,false));
        assertThrows(IllegalArgumentException.class,()->LayoutRequest.builder().maxIterations(-1).build());
        assertThrows(IllegalArgumentException.class,()->LayoutRequest.builder().nodeSizes(Map.of(0,Double.NaN)).build());
        assertThrows(IllegalArgumentException.class,()->new ForceAtlas2Layout<Integer>().compute(graph(1),
            LayoutRequest.<Integer>builder().nodeSizes(Map.of(9,1.0)).build()));
        Graph<Integer> weighted=new FastDirectedWeightedGraph<>(); weighted.addEdge(0,1,-1.0);
        assertThrows(IllegalArgumentException.class,()->new FruchtermanReingoldLayout<Integer>(
            new FruchtermanReingoldConfig(50,0.95,0,0,true)).compute(weighted));
        assertEquals(2,new FruchtermanReingoldLayout<Integer>().compute(weighted).getPositions().size());
    }

    @Test
    public void cancellationDuringForceEvaluationDiscardsIncompleteIteration() {
        for(double theta:new double[]{0,0.8}) {
            AtomicBoolean armed=new AtomicBoolean(false);
            java.util.concurrent.atomic.AtomicInteger checks=new java.util.concurrent.atomic.AtomicInteger();
            LayoutRequest<Integer> request=LayoutRequest.<Integer>builder().cancellation(
                ()->armed.get() && checks.incrementAndGet()>300).build();
            LayoutSession<Integer> session=new FruchtermanReingoldLayout<Integer>(
                new FruchtermanReingoldConfig(50,0.95,theta,0,false)).initialize(graph(500),request);
            Map<Integer,Pair<Double>> before=session.snapshot().getPositions();
            armed.set(true); session.step(1);
            assertEquals(before,session.snapshot().getPositions());
            assertEquals(0,session.snapshot().getDiagnostics().getIterations());
            assertEquals(LayoutDiagnostics.Termination.CANCELLED,session.snapshot().getDiagnostics().getTermination());
        }
    }

    @Test
    public void activeTimeLimitReportsExhaustionWithoutPartialMovement() {
        AtomicBoolean slow=new AtomicBoolean(false);
        LayoutRequest<Integer> request=LayoutRequest.<Integer>builder().timeLimitMillis(1).cancellation(()->{
            if(slow.get()) {
                try { Thread.sleep(2); }
                catch(InterruptedException error) { Thread.currentThread().interrupt(); return true; }
            }
            return false;
        }).build();
        LayoutSession<Integer> session=new ForceAtlas2Layout<Integer>().initialize(graph(4),request);
        Map<Integer,Pair<Double>> before=session.snapshot().getPositions();
        slow.set(true); session.step(10);
        assertEquals(before,session.snapshot().getPositions());
        assertEquals(LayoutDiagnostics.Termination.LIMIT_REACHED,session.snapshot().getDiagnostics().getTermination());
    }

    @Test
    public void approximateRepulsionIsCloseToExactOnDistributedPoints() {
        int n=128; double[] x=new double[n],y=new double[n],mass=new double[n];
        Random random=new Random(42);
        for(int i=0;i<n;i++) { x[i]=random.nextDouble()*100; y[i]=random.nextDouble()*100; mass[i]=1+i%4; }
        double[] dx=new double[n],dy=new double[n],exactX=new double[n],exactY=new double[n];
        QuadTree tree=new QuadTree(x,y,mass,()->false);
        for(int i=0;i<n;i++) {
            tree.accumulate(i,0.3,2,dx,dy);
            for(int j=0;j<n;j++) if(i!=j) QuadTree.pair(i,j,x,y,mass,2,exactX,exactY);
            assertTrue(Math.hypot(dx[i]-exactX[i],dy[i]-exactY[i]) <= 0.02*Math.hypot(exactX[i],exactY[i])+1e-8);
        }
    }

    @Test
    public void forceAtlasReferenceTrajectoryHasNoStandardMovementCap() {
        Graph<Integer> graph=graph(2);
        double[] x={-1,1},previous={0,0};
        double speed=1,efficiency=1;
        LayoutSession<Integer> session=new ForceAtlas2Layout<Integer>(
            new ForceAtlas2Config(100000,0,1,0,0,0,false,false)).initialize(graph,
            LayoutRequest.<Integer>builder().nodeOrder(Arrays.asList(0,1)).maxIterations(8)
                .initialPositions(Map.of(0,new Pair<>(x[0],0.0),1,new Pair<>(x[1],0.0))).build());
        // Independent scalar oracle: two isolated nodes, mass one, exact repulsion.
        for(int step=0;step<8;step++) {
            double force=100000/(x[0]-x[1]);
            double[] current={force,-force};
            double swing=0,traction=0;
            for(int i=0;i<2;i++) { swing+=Math.abs(current[i]-previous[i]); traction+=0.5*Math.abs(current[i]+previous[i]); }
            double estimated=0.05*Math.sqrt(2);
            double jitter=Math.max(Math.sqrt(estimated),Math.min(10,estimated*traction/4));
            if(swing>2*traction) { if(efficiency>0.05) efficiency*=0.5; jitter=Math.max(jitter,1); }
            double target=jitter*efficiency*traction/swing;
            if(swing>jitter*traction) { if(efficiency>0.05) efficiency*=0.7; }
            else if(speed<1000) efficiency*=1.3;
            speed+=Math.min(target-speed,0.5*speed);
            for(int i=0;i<2;i++) x[i]+=current[i]*speed/(1+Math.sqrt(speed*Math.abs(current[i]-previous[i])));
            session.step(1);
            if(step==0) assertTrue(session.snapshot().getDiagnostics().getMaximumDisplacement()>10);
            for(int i=0;i<2;i++) assertEquals(x[i],session.snapshot().getPositions().get(i).v1(),1e-9);
            previous=current;
        }
    }

    @Test
    public void forceAtlasRepulsionDoesNotRegularizeCloseOrCoincidentPairs() {
        double[] x={0,1e-8,0},y={0,0,0},mass={1,2,3},dx=new double[3],dy=new double[3];
        QuadTree.forceAtlasPair(0,1,x,y,mass,2,dx,dy);
        assertEquals(-4e8,dx[0],1e-6);
        QuadTree.forceAtlasPair(0,2,x,y,mass,2,dx,dy);
        assertEquals(-4e8,dx[0],1e-6);
        double[] bx=new double[3],by=new double[3];
        new QuadTree(x,y,mass,()->false,false).accumulate(0,0.8,2,bx,by);
        assertEquals(dx[0],bx[0],1e-6);
    }

    @Test
    public void forceAtlasOptionalModesUseReferenceForces() {
        Graph<Integer> graph=new FastDirectedWeightedGraph<>();
        graph.addEdge(0,1,2.0); graph.addEdge(0,2,4.0);
        LayoutRequest<Integer> request=LayoutRequest.<Integer>builder().nodeOrder(Arrays.asList(0,1,2)).maxIterations(1)
            .initialPositions(Map.of(0,new Pair<>(0.0,0.0),1,new Pair<>(20.0,0.0),2,new Pair<>(0.0,20.0))).build();
        ForceAtlas2Config plain=new ForceAtlas2Config(1,0,1,0,1,0,false,false);
        Map<Integer,Pair<Double>> baseline=new ForceAtlas2Layout<Integer>(plain).compute(graph,request).getPositions();

        double[][][] expectedForces={
            {{40*7.0/9-0.3,80*7.0/9-0.3},{0.4-40*7.0/9,-0.1},{-0.1,0.4-80*7.0/9}},
            {{-12000,-12000},{20000,-8000},{-8000,20000}},
            {{-0.3,19.7},{0.4,-0.1},{-0.1,-19.6}},
            {{9.7,4.7},{-9.6,-0.1},{-0.1,-4.6}}
        };
        for(int mode=0;mode<4;mode++) {
            ForceAtlas2Config config=new ForceAtlas2Config(1,0,1,0,1,0,false,false,
                mode==0,mode==1,mode==2,mode==3);
            LayoutRequest<Integer> sized=LayoutRequest.<Integer>builder().nodeOrder(Arrays.asList(0,1,2)).maxIterations(1)
                .initialPositions(request.getInitialPositions()).nodeSizes(Map.of(0,15.0,1,15.0,2,15.0)).build();
            LayoutResult<Integer> actual=new ForceAtlas2Layout<Integer>(config).compute(graph,sized);
            assertNotEquals(baseline,actual.getPositions());
            assertFirstForceAtlasStep(request.getInitialPositions(),new double[]{3,2,2},expectedForces[mode],mode==1,actual);
        }
        // Size forces use exact pairs even with a positive Barnes-Hut theta.
        ForceAtlas2Config exact=new ForceAtlas2Config(1,0,1,0,1,0,false,false,false,true,false,false);
        ForceAtlas2Config tree=new ForceAtlas2Config(1,0,1,0.8,1,0,false,false,false,true,false,false);
        assertEquals(new ForceAtlas2Layout<Integer>(exact).compute(graph,request).getPositions(),
            new ForceAtlas2Layout<Integer>(tree).compute(graph,request).getPositions());
    }

    // Independent first-step oracle for hand-calculated forces, including local damping.
    private void assertFirstForceAtlasStep(Map<Integer,Pair<Double>> initial,double[] masses,
            double[][] forces,boolean collision,LayoutResult<Integer> actual) {
        double swinging=0;
        for(int i=0;i<masses.length;i++) swinging+=masses[i]*Math.hypot(forces[i][0],forces[i][1]);
        double estimated=0.05*Math.sqrt(masses.length);
        double jitter=Math.max(Math.sqrt(estimated),Math.min(10,estimated*swinging/2/(masses.length*masses.length)));
        double speed=Math.min(1.5,jitter/2);
        for(int i=0;i<masses.length;i++) {
            double norm=Math.hypot(forces[i][0],forces[i][1]);
            double factor=speed/(1+Math.sqrt(speed*masses[i]*norm));
            if(collision) factor=norm==0?0:Math.min(0.1*factor*norm,10)/norm;
            assertEquals(initial.get(i).v1()+forces[i][0]*factor,actual.getPositions().get(i).v1(),1e-9);
            assertEquals(initial.get(i).v2()+forces[i][1]*factor,actual.getPositions().get(i).v2(),1e-9);
        }
    }

    @Test
    public void forceAtlasGravityAndLinLogFollowReferenceEquations() {
        Graph<Integer> single=graph(1);
        Map<Integer,Pair<Double>> initial=Map.of(0,new Pair<>(3.0,4.0));
        LayoutRequest<Integer> request=LayoutRequest.<Integer>builder().initialPositions(initial).maxIterations(1).build();
        for(boolean strong:new boolean[]{false,true}) {
            ForceAtlas2Config config=new ForceAtlas2Config(100,2,1,0,0,0,false,strong);
            double multiplier=strong?2:0.4;
            assertFirstForceAtlasStep(initial,new double[]{1},new double[][]{{-3*multiplier,-4*multiplier}},false,
                new ForceAtlas2Layout<Integer>(config).compute(single,request));
        }
        Graph<Integer> edge=graph(2); edge.addEdge(0,1);
        initial=Map.of(0,new Pair<>(0.0,0.0),1,new Pair<>(20.0,0.0));
        request=LayoutRequest.<Integer>builder().nodeOrder(Arrays.asList(0,1)).initialPositions(initial).maxIterations(1).build();
        double f=Math.log1p(20)-0.2;
        assertFirstForceAtlasStep(initial,new double[]{2,2},new double[][]{{f,0},{-f,0}},false,
            new ForceAtlas2Layout<Integer>(new ForceAtlas2Config(1,0,1,0,0,0,true,false)).compute(edge,request));
    }

    @Test
    public void frReferenceFrameTrajectoryUsesAreaScaleAndLinearCooling() {
        Graph<Integer> graph=graph(2); graph.addEdge(0,1);
        LayoutSession<Integer> session=new FruchtermanReingoldLayout<Integer>(
            new FruchtermanReingoldConfig(100,50,10,0,0,false)).initialize(graph,
            LayoutRequest.<Integer>builder().nodeOrder(Arrays.asList(0,1)).maxIterations(4)
                .initialPositions(Map.of(0,new Pair<>(-10.0,0.0),1,new Pair<>(10.0,0.0))).build());
        // k=50; forces alternate after overshooting equilibrium.
        double[] expected={-20,-27.5,-22.5,-25},movement={10,7.5,5,2.5};
        for(int i=0;i<4;i++) {
            session.step(1);
            assertEquals(expected[i],session.snapshot().getPositions().get(0).v1(),1e-12);
            assertEquals(-expected[i],session.snapshot().getPositions().get(1).v1(),1e-12);
            assertEquals(movement[i],session.snapshot().getDiagnostics().getMaximumDisplacement(),1e-12);
        }
    }

    @Test
    public void frReferenceFrameClampsCoordinatesAndRejectsPinsOutsideIt() {
        FruchtermanReingoldLayout<Integer> layout=new FruchtermanReingoldLayout<>(
            new FruchtermanReingoldConfig(100,100,20,0,0,false));
        LayoutRequest<Integer> request=LayoutRequest.<Integer>builder().maxIterations(1)
            .initialPositions(Map.of(0,new Pair<>(-49.0,0.0),1,new Pair<>(49.0,0.0))).build();
        LayoutResult<Integer> result=layout.compute(graph(2),request);
        assertEquals(-50,result.getPositions().get(0).v1(),0);
        assertEquals(50,result.getPositions().get(1).v1(),0);
        assertEquals(1,result.getDiagnostics().getMaximumDisplacement(),1e-12);
        LayoutRequest<Integer> outside=LayoutRequest.<Integer>builder().maxIterations(0)
            .initialPositions(Map.of(0,new Pair<>(100.0,-100.0))).build();
        assertEquals(new Pair<>(50.0,-50.0),layout.compute(graph(1),outside).getPositions().get(0));
        assertThrows(IllegalArgumentException.class,()->layout.initialize(graph(1),
            LayoutRequest.<Integer>builder().initialPositions(outside.getInitialPositions())
                .pinnedNodes(Set.of(0)).build()));
        assertThrows(IllegalArgumentException.class,()->new FruchtermanReingoldConfig(0,100,10,0,0,false));
    }

    @Test
    public void frUnboundedModeMatchesAnIndependentTwoDimensionalForceCalculation() {
        Graph<Integer> graph=graph(3); graph.addEdge(0,1);
        double[] x={-3,4,1},y={2,-1,5},dx=new double[3],dy=new double[3];
        double k=10,temperature=10;
        for(int i=0;i<3;i++) for(int j=0;j<3;j++) if(i!=j) {
            double vx=x[i]-x[j],vy=y[i]-y[j],d=Math.hypot(vx,vy);
            dx[i]+=vx/d*k*k/d; dy[i]+=vy/d*k*k/d;
        }
        double vx=x[0]-x[1],vy=y[0]-y[1],d=Math.hypot(vx,vy);
        dx[0]-=vx/d*d*d/k; dy[0]-=vy/d*d*d/k;
        dx[1]+=vx/d*d*d/k; dy[1]+=vy/d*d*d/k;
        Map<Integer,Pair<Double>> initial=new LinkedHashMap<>();
        for(int i=0;i<3;i++) initial.put(i,new Pair<>(x[i],y[i]));
        LayoutResult<Integer> result=new FruchtermanReingoldLayout<Integer>(
            new FruchtermanReingoldConfig(k,0.95,0,0,false)).compute(graph,
            LayoutRequest.<Integer>builder().nodeOrder(Arrays.asList(0,1,2)).initialPositions(initial).maxIterations(1).build());
        for(int i=0;i<3;i++) {
            double norm=Math.hypot(dx[i],dy[i]),factor=Math.min(temperature,norm)/norm;
            assertEquals(x[i]+dx[i]*factor,result.getPositions().get(i).v1(),1e-12);
            assertEquals(y[i]+dy[i]*factor,result.getPositions().get(i).v2(),1e-12);
        }
        double[] rx=new double[2],ry=new double[2];
        QuadTree.pair(0,1,new double[]{0,1e-8},new double[]{0,0},new double[]{1,1},100,rx,ry);
        assertEquals(-1e10,rx[0],1e-5);
    }

}
