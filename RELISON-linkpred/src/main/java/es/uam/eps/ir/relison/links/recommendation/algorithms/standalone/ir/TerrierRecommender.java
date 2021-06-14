/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.algorithms.standalone.ir;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.links.data.TerrierIndex;
import es.uam.eps.ir.relison.links.data.TerrierStructure;
import es.uam.eps.ir.relison.links.recommendation.UserFastRankingRecommender;
import es.uam.eps.ir.relison.utils.datatypes.Tuple2oo;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.ranksys.formats.parsing.Parsers;
import org.terrier.querying.ScoredDocList;
import org.terrier.realtime.memory.MemoryIndex;

import java.util.Map;
import java.util.Optional;

/**
 * IR-based recommender models created using the Terrier library.
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class TerrierRecommender<U> extends UserFastRankingRecommender<U>
{
    /**
     * Terrier RAM index.
     */
    private final TerrierIndex index;
    /**
     * Edge orientation for the target users.
     */
    private final EdgeOrientation uSel;
    /**
     * Edge orientation for the candidate users.
     */
    private final EdgeOrientation vSel;

    /**
     * Constructor.
     *
     * @param graph the social network graph.
     * @param uSel  target user neighborhood selection.
     * @param vSel  candidate user neighborhood selection
     */
    public TerrierRecommender(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        super(graph);
        this.uSel = uSel;
        this.vSel = vSel;
        this.index = new TerrierIndex(graph, uSel, vSel);
    }

    /**
     * Constructor.
     *
     * @param graph     the training graph.
     * @param uSel      orientation selection for the target user.
     * @param vSel      orientation selection for the candidate user.
     * @param structure Terrier basic structures for the algorithm.
     */
    public TerrierRecommender(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel, TerrierStructure structure)
    {
        super(graph);
        this.uSel = uSel;
        this.vSel = vSel;
        Tuple2oo<MemoryIndex, Map<Integer, String>> pair = structure.get(uSel, vSel);
        this.index = new TerrierIndex(graph, uSel, vSel, pair.v1(), pair.v2());
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx)
    {
        Int2DoubleMap scores = new Int2DoubleOpenHashMap();
        String weightingModel = this.getModel();
        Optional<Double> cvalue = this.getCValue();

        ScoredDocList results = this.index.query(uidx, weightingModel, cvalue);
        if (results != null)
        {
            results.forEach((doc) ->
            {
                double score = doc.getScore();
                int vidx = Parsers.ip.parse(doc.getMetadata(TerrierIndex.NODEID));
                scores.put(vidx, score);
            });
        }

        return scores;
    }

    /**
     * Obtains the name of the weighting model from Terrier.
     *
     * @return the name of the weighting model.
     */
    protected abstract String getModel();

    /**
     * Obtains the name of the c value for the model. If it is null,
     * it will take just the default parameter.
     *
     * @return the value if it exists, or an empty object otherwise.
     */
    protected abstract Optional<Double> getCValue();
}
