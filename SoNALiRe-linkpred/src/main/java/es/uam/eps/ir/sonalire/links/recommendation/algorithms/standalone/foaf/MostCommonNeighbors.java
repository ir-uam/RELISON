/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.algorithms.standalone.foaf;

import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

/**
 * Recommended that sorts candidate users according to the number of neighbors in common with the target one.
 *
 * <p>
 *     <b>References:</b>
 *     <ol>
 *         <li>D. Liben-Nowell, D., J. Kleinberg. The Link Prediction Problem for Social Networks. Journal of the American Society for Information Science and Technology 58(7) (2007)</li>
 *         <li>M.E.J. Newman. Clustering and Preferential Attachment in Growing Networks. Physical Review Letters E, 64(025102) (2001)</li>
 *     </ol>
 * </p>
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class MostCommonNeighbors<U> extends UserFastRankingRecommender<U>
{
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
     * @param graph user graph.
     * @param uSel  link orientation for the target users.
     * @param vSel  link orientation for the candidate users.
     */
    public MostCommonNeighbors(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        super(graph);
        this.uSel = uSel;
        this.vSel = vSel.invertSelection();
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx)
    {
        Int2DoubleOpenHashMap scoresMap = new Int2DoubleOpenHashMap();
        scoresMap.defaultReturnValue(0.0);

        graph.getNeighborhood(uidx, uSel).forEach(widx ->
            graph.getNeighborhood(widx, vSel).forEach(vidx ->
                scoresMap.addTo(vidx, 1.0)));

        return scoresMap;
    }
}
