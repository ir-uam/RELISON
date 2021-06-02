/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.community.modularity;

import es.uam.eps.ir.sonalire.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.sonalire.community.detection.modularity.Infomap;
import es.uam.eps.ir.sonalire.grid.Grid;
import es.uam.eps.ir.sonalire.grid.community.CommunityDetectionConfigurator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.sonalire.grid.community.CommunityDetectionIdentifiers.INFOMAP;

/**
 * Configurator for the Infomap community detection algorithm.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 *
 * @see Infomap
 */
public class InfomapConfigurator<U extends Serializable> implements CommunityDetectionConfigurator<U>
{
    /**
     * A temporary folder for intermediate files.
     */
    private final String tempFolder;

    /**
     * Constructor
     */
    public InfomapConfigurator(String tempFolder)
    {
        this.tempFolder = tempFolder;
    }

    /**
     * Identifier for the maximum number of iterations of the most external loop.
     */
    private final static String NUMTRIALS = "trials";
    @Override
    public Map<String, Supplier<CommunityDetectionAlgorithm<U>>> configure(Grid grid)
    {
        Map<String, Supplier<CommunityDetectionAlgorithm<U>>> map = new HashMap<>();
        if(grid.getIntegerValues().containsKey(NUMTRIALS))
        {
            List<Integer> trials = grid.getIntegerValues(NUMTRIALS);
            trials.forEach(trial -> map.put(INFOMAP+"_" + trial, () -> new Infomap<>(tempFolder, trial)));
        }
        else
        {
            map.put(INFOMAP, () -> new Infomap<>(tempFolder));
        }
        return map;
    }
}
