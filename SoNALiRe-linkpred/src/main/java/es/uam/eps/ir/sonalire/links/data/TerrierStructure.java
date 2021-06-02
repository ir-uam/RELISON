/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.data;

import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.utils.datatypes.Tuple2oo;
import org.terrier.realtime.memory.MemoryIndex;

import java.util.HashMap;
import java.util.Map;

/**
 * Structure for storing the different possible indexes and queries
 * for a Terrier index.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class TerrierStructure
{
    /**
     * The possible set of indexes
     */
    private final Map<EdgeOrientation, MemoryIndex> indexes;
    /**
     * The possible sets of queries
     */
    private final Map<EdgeOrientation, Map<Integer, String>> queries;

    /**
     * Constructor.
     *
     * @param graph the graph.
     */
    public TerrierStructure(FastGraph<?> graph)
    {
        this.indexes = new HashMap<>();
        this.queries = new HashMap<>();

        for (EdgeOrientation eo : EdgeOrientation.values())
        {
            TerrierIndex index = new TerrierIndex(graph, eo, eo);
            this.indexes.put(eo, index.getMemoryIndex());
            this.queries.put(eo, index.getQueries());
        }
    }

    /**
     * Obtains a pair index/queries given some orientations.
     *
     * @param uSel the orientation for the queries.
     * @param vSel the orientation for the index.
     *
     * @return a pair containing both values.
     */
    public Tuple2oo<MemoryIndex, Map<Integer, String>> get(EdgeOrientation uSel, EdgeOrientation vSel)
    {
        return new Tuple2oo<>(indexes.get(vSel), queries.get(uSel));
    }


}
