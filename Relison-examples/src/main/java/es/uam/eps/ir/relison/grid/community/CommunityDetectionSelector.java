/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.grid.community;

import es.uam.eps.ir.relison.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.relison.grid.Configurations;
import es.uam.eps.ir.relison.grid.Grid;
import es.uam.eps.ir.relison.grid.Parameters;
import es.uam.eps.ir.relison.grid.community.connectedness.StronglyConnectedComponentsConfigurator;
import es.uam.eps.ir.relison.grid.community.connectedness.WeaklyConnectedComponentsConfigurator;
import es.uam.eps.ir.relison.grid.community.modularity.*;
import es.uam.eps.ir.relison.grid.community.modularity.balanced.*;
import org.jooq.lambda.tuple.Tuple2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static es.uam.eps.ir.relison.grid.community.CommunityDetectionIdentifiers.*;

/**
 * Given a parameter configuration, this class obtains the corresponding community detection
 * algorithms, so they can be applied over networks.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 */
public class CommunityDetectionSelector<U extends Serializable>
{
    /**
     * A temporary directory for storing intermediate files.
     */
    private final String tempFolder;

    /**
     * Constructor.
     * @param tempFolder a temporary directory for storing intermediate files.
     */
    public CommunityDetectionSelector(String tempFolder)
    {
        this.tempFolder = tempFolder;
    }

    /**
     * Obtains a community configurator.
     * @param algorithm the name of the algorithm.
     * @return the community detection algorithm configurator if the algorithm exists, null otherwise
     */
    public CommunityDetectionConfigurator<U> getConfigurator(String algorithm)
    {
        return switch (algorithm)
        {
            // Connectedness
            case SCC -> new StronglyConnectedComponentsConfigurator<>();
            case WCC -> new WeaklyConnectedComponentsConfigurator<>();
            // Modularity
            case FASTGREEDY -> new FastGreedyConfigurator<>();
            case INFOMAP -> new InfomapConfigurator<>(tempFolder);
            case LOUVAIN -> new LouvainConfigurator<>();
            case LABELPROP -> new LabelPropagationConfigurator<>();
            // Edge metrics:
            case GIRVANNEWMAN, EDGEBETWENNESS -> new GirvanNewmanConfigurator<>();
            case BALANCEDFASTGREEDY -> new BalancedFastGreedyConfigurator<>();
            case SIZEWEIGHTEDFASTGREEDY -> new SizeWeightedFastGreedyConfigurator<>();
            case GINIWEIGHTEDFASTGREEDY -> new GiniWeightedFastGreedyConfigurator<>();
            case RATIOCUTSPECTRAL -> new RatioCutSpectralClusteringConfigurator<>();
            case NORMALIZEDCUTSPECTRAL -> new NormalizedCutSpectralClusteringConfigurator<>();
            default -> null;
        };
    }

    /**
     * Obtains the different community detection algorithms to apply.
     * @param algorithm the name of the algorithm.
     * @param grid      the parameter grid for the community detection approach.
     * @return the suppliers for the community detection algorithms, indexed by name. If the algorithm does not exist,
     * it returns null.
     */
    public Map<String, Supplier<CommunityDetectionAlgorithm<U>>> getCommunityDetectionAlgorithms(String algorithm, Grid grid)
    {
        CommunityDetectionConfigurator<U> config = this.getConfigurator(algorithm);
        if(config != null)
        {
            return config.configure(grid);
        }
        return null;
    }

    /**
     * Obtains the different community detection algorithms to apply.
     * @param algorithm the name of the algorithm.
     * @param parameters      the parameter configuration for a single community detection approach
     * @return a pair containing the name and the supplier for the file if everything is ok, null otherwise.
     */
    public Tuple2<String, Supplier<CommunityDetectionAlgorithm<U>>> getCommunityDetectionAlgorithm(String algorithm, Parameters parameters)
    {
        CommunityDetectionConfigurator<U> config = this.getConfigurator(algorithm);
        if(config != null)
        {
            Grid grid = parameters.toGrid();
            Map<String, Supplier<CommunityDetectionAlgorithm<U>>> comms = config.configure(grid);
            if(!comms.isEmpty())
            {
                List<String> algs = new ArrayList<>(comms.keySet());
                String name = algs.get(0);
                Supplier<CommunityDetectionAlgorithm<U>> supp = comms.get(name);
                return new Tuple2<>(name, supp);
            }
        }
        return null;
    }

    /**
     * Obtains the different community detection algorithms to apply. If a configuration does not result in an algorithm,
     * it is ignored.
     * @param algorithm the name of the algorithm.
     * @param configs   the different configurations for the algorithm.
     * @return the suppliers for the community detection algorithms, indexed by name. If the algorithm does not exist,
     * it returns null.
     */
    public Map<String, Supplier<CommunityDetectionAlgorithm<U>>> getCommunityDetectionAlgorithms(String algorithm, Configurations configs)
    {
        Map<String, Supplier<CommunityDetectionAlgorithm<U>>> detectors = new HashMap<>();
        CommunityDetectionConfigurator<U> config = this.getConfigurator(algorithm);
        if(config != null)
        {
            for(Parameters params : configs.getConfigurations())
            {
                Grid grid = params.toGrid();
                Map<String, Supplier<CommunityDetectionAlgorithm<U>>> comms = config.configure(grid);
                if(comms != null && !comms.isEmpty())
                {
                    List<String> algs = new ArrayList<>(comms.keySet());
                    String name = algs.get(0);
                    Supplier<CommunityDetectionAlgorithm<U>> supp = comms.get(name);
                    detectors.put(name, supp);
                }
            }

            return detectors;
        }
        return null;
    }




}
