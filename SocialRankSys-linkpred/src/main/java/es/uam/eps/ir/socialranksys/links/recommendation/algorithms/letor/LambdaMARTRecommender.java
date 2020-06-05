/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.letor;

import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.data.letor.FeatureInformation;
import es.uam.eps.ir.socialranksys.links.data.letor.Instance;
import es.uam.eps.ir.socialranksys.links.data.letor.io.InstanceSetReader;
import es.uam.eps.ir.socialranksys.links.data.letor.io.LETORFormatConstants;
import es.uam.eps.ir.socialranksys.links.data.letor.io.LETORInstanceReader;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.ranksys.formats.parsing.Parser;
import org.ranksys.formats.parsing.Parsers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that transforms the output of the JForest package to a recommendation
 * @param <U>
 * @see <a href=https://github.com/yasserg/jforests>JForests</a>
 * @author Javier Sanz-Cruzado.
 */
public class LambdaMARTRecommender<U> extends UserFastRankingRecommender<U>
{
    private final Int2ObjectMap<Int2DoubleMap> scoresMaps;
    
    /**
     * Constructor.
     * @param graph the graph.
     * @param instancesSet a file containing the set of instances for the missing links in the graph.
     * @param resultsSet the LamdbdaMART scores for each one of those instances.
     * @param parser parses the different users from the file instance set.
     */
    public LambdaMARTRecommender(FastGraph<U> graph, String instancesSet, String resultsSet, Parser<U> parser)
    {
        super(graph);
        this.scoresMaps = new Int2ObjectOpenHashMap<>();
        this.scoresMaps.defaultReturnValue(new Int2DoubleOpenHashMap());
        try(BufferedReader patBR = new BufferedReader(new InputStreamReader(new FileInputStream(instancesSet)));
            BufferedReader resBR = new BufferedReader(new InputStreamReader(new FileInputStream(resultsSet))))
        {
            InstanceSetReader<U> reader = new LETORInstanceReader<>(parser);
            
            // First, read the headers containing the feature information.
            String line;
            List<String> header = new ArrayList<>();
            while((line = patBR.readLine()).startsWith(LETORFormatConstants.COMMENT))
            {
                header.add(line);
            }
            
            FeatureInformation featInfo = reader.readHeader(header);
            int numFeats = featInfo.numFeats();
            String resLine = resBR.readLine();
            do
            {
                Instance<U> pattern = reader.readInstance(line, numFeats);
                int uidx = this.item2iidx(pattern.getOrigin());
                int vidx = this.item2iidx(pattern.getDest());
                double value = Parsers.dp.parse(resLine);
                
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
            }
            while((line = patBR.readLine()) != null && (resLine = resBR.readLine()) != null);
        }   
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }
    @Override
    public Int2DoubleMap getScoresMap(int uidx) 
    {
        return this.scoresMaps.getOrDefault(uidx, this.scoresMaps.defaultReturnValue());
    }

}
