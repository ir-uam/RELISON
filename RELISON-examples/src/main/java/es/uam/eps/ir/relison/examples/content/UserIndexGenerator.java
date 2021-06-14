/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.examples.content;

import es.uam.eps.ir.relison.content.index.IndexBuilder;
import es.uam.eps.ir.relison.content.index.lucene.LuceneForwardIndexBuilder;
import es.uam.eps.ir.relison.content.parsing.TextParser;
import es.uam.eps.ir.relison.content.parsing.ToLowerParser;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.io.graph.TextGraphReader;
import es.uam.eps.ir.relison.io.graph.TextMultiGraphReader;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.ranksys.formats.parsing.Parsers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static es.uam.eps.ir.relison.examples.AuxiliarVariables.TRUE;

/**
 * Class for generating a content index. In this index, each user is represented as a single document.
 * This type of index is used, for instance, for the Twittomender recommender.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class UserIndexGenerator
{
    /**
     * Program for generating a content index.
     * @param args execution arguments
     * <ol>
     *     <li><b>graph:</b> The route to the social network graph.</li>
     *      <li><b>multigraph:</b> true if the graph has multiple edges between users, false otherwise.</li>
     *      <li><b>directed:</b> true if the graph is directed, false otherwise.</li>
     *      <li><b>weighted:</b> true if the graph is weighted, false otherwise.</li>
     *      <li><b>selfLoops:</b> true if the graph accepts selfloops, false otherwise.</li>
     *     <li><b>information pieces:</b> A file containing the information contents published by each user.</li>
     *     <li><b>header:</b> true if the file has header, false otherwise</li>
     *     <li><b>orientation: </b> Selection of pieces: IN for the pieces of the incoming neighbors, OUT for the outgoing ones, UND for both, "own" for only the users' pieces</li>
     *     <li><b>index route:</b> directory in which to store the index.</li>
     * </ol>
     * @throws IOException if something goes wrong while reading the contents file / creating the index.
     */
    public static void main(String[] args) throws IOException
    {
        if(args.length < 10)
        {
            System.err.println("Invalid arguments");
            System.err.println("Usage:");
            System.err.println("\tgraph: the route to the social network graph");
            System.err.println("\tmultigraph: true if the graph has multiple edges between users, false otherwise.");
            System.err.println("\tdirected: true if the graph is directed, false otherwise.");
            System.err.println("\tweighted: true if the graph is weighted, false otherwise.");
            System.err.println("\tselfLoops: true if the graph accepts selfloops, false otherwise.");
            System.err.println("\tinformation pieces: a file containing the information pieces published by each user.");
            System.err.println("\theader: true if the information pieces file has a header, false otherwise.");
            System.err.println("\torientation: for selecting the set of pieces we are building the index with:");
            System.err.println("\t\town: if we represent each user with his/her own pieces.");
            System.err.println("\t\tIN: if we represent each user with the pieces of his/her incoming neighbors.");
            System.err.println("\t\tOUT: if we represent each user with the pieces of his/her outgoing neighbors.");
            System.err.println("\t\tUND: if we represent each user with the pieces of both his incoming and outgoing neighbors.");
            System.err.println("\t\tMUTUAL: if we represent each user with the pieces of his mutual neighbors (users belonging to both incoming and outgoing neighbors)");
            System.err.println("\tindex route: directory in which to store the index.");
            return;
        }

        // Read the parameters.
        String graphRoute = args[0];
        boolean multigraph = args[1].equalsIgnoreCase(TRUE);
        boolean directed = args[2].equalsIgnoreCase(TRUE);
        boolean weighted = args[3].equalsIgnoreCase(TRUE);
        boolean selfLoops = args[4].equalsIgnoreCase(TRUE);

        String infoRoute = args[5];
        boolean header = args[6].equalsIgnoreCase("true");
        boolean onlyOwn = args[7].equals("own");
        EdgeOrientation orient = onlyOwn ? EdgeOrientation.UND : EdgeOrientation.valueOf(args[8]);
        String indexRoute = args[9];

        // Create the Twittomender Index.
        IndexBuilder<Long> index = new LuceneForwardIndexBuilder<>();
        index.init(indexRoute);
        TextParser tparser = new ToLowerParser();

        // Read the graph.
        TextGraphReader<Long> greader = (multigraph) ? new TextMultiGraphReader<>(directed, weighted, selfLoops, "\t", Parsers.lp) : new TextGraphReader<>(directed, weighted, selfLoops, "\t", Parsers.lp);
        Graph<Long> graph = greader.read(graphRoute, weighted, false);
        Map<Long, String> docs = new HashMap<>();
        graph.getAllNodes().forEach(u -> docs.put(u, ""));

        // Prepare the user generated contents:
        // FORMAT OF THE FILE (TSV): infoID, userID, text, repropagatedCount, likeCount, created, truncated:
        File f = new File(infoRoute);
        CSVFormat format = CSVFormat.TDF.withHeader("infoId","userId","text","reprCount","likeCount","created","truncated");
        CSVParser parser = CSVParser.parse(f, StandardCharsets.UTF_8, format);
        
        int j = 0;
        
        long a = System.currentTimeMillis();
        // Obtain the different data records.
        for(CSVRecord record : parser.getRecords())
        {
            if(j == 0 && header)
            {
                ++j;
                continue;
            }
            
            String userIdStr = record.get("userId");
            String tweet = record.get("text");
            
            long userId = Long.parseLong(userIdStr);
            docs.put(userId, docs.get(userId) + " " + tweet);
        }
        
        long b = System.currentTimeMillis();
        System.out.println("All information pieces read (" + (b-a) + " ms.)" );
        List<Long> nodes = graph.getAllNodes().collect(Collectors.toCollection(ArrayList::new));
        j = 0;

        for(long node : nodes)
        {
            if(j % 100 == 0)
            {
                b = System.currentTimeMillis();
                System.out.println("User read: " + j + " (" + (b-a) + " ms.)");
            }

            StringBuilder builder = new StringBuilder();
            if(orient.equals(EdgeOrientation.UND))
            {
                builder.append(docs.get(node));
            }

            if(!onlyOwn)
            {
                builder.append(graph.getNeighbourhood(node, orient).map(docs::get)
                    .reduce(" ", (x,y) -> x + " " + y));
            }

            String text = tparser.parse(builder.toString());

            index.indexText(text, node);
            ++j;
        }

        index.close(indexRoute);
        b = System.currentTimeMillis();
        System.out.println("Index created (" + (b-a) + " ms.)");
    }
}
