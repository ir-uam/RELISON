/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.standalone.ir;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.data.TerrierStructure;

import java.util.Optional;

/**
 * Class that applies the DLH Divergence from Randomness model as a contact
 * recommendation algorithm.
 * <p>
 * Amati, G., Ambrosi, E., Bianchi, M., Gaibisso, C., Gambosi, G.: FUB, IASI-CNRand University of Tor Vergata at TREC 2007 Blog Track. In: Proceedings of the16th Text REtrieval Conference (TREC 2007). NIST (2007
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @see org.terrier.matching.models.DLH
 */
public class DLH<U> extends TerrierRecommender<U>
{
    /**
     * Constructor.
     *
     * @param graph the training graph.
     * @param uSel  orientation selection for the target user.
     * @param vSel  orientation selection for the candidate user.
     */
    public DLH(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel)
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
    public DLH(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel, TerrierStructure structure)
    {
        super(graph, uSel, vSel, structure);
    }

    @Override
    protected String getModel()
    {
        return new org.terrier.matching.models.DLH().getInfo();
    }

    @Override
    protected Optional<Double> getCValue()
    {
        return Optional.empty();
    }
}
