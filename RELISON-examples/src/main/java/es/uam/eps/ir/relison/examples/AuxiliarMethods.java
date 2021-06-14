/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.examples;

import com.esotericsoftware.yamlbeans.YamlReader;
import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.ranksys.metrics.SystemMetric;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.ranksys.rec.runner.RecommenderRunner;
import org.jooq.lambda.tuple.Tuple2;
import org.ranksys.formats.parsing.Parsers;
import org.ranksys.formats.rec.RecommendationFormat;
import org.ranksys.formats.rec.SimpleRecommendationFormat;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Class containing auxiliar methods for the Main functions.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class AuxiliarMethods
{
    /**
     * Reads a Yaml file
     * @param file the route to the file
     * @return a map containing the root of the Yaml file
     * @throws IOException if something fails while reading the file.
     */
    public static Map<String, Object> readYAML(String file) throws IOException
    {
        Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
        YamlReader yamlReader = new YamlReader(reader);
        return (Map<String, Object>) yamlReader.read();
    }

    /**
     * Computes a recommendation and evaluates it using nDCG metric.
     *
     * @param output      Route of the file in which to store the recommendation.
     * @param recommender The recommender to apply.
     * @param runner      The recommender runner
     * @param metric      The metric.
     *
     * @return the value of the metric.
     *
     * @throws IOException if something fails during the writing / reading of the recommendation file.
     */
    public static double computeAndEvaluate(String output, Recommender<Long, Long> recommender, RecommenderRunner<Long, Long> runner, SystemMetric<Long, Long> metric) throws IOException
    {
        RecommendationFormat<Long, Long> format = new SimpleRecommendationFormat<>(Parsers.lp, Parsers.lp);
        RecommendationFormat.Writer<Long, Long> writer;
        RecommendationFormat.Reader<Long, Long> reader;

        metric.reset();

        writer = format.getWriter(output);
        runner.run(recommender, writer);
        writer.close();

        reader = format.getReader(output);
        reader.readAll().forEach(rec ->
        {
            if (rec != null && rec.getItems() != null && !rec.getItems().isEmpty())
            {
                metric.add(rec);
            }
        });
        return metric.evaluate();
    }

    /**
     * Writes and evaluates a recommendation.
     *
     * @param output      Route of the file in which to store the recommendation.
     * @param recs        the recommendations to read and evaluate.
     * @param metrics     the metrics.
     *
     * @return the value of the metric.
     *
     * @throws IOException if something fails during the writing / reading of the recommendation file.
     */
    public static Map<String, Double> writeAndEvaluate(String output, Stream<Recommendation<Long,Long>> recs, Map<String, SystemMetric<Long, Long>> metrics) throws IOException
    {
        RecommendationFormat<Long, Long> format = new SimpleRecommendationFormat<>(Parsers.lp, Parsers.lp);
        RecommendationFormat.Writer<Long, Long> writer = format.getWriter(output);
        RecommendationFormat.Reader<Long, Long> reader;

        Map<String, Double> values = new HashMap<>();

        boolean wentright = recs.map(recommendation ->
        {
            try
            {
                writer.write(recommendation);
                return true;
            }
            catch (IOException e)
            {
                return false;
            }
        }).reduce(true, (x,y) -> x && y);

        if(!wentright)
        {
            throw new IOException("Something failed while writing the recommendation in file " + output);
        }

        reader = format.getReader(output);
        reader.readAll().forEach(rec ->
        {
            if (rec != null && rec.getItems() != null && !rec.getItems().isEmpty())
            {
                metrics.values().forEach(metric -> metric.add(rec));
            }
        });

        metrics.forEach((key, value) -> values.put(key, value.evaluate()));
        return values;
    }



    /**
     * Computes a recommendation and evaluates it using metrics.
     *
     * @param output      Route of the file in which to store the recommendation.
     * @param recommender The recommender to apply.
     * @param runner      The recommender runner
     * @param metrics      The metrics to evaluate
     *
     * @return the value of the metrics.
     *
     * @throws IOException if something fails during the writing / reading of the recommendation file.
     */
    public static Map<String, Double> computeAndEvaluate(String output, Recommender<Long, Long> recommender, RecommenderRunner<Long, Long> runner, Map<String, SystemMetric<Long, Long>> metrics) throws IOException
    {
        RecommendationFormat<Long, Long> format = new SimpleRecommendationFormat<>(Parsers.lp, Parsers.lp);
        RecommendationFormat.Writer<Long, Long> writer;
        RecommendationFormat.Reader<Long, Long> reader;

        Map<String, Double> values = new HashMap<>();

        File f = new File(output);
        if(f.exists()) // Recover the all recommendation.
        {
            try
            {
                reader = format.getReader(output);
                Stream<Recommendation<Long, Long>> stream = reader.readAll();
            }
            catch(Exception ioe) // if something fails while reading, execute again:
            {
                writer = format.getWriter(output);
                runner.run(recommender, writer);
                writer.close();
            }
        }
        else
        {
            writer = format.getWriter(output);
            runner.run(recommender, writer);
            writer.close();
        }

        reader = format.getReader(output);
        reader.readAll().forEach(rec ->
        {
            if (rec != null && rec.getItems() != null && !rec.getItems().isEmpty())
            {
                metrics.values().forEach(metric -> metric.add(rec));
            }
        });

        metrics.forEach((key, value) -> values.put(key, value.evaluate()));
        return values;
    }

    /**
     * Computes a recommendation and evaluates it using nDCG metric. It does not write the recommendation.
     *
     * @param recommender The recommender to apply.
     * @param runner      The recommender runner.
     * @param metric      The metric.
     *
     * @return the value of the metric.
     */
    public static double computeAndEvaluate(Recommender<Long, Long> recommender, RecommenderRunner<Long, Long> runner, SystemMetric<Long, Long> metric)
    {
        EmptyWriter<Long, Long> writer = new EmptyWriter<>();
        runner.run(recommender, writer);

        metric.reset();
        writer.readAll().forEach(rec ->
        {
            if (rec != null && rec.getItems() != null && !rec.getItems().isEmpty())
            {
                metric.add(rec);
            }
        });

        return metric.evaluate();
    }

    /**
     * Computes a recommendation and evaluates it using some metrics. It does not write the recommendation.
     *
     * @param recommender The recommender to apply.
     * @param runner      The recommender runner.
     * @param metrics      The metrics.
     *
     * @return the value of the metrics.
     */
    public static Map<String, Double> computeAndEvaluate(Recommender<Long, Long> recommender, RecommenderRunner<Long, Long> runner, Map<String, SystemMetric<Long, Long>> metrics)
    {
        EmptyWriter<Long, Long> writer = new EmptyWriter<>();
        runner.run(recommender, writer);

        Map<String, Double> values = new HashMap<>();
        metrics.values().forEach(SystemMetric::reset);
        writer.readAll().forEach(rec ->
        {
            if (rec != null && rec.getItems() != null && !rec.getItems().isEmpty())
            {
                metrics.values().forEach(metric -> metric.add(rec));
            }
        });

        metrics.forEach((key, value) -> values.put(key, value.evaluate()));
        return values;
    }

    /**
     * Given two maps with the same keys, generates a new file that prints the nDCG values for both.
     *
     * @param output    The output file.
     * @param first     the first map.
     * @param second    the second map.
     * @param firstId   identifier for the first map.
     * @param secondId  identifier for the second map.
     * @param maxLength maximum length of the recommendation.
     */
    public static void printFile(String output, Map<String, Double> first, Map<String, Double> second, String firstId, String secondId, int maxLength)
    {
        List<Tuple2<String, Double>> list = new ArrayList<>();
        int numVariants = 0;
        for (String variant : first.keySet())
        {
            double val = first.get(variant) - second.get(variant);
            list.add(new Tuple2<>(variant, val));
            ++numVariants;
        }

        list.sort((o1, o2) ->
        {
            int val = Double.compare(o1.v2, o2.v2);
            if (val == 0)
                return o1.v1.compareTo(o2.v1);
            return val;
        });

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output))))
        {
            bw.write("Variant\t%\tnDCG@" + maxLength + "(" + firstId + ")\tnDCG@" + maxLength + "(" + secondId + ")\tDifference");
            int i = 0;
            for (Tuple2<String, Double> tuple : list)
            {
                ++i;
                bw.write("\n" + tuple.v1 + "\t" + i / (numVariants + 0.0) + "\t" + first.get(tuple.v1) + "\t" + second.get(tuple.v1) + "\t" + tuple.v2);
            }
        }
        catch (IOException ioe)
        {
            System.err.println("ERROR: Something failed while writing the output file");
        }
    }


    /**
     * Given a list of maps with the same keys, prints the values for all the keys.
     *
     * @param output    The output file.
     * @param values    The list of maps containing the values.
     * @param ids       Identifiers for each map.
     * @param maxLength maximum length of the recommendation.
     */
    public static void printFile(String output, List<Map<String, Double>> values, List<String> ids, int maxLength)
    {
        if(values == null || values.isEmpty() || ids == null || ids.isEmpty()) return;

        int numIds = ids.size();

        // Obtain the variants:
        List<String> variants = new ArrayList<>(values.get(0).keySet());
        int numVariants = variants.size();

        // Sort the approaches in alphanumeric order.
        Collections.sort(variants);

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output))))
        {
            bw.write("Variant\tFraction");
            for(String id : ids)
            {
                bw.write("\t" + id + "@" + maxLength);
            }

            int i = 0;
            for(String variant : variants)
            {
                i += 1;
                bw.write("\n" + variant);
                bw.write("\t" + i/(numVariants + 0.0));
                for(int j = 0; j < numIds; ++j)
                {
                    bw.write("\t" + values.get(j).get(variant));
                }
            }
        }
        catch (IOException ioe)
        {
            System.err.println("ERROR: Something failed while writing the output file");
        }
    }

    /**
     * Given a list of maps with the same keys, prints the values for all the keys.
     *
     * @param output    The output file.
     * @param values    The list of maps containing the values.
     * @param ids       Identifiers for each map.
     */
    public static void printFile(String output, List<Map<String, Double>> values, List<String> ids)
    {
        if(values == null || values.isEmpty() || ids == null || ids.isEmpty()) return;

        int numIds = ids.size();

        // Obtain the variants:
        List<String> variants = new ArrayList<>(values.get(0).keySet());
        int numVariants = variants.size();

        // Sort the approaches in alphanumeric order.
        Collections.sort(variants);

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(output))))
        {
            bw.write("Variant\tFraction");
            for(String id : ids)
            {
                bw.write("\t" + id);
            }

            int i = 0;
            for(String variant : variants)
            {
                i += 1;
                bw.write("\n" + variant);
                bw.write("\t" + i/(numVariants + 0.0));
                for(int j = 0; j < numIds; ++j)
                {
                    bw.write("\t" + values.get(j).get(variant));
                }
            }
        }
        catch (IOException ioe)
        {
            System.err.println("ERROR: Something failed while writing the output file");
        }
    }

}
