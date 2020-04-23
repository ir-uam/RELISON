/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.multigraph.edges.fast;

import es.uam.eps.ir.socialranksys.index.AutoRelation;
import es.uam.eps.ir.socialranksys.graph.multigraph.edges.MultiEdgeTypes;
import es.uam.eps.ir.socialranksys.graph.multigraph.edges.MultiEdges;
import es.uam.eps.ir.socialranksys.utils.listcombiner.OrderedListCombiner;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Abstract fast implementation of class MultiEdges.
 * @author Javier Sanz-Cruzado Puig
 */
public abstract class FastMultiEdges implements MultiEdges
{
    /**
     * Number of edges.
     */
    protected long numEdges = 0L;
    /**
     * Relation for storing the weights of the edges.
     */
    protected final AutoRelation<List<Double>> weights;
    /**
     * Relation for storing the types of the edges.
     */
    protected final AutoRelation<List<Integer>> types;

    /**
     * Constructor.
     * @param weights The weights of the edges.
     * @param types The types of the edges
     */
    public FastMultiEdges(AutoRelation<List<Double>> weights, AutoRelation<List<Integer>> types)
    {
        this.weights = weights;
        this.types = types;
    }
    
    @Override
    public boolean containsEdge(int orig, int dest)
    {
        return this.weights.containsPair(orig, dest);
    }

    @Override
    public List<Double> getEdgeWeights(int orig, int dest)
    {
        return this.weights.getValue(orig, dest);
    }

    @Override
    public List<Integer> getEdgeTypes(int orig, int dest)
    {
        List<Integer> value = this.types.getValue(orig, dest);
        if(value == null)
            return MultiEdgeTypes.getErrorType();
        return value;
    }
    
    @Override
    public boolean addUser(int node)
    {
        return this.weights.addFirstItem(node) && this.types.addFirstItem(node);
    }
    
    @Override
    public int getNumEdges(int orig, int dest) 
    {
        List<Integer> value = this.types.getValue(orig, dest);
        if(value == null)
            return 0;
        else
            return value.size();
    }
    
    @Override
    public long getNumEdges()
    {
        return numEdges;
    }
    
    @Override
    public int getIncidentCount(int dest)
    {
        return this.types.getIdsFirst(dest).mapToInt(d -> d.getValue().size()).sum();
    }
    
    @Override
    public int getAdjacentCount(int dest)
    {
        return this.types.getIdsSecond(dest).mapToInt(d -> d.getValue().size()).sum();
    }
    
    @Override
    public IntStream getIsolatedNodes()
    {
        return this.weights.getIsolated();
    }
    
    @Override
    public boolean hasAdjacentEdges(int node)
    {
        return this.weights.hasSeconds(node);
    }
    
    @Override
    public boolean hasIncidentEdges(int node)
    {
        return this.weights.hasFirsts(node);
    }
    
    @Override
    public boolean hasEdges(int node)
    {
        return this.weights.hasFirsts(node) || this.weights.hasSeconds(node);
    }
    
    @Override
    public boolean hasMutualEdges(int node)
    {
        Stream<Integer> incident = this.getIncidentNodes(node);
        Stream<Integer> adjacent = this.getAdjacentNodes(node);
        
        return OrderedListCombiner.intersectionHaslements(incident, adjacent, Comparator.naturalOrder());
    }
}
