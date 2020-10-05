/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.community.detection.modularity;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.socialranksys.graph.Graph;

/**
 * Class for computing the community partition using the Leading Vector algorithm.
 * Problem: the JMod library does not actually work on Maven, so translating this into
 * our project is actually stopped.
 *
 * <p>
 * <b>Reference:</b> M.E.J. Newman.  Finding community structure in networks using the eigenvectors of matrices. Physical Review E 74 (2006)
 * </p>
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class LeadingVector<U> implements CommunityDetectionAlgorithm<U>
{
    @Override
    public Communities<U> detectCommunities(Graph<U> graph)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
