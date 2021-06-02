/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * Class for applying the main methods to the executions.
 * @author Javier Sanz-Cruzado Puig
 */
public class Main
{
    /**
     * Main method. Executes the main method in the class specified by the first
     * argument with the rest of execution arguments.
     *
     * @param args Arguments to select the class whose main method's execute and
     * arguments to execute it.
     */
    public static void main(String[] args)
    {
        try
        {
            String main = args[0];
            int index = 1;

            String classpath;

            switch (main)
            {
                case "sna" -> classpath = "es.uam.eps.ir.socialranksys.defexamples.sna.GraphAnalyzer";
                case "communities" -> classpath = "es.uam.eps.ir.socialranksys.defexamples.sna.CommunityDetector";
                case "recommendation" -> classpath = "es.uam.eps.ir.socialranksys.defexamples.links.recommendation.Recommendation";
                case "prediction" -> classpath = "es.uam.eps.ir.socialranksys.defexamples.links.prediction.LinkPrediction";
                case "index" ->
                {
                    index = 2;
                    if(args[1].equalsIgnoreCase("user"))
                        classpath = "es.uam.eps.ir.socialranksys.defexamples.content.UserIndexGenerator";
                    else if(args[1].equalsIgnoreCase("infopiece"))
                        classpath = "es.uam.eps.ir.socialranksys.defexamples.content.InformationPieceIndexGenerator";
                    else
                        classpath = null;
                }
                case "effects" -> classpath = "es.uam.eps.ir.socialranksys.defexamples.links.recommendation.evaluation.GraphMetricsEvaluation";
                case "reranking" -> classpath = "es.uam.eps.ir.socialranksys.defexampleslinks.recommendation.Reranking";
                case "featuregen" -> classpath = "es.uam.eps.ir.socialranksys.defexampleslinks.recommendation.MLFeatureGenerator";
                case "graphgen" -> classpath = "es.uam.eps.ir.socialranksys.defexamples.graphgen.RandomGraphGenerator";
                case "diffusion" -> classpath = "es.uam.eps.ir.socialranksys.defexamples.diffusion.Diffusion";
                case "diffusion-eval" -> classpath = "es.uam.eps.ir.socialranksys.defexamples.diffusion.DiffusionEvaluation";
                default -> classpath = null;
            }

            if(classpath == null) return;

            System.out.println(main);
            String[] executionArgs = Arrays.copyOfRange(args, index, args.length);
            Class[] argTypes = {executionArgs.getClass()};
            Object[] passedArgs = {executionArgs};
            Class.forName(classpath).getMethod("main", argTypes).invoke(null, passedArgs);
        }
        catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
        {
            System.err.println("The execution arguments were not right");
            ex.printStackTrace();
        }
    }
}