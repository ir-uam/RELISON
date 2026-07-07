/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.diffusion.data.Data;
import es.uam.eps.ir.relison.diffusion.data.Information;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.index.Index;
import es.uam.eps.ir.relison.index.Relation;
import es.uam.eps.ir.relison.index.fast.FastIndex;
import es.uam.eps.ir.relison.index.fast.FastWeightedPairwiseRelation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Builds the {@link Data} object a diffusion simulation needs. Currently provides a <em>synthetic</em> dataset: each
 * node of the loaded network is seeded with a configurable number of information pieces it "owns" (i.e. created),
 * each with a distinct timestamp so the simulator's timestamp cursor keeps advancing. This lets a simulation run on
 * any loaded graph without extra files.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public final class DiffusionData
{
    private DiffusionData()
    {
    }

    /**
     * Builds a synthetic diffusion dataset where every node owns {@code seedCount} information pieces.
     * @param graph     the loaded network (users are its nodes).
     * @param seedCount the number of information pieces created by each user (at least 1).
     * @return the diffusion data (no features).
     */
    public static Data<String, String, String> synthetic(Graph<String> graph, int seedCount)
    {
        int k = Math.max(1, seedCount);

        Index<String> users = new FastIndex<>();
        graph.getAllNodes().forEach(users::addObject);

        Index<String> pieces = new FastIndex<>();
        Map<Integer, Information<String>> infoMap = new HashMap<>();
        List<int[]> ownership = new ArrayList<>();   // (userIdx, pieceIdx)

        // Every piece is published at the same timestamp: all pieces are available from the first iteration (a
        // realistic "everyone starts with their own information" seeding) and, crucially, the timestamp set stays
        // tiny (a single value plus the Long.MAX_VALUE sentinel Data adds) instead of growing to numUsers*k. The
        // number of iterations is governed by the stop condition, not by the number of distinct timestamps.
        final long timestamp = 0L;
        for (int uidx = 0; uidx < users.numObjects(); ++uidx)
        {
            String user = users.idx2object(uidx);
            for (int j = 0; j < k; ++j)
            {
                String pieceId = user + "#" + j;
                int iidx = pieces.addObject(pieceId);
                infoMap.put(iidx, new Information<>(pieceId, timestamp));
                ownership.add(new int[]{uidx, iidx});
            }
        }

        Relation<Integer> userInfo = new FastWeightedPairwiseRelation<>();
        IntStream.range(0, users.numObjects()).forEach(userInfo::addFirstItem);
        IntStream.range(0, pieces.numObjects()).forEach(userInfo::addSecondItem);
        for (int[] o : ownership) userInfo.addRelation(o[0], o[1], 1);

        return new Data<>(graph, users, pieces, infoMap, userInfo);
    }

    /**
     * Builds a diffusion dataset from an explicit, user-provided list of information pieces. Each piece is a map with
     * {@code id}, {@code creator} (a node of the graph) and {@code timestamp}. Pieces with a blank id, a duplicate id
     * or a creator that is not a node of the graph are skipped.
     * @param graph  the loaded network (users are its nodes).
     * @param pieces the list of pieces (each a {@code Map} with {@code id}, {@code creator}, {@code timestamp}).
     * @return the diffusion data (no features).
     */
    public static Data<String, String, String> fromPieces(Graph<String> graph, List<?> pieces)
    {
        Index<String> users = new FastIndex<>();
        graph.getAllNodes().forEach(users::addObject);

        Index<String> pieceIndex = new FastIndex<>();
        Map<Integer, Information<String>> infoMap = new HashMap<>();
        List<int[]> ownership = new ArrayList<>();   // (userIdx, pieceIdx)
        Set<String> seenIds = new HashSet<>();

        for (Object o : pieces)
        {
            if (!(o instanceof Map)) continue;
            Map<?, ?> p = (Map<?, ?>) o;
            String id = str(p.get("id"));
            String creator = str(p.get("creator"));
            if (id.isEmpty()) continue;
            int uidx = users.object2idx(creator);
            if (uidx == -1) continue;            // creator is not a node of the graph
            if (!seenIds.add(id)) continue;      // duplicate piece id

            long timestamp = longValue(p.get("timestamp"), 0L);
            int iidx = pieceIndex.addObject(id);
            infoMap.put(iidx, new Information<>(id, timestamp));
            ownership.add(new int[]{uidx, iidx});
        }

        Relation<Integer> userInfo = new FastWeightedPairwiseRelation<>();
        IntStream.range(0, users.numObjects()).forEach(userInfo::addFirstItem);
        IntStream.range(0, pieceIndex.numObjects()).forEach(userInfo::addSecondItem);
        for (int[] o : ownership) userInfo.addRelation(o[0], o[1], 1);

        return new Data<>(graph, users, pieceIndex, infoMap, userInfo);
    }

    private static String str(Object o)
    {
        return o == null ? "" : o.toString().trim();
    }

    private static long longValue(Object o, long def)
    {
        if (o instanceof Number) return ((Number) o).longValue();
        if (o == null) return def;
        try { return (long) Double.parseDouble(o.toString().trim()); }
        catch (NumberFormatException e) { return def; }
    }
}
