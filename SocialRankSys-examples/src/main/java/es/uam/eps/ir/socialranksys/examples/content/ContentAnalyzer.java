/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.examples.content;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.io.graph.TextGraphReader;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.ranksys.formats.parsing.Parsers;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for generating Twittomender indexes.
 * @author Javier Sanz-Cruzado Puig
 */
public class ContentAnalyzer
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
        String indexRoute = args[3];

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

        Long2DoubleOpenHashMap counter = new Long2DoubleOpenHashMap();
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
            long userId = Long.parseLong(userIdStr);
            counter.put(userId, counter.get(userId)+1);
        }
        
        long b = System.currentTimeMillis();
        System.out.println("Counted indexed (" + (b-a) + " ms.)" );

        int total = 0;
        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(indexRoute))))
        {
            bw.write("user\tindegree\tnumtweets");
            for(long user : counter.keySet())
            {
                bw.write("\n"+user + "\t" + graph.inDegree(user) + "\t" + counter.get(user));
                total += counter.get(user);
            }
        }
        System.out.println("Total tweets: " + total);

        b = System.currentTimeMillis();
        System.out.println("Index created (" + (b-a) + " ms.)");
    }
}
