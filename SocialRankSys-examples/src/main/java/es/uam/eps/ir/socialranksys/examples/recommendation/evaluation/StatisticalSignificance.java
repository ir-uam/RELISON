/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.examples.recommendation.evaluation;

import es.uam.eps.ir.ranksys.core.preference.PreferenceData;
import es.uam.eps.ir.ranksys.metrics.RecommendationMetric;
import es.uam.eps.ir.ranksys.metrics.basic.NDCG;
import es.uam.eps.ir.ranksys.metrics.basic.Precision;
import es.uam.eps.ir.ranksys.metrics.basic.Recall;
import es.uam.eps.ir.ranksys.metrics.rel.BinaryRelevanceModel;
import es.uam.eps.ir.socialranksys.links.data.GraphSimplePreferenceData;
import es.uam.eps.ir.socialranksys.links.recommendation.metrics.accuracy.TRECAveragePrecision;
import org.apache.commons.math3.stat.inference.TTest;
import org.ranksys.formats.parsing.Parsers;
import org.ranksys.formats.preference.SimpleRatingPreferencesReader;
import org.ranksys.formats.rec.RecommendationFormat;
import org.ranksys.formats.rec.SimpleRecommendationFormat;
import org.ranksys.formats.rec.TRECRecommendationFormat;

import java.io.*;
import java.util.*;

import static org.ranksys.formats.parsing.Parsers.lp;

/**
 * Given some recommendations, computes the statistical significance between them.
 * @author Javier Sanz-Cruzado Puig
 */
public class StatisticalSignificance
{
    /**
     * Computes the p-values of different recommendations in terms of the precision, and writes
     * them to a file.
     * @param args <ul>
     *              <li><b>Test data path:</b> Route to the test data</li>
     *              <li><b>Rec. Path:</b> Path to the directory which contains all the recommendations</li>
     *              <li><b>Output:</b> File in which to store the statistical significance results</li>
     *              <li><b>Cutoff:</b> Number of items to consider in the recommendation</li>
     *              <li><b>Num. tails:</b> Number of tails to consider</li>
     *              <li><b>Directed:</b> True if the graph is directed, false if it is not</li>
     *              <li><b>Precision:</b> p if we want to check the statistical significance of precision, r if we want recall, ndcg if we want nDCG</li>
     *             </ul>
     * @throws IOException If something goes wrong while reading files
     */
    public static void main(String[] args) throws IOException
    {
        // Parameters
        if(args.length < 7)
        {
            System.err.println("Error: Invalid arguments ");
            for(int i = 0; i < args.length; ++i)
                System.err.print("\t" + i+":" + args[i]);

            System.err.println("Usage: testDataPath recPath outputPath cutoff threshold numTails directed format");
            return;
        }

        String testDataPath = args[0];
        String recPath = args[1];
        String outputPath = args[2];
        int cutoff = Parsers.ip.parse(args[3]);
        double threshold = 0.5;

        int numTails = Parsers.ip.parse(args[4]);
        if(numTails < 1 || numTails > 2)
        {
            System.err.println("Error: Invalid arguments: numTails must be 1 or 2 ");
            for(int i = 0; i < args.length; ++i)
                System.err.print("\t" + i+":" + args[i]);

            System.err.println("Usage: testDataPath recPath outputPath cutoff threshold numTails");
            return;
        }

        boolean directed = args[5].equalsIgnoreCase("true");
        String prec = args[6];
        String formatax = args[7];
        // Read train and test data
        PreferenceData<Long, Long> testData = GraphSimplePreferenceData.load(SimpleRatingPreferencesReader.get().read(testDataPath, lp, lp), directed, false);

        Map<Long, Integer> userList = new HashMap<>();
        testData.getUsersWithPreferences().forEach(user -> userList.put(user, userList.size()));

        // Relevance models definition
        BinaryRelevanceModel<Long, Long> binRel = new BinaryRelevanceModel<>(false, testData, threshold);

        // Configuration of the P@k metric
        RecommendationMetric<Long, Long> metric;

        String metr = prec.toLowerCase();
        switch(metr)
        {
            case "p":
                metric = new Precision<>(cutoff, binRel);
                break;
            case "r":
                metric = new Recall<>(cutoff, binRel);
                break;
            case "ndcg":
                metric = new NDCG<>(cutoff, new NDCG.NDCGRelevanceModel<>(false, testData, threshold));
                break;
            case "map":
                metric = new TRECAveragePrecision<>(cutoff, binRel);
                break;
            default:
                return;
        }

        Map<String, double[]> values = new HashMap<>();

        File directory = new File(recPath);
        if(!directory.isDirectory() || directory.list() == null || directory.list().length == 0)
        {
            System.err.println("Nothing to evaluate!");
        }

        
        // FILE FORMAT:
        // "user"\"Algorithm1"\"Algorithm2"\t...\t"AlgorithmN".
        // user\tmetricAlg1\tmetricAlg2\tmetricAlg3\t...\tmetricAlgN
        RecommendationFormat<Long, Long> format = formatax.equals("trec") ? new TRECRecommendationFormat<>(lp, lp) : new SimpleRecommendationFormat<>(lp, lp);
        String[] files = directory.list();
        
        List<String> recomms = new ArrayList<>();
        if(files == null)
        {
            System.err.println("Nothing to evaluate!");
            return;
        }
        for(String file : files)
        {           
            values.put(file, new double[userList.size()]);
            Map<Long, Double> indiv = new HashMap<>();
            format.getReader(recPath + file).readAll().forEach(rec -> 
            {
                if(userList.containsKey(rec.getUser()))
                {
                    double val = metric.evaluate(rec);
                    indiv.put(rec.getUser(), val);
                }
            });
            
            userList.forEach((key, value) -> values.get(file)[value] = indiv.getOrDefault(key, 0.0));
            
            recomms.add(file);
        }
        
        recomms.sort(Comparator.naturalOrder());
       
        // Compute the p-values.
        TTest ttest = new TTest();
        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath))))
        {
            for(String file : recomms)
            {
                bw.write("\t" + file);
            }
            bw.write("\n");
            for(String f1 : recomms)
            {
                bw.write(f1);
                
                for(String f2 : recomms)
                {
                    if(f1.equals(f2))
                    {
                        bw.write("\n");
                        break;
                    }
                    else
                    {
                        double value = ttest.pairedTTest(values.get(f1), values.get(f2));
                        if(Double.isNaN(value))
                            value = 1.0;
                        else if(numTails == 1)
                            value /= 2.0;
                        bw.write("\t" + value);
                    }
                }
            }
        }
    }
}
