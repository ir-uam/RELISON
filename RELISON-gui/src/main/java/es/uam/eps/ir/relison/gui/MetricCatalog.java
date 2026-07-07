/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.grid.sna.graph.GraphMetricIdentifiers;
import es.uam.eps.ir.relison.grid.sna.vertex.VertexMetricIdentifiers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes the metrics the GUI exposes and, for each, its tunable parameters. Controllers turn a parameter
 * specification plus the user-supplied values into a RELISON {@code Grid} via {@link Grids}.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public final class MetricCatalog
{
    /** A single exposed metric: its RELISON identifier, a human label, and its tunable parameters. */
    public static final class MetricDef
    {
        /** The RELISON metric identifier (the key the selectors match on). */
        public final String id;
        /** The label shown in the UI. */
        public final String label;
        /** The tunable parameters (possibly empty). */
        public final List<Param> params;

        MetricDef(String id, String label, List<Param> params)
        {
            this.id = id;
            this.label = label;
            this.params = params;
        }
    }

    private static final Map<String, MetricDef> VERTEX = new LinkedHashMap<>();
    private static final Map<String, MetricDef> GRAPH = new LinkedHashMap<>();

    static
    {
        // ---- Vertex metrics ----
        vertex(VertexMetricIdentifiers.DEGREE, "Degree", Param.orientation("orientation", "Orientation", "OUT"));
        vertex(VertexMetricIdentifiers.INVDEGREE, "Inverse degree", Param.orientation("orientation", "Orientation", "OUT"));
        vertex(VertexMetricIdentifiers.LOCALRECIPRATE, "Reciprocity rate", Param.orientation("orientation", "Orientation", "OUT"));
        vertex(VertexMetricIdentifiers.CORENESS, "Coreness", Param.orientation("orientation", "Orientation", "OUT"));
        vertex(VertexMetricIdentifiers.EIGEN, "Eigenvector centrality", Param.orientation("orientation", "Orientation", "OUT"));
        vertex(VertexMetricIdentifiers.LENGTH, "Length", Param.orientation("orientation", "Orientation", "OUT"));
        vertex(VertexMetricIdentifiers.FD, "Free discovery", Param.orientation("orient", "Orientation", "OUT"));
        vertex(VertexMetricIdentifiers.PAGERANK, "PageRank", Param.real("r", "Damping (r)", 0.85));
        vertex(VertexMetricIdentifiers.COMPLDEGREE, "Complementary degree", Param.orientation("orientation", "Orientation", "OUT"));
        vertex(VertexMetricIdentifiers.COMPLINVDEGREE, "Complementary inverse degree", Param.orientation("orientation", "Orientation", "OUT"));
        vertex(VertexMetricIdentifiers.COMPLLOCALCLUSTCOEF, "Complementary local clustering coefficient",
                Param.orientation("vSel", "First neighbours", "IN"), Param.orientation("wSel", "Second neighbours", "OUT"));
        vertex(VertexMetricIdentifiers.COMPLPAGERANK, "Complementary PageRank", Param.real("r", "Damping (r)", 0.85));
        vertex(VertexMetricIdentifiers.KATZ, "Katz centrality",
                Param.orientation("orientation", "Orientation", "OUT"), Param.real("alpha", "Alpha", 0.1));
        vertex(VertexMetricIdentifiers.HITS, "HITS", Param.bool("mode", "Authority (off = hub)", true));
        vertex(VertexMetricIdentifiers.LOCALCLUSTCOEF, "Local clustering coefficient",
                Param.orientation("vSel", "First neighbours", "IN"), Param.orientation("wSel", "Second neighbours", "OUT"));
        vertex(VertexMetricIdentifiers.CLOSENESS, "Closeness");
        vertex(VertexMetricIdentifiers.HARMONIC, "Harmonic centrality");
        vertex(VertexMetricIdentifiers.ECCENTRICITY, "Eccentricity");
        vertex(VertexMetricIdentifiers.BETWEENNESS, "Betweenness", Param.bool("norm", "Normalize", true));

        // ---- Graph-global metrics ----
        graph(GraphMetricIdentifiers.NUMEDGES, "Number of edges");
        graph(GraphMetricIdentifiers.DENSITY, "Density");
        graph(GraphMetricIdentifiers.CLUSTCOEF, "Clustering coefficient",
                Param.orientation("uSel", "First neighbours", "IN"), Param.orientation("vSel", "Second neighbours", "OUT"));
        graph(GraphMetricIdentifiers.CLUSTCOEFCOMPL, "Clustering coefficient complement",
                Param.orientation("uSel", "First neighbours", "IN"), Param.orientation("vSel", "Second neighbours", "OUT"));
        graph(GraphMetricIdentifiers.RECIPROCITYRATE, "Reciprocity");
        graph(GraphMetricIdentifiers.DEGREEGINI, "Degree Gini complement", Param.orientation("orientation", "Orientation", "OUT"));
        graph(GraphMetricIdentifiers.DEGREEASSORT, "Degree assortativity", Param.orientation("orientation", "Orientation", "OUT"));
        graph(GraphMetricIdentifiers.DEGREEPEARSON, "Degree Pearson correlation",
                Param.orientation("uSel", "Source neighbours", "OUT"), Param.orientation("vSel", "Target neighbours", "IN"));
        graph(GraphMetricIdentifiers.ASL, "Average shortest path length",
                Param.choice("mode", "Mode", "Non infinite distances", List.of("Non infinite distances", "Components")));
        graph(GraphMetricIdentifiers.ARSL, "Average reciprocal shortest path length");
        graph(GraphMetricIdentifiers.DIAMETER, "Diameter");
        graph(GraphMetricIdentifiers.RECIPRDIAMETER, "Reciprocal diameter");
        graph(GraphMetricIdentifiers.RADIUS, "Radius");
        graph(GraphMetricIdentifiers.RECIPRAVGECCENTRICITY, "Reciprocal average eccentricity");
        graph(GraphMetricIdentifiers.INFINITEDIST, "Infinite distances");
        graph(GraphMetricIdentifiers.INTEREDGEGINI, "Inter-edge Gini complement");
        graph(GraphMetricIdentifiers.COMPLETEEDGEGINI, "Complete edge Gini complement");
        graph(GraphMetricIdentifiers.SEMICOMPLETEEDGEGINI, "Semi-complete edge Gini complement");
    }

    private MetricCatalog()
    {
    }

    private static void vertex(String id, String label, Param... params)
    {
        VERTEX.put(id, new MetricDef(id, label, List.of(params)));
    }

    private static void graph(String id, String label, Param... params)
    {
        GRAPH.put(id, new MetricDef(id, label, List.of(params)));
    }

    /** @return the exposed vertex metric definitions, in display order. */
    public static Map<String, MetricDef> vertexMetrics()
    {
        return VERTEX;
    }

    /** @return the exposed graph-global metric definitions, in display order. */
    public static Map<String, MetricDef> graphMetrics()
    {
        return GRAPH;
    }

    /**
     * Builds the catalog payload for {@code GET /api/metrics}: each family as a list of {id, label, params} entries.
     * @return a JSON-serializable map keyed by family name.
     */
    public static Map<String, Object> toJson()
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("vertex", familyJson(VERTEX));
        result.put("graph", familyJson(GRAPH));
        result.put("pair", familyJson(PairMetricCatalog.pairMetrics()));
        result.put("community", CommunityCatalog.toJson());
        result.put("communityIndividual", CommunityCatalog.individualJson());
        result.put("communityGlobal", CommunityCatalog.globalJson());
        return result;
    }

    static List<Map<String, Object>> familyJson(Map<String, MetricDef> family)
    {
        List<Map<String, Object>> list = new ArrayList<>();
        for (MetricDef def : family.values())
        {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", def.id);
            entry.put("label", def.label);
            entry.put("params", paramsJson(def.params));
            list.add(entry);
        }
        return list;
    }

    static List<Map<String, Object>> paramsJson(List<Param> params)
    {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Param p : params) list.add(p.toJson());
        return list;
    }
}
