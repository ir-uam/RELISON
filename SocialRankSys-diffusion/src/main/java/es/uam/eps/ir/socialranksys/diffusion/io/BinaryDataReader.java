/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.io;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;
import es.uam.eps.ir.socialranksys.diffusion.data.Information;
import es.uam.eps.ir.socialranksys.diffusion.simulation.SimulationEdgeTypes;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.index.Index;
import es.uam.eps.ir.socialranksys.index.Relation;
import es.uam.eps.ir.socialranksys.index.fast.FastIndex;
import es.uam.eps.ir.socialranksys.index.fast.FastWeightedPairwiseRelation;
import es.uam.eps.ir.socialranksys.io.graph.BinaryGraphReader;
import es.uam.eps.ir.socialranksys.io.graph.GraphReader;
import es.uam.eps.ir.socialranksys.utils.datatypes.Triplet;
import org.ranksys.formats.rec.RecommendationFormat;
import org.ranksys.formats.rec.TRECRecommendationFormat;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.ranksys.formats.parsing.Parsers.lp;

/**
 * Reads data from a binary file.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class BinaryDataReader 
{
    /**
     * Read a simple version of the data.
     * @param file the file containing the data.
     * @return a Data object if ok, null if not.
     */
    public static Data<Long, Long, Long> read(String file)
    {
        if(file == null) return null;
        
        try(DataInputStream dos = new DataInputStream(new BufferedInputStream(new FileInputStream(file))))
        {
            // First, read the user index.
            Index<Long> userIndex = BinaryDataReader.readUserIndex(dos);
            
            // Read the graph.
            GraphReader<Long> graphReader = new BinaryGraphReader();
            Graph<Long> graph = graphReader.read(dos, true, true);
            
            // Read the information pieces and their relation with users.
            Triplet<Index<Long>, Map<Integer, Information<Long>>, Relation<Integer>> info = BinaryDataReader.readInfoPieces(dos, userIndex);
            
            Index<Long> infoPiecesIndex = info.v1();
            Map<Integer, Information<Long>> infoPieces = info.v2();
            Relation<Integer> userInfo = info.v3();
            
            Triplet<List<String>, Map<String, Index<Long>>, Map<String, Relation<Double>>> userFeats = BinaryDataReader.readFeatures(dos, userIndex);
            
            List<String> userFeatureNames = userFeats.v1();
            Map<String, Index<Long>> feats = userFeats.v2();
            Map<String, Relation<Double>> userFeatureRels = userFeats.v3();
            
            Triplet<List<String>, Map<String, Index<Long>>, Map<String, Relation<Double>>> infoFeats = BinaryDataReader.readFeatures(dos, userIndex);
            
            List<String> infoFeatureNames = infoFeats.v1();
            feats.putAll(infoFeats.v2());
            Map<String, Relation<Double>> infoFeatureRels = userFeats.v3();
            
            Relation<Long> realProp = BinaryDataReader.readRealProp(dos, userIndex, infoPiecesIndex);
            return new Data<>(graph, userIndex, infoPiecesIndex, infoPieces, userInfo, feats, userFeatureNames, userFeatureRels, infoFeatureNames, infoFeatureRels, realProp);
            
        } 
        catch (IOException ex) 
        {
            return null;
        }
    }
    
    /**
     * Read a simple version of the data.
     * @param file the file containing the data.
     * @param recFile File containing the recommendation.
     * @param topN the number of recommendations to take.
     * @return a Data object if ok, null if not.
     */
    public static Data<Long, Long, Long> read(String file, String recFile, int topN)
    {
        if(file == null) return null;
        
        try(DataInputStream dos = new DataInputStream(new BufferedInputStream(new FileInputStream(file))))
        {
            // First, read the user index.
            Index<Long> userIndex = BinaryDataReader.readUserIndex(dos);          
            
            // Read the graph.
            GraphReader<Long> graphReader = new BinaryGraphReader();
            Graph<Long> graph = graphReader.read(dos, true, true);
            
            RecommendationFormat<Long, Long> format = new TRECRecommendationFormat<>(lp,lp);
            
            // Add the links from recommendation.
            format.getReader(recFile).readAll().forEach(rec ->
                rec.getItems().stream().limit(topN).forEach(r -> graph.addEdge(rec.getUser(), r.v1, 1.0, SimulationEdgeTypes.RECOMMEND))
            );
            
            // Read the information pieces and their relation with users.
            Triplet<Index<Long>, Map<Integer, Information<Long>>, Relation<Integer>> info = BinaryDataReader.readInfoPieces(dos, userIndex);
            
            Index<Long> infoPiecesIndex = info.v1();
            Map<Integer, Information<Long>> infoPieces = info.v2();
            Relation<Integer> userInfo = info.v3();
            
            Triplet<List<String>, Map<String, Index<Long>>, Map<String, Relation<Double>>> userFeats = BinaryDataReader.readFeatures(dos, userIndex);
            
            List<String> userFeatureNames = userFeats.v1();
            Map<String, Index<Long>> feats = userFeats.v2();
            Map<String, Relation<Double>> userFeatureRels = userFeats.v3();
            
            Triplet<List<String>, Map<String, Index<Long>>, Map<String, Relation<Double>>> infoFeats = BinaryDataReader.readFeatures(dos, userIndex);
            
            List<String> infoFeatureNames = infoFeats.v1();
            feats.putAll(infoFeats.v2());
            Map<String, Relation<Double>> infoFeatureRels = userFeats.v3();
            
            Relation<Long> realProp = BinaryDataReader.readRealProp(dos, userIndex, infoPiecesIndex);
            return new Data<>(graph, userIndex, infoPiecesIndex, infoPieces, userInfo, feats, userFeatureNames, userFeatureRels, infoFeatureNames, infoFeatureRels, realProp);
            
        } 
        catch (IOException ex) 
        {
            return null;
        }
    }
    

    /**
     * Reads a user index.
     * @param dos A data stream for reading the data.
     * @return the user index.
     * @throws IOException if something fails while reading the data stream.
     */
    private static Index<Long> readUserIndex(DataInputStream dos) throws IOException 
    {
        Index<Long> userIndex = new FastIndex<>();
        int numUsers = dos.readInt();

        for(int i = 0; i < numUsers; ++i)
        {
            long userId = dos.readLong();
            userIndex.addObject(userId);
        }
        
        return userIndex;
    }

    /**
     * Reads about information pieces.
     * @param dos A data stream for reading the data.
     * @param userIndex The index for the users.
     * @return a triplet containing the index of information pieces, the additional information for those pieces and the user-piece relation.
     * @throws IOException if something fails while reading the data stream.
     */
    private static Triplet<Index<Long>, Map<Integer, Information<Long>>, Relation<Integer>> readInfoPieces(DataInputStream dos, Index<Long> userIndex) throws IOException 
    {
        Relation<Integer> userInfo = new FastWeightedPairwiseRelation<>();
        Index<Long> infoPiecesIndex = new FastIndex<>();
        Map<Integer, Information<Long>> infoPieces = new HashMap<>();
        
        int numUsers = userIndex.numObjects();
        for(int i = 0; i < numUsers; ++i)
        {
            userInfo.addFirstItem(i);
        }
        
        int numPieces = dos.readInt();
        
        for(int i = 0; i < numPieces; ++i)
        {
            long pieceId = dos.readLong();
            infoPiecesIndex.addObject(pieceId);
            
            long timestamp = dos.readLong();
            Information<Long> infoPiece = new Information<>(pieceId, timestamp);
            infoPieces.put(i, infoPiece);
            
            userInfo.addSecondItem(i);
            int numCreators = dos.readInt();
            for(int j = 0; j < numCreators; ++j)
            {
                userInfo.addRelation(dos.readInt(), i, 1);
            }
        }
        
        return new Triplet<>(infoPiecesIndex, infoPieces, userInfo);
    }

    /**
     * Reads information about features.
     * @param dos a data stream for reading the data.
     * @param index an index (user/pieces).
     * @return a triplet containing the list of parameter names, a map containing the possible values for each parameter, a map containing the relation between users and information pieces.
     * @throws IOException if something fails while reading the data stream.
     */
    private static Triplet<List<String>, Map<String, Index<Long>>, Map<String, Relation<Double>>> readFeatures(DataInputStream dos, Index<Long> index) throws IOException 
    {
        List<String> names = new ArrayList<>();
        Map<String, Index<Long>> indexes = new HashMap<>();
        Map<String, Relation<Double>> relations = new HashMap<>();
        
        int numFeats = dos.readInt();
        for(int i = 0; i < numFeats; ++i)
        {
            Triplet<String, Index<Long>, Relation<Double>> triplet = BinaryDataReader.readFeature(dos, index);
            names.add(triplet.v1());
            indexes.put(triplet.v1(), triplet.v2());
            relations.put(triplet.v1(), triplet.v3());
        }
        return new Triplet<>(names, indexes, relations);
    }

    /**
     * Reads information about a single feature.
     * @param dos a data stream for reading the data.
     * @param index an index (user/pieces).
     * @return a triplet containing the name of the parameter, the possible values for the parameter, the relation between users and values.
     * @throws IOException if something fails while reading the data stream.
     */
    private static Triplet<String, Index<Long>, Relation<Double>> readFeature(DataInputStream dos, Index<Long> index) throws IOException 
    {
        String name = dos.readUTF();
        Relation<Double> relation = new FastWeightedPairwiseRelation<>();
        Index<Long> featureIndex = new FastIndex<>();
        for(int i = 0; i < index.numObjects(); ++i)
        {
            relation.addFirstItem(i);
        }
        
        int numValues = dos.readInt();
        for(int i = 0; i < numValues; ++i)
        {
            long value = dos.readLong();
            featureIndex.addObject(value);
            
            relation.addSecondItem(i);
            int numAssign = dos.readInt();
            for(int j = 0; j < numAssign; ++j)
            {
                int assignId = dos.readInt();
                double val = dos.readDouble();
                relation.addRelation(assignId, i, val);
            }
        }
        
        return new Triplet<>(name, featureIndex, relation);
    }

    /**
     * Reads information about the real propagated information pieces.
     * @param dos the data stream for reading the data.
     * @param userIndex the user index.
     * @param infoPiecesIndex the information pieces index.
     * @return the relation between user pieces and repropagated information pieces (containing its timestamps as weights)
     * @throws IOException if something fails while reading the pieces.
     */
    private static Relation<Long> readRealProp(DataInputStream dos, Index<Long> userIndex, Index<Long> infoPiecesIndex) throws IOException 
    {
        Relation<Long> realProp = new FastWeightedPairwiseRelation<>();
        
        int numUsers = userIndex.numObjects();
        int numPieces = infoPiecesIndex.numObjects();
        
        for(int i = 0; i < numUsers; ++i)
        {
            realProp.addFirstItem(i);
        }
        
        for(int i = 0; i < numPieces; ++i)
        {
            realProp.addSecondItem(i);
        }
        
        for(int i = 0; i < numUsers; ++i)
        {
            int uidx = dos.readInt();
            int numRepr = dos.readInt();
            
            for(int j = 0; j < numRepr; ++j)
            {
                int iidx = dos.readInt();
                long ts = dos.readLong();
                realProp.addRelation(uidx, iidx, ts);
            }
        }
        
        return realProp;
    }
    
}
