/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;

import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.*;
import java.util.*;

/** Feature groups occupy z levels; nodes within each group occupy a centred x/y grid. */
public final class FeatureGrid3DLayout<U> extends AbstractStatic3DLayout<U> {
    private final List<List<U>> levels;
    private final double spacing, levelSpacing;

    public FeatureGrid3DLayout(List<? extends Collection<U>> groups,double spacing,double levelSpacing) {
        this.spacing=AbstractStaticLayout.positive(spacing,"spacing");
        this.levelSpacing=AbstractStaticLayout.positive(levelSpacing,"levelSpacing");
        Set<U> seen=new HashSet<>();List<List<U>> copy=new ArrayList<>();
        for(Collection<U> group:Objects.requireNonNull(groups)) {
            List<U> level=new ArrayList<>();for(U node:group)
                if(node==null||!seen.add(node))throw new IllegalArgumentException("Feature groups must contain unique, non-null nodes");else level.add(node);
            copy.add(level);
        }
        levels=Collections.unmodifiableList(copy);
    }
    @Override public LayoutDescriptor getDescriptor() { return new LayoutDescriptor("feature-grid-3d","Grid by feature 3D (levels)",3); }
    @Override protected Map<U,Point3D> place(List<U> nodes,LayoutRequest<U> request) {
        Set<U> expected=new HashSet<>(nodes);
        Set<U> grouped=new HashSet<>();levels.forEach(grouped::addAll);
        if(grouped.size()!=expected.size()||!grouped.equals(expected))
            throw new IllegalArgumentException("Feature groups must contain exactly the graph nodes");
        Map<U,Point3D> points=new LinkedHashMap<>();int count=levels.size();
        for(int z=0;z<count;z++) {
            List<U> group=levels.get(z);int columns=(int)Math.ceil(Math.sqrt(group.size()));
            int rows=columns==0?0:(int)Math.ceil((double)group.size()/columns);
            for(int i=0;i<group.size();i++) {
                int row=i/columns,column=i%columns;
                int rowCount=Math.min(columns,group.size()-row*columns);
                points.put(group.get(i),new Point3D((column-(rowCount-1)/2.0)*spacing,
                    ((rows-1)/2.0-row)*spacing,(z-(count-1)/2.0)*levelSpacing));
            }
        }
        return points;
    }
}
