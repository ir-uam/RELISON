/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.somepeas2019;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.socialranksys.community.io.CommunitiesWriter;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.grid.community.CommunityDetectionParamReader;
import es.uam.eps.ir.socialranksys.grid.community.CommunityDetectionSelector;
import es.uam.eps.ir.socialranksys.io.graph.TextGraphReader;
import org.ranksys.formats.parsing.Parsers;

/**
 * Executes several community detection algorithms over an individual graph.
 * @author Javier Sanz-Cruzado Puig
 */
public class CommunityDetector
{
    /**
     * Computes a community partition for several algorithms.
     * @param args Execution arguments
     * <ul>
     * <li><b>Graph:</b> The network graph</li>
     * <li><b>Directed:</b> true if the network is directed, false if it is not.</li>
     * <li><b>Weighted:</b> true if the network is weighted, false if it is not.</li>
     * <li><b>Grid:</b> XML file containing the configuration of the community detection algorithms</li>
     * <li><b>Output:</b> Directory in which to store the communities</li>
     * </ul>
     */
    public static void main(String[] args)
    {
        if(args.length < 5)
        {
            System.err.println("ERROR: Invalid arguments");
            System.err.println("Usage: <graph> <directed> <weighted> <grid> <output>");
            return;
        }
        String graphRoute = args[0];
        boolean directed = args[1].equalsIgnoreCase("true");
        boolean weighted = args[2].equalsIgnoreCase("false");
        String grid = args[3];
        String outputRoute = args[4];


        // Read the graph

        TextGraphReader<Long> greader = new TextGraphReader<>(directed, weighted, false, "\t", Parsers.lp);
        Graph<Long> graph = greader.read(graphRoute, weighted, false);

        // Grid Reader
        CommunityDetectionParamReader cdReader = new CommunityDetectionParamReader(grid);
        cdReader.readDocument();

        cdReader.getAlgorithms().parallelStream().forEach(algorithm ->
        {
            System.out.println("Algorithm " + algorithm + " started.");
            Long a = System.currentTimeMillis();
            CommunityDetectionSelector<Long> selector = new CommunityDetectionSelector<>();
            CommunityDetectionAlgorithm<Long> alg = selector.getCommunityDetectionAlgorithm(algorithm, cdReader.getParameters(algorithm)).v2();
            Communities<Long> comm = alg.detectCommunities(graph);
            CommunitiesWriter<Long> cwriter = new CommunitiesWriter<>();
            cwriter.write(comm, outputRoute + algorithm + ".txt", "\t");
            Long b = System.currentTimeMillis();
            System.out.println("Algorithm " + algorithm + " finished (" + (b-a) + " ms.)");
        });
    }
}
