/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.community;

import es.uam.eps.ir.socialranksys.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.socialranksys.grid.Parameters;
import es.uam.eps.ir.socialranksys.grid.community.connectedness.StronglyConnectedComponentsConfigurator;
import es.uam.eps.ir.socialranksys.grid.community.connectedness.WeaklyConnectedComponentsConfigurator;
import es.uam.eps.ir.socialranksys.grid.community.modularity.FastGreedyConfigurator;
import es.uam.eps.ir.socialranksys.grid.community.modularity.InfomapConfigurator;
import es.uam.eps.ir.socialranksys.grid.community.modularity.LouvainConfigurator;
import es.uam.eps.ir.socialranksys.grid.community.modularity.balanced.*;
import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;

import java.io.Serializable;

import static es.uam.eps.ir.socialranksys.grid.community.CommunityDetectionIdentifiers.*;

/**
 * Class that translates from a grid to the different community detection algorithns.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public class CommunityDetectionSelector<U extends Serializable>
{
    /**
     * Obtains a community configurator.
     * @param algorithm the name of the algorithm.
     * @return the community detection algorithm configurator if the algorithm exists, null otherwise
     */
    public CommunityDetectionConfigurator<U> getConfigurator(String algorithm)
    {
        CommunityDetectionConfigurator<U> config;
        switch(algorithm)
        {
            // Connectedness
            case SCC:
                config = new StronglyConnectedComponentsConfigurator<>();
                break;
            case WCC:
                config = new WeaklyConnectedComponentsConfigurator<>();
                break;
            case FASTGREEDY:
                config = new FastGreedyConfigurator<>();
                break;
            case INFOMAP:
                config = new InfomapConfigurator<>();
                break;
            case LOUVAIN:
                config = new LouvainConfigurator<>();
                break;
            // Balanced comm. size
            case BALANCEDFASTGREEDY:
                config = new BalancedFastGreedyConfigurator<>();
                break;
            case SIZEWEIGHTEDFASTGREEDY:
                config = new SizeWeightedFastGreedyConfigurator<>();
                break;
            case GINIWEIGHTEDFASTGREEDY:
                config = new GiniWeightedFastGreedyConfigurator<>();
                break;
            case RATIOCUTSPECTRAL:
                config = new RatioCutSpectralClusteringConfigurator<>();
                break;
            case NORMALIZEDCUTSPECTRAL:
                config = new NormalizedCutSpectralClusteringConfigurator<>();
                break;
            default:
                config = null;
        }
        return config;
    }
    /**
     * Obtains the corresponding community detection algorithm
     * @param algorithm the name of the algorithm.
     * @param params the parameters for the algorithm.
     * @return a pair containing the name and the algorithm if it exists, null if not.
     */
    public Tuple2oo<String, CommunityDetectionAlgorithm<U>> getCommunityDetectionAlgorithm(String algorithm, Parameters params)
    {
        CommunityDetectionConfigurator<U> config = this.getConfigurator(algorithm);
        
        if(config != null)
            return new Tuple2oo<>(algorithm, config.configure(params));
        return null;
    }
}
