/*
 *  Copyright (C) 2022-2022 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es / Terrier Team at University of Glasgow,
 *  http://http://terrierteam.dcs.gla.ac.uk
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.relison.sna.edgegroups.simple.detection.community;

import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.edgegroups.simple.detection.SimpleEdgePartitionDetectionAlgorithm;


/**
 * Implements an edge partition detection method based on communities. Between 0 and number of communities-1, we
 * retrieve links inside each community. An special category, with index INTRACOMMLINKS, represents the edges
 * travelling between communities.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzadopuig@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class CommunityBased<U> implements SimpleEdgePartitionDetectionAlgorithm<U>
{

    /**
     * Community partition
     */
    protected final Communities<U> comms;

    /**
     * Constructor.
     * @param comms the set of communities.
     */
    public CommunityBased(Communities<U> comms)
    {
        this.comms = comms;
    }


}