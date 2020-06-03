/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.metrics.distributions;

import es.uam.eps.ir.socialranksys.index.Index;
import es.uam.eps.ir.socialranksys.index.Relation;
import es.uam.eps.ir.socialranksys.index.fast.FastUnweightedPairwiseRelation;
import es.uam.eps.socialranksys.diffusion.simulation.Iteration;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Distribution for information pieces.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users.
 * @param <I> Type of the items.
 * @param <P> Type of the parameters.
 */
public class InfoPiecesDistribution<U extends Serializable,I extends Serializable,P> extends AbstractDistribution<U,I,P> 
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
    public InfoPiecesDistribution()
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
    public void update(Iteration<U, I, P> iteration)
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
            List<I> infos = data.getAllInformationPieces().collect(Collectors.toCollection(ArrayList::new));
            Index<I> iIndex = data.getInformationPiecesIndex();
            for(I info : infos)
            {
                int iidx = iIndex.object2idx(info);
                long sum = this.relation.numFirst(iidx);
                bw.write(info + "\t" + sum + "\n");
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
