/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.examples.content;

import es.uam.eps.ir.sonalire.content.index.IndexBuilder;
import es.uam.eps.ir.sonalire.content.index.lucene.LuceneForwardIndexBuilder;
import es.uam.eps.ir.sonalire.content.parsing.TextParser;
import es.uam.eps.ir.sonalire.content.parsing.ToLowerParser;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.graph.edges.EdgeOrientation;
import es.uam.eps.ir.sonalire.io.graph.TextGraphReader;
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

/**
 * Class for generating Twittomender indexes.
 * @author Javier Sanz-Cruzado Puig
 */
public class TwittomenderIndexGenerator 
{
    /**
     * Generates a Twittomender index
     * @param args Execution arguments
     * <ol>
     *     <li><b>Training graph:</b> File containing a training network.</li>
     *     <li><b>Information pieces:</b> The set of contents published by each user.</li>
     *     <li><b>Header:</b> true if the file has header, false otherwise</li>
     *     <li><b>Orientation: </b> Selection of pieces: IN for the pieces of the incoming neighbors, OUT for the outgoing ones, UND for both, "own" for only the users' pieces</li>
     *     <li><b>Index route:</b> Folder in which to store the index.</li>
     * </ol>
     * @throws IOException if something goes wrong while reading the contents file / creating the index.
     */
    public static void main(String[] args) throws IOException
    {
        if(args.length < 3)
        {
            System.err.println("Invalid arguments");
            System.err.println("Usage: <trainGraph> <info> <orientation> <indexRoute>");
            return;
        }

        // Read the parameters.
        String trainGraphRoute = args[0];
        String infoRoute = args[1];
        boolean header = args[2].equalsIgnoreCase("true");
        boolean onlyOwn = args[3].equals("own");
        EdgeOrientation orient = onlyOwn ? EdgeOrientation.UND : EdgeOrientation.valueOf(args[3]);
        String indexRoute = args[4];

        // Create the Twittomender Index.
        IndexBuilder<Long> index = new LuceneForwardIndexBuilder<>();
        index.init(indexRoute);
        TextParser tparser = new ToLowerParser();

        // Read the graph.
        TextGraphReader<Long> greader = new TextGraphReader<>(true, false, false, "\t", Parsers.lp);
        Graph<Long> graph = greader.read(trainGraphRoute, false, false);
        Map<Long, String> docs = new HashMap<>();
        graph.getAllNodes().forEach(u -> docs.put(u, ""));

        // Prepare the user generated contents for Twitter:
        File f = new File(infoRoute);
        CSVFormat format = CSVFormat.TDF.withHeader("tweetId","userId","text","retweetCount","favoriteCount","created","truncated");
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
        System.out.println("Tweets read (" + (b-a) + " ms.)" );
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
