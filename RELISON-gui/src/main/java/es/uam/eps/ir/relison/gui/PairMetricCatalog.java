/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.grid.sna.pair.PairMetricIdentifiers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static es.uam.eps.ir.relison.gui.MetricCatalog.MetricDef;

/**
 * Describes the pair/edge metrics the GUI exposes and their tunable parameters.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public final class PairMetricCatalog
{
    private static final Map<String, MetricDef> PAIR = new LinkedHashMap<>();

    static
    {
        pair(PairMetricIdentifiers.WEIGHT, "Edge weight");
        pair(PairMetricIdentifiers.RECIP, "Reciprocity");
        pair(PairMetricIdentifiers.DISTANCE, "Distance");
        pair(PairMetricIdentifiers.GEODESICS, "Geodesics");
        pair(PairMetricIdentifiers.FOAF, "Neighbour overlap (FOAF)", neighbourParams());
        pair(PairMetricIdentifiers.PREFATTACH, "Preferential attachment", neighbourParams());
        pair(PairMetricIdentifiers.EMBEDEDNESS, "Embeddedness", neighbourParams());
        pair(PairMetricIdentifiers.WEAKNESS, "Weakness", neighbourParams());
        pair(PairMetricIdentifiers.BETWEENNESS, "Edge betweenness", List.of(Param.bool("normalize", "Normalize", true)));
    }

    private PairMetricCatalog()
    {
    }

    private static void pair(String id, String label)
    {
        PAIR.put(id, new MetricDef(id, label, List.of()));
    }

    private static void pair(String id, String label, List<Param> params)
    {
        PAIR.put(id, new MetricDef(id, label, params));
    }

    /** @return the exposed pair metric definitions, in display order. */
    public static Map<String, MetricDef> pairMetrics()
    {
        return PAIR;
    }

    /** The standard uSel/vSel orientation parameters of neighbourhood-based pair metrics. */
    private static List<Param> neighbourParams()
    {
        return List.of(
                Param.orientation("uSel", "Source neighbours", "OUT"),
                Param.orientation("vSel", "Target neighbours", "IN"));
    }
}
