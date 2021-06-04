/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.io;

import es.uam.eps.ir.relison.diffusion.data.Data;
import es.uam.eps.ir.relison.index.Index;
import es.uam.eps.ir.relison.io.graph.BinaryGraphWriter;
import es.uam.eps.ir.relison.io.graph.GraphWriter;
import es.uam.eps.ir.relison.utils.datatypes.Tuple2ol;
import org.ranksys.core.util.tuples.Tuple2od;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Writes data in a binary file.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class BinaryDataWriter 
{
    /**
     * Write the data.
     * @param data the data to write.
     * @param file the file.
     * @return true if everything went OK, false otherwise.
     */
    public static boolean write(Data<Long, Long, Long> data, String file)
    {
        if(data == null) return false;
        
        try(DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file))))
        {
            // First, write the user index
            int numUsers = data.getUserIndex().numObjects();
            
            dos.writeInt(numUsers);
            for(int i = 0; i < numUsers; ++i)
            {
                dos.writeLong(data.getUserIndex().idx2object(i));
            }
            
            // Write the graph
            GraphWriter<Long> graphWriter = new BinaryGraphWriter();
            graphWriter.write(data.getGraph(), dos, true, true);
            
            // Write information pieces.
            int numPieces = data.numInformationPieces();
            dos.writeInt(numPieces);
            
            for(int i = 0; i < numPieces; ++i)
            {
                Long piece = data.getInformationPiecesIndex().idx2object(i);
                dos.writeLong(piece);
                dos.writeLong(data.getInformation(piece).getTimestamp());
                List<Long> creators = data.getCreators(piece).collect(Collectors.toCollection(ArrayList::new));
                dos.writeInt(creators.size());
                for(Long creator : creators)
                {
                    int cidx = data.getUserIndex().object2idx(creator);
                    dos.writeInt(cidx);
                }
            }
            
            // Write user features
            List<String> userFeatureNames = data.getUserFeatureNames();
            dos.writeInt(userFeatureNames.size());
            
            for(String feature : userFeatureNames)
            {
                dos.writeUTF(feature);
                Index<Long> fIndex = data.getFeatureIndex(feature);
                int numValues = fIndex.numObjects();
                dos.writeInt(numValues);
                
                for(int i = 0; i < numValues; ++i)
                {
                    Long f = fIndex.idx2object(i);
                    dos.writeLong(f);
                    
                    List<Tuple2od<Long>> users = data.getUsersWithFeature(feature, f).collect(Collectors.toCollection(ArrayList::new));
                    dos.writeInt(users.size());
                    for(Tuple2od<Long> tuple : users)
                    {
                        dos.writeInt(data.getUserIndex().object2idx(tuple.v1));
                        dos.writeDouble(tuple.v2);
                    }
                }  
            }
            
            
            // Write info pieces features
            List<String> infoFeatureNames = data.getInfoPiecesFeatureNames();
            dos.writeInt(infoFeatureNames.size());
            
            for(String feature : infoFeatureNames)
            {
                dos.writeUTF(feature);
                
                Index<Long> fIndex = data.getFeatureIndex(feature);
                int numValues = fIndex.numObjects();
                dos.writeInt(numValues);
                
                for(int i = 0; i < numValues; ++i)
                {
                    Long f = fIndex.idx2object(i);
                    dos.writeLong(f);
                    
                    List<Tuple2od<Long>> pieces = data.getInformationPiecesWithFeature(feature, f).collect(Collectors.toCollection(ArrayList::new));
                    for(Tuple2od<Long> tuple : pieces)
                    {
                        dos.writeInt(data.getInformationPiecesIndex().object2idx(tuple.v1()));
                        dos.writeDouble(tuple.v2());
                    }
                }
            }
            

            // Finally, write real propagated pieces
            for(int i = 0; i < numUsers; ++i)
            {
                dos.writeInt(i);
                long user = data.getUserIndex().idx2object(i);
                
                List<Tuple2ol<Long>> tuples = data.getRealPropagatedPiecesWithTimestamp(user).collect(Collectors.toCollection(ArrayList::new));
                dos.writeInt(tuples.size());
                
                for(Tuple2ol<Long> tuple : tuples)
                {
                    int iidx = data.getInformationPiecesIndex().object2idx(tuple.v1());
                    dos.writeInt(iidx);
                    dos.writeLong(tuple.v2());
                }               
            }

            return true;
        } 
        catch (IOException ex) 
        {
            return false;
        }
    }
    
}
