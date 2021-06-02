/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.diffusion.io;

import es.uam.eps.ir.ranksys.core.util.FastStringSplitter;
import es.uam.eps.ir.sonalire.diffusion.data.Data;
import es.uam.eps.ir.sonalire.diffusion.data.Information;
import es.uam.eps.ir.sonalire.diffusion.simulation.SimulationEdgeTypes;
import es.uam.eps.ir.sonalire.graph.Graph;
import es.uam.eps.ir.sonalire.index.Index;
import es.uam.eps.ir.sonalire.index.Relation;
import es.uam.eps.ir.sonalire.index.fast.FastIndex;
import es.uam.eps.ir.sonalire.index.fast.FastWeightedPairwiseRelation;
import es.uam.eps.ir.sonalire.io.graph.GraphReader;
import es.uam.eps.ir.sonalire.io.graph.TextGraphReader;
import es.uam.eps.ir.sonalire.io.graph.TextMultiGraphReader;
import es.uam.eps.ir.sonalire.utils.datatypes.Triplet;
import es.uam.eps.ir.sonalire.utils.datatypes.Tuple2oo;
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
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public class DataReader<U extends Serializable,I extends Serializable, F>
{
    /**
     * Read the data from files. Only the basic information is available.
     * @param graphFile     route of a file containing the network graph.
     * @param multigraph    true if the graph is a multigraph, false otherwise.
     * @param directed      true if the graph is directed, false otherwise.
     * @param weighted      true if the graph is weighted, false otherwise.
     * @param selfloops     true if the graph allows self loops, false otherwise.
     * @param readtypes     true if we must read the types of the edges, false otherwise.
     * @param uIndex        route to a file containing a list of user identifiers.
     * @param iIndex        route to a file containing a list of user identifiers.
     * @param infoFile      file containing information about the information pieces (creator and timestamps).
     * @param uParser       parser for the user identifiers.
     * @param iParser       parser for the information piece identifiers.
     * @return the data object.
     * @throws IOException if something fails while reading.
     */
    public Data<U,I, F> readData(String graphFile, boolean multigraph, boolean directed, boolean weighted, boolean selfloops, boolean readtypes, String uIndex, String iIndex, String infoFile, Parser<U> uParser, Parser<I> iParser) throws IOException
    {
        return this.readData(graphFile, multigraph, directed, weighted, selfloops, readtypes, uIndex, iIndex, infoFile, null, null, null, null, Integer.MAX_VALUE, uParser, iParser, null);
    }

    /**
     * Read the data from files. We have information about the features of users or items.
     * @param graphFile         route of a file containing the network graph.
     * @param multigraph        true if the graph is a multigraph, false otherwise.
     * @param directed          true if the graph is directed, false otherwise.
     * @param weighted          true if the graph is weighted, false otherwise.
     * @param selfloops         true if the graph allows self loops, false otherwise.
     * @param readtypes         true if we have to read the types of the edges, false otherwise.
     * @param uIndex            route to a file containing a list of user identifiers.
     * @param iIndex            route to a file containing a list of user identifiers.
     * @param infoFile          file containing information about the information pieces (creator and timestamps).
     * @param userFeatureFiles  an array containing the routes of files user features (might be null).
     * @param infoFeatureFiles  an array containing the routes of files with information pieces features (might be null).
     * @param uParser           parser for the user identifiers.
     * @param iParser           parser for the information piece identifiers.
     * @param pParser           parser for the features.
     * @return the data object.
     * @throws IOException if something fails while reading.
     */
    public Data<U,I, F> readData(String graphFile, boolean multigraph, boolean directed, boolean weighted, boolean selfloops, boolean readtypes, String uIndex, String iIndex, String infoFile, String[] userFeatureFiles, String[] infoFeatureFiles, Parser<U> uParser, Parser<I> iParser, Parser<F> pParser) throws IOException
    {
        return this.readData(graphFile, multigraph, directed, weighted, selfloops, readtypes, uIndex, iIndex, infoFile, userFeatureFiles, infoFeatureFiles, null, null, Integer.MAX_VALUE, uParser, iParser, pParser);
    }

    /**
     * Read the data from files. We have information about which users propagated which information (and when)
     * @param graphFile             route of a file containing the network graph.
     * @param multigraph            true if the graph is a multigraph, false otherwise.
     * @param directed              true if the graph is directed, false otherwise.
     * @param weighted              true if the graph is weighted, false otherwise.
     * @param selfloops             true if the graph allows self loops, false otherwise.
     * @param readtypes             true if the graph has types that we should read. NOTE: if we use recommendations, this parameter will be considered as false.
     * @param uIndex                route to a file containing a list of user identifiers.
     * @param iIndex                route to a file containing a list of user identifiers.
     * @param infoFile              file containing information about the information pieces (creator and timestamps).
     * @param realPropagatedFile    route to a file containing information about which information pieces were repropagated by each user in the real diffusion (might be null).
     * @param uParser               parser for the user identifiers.
     * @param iParser               parser for the information piece identifiers.
     * @return a data object to use in a diffusion simulation procedure.
     * @throws IOException if something fails while reading.
     */
    public Data<U,I, F> readData(String graphFile, boolean multigraph, boolean directed, boolean weighted, boolean selfloops, boolean readtypes, String uIndex, String iIndex, String infoFile, String realPropagatedFile, Parser<U> uParser, Parser<I> iParser) throws IOException
    {
        return this.readData(graphFile, multigraph, directed, weighted, selfloops, readtypes, uIndex, iIndex, infoFile, null, null, realPropagatedFile, null, Integer.MAX_VALUE, uParser, iParser, null);
    }

    /**
     * Read the data from files. We have information about which users propagated which information (and when) as well as information about the features of users or items.
     * @param graphFile             route of a file containing the network graph.
     * @param multigraph            true if the graph is a multigraph, false otherwise.
     * @param directed              true if the graph is directed, false otherwise.
     * @param weighted              true if the graph is weighted, false otherwise.
     * @param selfloops             true if the graph allows self loops, false otherwise.
     * @param readtypes             true if the graph has types that we should read. NOTE: if we use recommendations, this parameter will be considered as false.
     * @param uIndex                route to a file containing a list of user identifiers.
     * @param iIndex                route to a file containing a list of user identifiers.
     * @param userFeatureFiles      an array containing the routes of files user features (might be null).
     * @param infoFeatureFiles      an array containing the routes of files with information pieces features (might be null).
     * @param infoFile              file containing information about the information pieces (creator and timestamps).
     * @param realPropagatedFile    route to a file containing information about which information pieces were repropagated by each user in the real diffusion (might be null).
     * @param uParser               parser for the user identifiers.
     * @param iParser               parser for the information piece identifiers.
     * @param pParser               parser for the features.
     * @return a data object to use in a diffusion simulation procedure.
     * @throws IOException if something fails while reading.
     */
    public Data<U,I, F> readData(String graphFile, boolean multigraph, boolean directed, boolean weighted, boolean selfloops, boolean readtypes, String uIndex, String iIndex, String infoFile, String[] userFeatureFiles, String[] infoFeatureFiles, String realPropagatedFile, Parser<U> uParser, Parser<I> iParser, Parser<F> pParser) throws IOException
    {
        return this.readData(graphFile, multigraph, directed, weighted, selfloops, readtypes, uIndex, iIndex, infoFile, userFeatureFiles, infoFeatureFiles, realPropagatedFile, null, Integer.MAX_VALUE, uParser, iParser, pParser);
    }

    /**
     * Read the data from files. Only the basic information is available, in addition to a contact recommendation.
     * @param graphFile     route of a file containing the network graph.
     * @param multigraph    true if the graph is a multigraph, false otherwise.
     * @param directed      true if the graph is directed, false otherwise.
     * @param weighted      true if the graph is weighted, false otherwise.
     * @param selfloops     true if the graph allows self loops, false otherwise.
     * @param uIndex        route to a file containing a list of user identifiers.
     * @param iIndex        route to a file containing a list of user identifiers.
     * @param infoFile      file containing information about the information pieces (creator and timestamps).
     * @param recFile       file containing the results of applying a contact recommendation algorithm over the network (might be null).
     * @param topN          number of links (per user) to add from the recommendations.
     * @param uParser       parser for the user identifiers.
     * @param iParser       parser for the information piece identifiers.
     * @return the data object.
     * @throws IOException if something fails while reading.
     */
    public Data<U,I, F> readData(String graphFile, boolean multigraph, boolean directed, boolean weighted, boolean selfloops, String uIndex, String iIndex, String infoFile, String recFile, int topN, Parser<U> uParser, Parser<I> iParser) throws IOException
    {
        return this.readData(graphFile, multigraph, directed, weighted, selfloops, false, uIndex, iIndex, infoFile, null, null, null, recFile, topN, uParser, iParser, null);
    }

    /**
     * Read the data from files. We have information about the features of users or items, in addition to a contact recommendation.
     * @param graphFile         route of a file containing the network graph.
     * @param multigraph        true if the graph is a multigraph, false otherwise.
     * @param directed          true if the graph is directed, false otherwise.
     * @param weighted          true if the graph is weighted, false otherwise.
     * @param selfloops         true if the graph allows self loops, false otherwise.
     * @param uIndex            route to a file containing a list of user identifiers.
     * @param iIndex            route to a file containing a list of user identifiers.
     * @param infoFile          file containing information about the information pieces (creator and timestamps).
     * @param userFeatureFiles  an array containing the routes of files user features (might be null).
     * @param infoFeatureFiles  an array containing the routes of files with information pieces features (might be null).
     * @param recFile           file containing the results of applying a contact recommendation algorithm over the network (might be null).
     * @param topN              number of links (per user) to add from the recommendations.
     * @param uParser           parser for the user identifiers.
     * @param iParser           parser for the information piece identifiers.
     * @param pParser           parser for the features.
     * @return the data object.
     * @throws IOException if something fails while reading.
     */
    public Data<U,I, F> readData(String graphFile, boolean multigraph, boolean directed, boolean weighted, boolean selfloops, String uIndex, String iIndex, String infoFile, String[] userFeatureFiles, String[] infoFeatureFiles, String recFile, int topN, Parser<U> uParser, Parser<I> iParser, Parser<F> pParser) throws IOException
    {
        return this.readData(graphFile, multigraph, directed, weighted, selfloops, false, uIndex, iIndex, infoFile, userFeatureFiles, infoFeatureFiles, null, recFile, topN, uParser, iParser, pParser);
    }

    /**
     * Read the data from files. We have information about which users propagated which information (and when), in addition to a contact recommendation.
     * @param graphFile             route of a file containing the network graph.
     * @param multigraph            true if the graph is a multigraph, false otherwise.
     * @param directed              true if the graph is directed, false otherwise.
     * @param weighted              true if the graph is weighted, false otherwise.
     * @param selfloops             true if the graph allows self loops, false otherwise.
     * @param uIndex                route to a file containing a list of user identifiers.
     * @param iIndex                route to a file containing a list of user identifiers.
     * @param infoFile              file containing information about the information pieces (creator and timestamps).
     * @param realPropagatedFile    route to a file containing information about which information pieces were repropagated by each user in the real diffusion (might be null).
     * @param recFile               file containing the results of applying a contact recommendation algorithm over the network (might be null).
     * @param topN                  number of links (per user) to add from the recommendations.
     * @param uParser               parser for the user identifiers.
     * @param iParser               parser for the information piece identifiers.
     * @return a data object to use in a diffusion simulation procedure.
     * @throws IOException if something fails while reading.
     */
    public Data<U,I, F> readData(String graphFile, boolean multigraph, boolean directed, boolean weighted, boolean selfloops, String uIndex, String iIndex, String infoFile, String realPropagatedFile, String recFile, int topN, Parser<U> uParser, Parser<I> iParser) throws IOException
    {
        return this.readData(graphFile, multigraph, directed, weighted, selfloops, false, uIndex, iIndex, infoFile, null, null, realPropagatedFile, recFile, topN, uParser, iParser, null);
    }

    /**
     * Read the data from files. We have information about which users propagated which information (and when) as well as information about the features of users or items,
     * as well as a contact recommendation.
     * @param graphFile             route of a file containing the network graph.
     * @param multigraph            true if the graph is a multigraph, false otherwise.
     * @param directed              true if the graph is directed, false otherwise.
     * @param weighted              true if the graph is weighted, false otherwise.
     * @param selfloops             true if the graph allows self loops, false otherwise.
     * @param uIndex                route to a file containing a list of user identifiers.
     * @param iIndex                route to a file containing a list of user identifiers.
     * @param userFeatureFiles  an array containing the routes of files user features (might be null).
     * @param infoFeatureFiles  an array containing the routes of files with information pieces features (might be null).
     * @param infoFile              file containing information about the information pieces (creator and timestamps).
     * @param realPropagatedFile    route to a file containing information about which information pieces were repropagated by each user in the real diffusion (might be null).
     * @param recFile               file containing the results of applying a contact recommendation algorithm over the network (might be null).
     * @param topN                  number of links (per user) to add from the recommendations.
     * @param uParser               parser for the user identifiers.
     * @param iParser               parser for the information piece identifiers.
     * @param pParser           parser for the features.
     * @return a data object to use in a diffusion simulation procedure.
     * @throws IOException if something fails while reading.
     */
    public Data<U,I, F> readData(String graphFile, boolean multigraph, boolean directed, boolean weighted, boolean selfloops, String uIndex, String iIndex, String infoFile, String[] userFeatureFiles, String[] infoFeatureFiles, String realPropagatedFile, String recFile, int topN, Parser<U> uParser, Parser<I> iParser, Parser<F> pParser) throws IOException
    {
        return this.readData(graphFile, multigraph, directed, weighted, selfloops, false, uIndex, iIndex, infoFile, userFeatureFiles, infoFeatureFiles, realPropagatedFile, null, Integer.MAX_VALUE, uParser, iParser, pParser);
    }

    /**
     * Read the data from files.
     * @param graphFile             route of a file containing the network graph.
     * @param multigraph            true if the graph is a multigraph, false otherwise.
     * @param directed              true if the graph is directed, false otherwise.
     * @param weighted              true if the graph is weighted, false otherwise.
     * @param selfloops             true if the graph allows self loops, false otherwise.
     * @param readtypes             true if the graph has types that we should read. NOTE: if we use recommendations, this parameter will be considered as false.
     * @param uIndex                route to a file containing a list of user identifiers.
     * @param iIndex                route to a file containing a list of user identifiers.
     * @param infoFile              file containing information about the information pieces (creator and timestamps).
     * @param userFeatureFiles      an array containing the routes of files user features (might be null).
     * @param infoFeatureFiles      an array containing the routes of files with information pieces features (might be null).
     * @param realPropagatedFile    route to a file containing information about which information pieces were repropagated by each user in the real diffusion (might be null).
     * @param recFile               file containing the results of applying a contact recommendation algorithm over the network (might be null).
     * @param topN                  number of links (per user) to add from the recommendations.
     * @param uParser               parser for the user identifiers.
     * @param iParser               parser for the information piece identifiers.
     * @param pParser               parser for the feature values.
     * @return a data object to use in a diffusion simulation procedure.
     * @throws IOException if something fails while reading.
     */
    protected Data<U,I, F> readData(String graphFile, boolean multigraph, boolean directed, boolean weighted, boolean selfloops, boolean readtypes, String uIndex, String iIndex, String infoFile, String[] userFeatureFiles, String[] infoFeatureFiles, String realPropagatedFile, String recFile, int topN, Parser<U> uParser, Parser<I> iParser, Parser<F> pParser) throws IOException
    {
        // We first read the user and information pieces index.
        Index<U> userIndex = this.readIndex(uIndex, uParser);
        Index<I> infoIndex = this.readIndex(iIndex, iParser);

        // Then, we read the graph:
        GraphReader<U> greader = multigraph ? new TextMultiGraphReader<>(directed, weighted, selfloops, "\t", uParser) : new TextGraphReader<>(directed, weighted, selfloops, "\t", uParser);
        Graph<U> graph = greader.read(graphFile, weighted, recFile == null && readtypes);

        // We add to the graph all the recommendation edges.
        if(recFile != null)
        {
            RecommendationFormat<U, U> format = new SimpleRecommendationFormat<>(uParser, uParser);
            format.getReader(recFile).readAll().forEach(rec ->
            {
                U u = rec.getUser();
                rec.getItems().stream().limit(topN).forEach(r -> graph.addEdge(u, r.v1, 1.0, SimulationEdgeTypes.RECOMMEND));
            });
        }

            // We read the data about the information (i.e. the creators and timestamps of the information pieces)
        Tuple2oo<Map<Integer, Information<I>>, Relation<Integer>> information = this.readUserInformation(infoFile, userIndex, infoIndex, uParser, iParser);

        Map<Integer, Information<I>> infoMap = information.v1();
        Relation<Integer> userInfo = information.v2();

        // Now, we read the user and item features:
        Map<String, Index<F>> parameters = new HashMap<>();
        List<String> userParametersNames = new ArrayList<>();
        Map<String, Relation<Double>> userParameters = new HashMap<>();

        if(userFeatureFiles != null)
        {
            for (String userParamFile : userFeatureFiles)
            {
                Triplet<String, Index<F>, Relation<Double>> triplet = readFeaturesFile(userParamFile, userIndex, uParser, pParser);
                String pName = triplet.v1();
                parameters.put(pName, triplet.v2());
                userParametersNames.add(pName);
                userParameters.put(pName, triplet.v3());
            }
        }
        
        List<String> infoParametersNames = new ArrayList<>();
        Map<String, Relation<Double>> infoParameters = new HashMap<>();

        if(infoFeatureFiles != null)
        {
            for (String infoParamFile : infoFeatureFiles)
            {
                Triplet<String, Index<F>, Relation<Double>> triplet = readFeaturesFile(infoParamFile, infoIndex, iParser, pParser);
                String pName = triplet.v1();
                parameters.put(pName, triplet.v2());
                infoParametersNames.add(pName);
                infoParameters.put(pName, triplet.v3());
            }
        }

        Relation<Long> realPropagated = realPropagatedFile != null ? this.readRealPropagatedFile(realPropagatedFile, userIndex, infoIndex, uParser, iParser) : new FastWeightedPairwiseRelation<>();
        return new Data<>(graph, userIndex, infoIndex, infoMap, userInfo, parameters, userParametersNames, userParameters, infoParametersNames, infoParameters, realPropagated);
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
    private <T> Triplet<String, Index<F>, Relation<Double>> readFeaturesFile(String paramFile, Index<T> index, Parser<T> parser, Parser<F> pParser) throws IOException
    {
        String pname;
        Index<F> pIndex = new FastIndex<>();
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
                    F p = pParser.parse(tokens[1]);
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
