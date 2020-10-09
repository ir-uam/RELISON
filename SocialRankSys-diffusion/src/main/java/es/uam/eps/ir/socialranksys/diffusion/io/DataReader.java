/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.io;

import es.uam.eps.ir.ranksys.core.util.FastStringSplitter;
import es.uam.eps.ir.socialranksys.diffusion.data.Data;
import es.uam.eps.ir.socialranksys.diffusion.data.Information;
import es.uam.eps.ir.socialranksys.diffusion.simulation.SimulationEdgeTypes;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.index.Index;
import es.uam.eps.ir.socialranksys.index.Relation;
import es.uam.eps.ir.socialranksys.index.fast.FastIndex;
import es.uam.eps.ir.socialranksys.index.fast.FastWeightedPairwiseRelation;
import es.uam.eps.ir.socialranksys.io.graph.GraphReader;
import es.uam.eps.ir.socialranksys.io.graph.TextGraphReader;
import es.uam.eps.ir.socialranksys.io.graph.TextMultiGraphReader;
import es.uam.eps.ir.socialranksys.utils.datatypes.Triplet;
import es.uam.eps.ir.socialranksys.utils.datatypes.Tuple2oo;
import org.ranksys.formats.parsing.Parser;
import org.ranksys.formats.rec.RecommendationFormat;
import org.ranksys.formats.rec.SimpleRecommendationFormat;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.ranksys.formats.parsing.Parsers.dp;
import static org.ranksys.formats.parsing.Parsers.lp;

/**
 * Class for reading the data.
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @param <U> Type of the users.
 * @param <I> Type of the information pieces.
 * @param <P> Type of the parameters.
 */
public class DataReader<U extends Serializable,I extends Serializable,P> 
{

    /**
     * Read the data from files.
     * @param multigraph true if the graph is a multigraph.
     * @param directed true if the graph is directed.
     * @param weighted true if the graph is weighted
     * @param readtypes true if we must read the types.
     * @param uIndex file containing the user index.
     * @param iIndex file containing the information pieces index index.
     * @param graphFile file containing the graph.
     * @param infoFile file containing the additional information about the information pieces (creator and timestamps)
     * @param uParser parser for the user identifiers.
     * @param iParser parser for the information piece identifiers.
     * @return the data object.
     * @throws IOException if something fails while reading.
     */
    public Data<U,I,P> readData(boolean multigraph, boolean directed, boolean weighted, boolean readtypes, String uIndex, String iIndex, String graphFile, String infoFile, Parser<U> uParser, Parser<I> iParser) throws IOException
    {
        Index<U> userIndex = this.readIndex(uIndex, uParser);
        Index<I> itemIndex = this.readIndex(iIndex, iParser);

        GraphReader<U> greader = multigraph ? new TextMultiGraphReader<>(directed, weighted, false, "\t", uParser) : new TextGraphReader<>(directed, weighted, false, "\t", uParser);
        Graph<U> graph = greader.read(graphFile, weighted, readtypes);
        
        Tuple2oo<Map<Integer, Information<I>>, Relation<Integer>> information = this.readUserInformation(infoFile, userIndex, itemIndex, uParser, iParser);
        
        Map<Integer, Information<I>> infoMap = information.v1();
        Relation<Integer> userInfo = information.v2();
        
        return new Data<>(graph, userIndex, itemIndex, infoMap, userInfo);
    }
    
    /**
     * Read the data from files.
     * @param multigraph true if the graph is a multigraph.
     * @param directed true if the graph is directed.
     * @param weighted true if the graph is weighted
     * @param uIndex file containing the user index.
     * @param iIndex file containing the information pieces index index.
     * @param graphFile file containing the graph.
     * @param recFile recommendation file.
     * @param topN maximum cutoff of the recommendation.
     * @param infoFile file containing the additional information about the information pieces (creator and timestamps)
     * @param uParser parser for the user identifiers.
     * @param iParser parser for the information piece identifiers.
     * @return the data object.
     * @throws IOException if something fails while reading.
     */
    public Data<U,I,P> readData(boolean multigraph, boolean directed, boolean weighted, String uIndex, String iIndex, String graphFile, String recFile, int topN, String infoFile, Parser<U> uParser, Parser<I> iParser) throws IOException
    {
        Index<U> userIndex = this.readIndex(uIndex, uParser);
        Index<I> itemIndex = this.readIndex(iIndex, iParser);

        GraphReader<U> greader = multigraph ? new TextMultiGraphReader<>(directed, weighted, false, "\t", uParser) : new TextGraphReader<>(directed, weighted, false, "\t", uParser);
        Graph<U> graph = greader.read(graphFile, weighted, false);
       
        RecommendationFormat<U,U> format = new SimpleRecommendationFormat<>(uParser, uParser);
        
        format.getReader(recFile).readAll().forEach(rec -> 
        {
           U u = rec.getUser();
           rec.getItems().stream().limit(topN).forEach(r -> graph.addEdge(u, r.v1, 1.0, SimulationEdgeTypes.RECOMMEND));
        });
        
        Tuple2oo<Map<Integer, Information<I>>, Relation<Integer>> information = this.readUserInformation(infoFile, userIndex, itemIndex, uParser, iParser);
        
        Map<Integer, Information<I>> infoMap = information.v1();
        Relation<Integer> userInfo = information.v2();
        
        return new Data<>(graph, userIndex, itemIndex, infoMap, userInfo);
    }
    
    /**
     * Read the data from files.
     * @param multigraph true if the graph is a multigraph.
     * @param directed true if the graph is directed.
     * @param weighted true if the graph is weighted
     * @param readtypes true if we must read the types.
     * @param uIndex file containing the user index.
     * @param iIndex file containing the information pieces index index.
     * @param graphFile file containing the graph.
     * @param infoFile file containing the additional information about the information pieces (creator and timestamps)
     * @param userFeatureFiles List of files containing user features.
     * @param infoFeatureFiles List of files containing information pieces features.
     * @param uParser parser for the user identifiers.
     * @param iParser parser for the information piece identifiers.
     * @param pParser parser for the parameter values.
     * @return the data object
     * @throws IOException if something fails while reading.
     */
    public Data<U,I,P> readData(boolean multigraph, boolean directed, boolean weighted, boolean readtypes, String uIndex, String iIndex, String graphFile, String infoFile, String[] userFeatureFiles, String[] infoFeatureFiles, Parser<U> uParser, Parser<I> iParser, Parser<P> pParser) throws IOException
    {
        Index<U> userIndex = this.readIndex(uIndex, uParser);
        Index<I> itemIndex = this.readIndex(iIndex, iParser);

        GraphReader<U> greader = multigraph ? new TextMultiGraphReader<>(directed, weighted, false, "\t", uParser) : new TextGraphReader<>(directed, weighted, false, "\t", uParser);
        Graph<U> graph = greader.read(graphFile, weighted, readtypes);
 
        Tuple2oo<Map<Integer, Information<I>>, Relation<Integer>> information = this.readUserInformation(infoFile, userIndex, itemIndex, uParser, iParser);
        
        Map<Integer, Information<I>> infoMap = information.v1();
        Relation<Integer> userInfo = information.v2();
        
        Map<String, Index<P>> parameters = new HashMap<>();
        List<String> userParametersNames = new ArrayList<>();
        Map<String, Relation<Double>> userParameters = new HashMap<>();
        for(String userParamFile : userFeatureFiles)
        {
            Triplet<String, Index<P>, Relation<Double>> triplet = readFeaturesFile(userParamFile, userIndex, uParser, pParser);
            String pName = triplet.v1();
            parameters.put(pName, triplet.v2());
            userParametersNames.add(pName);
            userParameters.put(pName, triplet.v3());
        }
        
        List<String> infoParametersNames = new ArrayList<>();
        Map<String, Relation<Double>> infoParameters = new HashMap<>();
        for(String infoParamFile : infoFeatureFiles)
        {
            Triplet<String, Index<P>, Relation<Double>> triplet = readFeaturesFile(infoParamFile, itemIndex, iParser, pParser);
            String pName = triplet.v1();
            parameters.put(pName, triplet.v2());
            infoParametersNames.add(pName);
            infoParameters.put(pName, triplet.v3());
        }
        
        return new Data<>(graph, userIndex, itemIndex, infoMap, userInfo, parameters, userParametersNames, userParameters, infoParametersNames, infoParameters);
    }
    
        /**
     * Read the data from files.
     * @param multigraph true if the graph is a multigraph.
     * @param directed true if the graph is directed.
     * @param weighted true if the graph is weighted
     * @param readtypes true if we must read the types.
     * @param uIndex file containing the user index.
     * @param iIndex file containing the information pieces index index.
     * @param graphFile file containing the graph.
     * @param infoFile file containing the additional information about the information pieces (creator and timestamps)
     * @param userFeatureFiles List of files containing user features.
     * @param infoFeatureFiles List of files containing information pieces features.
     * @param realPropagatedFile file containing information about really propagated information pieces.
     * @param uParser parser for the user identifiers.
     * @param iParser parser for the information piece identifiers.
     * @param pParser parser for the parameter values.
     * @return the data object
     * @throws IOException if something fails while reading.
     */
    public Data<U,I,P> readData(boolean multigraph, boolean directed, boolean weighted, boolean readtypes, String uIndex, String iIndex, String graphFile, String infoFile, String[] userFeatureFiles, String[] infoFeatureFiles, String realPropagatedFile, Parser<U> uParser, Parser<I> iParser, Parser<P> pParser) throws IOException
    {
        Index<U> userIndex = this.readIndex(uIndex, uParser);
        Index<I> itemIndex = this.readIndex(iIndex, iParser);

        GraphReader<U> greader = multigraph ? new TextMultiGraphReader<>(directed, weighted, false, "\t", uParser) : new TextGraphReader<>(directed, weighted, false, "\t", uParser);
        Graph<U> graph = greader.read(graphFile, weighted, readtypes);
 
        
        RecommendationFormat<U,U> format = new SimpleRecommendationFormat<>(uParser, uParser);
                
        Tuple2oo<Map<Integer, Information<I>>, Relation<Integer>> information = this.readUserInformation(infoFile, userIndex, itemIndex, uParser, iParser);
        
        Map<Integer, Information<I>> infoMap = information.v1();
        Relation<Integer> userInfo = information.v2();
        
        Map<String, Index<P>> parameters = new HashMap<>();
        List<String> userParametersNames = new ArrayList<>();
        Map<String, Relation<Double>> userParameters = new HashMap<>();
        for(String userParamFile : userFeatureFiles)
        {
            Triplet<String, Index<P>, Relation<Double>> triplet = readFeaturesFile(userParamFile, userIndex, uParser, pParser);
            String pName = triplet.v1();
            parameters.put(pName, triplet.v2());
            userParametersNames.add(pName);
            userParameters.put(pName, triplet.v3());
        }
        
        List<String> infoParametersNames = new ArrayList<>();
        Map<String, Relation<Double>> infoParameters = new HashMap<>();
        for(String infoParamFile : infoFeatureFiles)
        {
            Triplet<String, Index<P>, Relation<Double>> triplet = readFeaturesFile(infoParamFile, itemIndex, iParser, pParser);
            String pName = triplet.v1();
            parameters.put(pName, triplet.v2());
            infoParametersNames.add(pName);
            infoParameters.put(pName, triplet.v3());
        }
        
        Relation<Long> realPropagated = this.readRealPropagatedFile(realPropagatedFile, userIndex, itemIndex, uParser, iParser);
        
        return new Data<>(graph, userIndex, itemIndex, infoMap, userInfo, parameters, userParametersNames, userParameters, infoParametersNames, infoParameters, realPropagated);
    }
    
    /**
     * Read the data from files.
     * @param multigraph true if the graph is a multigraph.
     * @param directed true if the graph is directed.
     * @param weighted true if the graph is weighted
     * @param readtypes true if we must read the types.
     * @param uIndex file containing the user index.
     * @param iIndex file containing the information pieces index index.
     * @param graphFile file containing the graph.
     * @param recFile recommendation file
     * @param topN maximum cutoff of the recommendation.
     * @param infoFile file containing the additional information about the information pieces (creator and timestamps)
     * @param userFeatureFiles List of files containing user features.
     * @param infoFeatureFiles List of files containing information pieces features.
     * @param uParser parser for the user identifiers.
     * @param iParser parser for the information piece identifiers.
     * @param pParser parser for the parameter values.
     * @return the data object
     * @throws IOException if something fails while reading.
     */
    public Data<U,I,P> readData(boolean multigraph, boolean directed, boolean weighted, boolean readtypes, String uIndex, String iIndex, String graphFile, String recFile, int topN, String infoFile, String[] userFeatureFiles, String[] infoFeatureFiles, Parser<U> uParser, Parser<I> iParser, Parser<P> pParser) throws IOException
    {
        Index<U> userIndex = this.readIndex(uIndex, uParser);
        Index<I> itemIndex = this.readIndex(iIndex, iParser);

        GraphReader<U> greader = multigraph ? new TextMultiGraphReader<>(directed, weighted, false, "\t", uParser) : new TextGraphReader<>(directed, weighted, false, "\t", uParser);
        Graph<U> graph = greader.read(graphFile, weighted, readtypes);
 
        
        RecommendationFormat<U,U> format = new SimpleRecommendationFormat<>(uParser, uParser);
        
        format.getReader(recFile).readAll().forEach(rec -> 
        {
           U u = rec.getUser();
           rec.getItems().stream().limit(topN).forEach(r -> graph.addEdge(u, r.v1, 1.0, SimulationEdgeTypes.RECOMMEND));
        });
        
        Tuple2oo<Map<Integer, Information<I>>, Relation<Integer>> information = this.readUserInformation(infoFile, userIndex, itemIndex, uParser, iParser);
        
        Map<Integer, Information<I>> infoMap = information.v1();
        Relation<Integer> userInfo = information.v2();
        
        Map<String, Index<P>> parameters = new HashMap<>();
        List<String> userParametersNames = new ArrayList<>();
        Map<String, Relation<Double>> userParameters = new HashMap<>();
        for(String userParamFile : userFeatureFiles)
        {
            Triplet<String, Index<P>, Relation<Double>> triplet = readFeaturesFile(userParamFile, userIndex, uParser, pParser);
            String pName = triplet.v1();
            parameters.put(pName, triplet.v2());
            userParametersNames.add(pName);
            userParameters.put(pName, triplet.v3());
        }
        
        List<String> infoParametersNames = new ArrayList<>();
        Map<String, Relation<Double>> infoParameters = new HashMap<>();
        for(String infoParamFile : infoFeatureFiles)
        {
            Triplet<String, Index<P>, Relation<Double>> triplet = readFeaturesFile(infoParamFile, itemIndex, iParser, pParser);
            String pName = triplet.v1();
            parameters.put(pName, triplet.v2());
            infoParametersNames.add(pName);
            infoParameters.put(pName, triplet.v3());
        }
        
        return new Data<>(graph, userIndex, itemIndex, infoMap, userInfo, parameters, userParametersNames, userParameters, infoParametersNames, infoParameters);
    }
    
    /**
     * Read the data from files.
     * @param multigraph true if the graph is a multigraph.
     * @param directed true if the graph is directed.
     * @param weighted true if the graph is weighted
     * @param readtypes true if we must read the types.
     * @param uIndex file containing the user index.
     * @param iIndex file containing the information pieces index index.
     * @param graphFile file containing the graph.
     * @param recFile recommendation file
     * @param topN maximum cutoff of the recommendation.
     * @param infoFile file containing the additional information about the information pieces (creator and timestamps)
     * @param userFeatureFiles List of files containing user features.
     * @param infoFeatureFiles List of files containing information pieces features.
     * @param realPropagatedFile file containing information about really propagated information pieces.
     * @param uParser parser for the user identifiers.
     * @param iParser parser for the information piece identifiers.
     * @param pParser parser for the parameter values.
     * @return the data object
     * @throws IOException if something fails while reading.
     */
    public Data<U,I,P> readData(boolean multigraph, boolean directed, boolean weighted, boolean readtypes, String uIndex, String iIndex, String graphFile, String recFile, int topN, String infoFile, String[] userFeatureFiles, String[] infoFeatureFiles, String realPropagatedFile, Parser<U> uParser, Parser<I> iParser, Parser<P> pParser) throws IOException
    {
        Index<U> userIndex = this.readIndex(uIndex, uParser);
        Index<I> itemIndex = this.readIndex(iIndex, iParser);

        GraphReader<U> greader = multigraph ? new TextMultiGraphReader<>(directed, weighted, false, "\t", uParser) : new TextGraphReader<>(directed, weighted, false, "\t", uParser);
        Graph<U> graph = greader.read(graphFile, weighted, readtypes);
 
        
        RecommendationFormat<U,U> format = new SimpleRecommendationFormat<>(uParser, uParser);

        format.getReader(recFile).readAll().forEach(rec ->
        {
           U u = rec.getUser();
           rec.getItems().stream().limit(topN).forEach(r -> graph.addEdge(u, r.v1, 1.0, SimulationEdgeTypes.RECOMMEND));
        });
        
        Tuple2oo<Map<Integer, Information<I>>, Relation<Integer>> information = this.readUserInformation(infoFile, userIndex, itemIndex, uParser, iParser);
        
        Map<Integer, Information<I>> infoMap = information.v1();
        Relation<Integer> userInfo = information.v2();
        
        Map<String, Index<P>> parameters = new HashMap<>();
        List<String> userParametersNames = new ArrayList<>();
        Map<String, Relation<Double>> userParameters = new HashMap<>();
        for(String userParamFile : userFeatureFiles)
        {
            Triplet<String, Index<P>, Relation<Double>> triplet = readFeaturesFile(userParamFile, userIndex, uParser, pParser);
            String pName = triplet.v1();
            parameters.put(pName, triplet.v2());
            userParametersNames.add(pName);
            userParameters.put(pName, triplet.v3());
        }
        
        List<String> infoParametersNames = new ArrayList<>();
        Map<String, Relation<Double>> infoParameters = new HashMap<>();
        for(String infoParamFile : infoFeatureFiles)
        {
            Triplet<String, Index<P>, Relation<Double>> triplet = readFeaturesFile(infoParamFile, itemIndex, iParser, pParser);
            String pName = triplet.v1();
            parameters.put(pName, triplet.v2());
            infoParametersNames.add(pName);
            infoParameters.put(pName, triplet.v3());
        }
        
        Relation<Long> realPropagated = this.readRealPropagatedFile(realPropagatedFile, userIndex, itemIndex, uParser, iParser);
        
        return new Data<>(graph, userIndex, itemIndex, infoMap, userInfo, parameters, userParametersNames, userParameters, infoParametersNames, infoParameters, realPropagated);
    }
    
    /**
     * Reads an index
     * @param indexFile the file containing the user index
     * @param parser the parser
     * @param <T> type of the parser.
     * @return the index.
     * @throws IOException if something fails while reading the file.
     */
    private <T> Index<T> readIndex(String indexFile, Parser<T> parser) throws IOException
    {
        Index<T> index = new FastIndex<>();
        try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(indexFile))))
        {
            String line;
            while((line = br.readLine()) != null)
            {
                index.addObject(parser.parse(line));
            }
        }   
        return index;
    }

    /**
     * Reads detailed data about the information.
     * @param infoFile information file
     * @param userIndex user index
     * @param itemIndex information pieces index.
     * @param uParser parser for the users.
     * @param iParser parser for the information pieces.
     * @return a pair containing the the map of extended information, and a relation between users and information pieces.
     * @throws IOException if something fails while reading the information file.
     */
    private Tuple2oo<Map<Integer, Information<I>>, Relation<Integer>> readUserInformation(String infoFile, Index<U> userIndex, Index<I> itemIndex, Parser<U> uParser, Parser<I> iParser) throws IOException
    {
        Relation<Integer> relation = new FastWeightedPairwiseRelation<>();
        IntStream.range(0, userIndex.numObjects()).forEach(relation::addFirstItem);
        IntStream.range(0, itemIndex.numObjects()).forEach(relation::addSecondItem);

        Map<Integer, Information<I>> information = new HashMap<>();
        try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(infoFile))))
        {
            String line = br.readLine(); //Header
            while((line = br.readLine()) != null)
            {
                String[] split = line.split("\t");
                I infoId = iParser.parse(split[0]);
                U userId = uParser.parse(split[1]);
                long timestamp = lp.parse(split[2]);
                
                int uidx = userIndex.object2idx(userId);
                int iidx = itemIndex.object2idx(infoId);
                
                if(uidx != -1 && iidx != -1)
                {
                    Information<I> info = new Information<>(infoId, timestamp);

                    information.put(iidx, info);
                    relation.addRelation(uidx,iidx,1);
                }
            }
        }
        
        return new Tuple2oo<>(information, relation);
    }
    
    
    /**
     * Reads a parameter file.
     * @param <T> Type of the object the parameters belong to.
     * @param paramFile the file containing the information about the parameters.
     * @param index a index of T objects.
     * @param parser a parser for the T objects.
     * @param pParser a parser for the parameter values.
     * @return a triplet containing the name of the feature, the feature index and the relation between T objects and features.
     * @throws IOException if something fails while reading the feature file.
     */
    private <T> Triplet<String, Index<P>, Relation<Double>> readFeaturesFile(String paramFile, Index<T> index, Parser<T> parser, Parser<P> pParser) throws IOException
    {
        String pname;
        Index<P> pIndex = new FastIndex<>();
        Relation<Double> relation = new FastWeightedPairwiseRelation<>();
        
        IntStream.range(0, index.numObjects()).forEach(relation::addFirstItem);
        try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(paramFile))))
        {
            String line = br.readLine();
            String[] header = line.split("\t");
            
            pname = header[1];
            
            boolean retrieveValue = header.length > 2;
            
            while((line = br.readLine()) != null)
            {
                CharSequence[] tokens = FastStringSplitter.split(line, '\t');
                T t = parser.parse(tokens[0]);
                int tidx = index.object2idx(t);

                if(tidx != -1)
                {
                    P p = pParser.parse(tokens[1]);
                    pIndex.addObject(p);
                    int pidx = pIndex.object2idx(p);

                    relation.addSecondItem(pidx);

                    double value = retrieveValue ? dp.parse(tokens[2]) : 1.0;

                    if(relation.containsPair(tidx, pidx))
                    {
                        relation.updatePair(tidx, pidx, relation.getValue(tidx, pidx) + value);
                    }
                    else
                    {
                        relation.addRelation(tidx, pidx, value);
                    }
                }
            }
        }
        
        return new Triplet<>(pname, pIndex, relation);
    }

    /**
     * Reads information about what information pieces were truly repropagated in the 
     * real world scenario.
     * @param realPropagatedFile a file containing the information.
     * @param userIndex user index
     * @param itemIndex information pieces index
     * @param uParser parser for users
     * @param iParser parser for information pieces
     * @return a relation between users and information pieces.
     * @throws IOException if something fails while reading the file.
     */
    private Relation<Long> readRealPropagatedFile(String realPropagatedFile, Index<U> userIndex, Index<I> itemIndex, Parser<U> uParser, Parser<I> iParser) throws IOException
    {
        // Initialize the relation
        Relation<Long> relation = new FastWeightedPairwiseRelation<>();
        IntStream.range(0, userIndex.numObjects()).forEach(relation::addFirstItem);
        IntStream.range(0, itemIndex.numObjects()).forEach(relation::addSecondItem);
        
        try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(realPropagatedFile))))
        {
            String line;
            while((line = br.readLine()) != null)
            {
                String[] split = line.split("\t");
                U u = uParser.parse(split[0]);
                I i = iParser.parse(split[1]);
                long timestamp = lp.parse(split[2]);
                
                if(userIndex.containsObject(u) && itemIndex.containsObject(i))
                {
                    int uidx = userIndex.object2idx(u);
                    int iidx = itemIndex.object2idx(i);
                    
                    relation.addRelation(uidx, iidx, timestamp);
                }
            }
        }   
        
        return relation;
    }
    
}
