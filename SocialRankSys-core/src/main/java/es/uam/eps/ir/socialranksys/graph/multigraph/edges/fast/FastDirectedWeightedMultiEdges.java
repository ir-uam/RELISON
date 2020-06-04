/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.multigraph.edges.fast;

import es.uam.eps.ir.socialranksys.index.IdxValue;
import es.uam.eps.ir.socialranksys.index.fast.FastWeightedAutoRelation;
import es.uam.eps.ir.socialranksys.graph.multigraph.edges.DirectedMultiEdges;
import es.uam.eps.ir.socialranksys.graph.multigraph.edges.MultiEdgeTypes;
import es.uam.eps.ir.socialranksys.graph.multigraph.edges.MultiEdgeWeights;
import es.uam.eps.ir.socialranksys.graph.multigraph.edges.WeightedMultiEdges;
import es.uam.eps.ir.socialranksys.utils.listcombiner.OrderedListCombiner;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Fast implementation of directed weighted edges for multigraphs.
 * @author Javier Sanz-Cruzado Puig
 */
public class FastDirectedWeightedMultiEdges extends FastMultiEdges implements DirectedMultiEdges, WeightedMultiEdges
{

    /**
     * Constructor.
     */
    public FastDirectedWeightedMultiEdges()
    {
        super(new FastWeightedAutoRelation<>(), new FastWeightedAutoRelation<>());
    }

    @Override
    public Stream<Integer> getIncidentNodes(int node)
    {
        return this.weights.getIdsFirst(node).map(IdxValue::getIdx);
    }

    @Override
    public Stream<Integer> getAdjacentNodes(int node)
    {
         return this.weights.getIdsSecond(node).map(IdxValue::getIdx);
    }

    @Override
    public Stream<MultiEdgeTypes> getIncidentTypes(int node)
    {
        return this.types.getIdsFirst(node).map(type -> new MultiEdgeTypes(type.getIdx(),type.getValue()));
    }

    @Override
    public Stream<MultiEdgeTypes> getAdjacentTypes(int node)
    {
        return this.types.getIdsSecond(node).map(type -> new MultiEdgeTypes(type.getIdx(),type.getValue()));
    }

    @Override
    public Stream<MultiEdgeWeights> getIncidentWeight(int node)
    {
        return this.weights.getIdsFirst(node).map(weight -> new MultiEdgeWeights(weight.getIdx(), weight.getValue()));
    }

    @Override
    public Stream<MultiEdgeWeights> getAdjacentWeight(int node)
    {
        return this.weights.getIdsSecond(node).map(weight -> new MultiEdgeWeights(weight.getIdx(), weight.getValue()));
    }
    
    @Override
    public Stream<MultiEdgeWeights> getNeighbourWeight(int node)
    {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    @Override
    public boolean addEdge(int orig, int dest, double weight, int type)
    {
        boolean failed;
       if(this.weights.containsPair(orig, dest))
       {
           List<Double> weightList = this.weights.getValue(orig, dest);
           weightList.add(weight);
           
           List<Integer> typeList = this.types.getValue(orig, dest);
           typeList.add(type);
           
           failed = this.weights.updatePair(orig, dest, weightList) & this.types.updatePair(orig, dest, typeList);
       }
       else
       {
           List<Double> weightList = new ArrayList<>();
           weightList.add(weight);
           
           List<Integer> typeList = new ArrayList<>();
           typeList.add(type);
           
           failed = this.weights.addRelation(orig, dest, weightList) & this.types.addRelation(orig, dest, typeList);
       }
       
       if(failed)
           this.numEdges++;
       return failed;
    }
    
    @Override
    public IntStream getNodesWithIncidentEdges() 
    {
        return this.weights.secondsWithFirsts();
    }

    @Override
    public IntStream getNodesWithAdjacentEdges() 
    {
        return this.weights.firstsWithSeconds();
    }

   
    @Override
    public IntStream getNodesWithEdges() 
    {
        Iterator<Integer> iteratorIncident = this.getNodesWithIncidentEdges().iterator();
        Iterator<Integer> iteratorAdjacent = this.getNodesWithAdjacentEdges().iterator();
        
        List<Integer> users = OrderedListCombiner.mergeLists(iteratorAdjacent, iteratorIncident, Comparator.naturalOrder(), (x, y) -> x);
        return users.stream().mapToInt(x->x);
    }
    
    @Override
    public IntStream getNodesWithMutualEdges()
    {
        List<Integer> users = new ArrayList<>();
        
        Iterator<Integer> iteratorIncident = this.getNodesWithIncidentEdges().iterator();
        while(iteratorIncident.hasNext())
        {
            int idx = iteratorIncident.next();
            Stream<Integer> incident = this.getIncidentNodes(idx);
            Stream<Integer> adjacent = this.getAdjacentNodes(idx);
            
            boolean value = OrderedListCombiner.intersectionHaslements(incident, adjacent, Comparator.naturalOrder());
            if(value)
            {
                users.add(idx);
            }
        }
        
        return users.stream().mapToInt(x->x);
    }    
    
}