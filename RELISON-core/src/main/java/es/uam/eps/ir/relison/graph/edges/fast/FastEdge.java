/*
 *  Copyright (C) 2022-2022 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es / Terrier Team at University of Glasgow,
 *  http://http://terrierteam.dcs.gla.ac.uk
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.relison.graph.edges.fast;

public class FastEdge
{
    private final int origin;
    private final int dest;
    private final double weight;
    private final int type;

    public FastEdge(int origin, int dest, double weight, int type)
    {
        this.origin = origin;
        this.dest = dest;
        this.weight = weight;
        this.type = type;
    }

    public int getOrigin()
    {
        return origin;
    }

    public int getDest()
    {
        return dest;
    }

    public double getWeight()
    {
        return weight;
    }

    public int getType()
    {
        return type;
    }
}
