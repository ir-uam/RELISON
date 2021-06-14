/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.algorithms.standalone.ir;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.links.data.TerrierStructure;

import java.util.Optional;

/**
 * Class that applies the DPH Divergence from Randomness model as a contact
 * recommendation algorithm.
 * <p>
 * <b>Reference:</b> G. Amati. Frequentist and Bayesian Approach to Information Retrieval. In: Proceedings of the 28th European Conference on Information Retrieval (ECIR 2006).pp. 13–24. No. 3936 in LNCS, Springer (2006)
 * </p>
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see org.terrier.matching.models.DPH
 */
public class DPH<U> extends TerrierRecommender<U>
{
    /**
     * Constructor.
     *
     * @param graph the training graph.
     * @param uSel  orientation selection for the target user.
     * @param vSel  orientation selection for the candidate user.
     */
    public DPH(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        super(graph, uSel, vSel);
    }

    /**
     * Constructor.
     *
     * @param graph     the training graph.
     * @param uSel      orientation selection for the target user.
     * @param vSel      orientation selection for the candidate user.
     * @param structure Terrier basic structures for the algorithm.
     */
    public DPH(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel, TerrierStructure structure)
    {
        super(graph, uSel, vSel, structure);
    }

    @Override
    protected String getModel()
    {
        return new org.terrier.matching.models.DPH().getInfo();
    }

    @Override
    protected Optional<Double> getCValue()
    {
        return Optional.empty();
    }
}
