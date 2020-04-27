/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.knn.similarities.ir;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.data.TerrierStructure;

import java.util.Optional;

/**
 *
 * @author Javier
 */
public class PL2Similarity extends TerrierIRSimilarity
{

    private final double c;
    
    public PL2Similarity(FastGraph<?> graph, EdgeOrientation uSel, EdgeOrientation vSel, double c)
    {
        super(graph, uSel, vSel);
        this.c = c;
    }
    
    /**
     * Constructor.
     * @param graph the training graph.
     * @param uSel orientation selection for the target user.
     * @param vSel orientation selection for the candidate user.
     * @param structure Terrier basic structures for the algorithm.
     */
    public PL2Similarity(FastGraph<?> graph, EdgeOrientation uSel, EdgeOrientation vSel, TerrierStructure structure, double c)
    {
        super(graph,uSel,vSel,structure);
        this.c = c;
    }

    @Override
    protected String getModel() {
        return "PL2";
    }

    @Override
    protected Optional<Double> getCValue() {
        return Optional.of(c);
    }
    
}
