/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys;

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

            String classpath;

            switch (main)
            {
                case "sna" -> classpath = "es.uam.eps.ir.socialranksys.sna.GraphAnalyzer";
                case "communities" -> classpath = "es.uam.eps.ir.socialranksys.sna.CommunityDetector";
                case "recommendation" -> classpath = "es.uam.eps.ir.socialranksys.links.recommendation.Recommendation";
                case "prediction" -> classpath = "es.uam.eps.ir.socialranksys.links.prediction.LinkPrediction";
                case "reranking" -> classpath = "es.uam.eps.ir.socialranksys.links.recommendation.Reranking";
                case "featuregen" -> classpath = "es.uam.eps.ir.socialranksys.links.recommendation.MLFeatureGenerator";
                case "graphgen" -> classpath = "es.uam.eps.ir.socialranksys.graphgen.RandomGraphGenerator";
                case "diffusion" -> classpath = "es.uam.eps.ir.socialranksys.diffusion.Diffusion";
                case "diffusion-eval" -> classpath = "es.uam.eps.ir.socialranksys.diffusion.DiffusionEvaluation";
                default -> classpath = null;
            }

            if(classpath == null) return;

            System.out.println(main);
            String[] executionArgs = Arrays.copyOfRange(args, 1, args.length);
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