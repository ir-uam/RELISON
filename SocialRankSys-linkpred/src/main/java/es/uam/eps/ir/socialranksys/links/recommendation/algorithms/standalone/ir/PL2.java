/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.ir;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.data.TerrierStructure;

import java.util.Optional;

/**
 * Class that applies the PL2 Divergence from Randomness model as a contact
 * recommendation algorithm.
 * <p>
 * <b>Reference:</b> G. Amati, C.J. Van Rijsbergen. Probabilistic Models of Information Retrieval Based on Measuring the Divergence from Randomness.
 * ACM Transactions on Information Systems 20(4), 357–389 (2002)</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see org.terrier.matching.models.PL2
 */
public class PL2<U> extends TerrierRecommender<U>
{
    /**
     * Parameter for tuning the importance of the candidate user length.
     */
    private final double c;

    /**
     * Constructor.
     *
     * @param graph the training graph.
     * @param uSel  orientation selection for the target user.
     * @param vSel  orientation selection for the candidate user.
     * @param c     parameter for tuning the importance of the candidate user length.
     */
    public PL2(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel, double c)
    {
        super(graph, uSel, vSel);
        this.c = c;
    }

    /**
     * Constructor.
     *
     * @param graph     the training graph.
     * @param uSel      orientation selection for the target user.
     * @param vSel      orientation selection for the candidate user.
     * @param structure Terrier basic structures for the algorithm.
     * @param c         parameter for tuning the importance of the candidate user length.
     */
    public PL2(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel, TerrierStructure structure, double c)
    {
        super(graph, uSel, vSel, structure);
        this.c = c;
    }

    @Override
    protected String getModel()
    {
        return "PL2";
    }

    @Override
    protected Optional<Double> getCValue()
    {
        return Optional.of(c);
    }
}
