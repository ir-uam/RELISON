/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.standalone.ir;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.ranksys.core.util.tuples.Tuple2id;

/**
 * Adaptation of the Query Likelihood Information Retrieval method, with Dirichlet regularization.
 * <p>
 * Ponte, J. M. Croft, W. B. A language modeling approach to information retrieval.
 * 21st Annual International ACM SIGIR Conference on Research and Development in Information Retrieval (SIGIR 1998).
 * Melbourne, Australia, August 1998, pp. 275-281.
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class QLD<U> extends UserFastRankingRecommender<U>
{
    /**
     * Parameter which controls the trade-off between the regularization term and the original term
     * in the formula.
     */
    private final double mu;
    /**
     * For each user, computes the proportion of neighbors it has, in comparison with the sum of all neighborhood sizes.
     */
    private final Int2DoubleMap pc;
    /**
     * Neighborhood sizes for the target user
     */
    private final Int2DoubleMap uSize;
    /**
     * Neighborhood sizes for the candidate user
     */
    private final Int2DoubleMap vSize;
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
     * @param graph The original social network graph.
     * @param uSel  Neighborhood selection for the target user.
     * @param vSel  Neighborhood selection for the candidate user.
     * @param mu    Parameter which controls the trade-off between the regularization term and the original probability.
     */
    public QLD(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel, double mu)
    {
        super(graph);

        this.uSel = uSel;
        this.vSel = vSel.invertSelection();
        this.mu = mu;
        this.uSize = new Int2DoubleOpenHashMap();

        EdgeOrientation wSel = vSel.invertSelection();
        if (!graph.isDirected() || (uSel.equals(vSel) && uSel.equals(EdgeOrientation.UND))) // Cases UND-UND
        {
            this.fullSize = this.getAllUidx().mapToDouble(vidx ->
            {
                double uS = graph.getNeighborhoodWeights(vidx, uSel).mapToDouble(Tuple2id::v2).sum();
                this.uSize.put(vidx, uS);
                return uS;
            }).sum();
            this.vSize = uSize;
            this.pc = uSize;
        }
        else if (uSel.equals(vSel)) //CASES IN-IN,OUT-OUT
        {
            this.pc = new Int2DoubleOpenHashMap();
            this.fullSize = this.getAllUidx().mapToDouble(vidx ->
            {
                double uS = graph.getNeighborhoodWeights(vidx, uSel).mapToDouble(Tuple2id::v2).sum();
                double wS = graph.getNeighborhoodWeights(vidx, wSel).mapToDouble(Tuple2id::v2).sum();
                this.uSize.put(vidx, uS);
                this.pc.put(vidx, wS);
                return uS;
            }).sum();
            this.vSize = uSize;
        }
        else if (uSel.equals(vSel.invertSelection())) // CASES IN-OUT,OUT-IN
        {
            this.vSize = new Int2DoubleOpenHashMap();
            this.fullSize = this.getAllUidx().mapToDouble(vidx ->
            {
                double uS = graph.getNeighborhoodWeights(vidx, uSel).mapToDouble(Tuple2id::v2).sum();
                double wS = graph.getNeighborhoodWeights(vidx, vSel).mapToDouble(Tuple2id::v2).sum();
                this.uSize.put(vidx, uS);
                this.vSize.put(vidx, wS);
                return uS;
            }).sum();
            this.pc = uSize;
        }
        else if (vSel.equals(EdgeOrientation.UND)) // CASES IN-UND, OUT-UND
        {
            this.vSize = new Int2DoubleOpenHashMap();
            this.fullSize = this.getAllUidx().mapToDouble(vidx ->
            {
                double uS = graph.getNeighborhoodWeights(vidx, uSel).mapToDouble(Tuple2id::v2).sum();
                double vS = graph.getNeighborhoodWeights(vidx, vSel).mapToDouble(Tuple2id::v2).sum();
                this.uSize.put(vidx, uS);
                this.vSize.put(vidx, vS);
                return uS;
            }).sum();
            this.pc = vSize;
        }
        else // CASES UND-IN, UND-OUT
        {
            this.vSize = new Int2DoubleOpenHashMap();
            this.pc = new Int2DoubleOpenHashMap();
            this.fullSize = this.getAllUidx().mapToDouble(vidx ->
            {
                double uS = graph.getNeighborhoodWeights(vidx, uSel).mapToDouble(Tuple2id::v2).sum();
                double vS = graph.getNeighborhoodWeights(vidx, vSel).mapToDouble(Tuple2id::v2).sum();
                double wS = uS - vS; // Considering that weight(UND,x,y) = weight(x,y) + weight(y,x)
                this.uSize.put(vidx, uS);
                this.vSize.put(vidx, vS);
                this.pc.put(vidx, wS);
                return uS;
            }).sum();
        }
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx)
    {
        Int2DoubleOpenHashMap scoresMap = new Int2DoubleOpenHashMap();
        scoresMap.defaultReturnValue(0.0);

        double norm = this.uSize.get(uidx);
        graph.getNeighborhoodWeights(uidx, uSel).forEach(w ->
        {
            double uWeight = w.v2;
            int widx = w.v1;
            double wPc = this.fullSize / (this.mu * this.pc.get(widx));

            graph.getNeighborhoodWeights(widx, vSel).forEach(v ->
            {
                double vWeight = v.v2;

                double val = uWeight * Math.log(vWeight * wPc + 1.0);
                if (Double.isNaN(val) || Double.isInfinite(val)) scoresMap.addTo(v.v1, Double.NEGATIVE_INFINITY);
                else scoresMap.addTo(v.v1, val);
            });
        });

        scoresMap.replaceAll((vidx, val) -> val - norm * Math.log(1.0 + this.vSize.get((int) vidx) / mu));

        return scoresMap;
    }
}
