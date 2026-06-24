/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.grid.community.CommunityDetectionIdentifiers;
import es.uam.eps.ir.relison.grid.sna.comm.indiv.IndividualCommunityMetricIdentifiers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes the community-detection algorithms the GUI exposes, their groups and their tunable parameters.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public final class CommunityCatalog
{
    /** A single exposed community-detection algorithm. */
    public static final class AlgorithmDef
    {
        /** The RELISON algorithm identifier. */
        public final String id;
        /** The label shown in the UI. */
        public final String label;
        /** The family/group used to organise the UI dropdown. */
        public final String group;
        /** The tunable parameters (possibly empty). */
        public final List<Param> params;

        AlgorithmDef(String id, String label, String group, List<Param> params)
        {
            this.id = id;
            this.label = label;
            this.group = group;
            this.params = params;
        }
    }

    private static final Map<String, AlgorithmDef> ALGORITHMS = new LinkedHashMap<>();
    /** Individual (per-community) metrics, reusing {@link MetricCatalog.MetricDef}. */
    private static final Map<String, MetricCatalog.MetricDef> INDIVIDUAL = new LinkedHashMap<>();

    static
    {
        // Individual community metrics.
        indiv(IndividualCommunityMetricIdentifiers.COMMSIZE, "Size");
        indiv(IndividualCommunityMetricIdentifiers.COMMDEGREE, "Degree", Param.orientation("orientation", "Orientation", "OUT"));
        indiv(IndividualCommunityMetricIdentifiers.VOLUME, "Volume", Param.orientation("orientation", "Orientation", "OUT"));

        // Connectedness (no parameters).
        add(CommunityDetectionIdentifiers.WCC, "Weakly connected components", "Connectedness");
        add(CommunityDetectionIdentifiers.SCC, "Strongly connected components", "Connectedness");

        // Modularity-based.
        add(CommunityDetectionIdentifiers.LOUVAIN, "Louvain", "Modularity", Param.real("threshold", "Threshold", 0.001));
        add(CommunityDetectionIdentifiers.FASTGREEDY, "FastGreedy", "Modularity");
        add(CommunityDetectionIdentifiers.GIRVANNEWMAN, "Girvan-Newman", "Modularity");
        add(CommunityDetectionIdentifiers.LABELPROP, "Label propagation", "Modularity");
        add(CommunityDetectionIdentifiers.INFOMAP, "Infomap (needs external binary)", "Modularity", Param.integer("trials", "Trials", 10));

        // Balanced / size-aware.
        add(CommunityDetectionIdentifiers.SIZEWEIGHTEDFASTGREEDY, "Size-weighted FastGreedy", "Balanced");
        add(CommunityDetectionIdentifiers.BALANCEDFASTGREEDY, "Balanced FastGreedy", "Balanced", Param.integer("size", "Community size", 10));
        add(CommunityDetectionIdentifiers.GINIWEIGHTEDFASTGREEDY, "Gini-weighted FastGreedy", "Balanced", Param.real("lambda", "Lambda", 0.5));

        // Spectral (need extra numeric libraries).
        add(CommunityDetectionIdentifiers.RATIOCUTSPECTRAL, "Ratio-cut spectral", "Spectral", Param.integer("k", "Clusters (k)", 2));
        add(CommunityDetectionIdentifiers.NORMALIZEDCUTSPECTRAL, "Normalized-cut spectral", "Spectral", Param.integer("k", "Clusters (k)", 2));
    }

    private CommunityCatalog()
    {
    }

    private static void add(String id, String label, String group, Param... params)
    {
        ALGORITHMS.put(id, new AlgorithmDef(id, label, group, List.of(params)));
    }

    private static void indiv(String id, String label, Param... params)
    {
        INDIVIDUAL.put(id, new MetricCatalog.MetricDef(id, label, List.of(params)));
    }

    /** @return the exposed algorithm definitions, in display order. */
    public static Map<String, AlgorithmDef> algorithms()
    {
        return ALGORITHMS;
    }

    /** @return the exposed individual (per-community) metric definitions, in display order. */
    public static Map<String, MetricCatalog.MetricDef> individualMetrics()
    {
        return INDIVIDUAL;
    }

    /** Catalog payload for the individual community metrics. */
    public static List<Map<String, Object>> individualJson()
    {
        return MetricCatalog.familyJson(INDIVIDUAL);
    }

    /**
     * Builds the catalog payload: each algorithm as {id, label, group, params}.
     * @return a JSON-serializable list.
     */
    public static List<Map<String, Object>> toJson()
    {
        List<Map<String, Object>> list = new ArrayList<>();
        for (AlgorithmDef def : ALGORITHMS.values())
        {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", def.id);
            entry.put("label", def.label);
            entry.put("group", def.group);
            entry.put("params", MetricCatalog.paramsJson(def.params));
            list.add(entry);
        }
        return list;
    }
}
