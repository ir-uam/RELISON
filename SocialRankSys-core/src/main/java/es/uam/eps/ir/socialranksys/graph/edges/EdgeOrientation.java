/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.edges;

/**
 * Indicates the orientation of the edges to take.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Sofia Marina Pepa
 */
public enum EdgeOrientation
{
    /**
     * Orientation for selecting the outgoing neighborhood (adjacent nodes).
     */
    OUT,
    /**
     * Orientation for selecting the incoming neighborhood (incident nodes).
     */
    IN,
    /**
     * Orientation for selecting the undirected neighbor (both adjacent and incident nodes).
     */
    UND,
    /**
     * Orientation for selecting neighbors which are both adjacent and incident.
     */
    MUTUAL;

    /**
     * Given an edge orientation, returns the opposite orientation.
     *
     * @return the opposite orientation.
     */
    public EdgeOrientation invertSelection()
    {
        return switch (this)
        {
            case OUT -> IN;
            case IN -> OUT;
            default -> this;
        };
    }

    /**
     * Selection for the complementary graph.
     *
     * @return the selection for the complementary graph.
     */
    public EdgeOrientation complementarySelection()
    {
        return switch (this)
        {
            case UND -> MUTUAL;
            case MUTUAL -> UND;
            default -> this;
        };
    }
}
