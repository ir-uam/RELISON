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
import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.index.Index;
import es.uam.eps.ir.relison.index.Relation;
import es.uam.eps.ir.relison.index.fast.FastIndex;
import es.uam.eps.ir.relison.index.fast.FastWeightedPairwiseRelation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
     * {@code id}, {@code creator} (a node of the graph), {@code timestamp} and an optional {@code features} list (each
     * feature a map {@code {param, value, weight}}). Pieces with a blank id, a duplicate id or a creator that is not a
     * node of the graph are skipped. User features are derived from the graph's node attributes (one feature parameter
     * per node attribute), so the user-feature variants of the feature metrics work without extra uploads.
     * @param graph       the loaded network (users are its nodes).
     * @param pieces      the list of pieces (each a {@code Map} with {@code id}, {@code creator}, {@code timestamp}, {@code features}).
     * @param communities the detected community partitions (each becomes a user feature: the user's community id).
     * @return the diffusion data, including info-piece features and node/community-derived user features.
     */
    public static Data<String, String, String> fromPieces(Graph<String> graph, List<?> pieces,
                                                          Map<String, Communities<String>> communities)
    {
        Index<String> users = new FastIndex<>();
        graph.getAllNodes().forEach(users::addObject);

        Index<String> pieceIndex = new FastIndex<>();
        Map<Integer, Information<String>> infoMap = new HashMap<>();
        List<int[]> ownership = new ArrayList<>();   // (userIdx, pieceIdx)
        Set<String> seenIds = new HashSet<>();
        // Collected info-piece feature entries: pieceIdx -> param -> list of (value, weight). Filled while iterating
        // the pieces (once the piece index is complete we turn these into per-parameter relations below).
        List<Object[]> infoFeatureEntries = new ArrayList<>();   // {int pieceIdx, String param, String value, double weight}

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
            collectFeatures(p.get("features"), iidx, infoFeatureEntries);
        }

        Relation<Integer> userInfo = new FastWeightedPairwiseRelation<>();
        IntStream.range(0, users.numObjects()).forEach(userInfo::addFirstItem);
        IntStream.range(0, pieceIndex.numObjects()).forEach(userInfo::addSecondItem);
        for (int[] o : ownership) userInfo.addRelation(o[0], o[1], 1);

        // Shared feature-value indexes (one per parameter name), plus the user and info feature relations.
        Map<String, Index<String>> featureIndexes = new LinkedHashMap<>();
        List<String> infoFeatureNames = new ArrayList<>();
        Map<String, Relation<Double>> infoFeatures = new LinkedHashMap<>();
        List<String> userFeatureNames = new ArrayList<>();
        Map<String, Relation<Double>> userFeatures = new LinkedHashMap<>();

        buildInfoFeatures(pieceIndex.numObjects(), infoFeatureEntries, featureIndexes, infoFeatureNames, infoFeatures);
        buildUserFeaturesFromNodeAttributes(graph, users, featureIndexes, userFeatureNames, userFeatures);
        buildUserFeaturesFromCommunities(communities, users, featureIndexes, userFeatureNames, userFeatures);

        return new Data<>(graph, users, pieceIndex, infoMap, userInfo,
                featureIndexes, userFeatureNames, userFeatures, infoFeatureNames, infoFeatures);
    }

    /** Collects a piece's declared features (a list of {@code {param, value, weight}} maps) into the entry buffer. */
    private static void collectFeatures(Object featuresObj, int pieceIdx, List<Object[]> entries)
    {
        if (!(featuresObj instanceof List)) return;
        for (Object fo : (List<?>) featuresObj)
        {
            if (!(fo instanceof Map)) continue;
            Map<?, ?> f = (Map<?, ?>) fo;
            String param = str(f.get("param"));
            String value = str(f.get("value"));
            if (param.isEmpty() || value.isEmpty()) continue;
            entries.add(new Object[]{pieceIdx, param, value, doubleValue(f.get("weight"), 1.0)});
        }
    }

    /** Turns the collected info-feature entries into one {@code (value index, relation)} per parameter name. */
    private static void buildInfoFeatures(int numPieces, List<Object[]> entries, Map<String, Index<String>> featureIndexes,
                                          List<String> names, Map<String, Relation<Double>> relations)
    {
        for (Object[] e : entries)
        {
            int pieceIdx = (int) e[0];
            String param = (String) e[1], value = (String) e[2];
            double weight = (double) e[3];

            Index<String> valIndex = featureIndexes.computeIfAbsent(param, k -> new FastIndex<>());
            Relation<Double> rel = relations.get(param);
            if (rel == null)
            {
                rel = new FastWeightedPairwiseRelation<>();
                for (int i = 0; i < numPieces; ++i) rel.addFirstItem(i);
                relations.put(param, rel);
                names.add(param);
            }
            valIndex.addObject(value);
            int vidx = valIndex.object2idx(value);
            rel.addSecondItem(vidx);
            if (rel.containsPair(pieceIdx, vidx)) rel.updatePair(pieceIdx, vidx, rel.getValue(pieceIdx, vidx) + weight);
            else rel.addRelation(pieceIdx, vidx, weight);
        }
    }

    /** Derives one user-feature parameter per node attribute (value = the attribute value as text, weight 1). */
    private static void buildUserFeaturesFromNodeAttributes(Graph<String> graph, Index<String> users,
                                                            Map<String, Index<String>> featureIndexes,
                                                            List<String> names, Map<String, Relation<Double>> relations)
    {
        graph.getNodeAttributeNames().forEach(attrName ->
        {
            Index<String> valIndex = featureIndexes.computeIfAbsent(attrName, k -> new FastIndex<>());
            Relation<Double> rel = new FastWeightedPairwiseRelation<>();
            for (int i = 0; i < users.numObjects(); ++i) rel.addFirstItem(i);

            graph.getNodeAttributes(attrName).forEach(w ->
            {
                int uidx = users.object2idx(w.getIdx());
                if (uidx == -1 || w.getValue() == null) return;
                String value = w.getValue().toString();
                valIndex.addObject(value);
                int vidx = valIndex.object2idx(value);
                rel.addSecondItem(vidx);
                if (!rel.containsPair(uidx, vidx)) rel.addRelation(uidx, vidx, 1.0);
            });

            names.add(attrName);
            relations.put(attrName, rel);
        });
    }

    /** Derives one user-feature parameter per detected community partition (value = the user's community id). */
    private static void buildUserFeaturesFromCommunities(Map<String, Communities<String>> communities, Index<String> users,
                                                         Map<String, Index<String>> featureIndexes,
                                                         List<String> names, Map<String, Relation<Double>> relations)
    {
        if (communities == null) return;
        for (Map.Entry<String, Communities<String>> entry : communities.entrySet())
        {
            String partition = entry.getKey();
            Communities<String> comms = entry.getValue();
            if (comms == null) continue;

            Index<String> valIndex = featureIndexes.computeIfAbsent(partition, k -> new FastIndex<>());
            Relation<Double> rel = new FastWeightedPairwiseRelation<>();
            for (int i = 0; i < users.numObjects(); ++i) rel.addFirstItem(i);

            for (int uidx = 0; uidx < users.numObjects(); ++uidx)
            {
                String user = users.idx2object(uidx);
                int community = comms.getCommunity(user);
                if (community < 0) continue;      // user not assigned to any community
                String value = String.valueOf(community);
                valIndex.addObject(value);
                int vidx = valIndex.object2idx(value);
                rel.addSecondItem(vidx);
                if (!rel.containsPair(uidx, vidx)) rel.addRelation(uidx, vidx, 1.0);
            }

            names.add(partition);
            relations.put(partition, rel);
        }
    }

    private static String str(Object o)
    {
        return o == null ? "" : o.toString().trim();
    }

    private static double doubleValue(Object o, double def)
    {
        if (o instanceof Number) return ((Number) o).doubleValue();
        if (o == null) return def;
        try { return Double.parseDouble(o.toString().trim()); }
        catch (NumberFormatException e) { return def; }
    }

    private static long longValue(Object o, long def)
    {
        if (o instanceof Number) return ((Number) o).longValue();
        if (o == null) return def;
        try { return (long) Double.parseDouble(o.toString().trim()); }
        catch (NumberFormatException e) { return def; }
    }
}
