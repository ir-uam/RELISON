/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.ir;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.ranksys.core.util.tuples.Tuple2id;

/**
 * Adaptation of the Query Likelihood Information Retrieval method, with Laplace regularization.
 * <p>
 * Ponte, J. M. Croft, W. B. A language modeling approach to information retrieval.
 * 21st Annual International ACM SIGIR Conference on Research and Development in Information Retrieval (SIGIR 1998).
 * Melbourne, Australia, August 1998, pp. 275-281.
 * </p>
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class QLL<U> extends UserFastRankingRecommender<U>
{
    /**
     * Parameter which controls the trade-off between the regularization term and the original term
     * in the formula.
     */
    private final double gamma;
    /**
     * Target users neighborhood sizes
     */
    private final Int2DoubleMap uSize;
    /**
     * Candidate users neighborhood sizes
     */
    private final Int2DoubleMap vSize;
    /**
     * Neighborhood selection for the target users.
     */
    private final EdgeOrientation uSel;
    /**
     * Neighborhood selection for the candidate users.
     */
    private final EdgeOrientation vSel;
    /**
     * The sum of the user lengths.
     */
    private final double fullSize;

    /**
     * Constructor.
     *
     * @param graph the original social network graph.
     * @param uSel  neighborhood selection for the target user.
     * @param vSel  neighborhood selection for the candidate user.
     * @param gamma regularization parameter
     */
    public QLL(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel, double gamma)
    {
        super(graph);
        this.uSel = uSel;
        this.vSel = vSel.invertSelection();
        this.gamma = gamma;

        this.uSize = new Int2DoubleOpenHashMap();

        if (!graph.isDirected() || uSel.equals(vSel))
        {
            this.fullSize = this.getAllUidx().mapToDouble(vidx ->
            {
                double s = graph.getNeighborhoodWeights(vidx, uSel).mapToDouble(Tuple2id::v2).sum();
                this.uSize.put(vidx, s);
                return s;
            }).sum();
            vSize = uSize;
        }
        else
        {
            this.vSize = new Int2DoubleOpenHashMap();
            this.fullSize = this.getAllUidx().mapToDouble(vidx ->
            {
                double uS = graph.getNeighborhoodWeights(vidx, uSel).mapToDouble(Tuple2id::v2).sum();
                double vS = graph.getNeighborhoodWeights(vidx, vSel).mapToDouble(Tuple2id::v2).sum();

                this.uSize.put(vidx, uS);
                this.vSize.put(vidx, vS);
                return vS;
            }).sum();
        }
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx)
    {
        Int2DoubleOpenHashMap scoresMap = new Int2DoubleOpenHashMap();
        scoresMap.defaultReturnValue(0.0);

        double uS = this.uSize.get(uidx);
        graph.getNeighborhoodWeights(uidx, uSel).forEach(w ->
        {
            double uW = w.v2;
            int widx = w.v1;

            graph.getNeighborhoodWeights(widx, vSel).forEach(v ->
            {
                double val = uW * Math.log((v.v2 + this.gamma) / this.gamma);
                scoresMap.addTo(v.v1, val);
            });
        });

        for (int vidx : scoresMap.keySet())
        {
            scoresMap.replace(vidx, scoresMap.get(vidx) + uS * Math.log(this.gamma / (this.vSize.get(vidx) + gamma * this.numUsers())));
        }

        return scoresMap;
    }
}
