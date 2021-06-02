/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.community.modularity.balanced;

import es.uam.eps.ir.sonalire.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.sonalire.community.detection.modularity.balanced.NormalizedCutSpectralClustering;
import es.uam.eps.ir.sonalire.grid.Grid;
import es.uam.eps.ir.sonalire.grid.community.CommunityDetectionConfigurator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.sonalire.grid.community.CommunityDetectionIdentifiers.NORMALIZEDCUTSPECTRAL;

/**
 * Configurator for the Spectral Clustering community detection based on minimizing
 * the normalized cut.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 *
 * @see NormalizedCutSpectralClustering
 */
public class NormalizedCutSpectralClusteringConfigurator<U extends Serializable> implements CommunityDetectionConfigurator<U>
{
    /**
     * Identifier for the desired number of communities
     */
    private final static String K = "k";

    @Override
    public Map<String, Supplier<CommunityDetectionAlgorithm<U>>> configure(Grid grid)
    {
        Map<String, Supplier<CommunityDetectionAlgorithm<U>>> conf = new HashMap<>();
        List<Integer> ks = grid.getIntegerValues(K);

        ks.forEach(k -> conf.put(NORMALIZEDCUTSPECTRAL + "_" + k, () -> new NormalizedCutSpectralClustering<>(k)));
        return conf;
    }
    
}
