/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.internal.AbstractStatic3DLayout;
import java.util.*;
/**
 * Restores supplied x/y/z coordinates; requires complete coverage.
 * @param <U> node type
 */
public final class Preset3DLayout<U> extends AbstractStatic3DLayout<U> {
    @Override public LayoutDescriptor getDescriptor() { return new LayoutDescriptor("preset-3d","Preset 3D",3); }
    @Override protected Map<U,Point3D> place(List<U> nodes,LayoutRequest<U> request) {
        if(!request.getInitialPositions3D().keySet().equals(new HashSet<>(nodes)))
            throw new IllegalArgumentException("Preset 3D requires coordinates for every node");
        Map<U,Point3D> result=new LinkedHashMap<>();for(U node:nodes) result.put(node,request.getInitialPositions3D().get(node));return result;
    }
}
