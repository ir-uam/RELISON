/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.gui;

import java.util.List;

/**
 * A computed recommendation / link-prediction over a session's graph: the ranked list of recommended links together
 * with the mode and cutoff that produced it. Stored in the {@link GraphSession} so re-selecting the same model
 * (algorithm + parameters + mode + cutoff) reuses the result instead of recomputing it.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class RecommendationResult
{
    /** A single recommended link. */
    public static final class RecEdge
    {
        /** The source (target user) of the recommended link. */
        public final String source;
        /** The destination (recommended user) of the recommended link. */
        public final String target;
        /** The score the algorithm assigned to the link. */
        public final double score;

        public RecEdge(String source, String target, double score)
        {
            this.source = source;
            this.target = target;
            this.score = score;
        }
    }

    /** The user-facing label of the model (algorithm + parameter suffix). */
    public final String label;
    /** Either {@code "recommendation"} (per-node cutoff) or {@code "prediction"} (total cutoff). */
    public final String mode;
    /** The cutoff used (links per node, or total links). */
    public final int cutoff;
    /** The recommended links, in descending score order. */
    public final List<RecEdge> edges;

    public RecommendationResult(String label, String mode, int cutoff, List<RecEdge> edges)
    {
        this.label = label;
        this.mode = mode;
        this.cutoff = cutoff;
        this.edges = edges;
    }
}
