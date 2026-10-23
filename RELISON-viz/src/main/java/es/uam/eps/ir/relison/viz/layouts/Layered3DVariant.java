/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.layouts;

/** Geometry for the spatial shell, score and ego layouts. */
public enum Layered3DVariant {
    /** Replace each 2D ring with a sphere of the same radius. */
    SPHERE,
    /** Preserve the 2D rings and use scaled group index, raw score or hops as z. */
    CONE
}
