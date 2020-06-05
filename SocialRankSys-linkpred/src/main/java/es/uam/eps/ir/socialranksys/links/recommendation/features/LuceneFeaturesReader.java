/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.features;

import es.uam.eps.ir.socialranksys.content.ContentVector;
import es.uam.eps.ir.socialranksys.content.index.exceptions.WrongModeException;
import es.uam.eps.ir.socialranksys.content.index.twittomender.LuceneTwittomenderIndex;
import es.uam.eps.ir.socialranksys.content.index.weighting.TFIDFWeightingScheme;
import es.uam.eps.ir.socialranksys.content.index.weighting.WeightingScheme;
import es.uam.eps.ir.socialranksys.graph.Graph;
import org.jooq.lambda.tuple.Tuple3;
import org.openide.util.Exceptions;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Class that loads features from a lucene index.
 * @author Javier Sanz-Cruzado Puig
 */
public class LuceneFeaturesReader
{
    /**
     * Loads features from an index.
     * @param route The index route.
     * @param graph the graph containing the users to retrieve.
     * @return a stream containing the features.
     */
    public static Stream<Tuple3<Long,String,Double>> load(String route, Graph<Long> graph)
    {
        List<Tuple3<Long, String, Double>> features = new ArrayList<>();
        LuceneTwittomenderIndex luceneIndex = new LuceneTwittomenderIndex(route, true);
        luceneIndex.setReadMode();
        
        WeightingScheme ws = new TFIDFWeightingScheme();
        graph.getAllNodes().forEach(u -> 
        {
            try 
            {
                ContentVector<Long> cv = luceneIndex.readUser(u, ws);
                cv.getVector().getVector().forEach(t -> features.add(new Tuple3<>(u, t.v1(), t.v2())));
            } 
            catch (WrongModeException ex)
            {
                Exceptions.printStackTrace(ex);
            }
        });
        
        return features.stream();
        
        
    }
}
