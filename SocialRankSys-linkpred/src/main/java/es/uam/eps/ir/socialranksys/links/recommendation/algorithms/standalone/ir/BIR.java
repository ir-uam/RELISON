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

/**
 * Binary independent retrieval algorithm, using a term-based query processing mechanism.
 * <p>
 * Sparck Jones, K., Walker, S., Roberton S.E. A Probabilistic Model of Information Retrieval: Development and Comparative Experiments.
 * Information Processing and Management 36. February 2000, pp. 779-808 (part 1), pp. 809-840 (part 2).
 * </p>
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class BIR<U> extends BM25<U>
{
    /**
     * Constructor
     *
     * @param graph graph
     * @param uSel  selection of the neighbours of the target user
     * @param vSel  selection of the neighbours of the candidate user
     */
    public BIR(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        super(graph, uSel, vSel, EdgeOrientation.UND, 0.0, Double.POSITIVE_INFINITY);
    }
}
