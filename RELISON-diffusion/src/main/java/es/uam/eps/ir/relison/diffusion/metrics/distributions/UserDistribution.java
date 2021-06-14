/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.metrics.distributions;

import es.uam.eps.ir.relison.diffusion.simulation.Iteration;
import es.uam.eps.ir.relison.index.Index;
import es.uam.eps.ir.relison.index.Relation;
import es.uam.eps.ir.relison.index.fast.FastUnweightedPairwiseRelation;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Distribution for information pieces.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the the user / information pieces features.
 */
public class UserDistribution<U extends Serializable,I extends Serializable, F> extends AbstractDistribution<U,I, F>
{
    
    /**
     * Name of the distribution
     */
    private final static String INFOPIECES = "info";
    /**
     * Relation between users and tags.
     */
    private Relation<Integer> relation;

    
    /**
     * Constructor. 
     */
    public UserDistribution()
    {
        super(INFOPIECES);
        this.relation = new FastUnweightedPairwiseRelation<>();
        this.initialized = false;
    }
    
    @Override
    protected void initialize() 
    {
        if(!this.isInitialized())
        {
            this.relation = new FastUnweightedPairwiseRelation<>();
            IntStream.range(0, data.numUsers()).forEach(uidx -> relation.addFirstItem(uidx));
            IntStream.range(0, data.numInformationPieces()).forEach(iidx -> relation.addSecondItem(iidx));
            this.initialized = true;
        }
    }

    @Override
    public void update(Iteration<U, I, F> iteration)
    {
        if(this.isInitialized())
        {
            iteration.getReceivingUsers().forEach(u -> 
            {
                int uidx = data.getUserIndex().object2idx(u);
                iteration.getSeenInformation(u).forEach(i -> 
                {
                    int iidx = data.getInformationPiecesIndex().object2idx(i.v1());
                    relation.addRelation(uidx, iidx, 1);
                });
            });
        }
        
    }

    @Override
    public void print(String file) 
    {
        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file))))
        {
            List<U> users = data.getAllUsers().collect(Collectors.toCollection(ArrayList::new));
            Index<U> uIndex = data.getUserIndex();
            for(U user : users)
            {
                int uidx = uIndex.object2idx(user);
                long sum = this.relation.numSecond(uidx);
                bw.write(user + "\t" + sum + "\n");
            }
        } 
        catch (IOException ex) 
        {
            ex.printStackTrace();
        }
    }
 

    @Override
    public void clear() 
    {
        this.relation = null;
        this.initialized = false;
    }  
}
