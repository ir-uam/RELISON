/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.data.filter;

import es.uam.eps.ir.relison.diffusion.data.Data;
import es.uam.eps.ir.relison.diffusion.simulation.SimulationEdgeTypes;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.generator.EmptyGraphGenerator;
import es.uam.eps.ir.relison.graph.generator.GraphGenerator;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.relison.index.Index;
import es.uam.eps.ir.relison.index.fast.FastIndex;

import java.io.Serializable;

/**
 * Filter that removes recommended and not relevant edges from the graph.
 * 
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user and information pieces features.
 */
public class RelevantEdgesFilter<U extends Serializable,I extends Serializable, F> extends AbstractDataFilter<U,I, F>
{
    /**
     * The definitive of tags to filter.
     */
    private final Graph<U> testGraph;
    
    /**
     * Constructor.
     * @param testGraph a graph containing the relevant edges.
     */
    public RelevantEdgesFilter(Graph<U> testGraph)
    {
        this.testGraph = testGraph;
    }
    
    @Override
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
                        if(fullGraph.getEdgeType(u, v) == SimulationEdgeTypes.TRAINING)
                        {
                            double weight = fullGraph.getEdgeWeight(u, v);
                            int type = fullGraph.getEdgeType(u, v);
                            graph.addEdge(u, v, weight, type, true);
                        }
                        else // if(fullGraph.getEdgeType(u,v) == SimulationEdgeTypes.RECOMMEND)
                        {
                            if(this.testGraph.containsEdge(u, v))
                            {
                                double weight = fullGraph.getEdgeWeight(u, v);
                                int type = fullGraph.getEdgeType(u,v);
                                graph.addEdge(u, v, weight, type, true);
                            }
                        }
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
    
    
    @Override
    protected Index<U> filterUsers(Data<U, I, F> data)
    {
        Index<U> uIndex = new FastIndex<>();
        data.getAllUsers().sorted().forEach(uIndex::addObject);
        return uIndex;
    }

    @Override
    protected Index<I> filterInfoPieces(Data<U, I, F> data)
    {
        Index<I> iIndex = new FastIndex<>();
        data.getAllInformationPieces().sorted().forEach(iIndex::addObject);
        return iIndex;
    }

    @Override
    protected Index<F> filterParameters(Data<U, I, F> data, String name, Index<I> iIndex)
    {
        Index<F> pIndex = new FastIndex<>();
        
        if(data.doesFeatureExist(name))
        {
            data.getAllFeatureValues(name).sorted().forEach(pIndex::addObject);
        }
        
        return pIndex;
    }
}
