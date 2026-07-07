/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.grid.links.recommendation.algorithms.AlgorithmIdentifiers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes the contact-recommendation / link-prediction algorithms the GUI exposes, their groups and their tunable
 * parameters.
 *
 * <p>Only <em>structural</em> algorithms are listed: those that RELISON can train from the loaded network alone (no
 * external feature files or content indexes). The parameter names match the keys each algorithm's
 * {@code *GridSearch} reads, so {@link Grids#build(List, Map)} produces a usable single-configuration grid for the
 * flat algorithms; the two k-NN algorithms need a nested similarity grid and are handled specially by
 * {@link RecommendationController}.</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public final class RecommendationCatalog
{
    /** A single exposed recommendation algorithm. */
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

    static
    {
        // Friends of friends (neighbourhood overlap). All read uSel/vSel (and some a wSel) orientations.
        add(AlgorithmIdentifiers.ADAMIC, "Adamic-Adar", "Friends of friends", uSel(), vSel(), wSel());
        add(AlgorithmIdentifiers.JACCARD, "Jaccard", "Friends of friends", uSel(), vSel());
        add(AlgorithmIdentifiers.MCN, "Most common neighbours", "Friends of friends", uSel(), vSel());
        add(AlgorithmIdentifiers.COSINE, "Cosine", "Friends of friends", uSel(), vSel(), weighted());
        add(AlgorithmIdentifiers.SORENSEN, "Sorensen", "Friends of friends", uSel(), vSel());
        add(AlgorithmIdentifiers.HPI, "Hub promoted index", "Friends of friends", uSel(), vSel());
        add(AlgorithmIdentifiers.HDI, "Hub depressed index", "Friends of friends", uSel(), vSel());
        add(AlgorithmIdentifiers.LOCALLHN, "Local LHN", "Friends of friends", uSel(), vSel());
        add(AlgorithmIdentifiers.RESALLOC, "Resource allocation", "Friends of friends", uSel(), vSel(), wSel());

        // Information retrieval models.
        add(AlgorithmIdentifiers.BM25, "BM25", "Information retrieval",
                Param.real("b", "b", 0.75), Param.real("k", "k1", 1.2), uSel(), vSel(), dlSel(), weighted());
        add(AlgorithmIdentifiers.EBM25, "Extreme BM25", "Information retrieval",
                Param.real("b", "b", 0.75), uSel(), vSel(), dlSel(), weighted());
        add(AlgorithmIdentifiers.VSM, "Vector space model", "Information retrieval", uSel(), vSel(), weighted());
        add(AlgorithmIdentifiers.PIVOTEDVSM, "Pivoted normalization VSM", "Information retrieval",
                Param.real("s", "s", 0.2), uSel(), vSel(), weighted());
        add(AlgorithmIdentifiers.PL2, "PL2", "Information retrieval",
                Param.real("c", "c", 1.0), uSel(), vSel(), weighted());
        add(AlgorithmIdentifiers.QLJM, "QL Jelinek-Mercer", "Information retrieval",
                Param.real("lambda", "Lambda", 0.5), uSel(), vSel(), weighted());
        add(AlgorithmIdentifiers.QLD, "QL Dirichlet", "Information retrieval",
                Param.real("mu", "Mu", 100.0), uSel(), vSel(), weighted());
        add(AlgorithmIdentifiers.QLL, "QL Laplace", "Information retrieval",
                Param.real("phi", "Phi", 0.5), uSel(), vSel(), weighted());
        add(AlgorithmIdentifiers.DLH, "DLH", "Information retrieval", uSel(), vSel(), weighted());
        add(AlgorithmIdentifiers.DPH, "DPH", "Information retrieval", uSel(), vSel(), weighted());
        add(AlgorithmIdentifiers.DFREE, "DFRee", "Information retrieval", uSel(), vSel(), weighted());
        add(AlgorithmIdentifiers.DFREEKLIM, "DFReeKLIM", "Information retrieval", uSel(), vSel(), weighted());
        add(AlgorithmIdentifiers.BIR, "Binary independent retrieval", "Information retrieval", uSel(), vSel());

        // Path-based.
        add(AlgorithmIdentifiers.DISTANCE, "Shortest distance", "Path-based",
                Param.orientation("orientation", "Orientation", "OUT"));
        add(AlgorithmIdentifiers.KATZ, "Katz", "Path-based",
                Param.real("b", "Damping (b)", 0.1), Param.orientation("orientation", "Orientation", "OUT"));
        add(AlgorithmIdentifiers.LPI, "Local path index", "Path-based",
                Param.real("b", "Damping (b)", 0.1), Param.integer("k", "Path length (k)", 3),
                Param.orientation("orientation", "Orientation", "OUT"));
        add(AlgorithmIdentifiers.GLOBALLHN, "Global LHN", "Path-based",
                Param.real("phi", "Phi", 0.1), Param.orientation("orientation", "Orientation", "OUT"));
        add(AlgorithmIdentifiers.MATRIXFOREST, "Matrix forest", "Path-based",
                Param.real("alpha", "Alpha", 1.0), Param.orientation("orient", "Orientation", "OUT"));
        add(AlgorithmIdentifiers.PIC, "Pseudo-inverse cosine", "Path-based",
                Param.orientation("orient", "Orientation", "OUT"));

        // Random walks.
        add(AlgorithmIdentifiers.PAGERANK, "PageRank", "Random walks", Param.real("r", "Teleport (r)", 0.85));
        add(AlgorithmIdentifiers.PERSPAGERANK, "Personalized PageRank", "Random walks", Param.real("r", "Teleport (r)", 0.5));
        add(AlgorithmIdentifiers.HITS, "HITS", "Random walks", Param.bool("mode", "Authorities (vs. hubs)", true));
        add(AlgorithmIdentifiers.SALSA, "SALSA", "Random walks", Param.bool("mode", "Authorities (vs. hubs)", true));
        add(AlgorithmIdentifiers.PERSHITS, "Personalized HITS", "Random walks",
                Param.bool("mode", "Authorities (vs. hubs)", true), Param.real("alpha", "Alpha", 0.5));
        add(AlgorithmIdentifiers.PERSSALSA, "Personalized SALSA", "Random walks",
                Param.bool("mode", "Authorities (vs. hubs)", true), Param.real("alpha", "Alpha", 0.5));
        add(AlgorithmIdentifiers.PROPFLOW, "PropFlow", "Random walks",
                Param.integer("maxLength", "Max length", 3), Param.orientation("orientation", "Orientation", "OUT"), weighted());
        add(AlgorithmIdentifiers.COMMUTE, "Commute time PageRank", "Random walks", Param.real("r", "Teleport (r)", 0.85));
        add(AlgorithmIdentifiers.HITTING, "Hitting time PageRank", "Random walks", Param.real("r", "Teleport (r)", 0.85));
        add(AlgorithmIdentifiers.COMMUTEPERS, "Commute time pers. PageRank", "Random walks", Param.real("r", "Teleport (r)", 0.5));
        add(AlgorithmIdentifiers.HITTINGPERS, "Hitting time pers. PageRank", "Random walks", Param.real("r", "Teleport (r)", 0.5));

        // Twitter.
        add(AlgorithmIdentifiers.MONEY, "Money", "Twitter",
                Param.bool("mode", "Use authorities", true), Param.real("alpha", "Alpha", 1.0),
                Param.real("r", "Teleport (r)", 0.5), Param.integer("neigh", "Neighbours", 10));
        add(AlgorithmIdentifiers.LOVE, "Love", "Twitter",
                Param.bool("mode", "Use authorities", true), Param.real("alpha", "Alpha", 1.0),
                Param.real("r", "Teleport (r)", 0.5), Param.integer("neigh", "Neighbours", 10));
        add(AlgorithmIdentifiers.TWITTERAVGCOS, "Average cosine", "Twitter",
                Param.real("r", "Teleport (r)", 0.5), Param.integer("neigh", "Neighbours", 10));
        add(AlgorithmIdentifiers.TWITTERCENTROIDCOS, "Centroid cosine", "Twitter",
                Param.real("r", "Teleport (r)", 0.5), Param.integer("neigh", "Neighbours", 10));
        add(AlgorithmIdentifiers.TWITTERMAXCOS, "Maximum cosine", "Twitter",
                Param.real("r", "Teleport (r)", 0.5), Param.integer("neigh", "Neighbours", 10));

        // Matrix factorization.
        add(AlgorithmIdentifiers.IMF, "Implicit MF", "Matrix factorization",
                Param.integer("k", "Latent factors (k)", 10), Param.real("lambda", "Lambda", 0.1),
                Param.real("alpha", "Alpha", 1.0), weighted());
        add(AlgorithmIdentifiers.FASTIMF, "Fast implicit MF", "Matrix factorization",
                Param.integer("k", "Latent factors (k)", 10), Param.real("lambda", "Lambda", 0.1),
                Param.real("alpha", "Alpha", 1.0), weighted());

        // Nearest-neighbours collaborative filtering. The similarity is a (Cosine) recommender supplied through a
        // nested grid built by RecommendationController; uSel/vSel here parameterise that inner similarity.
        add(AlgorithmIdentifiers.UB, "User-based kNN", "Nearest neighbours",
                Param.integer("k", "Neighbours (k)", 10), Param.integer("q", "Score exponent (q)", 1),
                uSel(), vSel(), weighted());
        add(AlgorithmIdentifiers.IB, "Item-based kNN", "Nearest neighbours",
                Param.integer("k", "Neighbours (k)", 10), Param.integer("q", "Score exponent (q)", 1),
                uSel(), vSel(), weighted());

        // Baselines.
        add(AlgorithmIdentifiers.POP, "Popularity", "Baselines");
        add(AlgorithmIdentifiers.RANDOM, "Random", "Baselines");
    }

    private RecommendationCatalog()
    {
    }

    private static void add(String id, String label, String group, Param... params)
    {
        ALGORITHMS.put(id, new AlgorithmDef(id, label, group, List.of(params)));
    }

    private static Param uSel()
    {
        return Param.orientation("uSel", "Target neighbourhood (uSel)", "OUT");
    }

    private static Param vSel()
    {
        return Param.orientation("vSel", "Candidate neighbourhood (vSel)", "OUT");
    }

    private static Param wSel()
    {
        return Param.orientation("wSel", "Weighting neighbourhood (wSel)", "OUT");
    }

    private static Param dlSel()
    {
        return Param.orientation("dlSel", "Length neighbourhood (dlSel)", "OUT");
    }

    private static Param weighted()
    {
        return Param.bool("weighted", "Use edge weights", false);
    }

    /** @return the exposed algorithm definitions, in display order. */
    public static Map<String, AlgorithmDef> algorithms()
    {
        return ALGORITHMS;
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
