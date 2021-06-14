/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.examples.content;

import es.uam.eps.ir.relison.content.index.AbstractIndexBuilder;
import es.uam.eps.ir.relison.content.index.individual.IndividualContentIndexBuilder;
import es.uam.eps.ir.relison.content.index.individual.WrapperIndividualContentIndexBuilder;
import es.uam.eps.ir.relison.content.index.lucene.LuceneForwardIndexBuilder;
import es.uam.eps.ir.relison.content.parsing.TextParser;
import es.uam.eps.ir.relison.content.parsing.ToLowerParser;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Class for generating a context index. In this index, each information piece appears as a document in the index.
 * This index can be used at the content-based recommender.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class InformationPieceIndexGenerator
{
    /**
     * Generates a content index.
     * @param args Execution arguments
     * <ol>
     *     <li><b>information pieces:</b> a file containing the information pieces to process.</li>
     *     <li><b>header:</b> true if the information pieces file has header, false otherwise</li>
     *     <li><b>index route:</b> directory in which to store the index.</li>
     * </ol>
     * @throws IOException if something goes wrong while reading the contents file / creating the index.
     */
    public static void main(String[] args) throws IOException
    {
        if(args.length < 3)
        {
            System.err.println("Invalid arguments");
            System.err.println("Usage:");
            System.err.println("\tinformation pieces: a file containing the information pieces published by each user.");
            System.err.println("\theader: true if the information pieces file has a header, false otherwise.");
            System.err.println("\tindex route: directory in which to store the index.");
            return;
        }

        String infoRoute = args[0];
        boolean header = args[1].equalsIgnoreCase("true");
        String indexRoute = args[2];

        // Create the index.
        AbstractIndexBuilder<Long> index = new LuceneForwardIndexBuilder<>();
        IndividualContentIndexBuilder<Long, Long> builder = new WrapperIndividualContentIndexBuilder<>(index);
        builder.init(indexRoute);
        TextParser tparser = new ToLowerParser();

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

            String tweetIdStr = record.get("infoId");
            String userIdStr = record.get("userId");
            String tweet = record.get("text");

            long tweetId = Long.parseLong(tweetIdStr);
            long userId = Long.parseLong(userIdStr);

            builder.indexText(tweet, tweetId, userId);
        }
        
        long b = System.currentTimeMillis();
        System.out.println("All pieces indexed (" + (b-a) + " ms.)" );

        builder.close(indexRoute);
        b = System.currentTimeMillis();
        System.out.println("Index created (" + (b-a) + " ms.)");
    }
}
