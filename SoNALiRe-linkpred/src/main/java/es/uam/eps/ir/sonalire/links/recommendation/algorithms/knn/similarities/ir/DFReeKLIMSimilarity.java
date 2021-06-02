/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.algorithms.knn.similarities.ir;

import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.links.data.TerrierStructure;
import es.uam.eps.ir.sonalire.links.recommendation.algorithms.standalone.ir.DFReeKLIM;

import java.util.Optional;

/**
 * Similarity based on the DFReeKLIM method on Information Retrieval.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see DFReeKLIM
 */
public class DFReeKLIMSimilarity extends TerrierIRSimilarity
{
    /**
     * Constructor.
     * @param graph     the training graph.
     * @param uSel      orientation selection for the target user.
     * @param vSel      orientation selection for the candidate user.
     */
    public DFReeKLIMSimilarity(FastGraph<?> graph, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        super(graph, uSel, vSel);
    }
    
    /**
     * Constructor.
     * @param graph     the training graph.
     * @param uSel      orientation selection for the target user.
     * @param vSel      orientation selection for the candidate user.
     * @param structure Terrier basic structures for the algorithm.
     */
    public DFReeKLIMSimilarity(FastGraph<?> graph, EdgeOrientation uSel, EdgeOrientation vSel, TerrierStructure structure)
    {
        super(graph,uSel,vSel,structure);
    }

    @Override
    protected String getModel() {
        return "DFReeKLIM";
    }

    @Override
    protected Optional<Double> getCValue()
    {
        return Optional.empty();
    }
    
}
