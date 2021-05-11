/* 
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.diffusion.data.filter;

import es.uam.eps.ir.socialranksys.diffusion.data.Data;
import es.uam.eps.ir.socialranksys.diffusion.data.Information;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.generator.EmptyGraphGenerator;
import es.uam.eps.ir.socialranksys.graph.generator.GraphGenerator;
import es.uam.eps.ir.socialranksys.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.socialranksys.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.socialranksys.index.Index;
import es.uam.eps.ir.socialranksys.index.Relation;
import es.uam.eps.ir.socialranksys.index.fast.FastWeightedPairwiseRelation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Interface for filtering unnecessary data for simulations.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public abstract class AbstractDataFilter<U extends Serializable,I extends Serializable, F> implements DataFilter<U,I, F>
{

    @Override
    public Data<U,I, F> filter(Data<U,I, F> fullData)
    {
        Index<U> userIndex = this.filterUsers(fullData);
        Index<I> infoPiecesIndex = this.filterInfoPieces(fullData);
        Map<Integer, Information<I>> information = this.filterInformation(fullData, infoPiecesIndex);
        Graph<U> graph = this.filterGraph(fullData, userIndex);
        Relation<Integer> userInformation = this.filterUserInformation(fullData, userIndex, infoPiecesIndex);
        Map<String, Index<F>> parameters = new HashMap<>();
        List<String> userParameters = new ArrayList<>();
        Map<String, Relation<Double>> userRelation = new HashMap<>();
        for(String name : fullData.getUserFeatureNames())
        {
            Index<F> pIndex = this.filterParameters(fullData, name, infoPiecesIndex);
            parameters.put(name, pIndex);
            userParameters.add(name);
            
            Relation<Double> relation = this.filterUserParameterRelation(fullData, name, userIndex, pIndex);
            userRelation.put(name, relation);
        }
        List<String> infoParameters = new ArrayList<>();
        Map<String, Relation<Double>> infoRelation = new HashMap<>();
        for(String name : fullData.getInfoPiecesFeatureNames())
        {
            Index<F> pIndex = this.filterParameters(fullData, name, infoPiecesIndex);
            parameters.put(name, pIndex);
            infoParameters.add(name);
            
            Relation<Double> relation = this.filterInfoParameterRelation(fullData, name, infoPiecesIndex, pIndex);
            infoRelation.put(name, relation);
            
        }
        
        Relation<Long> realProp = this.filterRealPropagatedRelation(fullData, userIndex,infoPiecesIndex);
        return new Data<>(graph, userIndex, infoPiecesIndex, information, userInformation, parameters, userParameters, userRelation, infoParameters, infoRelation, realProp);
    }
    
    /**
     * Filters the set of users.
     * @param data the original data.
     * @return the filtered index.
     */
    protected abstract Index<U> filterUsers(Data<U,I, F> data);
    
    /**
     * Filters the set of information pieces.
     * @param data the original data.
     * @return the filtered index.
     */
    protected abstract Index<I> filterInfoPieces(Data<U,I, F> data);
  
    /**
     * Filters the set of feature values
     * @param data the original data.
     * @param name the feature name.
     * @param iIndex the filtered index of information pieces.
     * @return the filtered index.
     */
    protected abstract Index<F> filterParameters(Data<U,I, F> data, String name, Index<I> iIndex);

    /**
     * Obtains the filtered graph.
     * @param data the original data.
     * @param index the user index.
     * @return the filtered graph.
     */
    protected Graph<U> filterGraph(Data<U,I, F> data, Index<U> index)
    {
        try 
        {
            Graph<U> fullGraph = data.getGraph();
            boolean directed = fullGraph.isDirected();
            boolean weighted = fullGraph.isWeighted();
            
            GraphGenerator<U> ggen = new EmptyGraphGenerator<>();
            ggen.configure(directed, weighted);
            Graph<U> graph = ggen.generate();
            
            index.getAllObjects().forEach(u ->
                index.getAllObjects().forEach(v -> 
                {
                    if(fullGraph.containsEdge(u, v))
                    {
                        double weight = fullGraph.getEdgeWeight(u, v);
                        int type = fullGraph.getEdgeType(u, v);
                        graph.addEdge(u, v, weight, type, true);
                    }
                })
            );
            
            return graph;
        } 
        catch (GeneratorNotConfiguredException | GeneratorBadConfiguredException ex)
        {
            return null;
        }
    }

    /**
     * Filters the expanded information.
     * @param fullData the original data.
     * @param infoPiecesIndex the filtered information pieces index.
     * @return the expanded information for the filtered data.
     */
    protected Map<Integer, Information<I>> filterInformation(Data<U, I, F> fullData, Index<I> infoPiecesIndex)
    {
        Map<Integer, Information<I>> information = new HashMap<>();
        infoPiecesIndex.getAllObjects().forEach(i -> 
        {
            int iidx = infoPiecesIndex.object2idx(i);
            if(fullData.containsInformationPiece(i) && fullData.getInformation(i) != null)
            {
                Information<I> info = new Information<>(i, fullData.getInformation(i).getTimestamp());
                information.put(iidx, info);
            }
        });
        return information;
    }

    /**
     * Filters the relation between users and a parameter.
     * @param fullData the original data.
     * @param name the parameter name.
     * @param userIndex the filtered user index.
     * @param pIndex the filtered parameter index.
     * @return the filtered relation.
     */
    protected Relation<Double> filterUserParameterRelation(Data<U, I, F> fullData, String name, Index<U> userIndex, Index<F> pIndex)
    {
        Relation<Double> relation = new FastWeightedPairwiseRelation<>();
        
        IntStream.range(0, userIndex.numObjects()).forEach(relation::addFirstItem);
        IntStream.range(0, pIndex.numObjects()).forEach(relation::addSecondItem);
        
        userIndex.getAllObjects().forEach(u -> 
        {
            int uidx = userIndex.object2idx(u);
            fullData.getUserFeatures(u, name).forEach(p -> 
            {
                if(pIndex.containsObject(p.v1))
                {
                    int pidx = pIndex.object2idx(p.v1);
                    relation.addRelation(uidx, pidx, p.v2);
                }
            });
        });
        
        return relation;
    }
    
    /**
     * Filters the relation between information pieces and a parameter.
     * @param fullData the original data.
     * @param name the parameter name.
     * @param infoPiecesIndex the filtered information pieces index.
     * @param pIndex the filtered parameter index.
     * @return the filtered relation.
     */
    protected Relation<Double> filterInfoParameterRelation(Data<U, I, F> fullData, String name, Index<I> infoPiecesIndex, Index<F> pIndex)
    {
        Relation<Double> relation = new FastWeightedPairwiseRelation<>();
        
        IntStream.range(0, infoPiecesIndex.numObjects()).forEach(relation::addFirstItem);
        IntStream.range(0, pIndex.numObjects()).forEach(relation::addSecondItem);
        
        infoPiecesIndex.getAllObjects().forEach(i ->
        {
            int iidx = infoPiecesIndex.object2idx(i);
            fullData.getInfoPiecesFeatures(i, name).forEach(p -> 
            {
                if(pIndex.containsObject(p.v1))
                {
                    int pidx = pIndex.object2idx(p.v1);
                    relation.addRelation(iidx, pidx, p.v2);
                }
            });
        });
        
        return relation;    
    }

    /**
     * Filters the relation between users and information pieces.
     * @param fullData the previous data.
     * @param userIndex the filtered user index.
     * @param infoPiecesIndex the filtered information pieces index.
     * @return the filtered relation.
     */
    protected Relation<Integer> filterUserInformation(Data<U, I, F> fullData, Index<U> userIndex, Index<I> infoPiecesIndex)
    {
        Relation<Integer> relation = new FastWeightedPairwiseRelation<>();
        IntStream.range(0, userIndex.numObjects()).forEach(relation::addFirstItem);
        IntStream.range(0, infoPiecesIndex.numObjects()).forEach(relation::addSecondItem);
        
        userIndex.getAllObjects().forEach(u -> 
        {
            int uidx = userIndex.object2idx(u);
            fullData.getPieces(u).forEach(i ->
            {
               if(infoPiecesIndex.containsObject(i))
               {
                   int iidx = infoPiecesIndex.object2idx(i);
                   relation.addRelation(uidx, iidx, 1);
               }
            });
        });
        
        return relation;
    }

    /**
     * Filters the relation between users and propagated pieces of information in real life.
     * @param fullData the whole data.
     * @param userIndex the previously filtered user index.
     * @param infoPiecesIndex the filtered information pieces index.
     * @return the filtered relation.
     */
    protected Relation<Long> filterRealPropagatedRelation(Data<U, I, F> fullData, Index<U> userIndex, Index<I> infoPiecesIndex)
    {
        Relation<Long> relation = new FastWeightedPairwiseRelation<>();
        IntStream.range(0, userIndex.numObjects()).forEach(relation::addFirstItem);
        IntStream.range(0, infoPiecesIndex.numObjects()).forEach(relation::addSecondItem);
        
        userIndex.getAllObjects().forEach(u -> 
        {
            int uidx = userIndex.object2idx(u);
            fullData.getRealPropagatedPieces(u).forEach(i -> 
            {
               int iidx = infoPiecesIndex.object2idx(i);
               if(infoPiecesIndex.containsObject(i))
               {
                   relation.addRelation(uidx, iidx, fullData.getRealPropagatedTimestamp(u, i));
               }
            });
        });
        
        return relation;
    }
}
