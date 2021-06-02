/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.features;

import es.uam.eps.ir.sonalire.content.index.freq.FreqVector;
import es.uam.eps.ir.sonalire.content.index.freq.TermFreq;
import es.uam.eps.ir.sonalire.content.index.lucene.LuceneForwardIndex;
import es.uam.eps.ir.sonalire.content.search.VSMSearchEngine;
import es.uam.eps.ir.sonalire.graph.Graph;
import org.jooq.lambda.tuple.Tuple3;
import org.ranksys.formats.parsing.Parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Class that loads tf-idf features from a content index.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class LuceneTfIdfFeaturesReader
{
    /**
     * Loads features from an index.
     * @param route     the index route.
     * @param graph     the graph containing the users to retrieve.
     * @param uParser   reads users from text.
     * @param <U>       the type of the users.
     * @return a stream containing the features.
     */
    public static <U> Stream<Tuple3<U,String,Double>> load(String route, Graph<U> graph, Parser<U> uParser)
    {
        List<Tuple3<U, String, Double>> features = new ArrayList<>();

        try
        {
            // First, load the index.
            LuceneForwardIndex<U> luceneIndex = new LuceneForwardIndex<>(route, uParser);
            long numUsers = graph.getVertexCount();

            // Then, for each user:
            graph.getAllNodes().forEach(u ->
            {
                try
                {
                    // Load the frequency vector
                    int uidx = luceneIndex.getContentId(u);
                    FreqVector fv = luceneIndex.getContentVector(uidx);
                    if(fv != null)
                    {
                        // Find the tf-idf coordinates for each of those vectors.
                        for (TermFreq freq : fv)
                        {
                            double tf = freq.getFreq() + 0.0;
                            String term = freq.getTerm();
                            double df = luceneIndex.getDocFreq(term);

                            // add to the features list.
                            features.add(new Tuple3<>(u, term, VSMSearchEngine.tfidf(tf, df, numUsers + 0.0)));
                        }
                    }
                }
                catch(IOException aux)
                {
                    // If something fails, consider that the user does not have any features.
                }
            });

            return features.stream();
        }
        catch(IOException ioe)
        {
            return Stream.empty();
        }
    }
}
