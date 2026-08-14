/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.grid.links.recommendation.metrics.RecommMetricIdentifiers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The catalog of contact-recommendation evaluation metrics the GUI exposes, mirroring
 * {@link RecommMetricIdentifiers} (RELISON's wrappers over the RankSys metric framework).
 *
 * <p>Every metric is evaluated at a ranking cutoff, so they all share a single {@code cutoff} parameter; the
 * configured metric is built by RELISON's {@code RecommMetricGridSelector}, which names each variant
 * {@code "<metric>@<cutoff>"} — those names become the columns of the results table.</p>
 *
 * <p>The catalog covers the basic accuracy and novelty/diversity metrics; the structural ones RELISON also offers
 * (mean prediction distance, community recall, ERR-IA) are deliberately left out, since the network-level view is
 * already covered by the Metrics and Communities tabs.</p>
 *
 * <p>Two metrics are computed over user feature data ({@code needsFeatures}); the flag lets the frontend point the
 * user at the node attribute they depend on, and warn when none is chosen (they would otherwise be degenerate).</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public final class RecommendationEvalCatalog
{
    private RecommendationEvalCatalog()
    {
    }

    /** A single evaluation metric: its identifier, presentation and the extra data it depends on. */
    public static final class MetricDef
    {
        public final String id;
        public final String label;
        public final String group;
        /** Whether the metric is computed over user feature data (ILD, Unexpectedness). */
        public final boolean needsFeatures;
        public final List<Param> params;

        MetricDef(String id, String label, String group, boolean needsFeatures, List<Param> params)
        {
            this.id = id;
            this.label = label;
            this.group = group;
            this.needsFeatures = needsFeatures;
            this.params = params;
        }
    }

    private static final List<Param> CUTOFF = List.of(Param.integer("cutoff", "Cutoff", 10));

    private static final Map<String, MetricDef> METRICS = build();

    private static Map<String, MetricDef> build()
    {
        Map<String, MetricDef> m = new LinkedHashMap<>();
        // Accuracy: how many of the held-out test links the ranking recovers.
        add(m, RecommMetricIdentifiers.P, "Precision", "Accuracy", false);
        add(m, RecommMetricIdentifiers.R, "Recall", "Accuracy", false);
        add(m, RecommMetricIdentifiers.NDCG, "nDCG", "Accuracy", false);
        add(m, RecommMetricIdentifiers.MAP, "MAP", "Accuracy", false);
        // Novelty and diversity: properties of the ranking itself rather than of its overlap with the test set.
        add(m, RecommMetricIdentifiers.LTN, "Long-tail novelty", "Novelty & diversity", false);
        add(m, RecommMetricIdentifiers.PGC, "Predicted Gini complement", "Novelty & diversity", false);
        add(m, RecommMetricIdentifiers.ILD, "Intra-list diversity", "Novelty & diversity", true);
        add(m, RecommMetricIdentifiers.UNEXP, "Unexpectedness", "Novelty & diversity", true);
        return m;
    }

    private static void add(Map<String, MetricDef> m, String id, String label, String group, boolean feats)
    {
        m.put(id, new MetricDef(id, label, group, feats, CUTOFF));
    }

    /** @return the available metrics, keyed by their RELISON identifier. */
    public static Map<String, MetricDef> metrics()
    {
        return METRICS;
    }

    /**
     * Serializes the catalog for the frontend.
     * @return one entry per metric, in catalog order.
     */
    public static List<Map<String, Object>> toJson()
    {
        List<Map<String, Object>> list = new ArrayList<>();
        for (MetricDef def : METRICS.values())
        {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", def.id);
            entry.put("label", def.label);
            entry.put("group", def.group);
            entry.put("needsFeatures", def.needsFeatures);
            entry.put("params", MetricCatalog.paramsJson(def.params));
            list.add(entry);
        }
        return list;
    }
}
