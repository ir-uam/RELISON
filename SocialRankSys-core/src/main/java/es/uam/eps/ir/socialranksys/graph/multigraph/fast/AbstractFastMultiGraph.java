/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.graph.multigraph.fast;


import es.uam.eps.ir.ranksys.fast.preference.IdxPref;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.Weight;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeType;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeWeight;
import es.uam.eps.ir.socialranksys.graph.multigraph.Weights;
import es.uam.eps.ir.socialranksys.graph.multigraph.edges.MultiEdgeTypes;
import es.uam.eps.ir.socialranksys.graph.multigraph.edges.MultiEdgeWeights;
import es.uam.eps.ir.socialranksys.graph.multigraph.edges.MultiEdges;
import es.uam.eps.ir.socialranksys.index.Index;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Fast implementation of a multi graph
 *
 * @param <U> User type
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class AbstractFastMultiGraph<U> implements FastMultiGraph<U>, Serializable
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
    public AbstractFastMultiGraph(Index<U> vertices, MultiEdges edges)
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

        this.edges.getIncidentWeights(this.vertices.object2idx(node)).forEach(weight -> weight.getValue().forEach(w ->
             weights.add(new Weight<>(this.vertices.idx2object(weight.getIdx()), w))));

        return weights.stream();
    }

    @Override
    public Stream<Weight<U, Double>> getAdjacentNodesWeights(U node)
    {
        List<Weight<U, Double>> weights = new ArrayList<>();

        this.edges.getAdjacentWeights(this.vertices.object2idx(node)).forEach(weight -> weight.getValue().forEach(w -> weights.add(new Weight<>(this.vertices.idx2object(weight.getIdx()), w))));

        return weights.stream();
    }

    @Override
    public Stream<Weight<U, Double>> getNeighbourNodesWeights(U node)
    {
        List<Weight<U, Double>> weights = new ArrayList<>();

        this.edges.getNeighbourWeights(this.vertices.object2idx(node)).forEach(weight -> weight.getValue().forEach(w ->
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
        return this.edges.getIncidentWeights(this.vertices.object2idx(node))
                .map((weight) -> new Weights<>(this.vertices.idx2object(weight.getIdx()), weight.getValue()));
    }

    @Override
    public Stream<Weights<U, Double>> getAdjacentNodesWeightsLists(U node)
    {
        return this.edges.getAdjacentWeights(this.vertices.object2idx(node))
                .map((weight) -> new Weights<>(this.vertices.idx2object(weight.getIdx()), weight.getValue()));
    }

    @Override
    public Stream<Weights<U, Double>> getNeighbourNodesWeightsLists(U node)
    {
        return this.edges.getIncidentWeights(this.vertices.object2idx(node))
                .map((weight) -> new Weights<>(this.vertices.idx2object(weight.getIdx()), weight.getValue()));
    }

    @Override
    public Stream<Weights<U, Double>> getNeighbourhoodWeightsLists(U node, EdgeOrientation orientation)
    {
        return this.edges.getNeighbourWeights(this.vertices.object2idx(node))
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
        if(this.getNumEdges(orig, dest) == 1)
        {
            return this.edges.updateEdgeWeight(object2idx(orig), object2idx(dest), weight, 0);
        }
        throw new UnsupportedOperationException("The edge to update was not specified.");
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

    @Override
    public Stream<MultiEdgeWeights> getNeighbourhoodWeightsLists(int uidx, EdgeOrientation orientation)
    {
        return switch (orientation)
        {
            case IN -> this.edges.getIncidentWeights(uidx);
            case OUT -> this.edges.getAdjacentWeights(uidx);
            case UND -> this.edges.getNeighbourWeights(uidx);
            case MUTUAL -> this.edges.getMutualWeights(uidx);
        };

    }

    @Override
    public Stream<MultiEdgeTypes> getNeighbourhoodTypesLists(int uidx, EdgeOrientation orientation)
    {
        return switch (orientation)
        {
            case IN -> this.edges.getIncidentTypes(uidx);
            case OUT -> this.edges.getAdjacentTypes(uidx);
            case UND -> this.edges.getNeighbourTypes(uidx);
            case MUTUAL -> this.edges.getMutualTypes(uidx);
        };

    }

    @Override
    public int getNumEdges(int uidx, int vidx)
    {
        return this.edges.getNumEdges(uidx, vidx);
    }

    @Override
    public List<Double> getEdgeWeights(int uidx, int vidx)
    {
        return this.edges.getEdgeWeights(uidx, vidx);
    }

    @Override
    public List<Integer> getEdgeTypes(int uidx, int vidx)
    {
        return this.edges.getEdgeTypes(uidx, vidx);
    }

    @Override
    public Index<U> getIndex()
    {
        return this.vertices;
    }

    @Override
    public double getEdgeWeight(int uidx, int vidx)
    {
        return this.getEdgeWeights(uidx, vidx).stream().mapToDouble(x -> x).sum();
    }

    @Override
    public Stream<Integer> getNeighborhood(int uidx, EdgeOrientation orientation)
    {
        return switch (orientation)
        {
            case OUT -> this.edges.getAdjacentNodes(uidx);
            case IN -> this.edges.getIncidentNodes(uidx);
            case MUTUAL -> this.edges.getMutualNodes(uidx);
            default -> this.edges.getNeighbourNodes(uidx);
        };
    }

    @Override
    public Stream<IdxPref> getNeighborhoodWeights(int uidx, EdgeOrientation orientation)
    {
        List<IdxPref> weights = new ArrayList<>();

        switch (orientation)
        {
            case OUT -> this.edges.getAdjacentWeights(uidx).forEach(w -> w.getValue().forEach(val -> weights.add(new IdxPref(w.getIdx(), val))));
            case IN -> this.edges.getIncidentWeights(uidx).forEach(w -> w.getValue().forEach(val -> weights.add(new IdxPref(w.getIdx(), val))));
            case MUTUAL -> this.edges.getMutualWeights(uidx).forEach(w -> w.getValue().forEach(val -> weights.add(new IdxPref(w.getIdx(), val))));
            default -> this.edges.getNeighbourWeights(uidx).forEach(w -> w.getValue().forEach(val -> weights.add(new IdxPref(w.getIdx(), val))));
        }

        return weights.stream();
    }

    @Override
    public Stream<EdgeType> getNeighborhoodTypes(int uidx, EdgeOrientation orientation)
    {
        List<EdgeType> edgeTypes = new ArrayList<>();
        switch (orientation)
        {
            case OUT -> this.edges.getAdjacentTypes(uidx).forEach(w -> w.getValue().forEach(val -> edgeTypes.add(new EdgeType(w.getIdx(), val))));
            case IN -> this.edges.getIncidentTypes(uidx).forEach(w -> w.getValue().forEach(val -> edgeTypes.add(new EdgeType(w.getIdx(), val))));
            case MUTUAL -> this.edges.getMutualTypes(uidx).forEach(w -> w.getValue().forEach(val -> edgeTypes.add(new EdgeType(w.getIdx(), val))));
            default -> this.edges.getNeighbourTypes(uidx).forEach(w -> w.getValue().forEach(val -> edgeTypes.add(new EdgeType(w.getIdx(), val))));
        }

        return edgeTypes.stream();
    }

    @Override
    public IntStream getAllNodesIds()
    {
        return this.vertices.getAllObjectsIds();
    }

    @Override
    public boolean containsEdge(int uidx, int vidx)
    {
        return this.edges.containsEdge(uidx, vidx);
    }

    @Override
    public boolean addEdge(int nodeA, int nodeB, double weight, int type)
    {
        return this.edges.addEdge(nodeA, nodeB, weight, type);
    }

    @Override
    public boolean updateEdgeWeight(int nodeA, int nodeB, double weight)
    {
        if(this.getNumEdges(nodeA, nodeB) == 1)
        {
            return this.edges.updateEdgeWeight(nodeA, nodeB, weight, 0);
        }
        throw new UnsupportedOperationException("The edge to update was not specified.");
    }

    @Override
    public IntStream getNodesIdsWithEdges(EdgeOrientation direction)
    {
        switch (direction)
        {
            case IN:
                return this.edges.getNodesWithIncidentEdges();
            case OUT:
                return this.edges.getNodesWithAdjacentEdges();
            case UND:
                return this.edges.getNodesWithEdges();
            case MUTUAL:
                return this.edges.getNodesWithMutualEdges();
            default:
                break;
        }

        return IntStream.empty();
    }

    @Override
    public IntStream getIsolatedNodeIds()
    {
        return this.edges.getIsolatedNodes();
    }

    @Override
    public boolean removeEdge(U nodeA, U nodeB)
    {
        if(this.getNumEdges(nodeA, nodeB) == 1)
        {
            return this.removeEdges(nodeA, nodeB);
        }
        throw new UnsupportedOperationException("The edge to remove was not specified.");
    }

    @Override
    public boolean removeEdge(U nodeA, U nodeB, int idx)
    {
        return this.edges.removeEdge(this.object2idx(nodeA), this.object2idx(nodeB), idx);
    }

    @Override
    public boolean removeEdges(U nodeA, U nodeB)
    {
        return this.edges.removeEdges(this.object2idx(nodeA), this.object2idx(nodeB));
    }

    @Override
    public boolean removeNode(U node)
    {
        int uidx = this.vertices.object2idx(node);
        if(this.edges.removeNode(uidx))
        {
            return this.vertices.removeObject(node) >= 0;
        }
        return false;
    }

    @Override
    public boolean updateEdgeWeight(U orig, U dest, double weight, int idx)
    {
        return this.updateEdgeWeight(this.object2idx(orig), this.object2idx(dest), weight, idx);
    }

    @Override
    public boolean updateEdgeWeight(int uidx, int vidx, double weight, int idx)
    {
        return this.edges.updateEdgeWeight(uidx, vidx, weight, idx);
    }
}
