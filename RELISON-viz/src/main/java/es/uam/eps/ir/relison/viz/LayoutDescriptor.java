/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz;


import java.util.Objects;

/** Stable algorithm identity for consumers such as layout menus. */
public final class LayoutDescriptor
{
    private final String id;
    private final String name;
    private final int spaceDimensions;

    /**
     * @param id stable identifier
     * @param name display name
     */
    public LayoutDescriptor(String id, String name)
    { this(id, name, 2); }
    /**
     * @param id stable identifier
     * @param name display name
     * @param spaceDimensions 2 or 3
     */
    public LayoutDescriptor(String id, String name, int spaceDimensions)
    {
        if (spaceDimensions != 2 && spaceDimensions != 3) throw new IllegalArgumentException("Dimensions must be 2 or 3");
        this.spaceDimensions = spaceDimensions;
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        if (id.isBlank() || name.isBlank()) throw new IllegalArgumentException("Identity must not be blank");
    }
    /** @return algorithm identifier */
    public String getId() { return id; }
    /** @return display name */
    public String getName() { return name; }
    /** @return number of layout dimensions */
    public int getSpaceDimensions() { return spaceDimensions; }
}
