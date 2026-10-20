/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.internal;
import java.util.*;

/** Dynamic uniform spatial hash for sphere collision candidates. */
public final class SphereSpatialIndex {
    private final double side;
    private final Map<Cell,Set<Integer>> cells=new HashMap<>();
    private final Cell[] locations;
    /** Creates an index whose cell side bounds every possible collision distance.
     * @param x centres along x
     * @param y centres along y
     * @param z centres along z
     * @param side positive finite upper bound on pair collision distances
     */
    public SphereSpatialIndex(double[] x,double[] y,double[] z,double side) {
        if(!Double.isFinite(side)||side<=0||x.length!=y.length||x.length!=z.length)throw new IllegalArgumentException("Invalid sphere index dimensions or cell size");
        this.side=side;locations=new Cell[x.length];for(int i=0;i<x.length;i++)move(i,x[i],y[i],z[i]);
    }
    /** Updates a centre after a separation move.
     * @param node node index
     * @param x centre x
     * @param y centre y
     * @param z centre z
     * @return whether the centre entered a different cell
     */
    public boolean move(int node,double x,double y,double z) {
        Cell cell=new Cell(coordinate(x),coordinate(y),coordinate(z)),old=locations[node];
        if(cell.equals(old))return false;
        if(old!=null){Set<Integer> bucket=cells.get(old);bucket.remove(node);if(bucket.isEmpty())cells.remove(old);}
        cells.computeIfAbsent(cell,key->new HashSet<>()).add(node);locations[node]=cell;return true;
    }
    /** Returns candidate neighbours in deterministic index order.
     * @param node query node
     * @param after omit indices already visited in the current pair scan
     * @return candidates from the centre's cell and its 26 neighbours
     */
    public NavigableSet<Integer> candidates(int node,int after) {
        Cell center=locations[node];NavigableSet<Integer> result=new TreeSet<>();
        for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++) {
            if(overflow(center.x,dx)||overflow(center.y,dy)||overflow(center.z,dz))continue;
            Set<Integer> bucket=cells.get(new Cell(center.x+dx,center.y+dy,center.z+dz));
            if(bucket!=null)for(int index:bucket)if(index>after)result.add(index);
        }
        return result;
    }
    /** Returns the largest bucket occupancy for dense-scene fallback selection.
     * @return maximum number of centres in one cell */
    public int maximumBucketSize(){int size=0;for(Set<Integer> bucket:cells.values())size=Math.max(size,bucket.size());return size;}
    private long coordinate(double value) {
        if(!Double.isFinite(value))throw new IllegalArgumentException("3D overlap coordinate overflow");
        // Java saturates double-to-long casts. Extreme coordinates collapse
        // into end buckets, producing extra candidates rather than lost pairs.
        return (long)Math.floor(value/side);
    }
    private static boolean overflow(long value,int delta) { return (value==Long.MAX_VALUE&&delta==1)||(value==Long.MIN_VALUE&&delta==-1); }
    private static final class Cell {
        final long x,y,z;
        Cell(long x,long y,long z){this.x=x;this.y=y;this.z=z;}
        @Override public boolean equals(Object other){if(!(other instanceof Cell))return false;Cell c=(Cell)other;return x==c.x&&y==c.y&&z==c.z;}
        @Override public int hashCode(){int h=Long.hashCode(x);h=31*h+Long.hashCode(y);return 31*h+Long.hashCode(z);}
    }
}
