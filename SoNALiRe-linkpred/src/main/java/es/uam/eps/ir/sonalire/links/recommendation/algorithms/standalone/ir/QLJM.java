/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.algorithms.standalone.ir;

import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.ranksys.core.util.tuples.Tuple2id;

/**
 * Adaptation of the Query Likelihood Information Retrieval method, with Jelinek-Mercer regularization.
 * <p>
 * <b>Reference:</b> J.M. Ponte, W.B. Croft. A language modeling approach to information retrieval.
 * 21st Annual International ACM SIGIR Conference on Research and Development in Information Retrieval (SIGIR 1998).
 * Melbourne, Australia, 275-281 (1998)
 * </p>
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class QLJM<U> extends UserFastRankingRecommender<U>
{
    /**
     * Parameter which controls the trade-off between the regularization term and the original term
     * in the formula.
     */
    private final double lambda;
    /**
     * For each user, computes the proportion of neighbors it has, in comparison with the sum of all neighborhood sizes.
     */
    private final Int2DoubleMap pc;
    /**
     * Neighborhood sizes
     */
    private final Int2DoubleMap size;
    /**
     * Sum of the neighborhood sizes
     */
    private final double fullSize;
    /**
     * Neighborhood selection for the target users.
     */
    private final EdgeOrientation uSel;
    /**
     * Neighborhood selection for the candidate users.
     */
    private final EdgeOrientation vSel;

    /**
     * Constructor.
     *
     * @param graph  the original social network graph.
     * @param uSel   neighborhood selection for the target user.
     * @param vSel   neighborhood selection for the candidate user.
     * @param lambda parameter which controls the trade-off between the regularization term and the original probability.
     */
    public QLJM(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel, double lambda)
    {
        super(graph);
        this.uSel = uSel;
        this.vSel = vSel.invertSelection();
        this.lambda = lambda / (1 - lambda);
        this.size = new Int2DoubleOpenHashMap();

        EdgeOrientation wSel = vSel.invertSelection();
        if (!graph.isDirected() || vSel.equals(EdgeOrientation.UND)) // vSel == wSel
        {
            this.fullSize = this.getAllUidx().mapToDouble(vidx ->
            {
                double vS = graph.getNeighborhoodWeights(vidx, vSel).mapToDouble(Tuple2id::v2).sum();
                this.size.put(vidx, vS);
                return vS;
            }).sum();
            this.pc = size;
        }
        else
        {
            this.pc = new Int2DoubleOpenHashMap();
            this.fullSize = this.getAllUidx().mapToDouble(vidx ->
            {
                double vS = graph.getNeighborhoodWeights(vidx, vSel).mapToDouble(Tuple2id::v2).sum();
                double wS = graph.getNeighborhoodWeights(vidx, wSel).mapToDouble(Tuple2id::v2).sum();

                this.size.put(vidx, vS);
                this.pc.put(vidx, wS);
                return vS;
            }).sum();
        }
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx)
    {
        Int2DoubleOpenHashMap scoresMap = new Int2DoubleOpenHashMap();
        scoresMap.defaultReturnValue(0.0);

        graph.getNeighborhoodWeights(uidx, uSel).forEach(w ->
        {
            double uW = w.v2;
            int widx = w.v1;
            double wPc = this.fullSize / (this.pc.get(widx));

            graph.getNeighborhoodWeights(widx, vSel).forEach(v ->
            {
                double s = this.size.getOrDefault(v.v1, 0.0);
                double val = lambda * wPc * (v.v2 / s);
                if (Double.isNaN(val) || Double.isInfinite(val)) scoresMap.addTo(v.v1, Double.NEGATIVE_INFINITY);
                else scoresMap.addTo(v.v1, uW * Math.log(val + 1.0));
            });
        });

        return scoresMap;
    }
}
