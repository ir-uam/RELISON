/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.standalone.contentbased;

import es.uam.eps.ir.socialranksys.content.TextVector;
import es.uam.eps.ir.socialranksys.content.index.ContentIndex;
import es.uam.eps.ir.socialranksys.content.index.exceptions.WrongModeException;
import es.uam.eps.ir.socialranksys.content.index.weighting.WeightingScheme;
import es.uam.eps.ir.socialranksys.content.similarities.ContentSimilarity;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Content-based contact recommendation algorithm, based on a TF-IDF scheme.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the content identifiers.
 */
public class CentroidIRRecommender<U,I> extends UserFastRankingRecommender<U>
{
    /**
     * Centroids for each user.
     */
    private final Map<U, TextVector> centroids;
    /**
     * Similarity function for comparing users
     */
    private final ContentSimilarity sim;
    /**
     * Constructor. Takes some of the neighborhoods
     * @param graph Graph.
     * @param cIndex Content index.
     * @param weighting Weighting scheme
     * @param uSel Information pieces selection (incoming neighborhood, outgoing, or all of them, including own pieces)
     * @param sim Similarity function for comparing items.
     */
    public CentroidIRRecommender(FastGraph<U> graph, ContentIndex<U,I> cIndex, WeightingScheme weighting, EdgeOrientation uSel, ContentSimilarity sim)
    {
        super(graph);
        centroids = new HashMap<>();
        cIndex.setReadMode();

        this.sim = sim;
        Map<U, Object2DoubleMap<String>> weights = new HashMap<>();
        uIndex.getAllUsers().forEach(u -> 
        {
            Object2DoubleMap<String> map = new Object2DoubleOpenHashMap<>();
            map.defaultReturnValue(0.0);
            weights.put(u, map);
        });
        
        // Build the centroids
        try 
        {
            // Obtain all the terms
            cIndex.getAllTerms().forEach(term ->
            {
                try 
                {
                    // For each term, obtain all the documents containing that term
                    cIndex.getContents(term, weighting).forEach(doc ->
                    {
                        // Create the centroids
                        U user = doc.getCreator();
                        
                        if(graph.containsVertex(user))
                        {
                            graph.getNeighbourhood(user, uSel.invertSelection()).forEach(u -> 
                            {
                                Object2DoubleMap<String> centroid = weights.get(u);

                                if(centroid.containsKey(term))
                                    centroid.put(term, centroid.getDouble(term) + doc.getScore());
                                else
                                    centroid.put(term, doc.getScore());
                            });
                        }
                        
                        // In case we select the undirected neighborhood, we add the users' neighborhood
                        if(uSel.equals(EdgeOrientation.UND))
                        {
                            Object2DoubleMap<String> centroid = weights.get(user);
                            if(centroid == null)
                            {
                                Object2DoubleMap<String> map = new Object2DoubleOpenHashMap<>();
                                map.defaultReturnValue(0.0);
                                weights.put(user, map);
                                centroid = weights.get(user);
                            }

                            if(centroid.containsKey(term))
                                centroid.put(term, centroid.getDouble(term) + doc.getScore());
                            else
                                centroid.put(term, doc.getScore());
                        }
                    });
                }
                catch (WrongModeException ex)
                {
                    System.err.println("Bad configured index.");
                }
            });

            uIndex.getAllUsers().forEach(u -> 
            {
                double module = Math.sqrt(weights.get(u).values().stream().mapToDouble(v -> v*v).sum());
                centroids.put(u, new TextVector(weights.get(u), module));
            });

        } 
        catch (WrongModeException ex) 
        {
                System.err.println("Bad configured index.");
        }
    }
    
    /**
     * Constructor. Takes some of the neighborhoods
     * @param graph Graph.
     * @param cIndex Content index.
     * @param weighting Weighting scheme
     * @param sim Similarity function for comparing items.
     */
    public CentroidIRRecommender(FastGraph<U> graph, ContentIndex<U,I> cIndex, WeightingScheme weighting, ContentSimilarity sim)
    {
        super(graph);
        centroids = new HashMap<>();
        cIndex.setReadMode();
        this.sim = sim;
        Map<U, Object2DoubleMap<String>> weights = new HashMap<>();
        uIndex.getAllUsers().forEach(u -> 
        {
            Object2DoubleMap<String> map = new Object2DoubleOpenHashMap<>();
            map.defaultReturnValue(0.0);
            weights.put(u, map);
        });
        
        try 
        {
            cIndex.getAllTerms().forEach(term ->
            {
                try 
                {
                    cIndex.getContents(term, weighting).forEach(doc ->
                    {
                        U user = doc.getCreator();
                        
                        Object2DoubleMap<String> centroid = weights.get(user);
                        
                        if(centroid == null)
                        {
                            Object2DoubleMap<String> map = new Object2DoubleOpenHashMap<>();
                            map.defaultReturnValue(0.0);
                            weights.put(user, map);
                            centroid = weights.get(user);
                        }
                        
                        if(centroid.containsKey(term))
                            centroid.put(term, centroid.getDouble(term) + doc.getScore());
                        else
                            centroid.put(term, doc.getScore());
                    });
                }
                catch (WrongModeException ex) 
                {
                    System.err.println("Bad configured index.");
                }
            });

            uIndex.getAllUsers().forEach(u -> 
            {
                double module = Math.sqrt(weights.get(u).values().stream().mapToDouble(v -> v*v).sum());
                centroids.put(u, new TextVector(weights.get(u), module));
            });
        }
        catch (WrongModeException ex) 
        {
                System.err.println("Bad configured index.");
        }
    }
    
    
    @Override
    public Int2DoubleMap getScoresMap(int uidx) 
    {
        Int2DoubleMap scores = new Int2DoubleOpenHashMap();
        
        U u = this.uidx2user(uidx);
        this.iIndex.getAllIidx().forEach(iidx -> 
        {
            U v = this.iidx2item(iidx);
            scores.put(iidx, sim.similarity(centroids.get(u),centroids.get(v)));
        });
        
        return scores;
    }

    
}
