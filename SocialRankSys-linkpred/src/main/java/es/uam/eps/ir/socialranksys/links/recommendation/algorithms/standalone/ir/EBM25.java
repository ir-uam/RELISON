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
 * Adaptation of an extreme version of the BM25 algorithm, where the k parameter tends to infinity, without term discrimination.
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see BM25
 */
public class EBM25<U> extends BM25<U>
{
    /**
     * Constructor
     *
     * @param graph the training graph
     * @param uSel  selection of the neighbours of the target user
     * @param vSel  selection of the neighbours of the candidate user
     * @param dlSel selection of the neighbours for the document length
     * @param b     tunes the effect of the neighborhood size. Between 0 and 1
     */
    public EBM25(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel, EdgeOrientation dlSel, double b)
    {
        super(graph, uSel, vSel, dlSel, b, Double.POSITIVE_INFINITY);
    }
}
