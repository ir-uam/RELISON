/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.socialranksys.diffusion.metrics.informationpieces.global;

import es.uam.eps.ir.socialranksys.index.Relation;
import es.uam.eps.ir.socialranksys.index.fast.FastWeightedPairwiseRelation;
import es.uam.eps.ir.socialranksys.utils.indexes.GiniIndex;
import es.uam.eps.socialranksys.diffusion.metrics.AbstractGlobalSimulationMetric;
import es.uam.eps.socialranksys.diffusion.simulation.Iteration;

import java.io.Serializable;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * It finds the distribution of the times each information piece is received and seen
 * during the simulation.
 * 
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the user
 * @param <I> type of the information
 * @param <P> type of the parameters
 */
public class InformationPieceGiniComplement<U extends Serializable,I extends Serializable,P> extends AbstractGlobalSimulationMetric<U,I,P>
{
    
    /**
     * Relation between users and received pieces of information
     */
    private Relation<Integer> relation;
    
    /**
     * Name fixed value
     */
    private final static String INFOPIECEGINI = "infopiece-ginicompl";

    /**
     * Speed value
     */
    private double speed;
    
    /**
     * Constructor.
     */
    public InformationPieceGiniComplement() 
    {
        super(INFOPIECEGINI);
        this.speed = 0.0;
        this.relation = new FastWeightedPairwiseRelation<>();
    }

    @Override
    public void clear() 
    {
        this.speed = 0.0;
        this.relation = new FastWeightedPairwiseRelation<>();
        this.initialized = false;
    }

    @Override
    public double calculate() 
    {
        GiniIndex gini = new GiniIndex();

        // Get the number of different information pieces
        int numPieces = this.data.numInformationPieces();
        Stream<Double> stream = this.relation.getAllSecond().mapToDouble(second -> this.relation.numFirst(second) + 0.0).boxed();
        
        return 1.0 - gini.compute(stream, true, numPieces, this.speed);
    }

    @Override
    public void update(Iteration<U, I, P> iteration)
    {
        if(this.isInitialized())
        {
            iteration.getReceivingUsers().forEach(u -> 
            {
                int uidx = data.getUserIndex().object2idx(u);
                iteration.getSeenInformation(u).forEach(piece -> {
                    int iidx = data.getInformationPiecesIndex().object2idx(piece.v1());
                    this.relation.addRelation(uidx, iidx, 1);
                });

            });
            this.speed += iteration.getNumUniqueSeen() + 0.0;
        }
        
    }
    
    @Override
    protected void initialize()
    {
        this.speed = 0.0;
        IntStream.range(0, this.data.numUsers()).forEach(uidx -> this.relation.addFirstItem(uidx));
        IntStream.range(0, this.data.numInformationPieces()).forEach(iidx -> this.relation.addSecondItem(iidx));
        this.initialized = true;
    }

    
}
