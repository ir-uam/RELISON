package es.uam.eps.ir.socialranksys.ecir2019;

/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import es.uam.eps.ir.ranksys.core.preference.PreferenceData;
import es.uam.eps.ir.ranksys.rec.runner.RecommenderRunner;
import es.uam.eps.ir.ranksys.rec.runner.fast.FastFilterRecommenderRunner;
import es.uam.eps.ir.ranksys.rec.runner.fast.FastFilters;
import es.uam.eps.ir.socialranksys.graph.Adapters;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.fast.FastDirectedUnweightedGraph;
import es.uam.eps.ir.socialranksys.graph.fast.FastDirectedWeightedGraph;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.io.graph.TextGraphReader;
import es.uam.eps.ir.socialranksys.links.data.GraphSimplePreferenceData;
import es.uam.eps.ir.socialranksys.links.data.updateable.index.fast.FastUpdateableGraphIndex;
import es.uam.eps.ir.socialranksys.links.data.updateable.preference.fast.updateable.FastUpdateablePreferenceData;
import es.uam.eps.ir.socialranksys.links.data.updateable.preference.fast.updateable.GraphSimpleUpdateableFastPreferenceData;
import es.uam.eps.ir.socialranksys.links.recommendation.SocialFastFilters;
import es.uam.eps.ir.socialranksys.links.recommendation.updateable.UpdateableRecommender;
import es.uam.eps.ir.socialranksys.links.recommendation.updateable.ir.UpdateableBM25;
import es.uam.eps.ir.socialranksys.links.recommendation.updateable.knn.sim.UpdateableGraphCosineSimilarity;
import es.uam.eps.ir.socialranksys.links.recommendation.updateable.knn.sim.UpdateableGraphSimilarity;
import es.uam.eps.ir.socialranksys.links.recommendation.updateable.knn.user.UpdateableUBkNN;
import es.uam.eps.ir.socialranksys.links.recommendation.updateable.mf.MFGraphUpdateableRecommender;
import es.uam.eps.ir.socialranksys.links.recommendation.updateable.mf.UpdateableFactorization;
import es.uam.eps.ir.socialranksys.links.recommendation.updateable.mf.UpdateableFactorizer;
import es.uam.eps.ir.socialranksys.links.recommendation.updateable.mf.als.HKVUpdateableFactorizer;
import org.jooq.lambda.tuple.Tuple3;
import org.ranksys.formats.parsing.Parsers;
import org.ranksys.formats.preference.SimpleRatingPreferencesReader;
import org.ranksys.formats.rec.RecommendationFormat;
import org.ranksys.formats.rec.TRECRecommendationFormat;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation.*;
import static org.ranksys.formats.parsing.Parsers.lp;


/**
 * Class for comparing the speed of several recommendation algorithms when we new 
 * users and edges are added to a graph.
 * @author Javier Sanz-Cruzado Puig
 */
public class SpeedComparisonUpdatingRecommendation
{
    /**
     * Computes the corresponding speed.
     * @param args <ul>
     * <li><b>Train:</b> Training data </li>
     * <li><b>Test:</b> Test data</li>
     * <li><b>Directed:</b> "true" if the graph is directed, "false" if not</li>
     * <li><b>Weighted:</b> "true" if the graph is directed, "false" if not</li>
     * <li><b>Max. Length:</b> maximum length of the recommendation ranking for a single user.</li>
     * <li><b>Num. Reps:</b> number of times each algorithm will be executed</li>
     * <li><b>Output folder:</b> the folder in which results will be stored (recommendations and times)</li>
     * </ul>
     * @throws IOException If something fails while reading.
     */
    public static void main(String[] args) throws IOException
    {
        if(args.length < 7)
        {
            System.err.println("Invalid arguments.");
            System.err.println("Usage:");
            System.err.println("\ttrain: Training data");
            System.err.println("\ttest: Test data");
            System.err.println("\tdirected: true if the graph is directed, false if not");
            System.err.println("\tweighted: true if the graph is weighted, false if not");
            System.err.println("\tmaxLength: maximum length of the recommendation ranking");
            System.err.println("\tnumReps: times each algorithm will be run");
            System.err.println("\toutputFile: folder in which to store the result");
            return;
        }
        
        // Parameter reading
        String trainDataPath = args[0];
        String testDataPath = args[1];
        boolean directed = args[2].equalsIgnoreCase("true");
        boolean weighted = args[3].equalsIgnoreCase("true");
        int maxLength = Parsers.ip.parse(args[4]);
        int numReps = Parsers.ip.parse(args[5]);
        String output = args[6];
        String algorithm = args[7];
        
        long timea = System.currentTimeMillis();
        
        // Read the graphs
        
        // Training graph
        TextGraphReader<Long> greader = new TextGraphReader<>(directed, weighted, true, "\t", lp);
        Graph<Long> auxgraph = greader.read(trainDataPath, weighted, false);
        FastGraph<Long> graph = (FastGraph<Long>) Adapters.removeAutoloops(auxgraph);

        // Test graph
        FastUpdateablePreferenceData<Long, Long> trainData = GraphSimpleUpdateableFastPreferenceData.load(graph);
        PreferenceData<Long, Long> testData = GraphSimplePreferenceData.load(SimpleRatingPreferencesReader.get().read(testDataPath, lp, lp), directed, weighted);
        
        long timeb = System.currentTimeMillis();
        System.out.println("Data read (" +(timeb-timea) + " ms.)");

        Random rng = new Random(0);
        for(int i = 0; i < numReps; ++i)
        {
            // Get all users
            assert graph != null;
            List<Long> users = graph.getAllNodes().sorted().collect(Collectors.toList());
            Collections.shuffle(users, rng);
            
            //Get the initial number of users (10%)
            int limit = Math.max((10*users.size())/100,1);



            // Configure the initial graph, containing only 10% users and edges between them.
            FastGraph<Long> initialgraph;
            Set<Long> targetUsers = new HashSet<>();

            if(algorithm.equalsIgnoreCase("bm25"))
            {
                initialgraph = new FastDirectedWeightedGraph<>();
            }
            else
            {
                initialgraph = new FastDirectedUnweightedGraph<>();
            }

            // Add the nodes.
            for(int k = 0;k < limit; ++k)
            {
                initialgraph.addNode(users.get(k));
                targetUsers.add(users.get(k));
            }

            // Add the edges
            initialgraph.getAllNodes().forEach(u ->
                graph.getAdjacentNodesWeights(u)
                    .filter(w -> targetUsers.contains(w.getIdx()))
                    .forEach(w -> initialgraph.addEdge(u,w.getIdx(),w.getValue())));

            FastUpdateablePreferenceData<Long, Long> data = GraphSimpleUpdateableFastPreferenceData.load(initialgraph);
            // Select the set of users to be recommended, the format, and the filters to apply to the recommendation
            String filename = output;

            filename += algorithm + "_" + i;

            try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename + ".txt"))))
            {
                bw.write("Iteration\tUpdate time\tRecomm. time");
                bw.write("\n"+ limit);

                long a = System.nanoTime();
                UpdateableRecommender<Long, Long> rec;

                // Initialize the recommender
                if(algorithm.equalsIgnoreCase("bm25"))
                {
                    rec = new UpdateableBM25<>(initialgraph, UND, IN, OUT, 0.1, 1);
                }
                else if(algorithm.equalsIgnoreCase("mf"))
                {
                    UpdateableFactorizer<Long, Long> factorizer = new HKVUpdateableFactorizer<>(150.0, x -> (1 + 40.0*x), 20);
                    UpdateableFactorization<Long, Long> factorization = factorizer.factorize(10, data);
                    rec = new MFGraphUpdateableRecommender<>(data, factorization, factorizer);
                }
                else if(algorithm.equalsIgnoreCase("ub"))
                {
                    UpdateableGraphSimilarity sim = new UpdateableGraphCosineSimilarity(initialgraph);
                    rec = new UpdateableUBkNN<>(data, sim, 10);
                }
                else
                {
                    System.err.println("ERROR: Invalid algorithm");
                    return;
                }
                long b = System.nanoTime();

                //List<Recommendation<Long,Long>> reclist = new ArrayList<>();

                bw.write("\t" + (b-a));
                FastUpdateableGraphIndex<Long> index = new FastUpdateableGraphIndex<>(initialgraph);
                Function<Long, IntPredicate> filter = FastFilters.and(FastFilters.notInTrain(data), FastFilters.notSelf(index), SocialFastFilters.notReciprocal(initialgraph, index));
                RecommenderRunner<Long,Long> runner = new FastFilterRecommenderRunner<>(index, index, targetUsers.stream(), filter, maxLength);

                // Generate the first recommendation
                a = System.nanoTime();
                runner.run(rec, recomm ->
                {

                });
                b = System.nanoTime();
                bw.write("\t" + (b-a) + "\n");
                RecommendationFormat<Long, Long> format = new TRECRecommendationFormat<>(lp, lp);

                // While there are more users, update the recommendations.
                for(int k = limit; k < graph.getVertexCount(); ++k)
                {
                    Long u = users.get(k);
                    targetUsers.add(u);
                    Stream<Tuple3<Long,Long,Double>> adjacent = graph.getAdjacentNodesWeights(u).filter(x -> targetUsers.contains(x.getIdx())).map(x -> new Tuple3<>(u,x.getIdx(),x.getValue()));
                    Stream<Tuple3<Long,Long,Double>> incident = graph.getIncidentNodesWeights(u).filter(x -> targetUsers.contains(x.getIdx())).map(x -> new Tuple3<>(x.getIdx(), u, x.getValue()));

                    Stream<Tuple3<Long,Long,Double>> stream = Stream.concat(adjacent, incident);

                    //reclist.clear();

                    a = System.nanoTime();
                    rec.updateAddUser(u);
                    rec.updateAddItem(u);
                    rec.update(stream);
                    b = System.nanoTime();
                    bw.write(k + "\t" + (b-a));
                    runner = new FastFilterRecommenderRunner<>(index, index, targetUsers.stream(), filter, maxLength);
                    a = System.nanoTime();
                    runner.run(rec, recomm ->
                    {

                    });
                    b = System.nanoTime();
                    bw.write("\t" + (b-a) + "\n");

                    if(k % 200 == 0 || k == graph.getVertexCount() - 1)
                    {
                        System.err.println("k=" + k);
                        /*try(RecommendationFormat.Writer<Long, Long> writer = format.getWriter(filename + k + ".txt"))
                        {
                            for(Recommendation<Long,Long> r : reclist)
                            {
                                writer.write(r);
                            }
                        } */
                    }
                }

                System.out.println("RECOMMENDER " + algorithm + "finished");

            }
            catch(IOException ioe)
            {
                System.err.println("ERROR");
                return;
            }

        }
    }
}
