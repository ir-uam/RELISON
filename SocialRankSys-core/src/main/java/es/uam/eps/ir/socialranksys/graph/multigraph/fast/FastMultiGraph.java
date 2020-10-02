/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.multigraph.fast;


import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.Weight;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeType;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeWeight;
import es.uam.eps.ir.socialranksys.graph.multigraph.MultiGraph;
import es.uam.eps.ir.socialranksys.graph.multigraph.Weights;
import es.uam.eps.ir.socialranksys.graph.multigraph.edges.MultiEdges;
import es.uam.eps.ir.socialranksys.index.Index;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Fast implementation of a multi graph
 *
 * @param <U> User type
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class FastMultiGraph<U> implements MultiGraph<U>, Serializable
{
    /**
     * Index of vertices
     */
    protected final Index<U> vertices;
    /**
     * Edges in the network
     */
    protected final MultiEdges edges;

    /**
     * Constructor
     *
     * @param vertices A index for the vertices of the graph
     * @param edges    Edges
     */
    public FastMultiGraph(Index<U> vertices, MultiEdges edges)
    {
        this.vertices = vertices;
        this.edges = edges;
    }

    @Override
    public boolean addNode(U node)
    {
        if (vertices.containsObject(node))
        {
            return false;
        }
        int idx = vertices.addObject(node);

        if (idx != -1)
        {
            return edges.addUser(idx);
        }
        return false;
    }

    @Override
    public boolean addEdge(U nodeA, U nodeB, double weight, int type, boolean insertNodes)
    {
        if (insertNodes)
        {
            if (this.addNode(nodeA))
            {
                this.edges.addUser(EdgeType.getDefaultValue());
            }
            if (this.addNode(nodeB))
            {
                this.edges.addUser(EdgeType.getDefaultValue());
            }
        }

        if (this.containsVertex(nodeA) && this.containsVertex(nodeB))
        {
            return this.edges.addEdge(this.vertices.object2idx(nodeA), this.vertices.object2idx(nodeB), weight, type);
        }
        return false;
    }

    @Override
    public Stream<U> getAllNodes()
    {
        return this.vertices.getAllObjects();
    }

    @Override
    public Stream<U> getIncidentNodes(U node)
    {
        return this.edges.getIncidentNodes(this.vertices.object2idx(node)).map(this.vertices::idx2object);
    }

    @Override
    public Stream<U> getAdjacentNodes(U node)
    {
        return this.edges.getAdjacentNodes(this.vertices.object2idx(node)).map(this.vertices::idx2object);
    }

    @Override
    public Stream<U> getNeighbourNodes(U node)
    {
        return this.edges.getNeighbourNodes(this.vertices.object2idx(node)).map(this.vertices::idx2object);
    }

    @Override
    public Stream<U> getNeighbourhood(U node, EdgeOrientation direction)
    {
        return switch (direction)
        {
            case IN -> this.getIncidentNodes(node);
            case OUT -> this.getAdjacentNodes(node);
            default -> this.getNeighbourNodes(node);
        };
    }

    @Override
    public int getIncidentEdgesCount(U node)
    {
        return this.edges.getIncidentCount(this.vertices.object2idx(node));
    }

    @Override
    public int getAdjacentEdgesCount(U node)
    {
        return this.edges.getAdjacentCount(this.vertices.object2idx(node));
    }

    @Override
    public int getNeighbourEdgesCount(U node)
    {
        return this.edges.getNeighbourCount(this.vertices.object2idx(node));
    }

    @Override
    public int getNeighbourhoodSize(U node, EdgeOrientation direction)
    {
        return switch (direction)
        {
            case IN -> this.getIncidentNodesCount(node);
            case OUT -> this.getAdjacentNodesCount(node);
            default -> this.getNeighbourNodesCount(node);
        };
    }

    @Override
    public boolean containsVertex(U node)
    {
        return this.vertices.containsObject(node);
    }

    @Override
    public boolean containsEdge(U nodeA, U nodeB)
    {
        if (this.containsVertex(nodeA) && this.containsVertex(nodeB))
        {
            return this.edges.containsEdge(this.vertices.object2idx(nodeA), this.vertices.object2idx(nodeB));
        }
        return false;
    }

    @Override
    public double getEdgeWeight(U nodeA, U nodeB)
    {
        List<Double> weights = this.edges.getEdgeWeights(this.vertices.object2idx(nodeA), this.vertices.object2idx(nodeB));
        if (weights != null && weights.size() > 0)
        {
            return weights.get(0);
        }
        return EdgeWeight.getErrorValue();
    }

    @Override
    public Stream<Weight<U, Double>> getIncidentNodesWeights(U node)
    {
        List<Weight<U, Double>> weights = new ArrayList<>();

        this.edges.getIncidentWeight(this.vertices.object2idx(node)).forEach(weight -> weight.getValue().forEach(w ->
             weights.add(new Weight<>(this.vertices.idx2object(weight.getIdx()), w))));

        return weights.stream();
    }

    @Override
    public Stream<Weight<U, Double>> getAdjacentNodesWeights(U node)
    {
        List<Weight<U, Double>> weights = new ArrayList<>();

        this.edges.getAdjacentWeight(this.vertices.object2idx(node)).forEach(weight -> weight.getValue().forEach(w -> weights.add(new Weight<>(this.vertices.idx2object(weight.getIdx()), w))));

        return weights.stream();
    }

    @Override
    public Stream<Weight<U, Double>> getNeighbourNodesWeights(U node)
    {
        List<Weight<U, Double>> weights = new ArrayList<>();

        this.edges.getNeighbourWeight(this.vertices.object2idx(node)).forEach(weight -> weight.getValue().forEach(w ->
            weights.add(new Weight<>(this.vertices.idx2object(weight.getIdx()), w))));

        return weights.stream();
    }

    @Override
    public Stream<Weight<U, Double>> getNeighbourhoodWeights(U node, EdgeOrientation direction)
    {
        return switch (direction)
        {
            case IN -> this.getIncidentNodesWeights(node);
            case OUT -> this.getAdjacentNodesWeights(node);
            default -> this.getNeighbourNodesWeights(node);
        };
    }

    @Override
    public int getEdgeType(U nodeA, U nodeB)
    {
        List<Integer> types = this.edges.getEdgeTypes(this.vertices.object2idx(nodeA), this.vertices.object2idx(nodeB));
        if (types != null && types.size() > 0)
        {
            return types.get(0);
        }
        return EdgeType.getErrorType();
    }

    @Override
    public Stream<Weight<U, Integer>> getIncidentNodesTypes(U node)
    {
        List<Weight<U, Integer>> types = new ArrayList<>();

        this.edges.getIncidentTypes(this.vertices.object2idx(node)).forEach(type -> type.getValue().forEach(t ->
            types.add(new Weight<>(this.vertices.idx2object(type.getIdx()), t))));

        return types.stream();
    }

    @Override
    public Stream<Weight<U, Integer>> getAdjacentNodesTypes(U node)
    {
        List<Weight<U, Integer>> types = new ArrayList<>();

        this.edges.getAdjacentTypes(this.vertices.object2idx(node)).forEach(type -> type.getValue().forEach(t -> types.add(new Weight<>(this.vertices.idx2object(type.getIdx()), t))));

        return types.stream();
    }

    @Override
    public Stream<Weight<U, Integer>> getNeighbourNodesTypes(U node)
    {
        List<Weight<U, Integer>> types = new ArrayList<>();

        this.edges.getNeighbourTypes(this.vertices.object2idx(node)).forEach(type -> type.getValue().forEach(t -> types.add(new Weight<>(this.vertices.idx2object(type.getIdx()), t))));

        return types.stream();
    }

    @Override
    public Stream<Weight<U, Integer>> getNeighbourhoodTypes(U node, EdgeOrientation direction)
    {
        return switch (direction)
        {
            case IN -> this.getIncidentNodesTypes(node);
            case OUT -> this.getAdjacentNodesTypes(node);
            default -> this.getNeighbourNodesTypes(node);
        };
    }

    @Override
    public long getVertexCount()
    {
        return this.vertices.numObjects();
    }

    @Override
    public int getNumEdges(U nodeA, U nodeB)
    {
        return this.edges.getNumEdges(this.vertices.object2idx(nodeA), this.vertices.object2idx(nodeB));
    }

    @Override
    public List<Double> getEdgeWeights(U nodeA, U nodeB)
    {
        return this.edges.getEdgeWeights(this.vertices.object2idx(nodeA), this.vertices.object2idx(nodeB));
    }

    @Override
    public Stream<Weights<U, Double>> getIncidentNodesWeightsLists(U node)
    {
        return this.edges.getIncidentWeight(this.vertices.object2idx(node))
                .map((weight) -> new Weights<>(this.vertices.idx2object(weight.getIdx()), weight.getValue()));
    }

    @Override
    public Stream<Weights<U, Double>> getAdjacentNodesWeightsLists(U node)
    {
        return this.edges.getAdjacentWeight(this.vertices.object2idx(node))
                .map((weight) -> new Weights<>(this.vertices.idx2object(weight.getIdx()), weight.getValue()));
    }

    @Override
    public Stream<Weights<U, Double>> getNeighbourNodesWeightsLists(U node)
    {
        return this.edges.getIncidentWeight(this.vertices.object2idx(node))
                .map((weight) -> new Weights<>(this.vertices.idx2object(weight.getIdx()), weight.getValue()));
    }

    @Override
    public Stream<Weights<U, Double>> getNeighbourhoodWeightsLists(U node, EdgeOrientation orientation)
    {
        return this.edges.getNeighbourWeight(this.vertices.object2idx(node))
                .map((weight) -> new Weights<>(this.vertices.idx2object(weight.getIdx()), weight.getValue()));
    }

    @Override
    public List<Integer> getEdgeTypes(U nodeA, U nodeB)
    {
        return this.edges.getEdgeTypes(this.vertices.object2idx(nodeA), this.vertices.object2idx(nodeB));
    }

    @Override
    public Stream<Weights<U, Integer>> getIncidentNodesTypesLists(U node)
    {
        return this.edges.getIncidentTypes(this.vertices.object2idx(node))
                .map((weight) -> new Weights<>(this.vertices.idx2object(weight.getIdx()), weight.getValue()));
    }

    @Override
    public Stream<Weights<U, Integer>> getAdjacentNodesTypesLists(U node)
    {
        return this.edges.getAdjacentTypes(this.vertices.object2idx(node))
                .map((weight) -> new Weights<>(this.vertices.idx2object(weight.getIdx()), weight.getValue()));
    }

    @Override
    public Stream<Weights<U, Integer>> getNeighbourNodesTypesLists(U node)
    {
        return this.edges.getNeighbourTypes(this.vertices.object2idx(node))
                .map((weight) -> new Weights<>(this.vertices.idx2object(weight.getIdx()), weight.getValue()));
    }

    @Override
    public long getEdgeCount()
    {
        return this.edges.getNumEdges();
    }

    @Override
    public Graph<U> complement()
    {
        throw new UnsupportedOperationException("The multigraph cannot be complemented");
    }

    @Override
    public boolean updateEdgeWeight(U orig, U dest, double weight)
    {
        throw new UnsupportedOperationException("Edges weights cannot be updated in multigraphs");
    }

    @Override
    public int object2idx(U u)
    {
        return this.vertices.object2idx(u);
    }

    @Override
    public U idx2object(int idx)
    {
        return this.vertices.idx2object(idx);
    }

    @Override
    public Stream<U> getIsolatedNodes()
    {
        return this.edges.getIsolatedNodes().mapToObj(vertices::idx2object);
    }

    @Override
    public Stream<U> getNodesWithEdges(EdgeOrientation orient)
    {
        return switch (orient)
        {
            case IN -> this.edges.getNodesWithIncidentEdges().mapToObj(vertices::idx2object);
            case OUT -> this.edges.getNodesWithAdjacentEdges().mapToObj(vertices::idx2object);
            case UND -> this.edges.getNodesWithEdges().mapToObj(vertices::idx2object);
            case MUTUAL -> this.edges.getNodesWithMutualEdges().mapToObj(vertices::idx2object);
            default -> null;
        };
    }

    @Override
    public Stream<U> getNodesWithAdjacentEdges()
    {
        return this.getNodesWithEdges(EdgeOrientation.OUT);
    }

    @Override
    public Stream<U> getNodesWithIncidentEdges()
    {
        return this.getNodesWithEdges(EdgeOrientation.IN);
    }

    @Override
    public Stream<U> getNodesWithEdges()
    {
        return this.getNodesWithEdges(EdgeOrientation.UND);
    }

    @Override
    public Stream<U> getNodesWithMutualEdges()
    {
        return this.getNodesWithEdges(EdgeOrientation.MUTUAL);
    }

    @Override
    public boolean hasAdjacentEdges(U u)
    {
        return this.edges.hasAdjacentEdges(this.object2idx(u));
    }

    @Override
    public boolean hasIncidentEdges(U u)
    {
        return this.edges.hasIncidentEdges(this.object2idx(u));
    }

    @Override
    public boolean hasEdges(U u)
    {
        return this.edges.hasEdges(this.object2idx(u));
    }

    @Override
    public boolean hasMutualEdges(U u)
    {
        return this.edges.hasMutualEdges(this.object2idx(u));
    }
}
