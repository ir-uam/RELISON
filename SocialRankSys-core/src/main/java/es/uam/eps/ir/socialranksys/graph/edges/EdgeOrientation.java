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
    OUT, IN, UND, MUTUAL;

    /**
     * Given an edge orientation, returns the opposite orientation.
     *
     * @return the opposite orientation.
     */
    public EdgeOrientation invertSelection()
    {
        switch (this)
        {
            case OUT:
                return IN;
            case IN:
                return OUT;
            default:
                return this;
        }
    }

    /**
     * Selection for the complementary graph.
     *
     * @return the selection for the complementary graph.
     */
    public EdgeOrientation complementarySelection()
    {
        switch (this)
        {
            case UND:
                return MUTUAL;
            case MUTUAL:
                return UND;
            default:
                return this;
        }
    }
}
