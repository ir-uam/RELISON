/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.recommendation.algorithms.supervised;

import edu.uci.jforests.applications.RankingApp;
import edu.uci.jforests.config.TrainingConfig;
import edu.uci.jforests.dataset.RankingDataset;
import edu.uci.jforests.dataset.RankingDatasetLoader;
import edu.uci.jforests.input.RankingRaw2BinConvertor;
import edu.uci.jforests.learning.LearningUtils;
import edu.uci.jforests.learning.trees.Ensemble;
import edu.uci.jforests.sample.RankingSample;
import edu.uci.jforests.sample.Sample;
import edu.uci.jforests.util.IOUtils;
import es.uam.eps.ir.sonalire.graph.fast.FastGraph;
import es.uam.eps.ir.sonalire.links.data.letor.FeatureInformation;
import es.uam.eps.ir.sonalire.links.data.letor.Instance;
import es.uam.eps.ir.sonalire.links.data.letor.io.InstanceSetReader;
import es.uam.eps.ir.sonalire.links.data.letor.io.LETORFormatConstants;
import es.uam.eps.ir.sonalire.links.data.letor.io.LETORInstanceReader;
import es.uam.eps.ir.sonalire.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.ranksys.formats.parsing.Parser;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * Class for applying the LambdaMART algorithm. This uses the implementation by Yasser Ganjisaffar, Rich Caruana and
 * Cristina Lopes in <a href="https://github.com/yasserg/jforests">https://github.com/yasserg/jforests</a>.
 *
 * <p><b>Reference: </b> Y. Ganjisaffar, R. Caruana, C. Lopes. Bagging Gradient-Boosted Trees for High Precision, Low Variance Ranking Models. 34th Annual International ACM SIGIR conference on Research and development in Information Retrieval (SIGIR 2011), 85-94 (2011)</p>
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class LambdaMARTJForestsRecommender<U> extends UserFastRankingRecommender<U>
{
    /**
     * The final recommendation scores.
     */
    private final Int2ObjectMap<Int2DoubleMap> scoresMaps;

    /**
     * Constructor.
     * @param graph         the training network.
     * @param trainLETOR    a file containing the training patterns (in the LETOR format).
     * @param validLETOR    a file containing the validation patterns (in the LETOR format).
     * @param testLETOR     a file containing the test patterns (in the LETOR format).
     * @param config        a file containing the configuration for the LambdaMART algorithm.
     * @param tmp           a temporary directory in which to store intermediate files.
     * @param uParser       a user parser.
     * @throws Exception    if something fails while applying the LambdaMART algorithm.
     */
    public LambdaMARTJForestsRecommender(FastGraph<U> graph, String trainLETOR, String validLETOR, String testLETOR, String config, String tmp, Parser<U> uParser) throws Exception
    {
        super(graph);

        // First, we create the temporary directory to store.
        File directory = new File(tmp);
        if(!directory.exists() && !directory.mkdir())
        {
            throw new Exception("ERROR: Could not create the temporary folder.");
        }

        // Then, as a first step, we generate the binary files for training, validation and test datasets.

        // As a first step, we generate the binary files for the training, validation and test datasets.
        String[] files = new String[3];
        files[0] = trainLETOR + ".letor";
        files[1] = validLETOR + ".letor";
        files[2] = testLETOR + ".letor";
        new RankingRaw2BinConvertor().convert(tmp, files);

        // Once the binary files have been generated, we train the ensemble.
        InputStream configInputStream = new FileInputStream(config);
        Properties configProperties = new Properties();
        configProperties.load(configInputStream);

        configProperties.put(TrainingConfig.TRAIN_FILENAME, tmp + trainLETOR + ".bin");
        configProperties.put(TrainingConfig.VALID_FILENAME, tmp + validLETOR + ".bin");

        Ensemble ensemble;
        RankingApp app = new RankingApp();
        ensemble = app.run(configProperties);

        // Finally, we run the ensemble over the test set:
        InputStream in = new IOUtils().getInputStream(testLETOR + ".bin");
        Sample sample;
        RankingDataset dataset = new RankingDataset();
        RankingDatasetLoader.load(in, dataset);
        sample = new RankingSample(dataset);
        in.close();

        double[] predictions = new double[sample.size];
        LearningUtils.updateScores(sample, predictions, ensemble);

        // Now, I do have in predictions the recommendation scores for each example. Continuing:
        this.scoresMaps = new Int2ObjectOpenHashMap<>();
        this.scoresMaps.defaultReturnValue(new Int2DoubleOpenHashMap());
        try(BufferedReader patBR = new BufferedReader(new InputStreamReader(new FileInputStream(testLETOR + ".letor"))))
        {
            int i = 0;
            InstanceSetReader<U> reader = new LETORInstanceReader<>(uParser);

            // First, read the headers containing the feature information.
            String line;
            List<String> header = new ArrayList<>();
            while((line = patBR.readLine()).startsWith(LETORFormatConstants.COMMENT))
            {
                header.add(line);
            }

            FeatureInformation featInfo = reader.readHeader(header);
            int numFeats = featInfo.numFeats();
            do
            {
                Instance<U> pattern = reader.readInstance(line, numFeats);
                int uidx = this.item2iidx(pattern.getOrigin());
                int vidx = this.item2iidx(pattern.getDest());
                double value = predictions[i];

                if(!this.scoresMaps.containsKey(uidx))
                {
                    Int2DoubleMap scoreMap = new Int2DoubleOpenHashMap();
                    scoreMap.defaultReturnValue(Double.NEGATIVE_INFINITY);
                    scoreMap.put(vidx, value);
                    this.scoresMaps.put(uidx, scoreMap);
                }
                else
                {
                    scoresMaps.get(uidx).put(vidx, value);
                }

                ++i;
            }
            while((line = patBR.readLine()) != null && i < predictions.length);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }

        // And, finally, remove the temporary folder:
        boolean deleted = deleteDirectory(directory);
        if(!deleted)
        {
            throw new Exception("ERROR: Could not delete the temporary folder.");
        }
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx)
    {
        return this.scoresMaps.getOrDefault(uidx, this.scoresMaps.defaultReturnValue());
    }

    /**
     * Recursively deletes the contents of a folder.
     * @param dir the directory.
     */
    public static boolean deleteDirectory(File dir)
    {
        boolean out = true;
        for(File file: Objects.requireNonNull(dir.listFiles()))
        {
            if (file.isDirectory())
                deleteDirectory(file);
            else
                out = out && file.delete();
        }
        return out && dir.delete();
    }
}