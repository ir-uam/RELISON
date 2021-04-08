/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.recsys2018;

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.io.TextCommunitiesReader;
import es.uam.eps.ir.socialranksys.graph.Adapters;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.GlobalRerankerFunction;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.RerankerGridReader;
import es.uam.eps.ir.socialranksys.grid.links.recommendation.rerankers.RerankerGridSelector;
import es.uam.eps.ir.socialranksys.io.graph.TextGraphReader;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.GlobalReranker;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.normalizer.Normalizers;
import org.jooq.lambda.Unchecked;
import org.ranksys.formats.parsing.Parsers;
import org.ranksys.formats.rec.RecommendationFormat;
import org.ranksys.formats.rec.SimpleRecommendationFormat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.ranksys.formats.parsing.Parsers.lp;

/**
 * Program for reranking recommendation results.
 * @author Javier Sanz-Cruzado
 */
public class StructuralDiversityReranking
{
    /**
     * Generates rerankers.
     * @param args Execution arguments:
     * <ul>
     *  <li><b>Train:</b>Route to the training graph</li>
     *  <li><b>RecFile/Folder:</b>Route to the recommendation/recommendations to rerank</li>
     *  <li><b>Comm.File:</b>Route to a file containing the communities of the training graph</li>
     *  <li><b>Grid:</b> Route to the XML file containing the selection of rerankers to execute</li>
     *  <li><b>Cutoff:</b> Number of elements to maintain in the recommendation</li>
     *  <li><b>Directed:</b> True if the graph is directed, false if it is not</li>
     *  <li><b>Weighted:</b> True if the graph is weighted, false if it is not</li>
     *  <li><b>Normalize:</b> True if the scores have to be normalized, false if they do not have to</li>
     *  <li><b>Rank:</b> True if the normalization (if any) is rank-based (rank-sim) or score-based (min-max)</li>
     * </ul>
     */
    public static void main(String[] args)
    {
        if(args.length < 10)
        {
            System.err.println("Invalid arguments.");
            System.err.println("Usage:");
            System.err.println("\ttrain: Training graph");
            System.err.println("\trecFile/Folder: Route to the recommendation/recommendations to rerank");
            System.err.println("\tCommunities file: Route to a file containing the communities");
            System.err.println("\tOutput folder: Route in which to store the different rerankers");
            System.err.println("\tGrid: File containing the different rerankers to execute");
            System.err.println("\tCutoff: Number of recommended users to return");
            System.err.println("\tMax. length: the number of recommended users to consider.");
            System.err.println("\tDirected: Indicates if the graph is directed");
            System.err.println("\tNormalize: the normalization function: ranksim, zscore, minmax or none");
            return;
        }

        // Obtain the parameters.
        String trainDataPath = args[0];
        String recDataPath = args[1];
        String communities = args[2];
        String outputPath = args[3];
        String gridPath = args[4];
        int cutoff = Integer.parseInt(args[5]);
        int maxLength = Integer.parseInt(args[6]);
        boolean directed = args[7].equalsIgnoreCase("true");
        boolean weighted = args[8].equalsIgnoreCase("true");
        String normalizer = args[9];

        long timea = System.currentTimeMillis();

        // Read the training graph.
        TextGraphReader<Long> greader = new TextGraphReader<>(directed, weighted, false, "\t", Parsers.lp);
        Graph<Long> auxgraph = greader.read(trainDataPath, weighted, false);
        Graph<Long> graph = Adapters.removeAutoloops(auxgraph);

        // Read the community partition of the network.
        TextCommunitiesReader<Long> creader = new TextCommunitiesReader<>("\t",lp);
        Communities<Long> comms = creader.read(communities);
        long timeb = System.currentTimeMillis();
        System.out.println("Data read (" +(timeb-timea) + " ms.)");
        timea = System.currentTimeMillis();

        // Select the rerankers to apply
        RerankerGridReader gridreader = new RerankerGridReader(gridPath);
        gridreader.readDocument();
        Map<String, GlobalRerankerFunction<Long>> rerankerMap = new HashMap<>();
        gridreader.getRerankers().forEach(reranker -> 
        {
            RerankerGridSelector<Long> rgs = new RerankerGridSelector<>();
            rerankerMap.putAll(rgs.getRerankers(reranker, gridreader.getGrid(reranker)));
        });

        // Obtain the normalizer
        Supplier<Normalizer<Long>> norm = switch(normalizer.toLowerCase())
        {
            case "minmax" -> Normalizers.minmax();
            case "zscore" -> Normalizers.zscore();
            case "ranksim" -> Normalizers.ranksim();
            default -> Normalizers.noNorm();
        };
        
        
        List<String> recFiles = new ArrayList<>();
        File recs = new File(recDataPath);
        if(!recs.isDirectory()) // if it is a single file
        {
            recFiles.add(recDataPath);
        }
        else // if it is a list of recommenders
        {
            String[] list = recs.list();
            if(list != null)
            {
                for (String rec : list)
                {
                    recFiles.add(recDataPath + rec);
                }
            }
        }
        
        timeb = System.currentTimeMillis();
        System.out.println("Grid read (" +(timeb-timea) + " ms.)");

        
        // Perform the reranking.
        RecommendationFormat<Long,Long> format = new SimpleRecommendationFormat<>(lp, lp);
        recFiles.forEach(rec -> 
        {
            File f = new File(rec);
            if(!f.isDirectory())
            {
                String[] split = rec.split("/");
                //String[] split =  rec.split("\\Q\\\\E");
                String recName = split[split.length - 1];
                try
                {
                    System.out.println("Starting algorithm " + recName);

                    List<Recommendation<Long, Long>> recommendations = format.getReader(rec).readAll().collect(Collectors.toCollection(ArrayList::new));
                    rerankerMap.forEach((name, rerankerSupplier) ->
                    {
                        System.out.println("Running " + name);
                        String recOut = String.format("%s_%s", outputPath + recName, name + ".txt");
                        GlobalReranker<Long, Long> reranker = rerankerSupplier.apply(cutoff, norm, graph, comms);
                        try (RecommendationFormat.Writer<Long, Long> writer = format.getWriter(recOut))
                        {
                            long startTime = System.nanoTime();
                            reranker.rerankRecommendations(recommendations.stream(), maxLength)
                                    .forEach(Unchecked.consumer(writer::write));
                            long difference = System.nanoTime() - startTime;
                            System.out.println(name + ": " + TimeUnit.NANOSECONDS.toSeconds(difference) + "," + (TimeUnit.NANOSECONDS.toMillis(difference) - TimeUnit.NANOSECONDS.toSeconds(difference) * 1000) + " s.");
                        }
                        catch (IOException ioe)
                        {
                            ioe.printStackTrace();
                        }
                    });

                    System.out.println("Ending algorithm " + rec);

                }
                catch (IOException ex)
                {
                    ex.printStackTrace();
                }
            }
        });
        
    }
}
