/*
 *  Copyright (C) 2022-2022 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es / Terrier Team at University of Glasgow,
 *  http://http://terrierteam.dcs.gla.ac.uk
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.relison.graph.multigraph.edges.fast;

import es.uam.eps.ir.relison.graph.edges.fast.FastEdge;

public class FastMultiEdge extends FastEdge
{
    private final int edgeNum;

    public FastMultiEdge(int origin, int dest, double weight, int type, int edgeNum)
    {
        super(origin, dest, weight, type);
        this.edgeNum = edgeNum;
    }

    public int getEdgeNum()
    {
        return edgeNum;
    }
}
