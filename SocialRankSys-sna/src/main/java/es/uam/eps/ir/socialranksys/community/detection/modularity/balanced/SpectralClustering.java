/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.community.detection.modularity.balanced;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;
import cern.colt.matrix.linalg.EigenvalueDecomposition;
import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.community.clustering.KMeans;
import es.uam.eps.ir.socialranksys.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.index.Index;
import es.uam.eps.ir.socialranksys.index.fast.FastIndex;
import org.ranksys.core.util.tuples.Tuple2id;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Community detection algorithm for balanced communities.
 *
 * <p>
 * <b>Reference:</b> R. Zafarani, M.A. Abassi, H. Liu. Social Media Mining: An Introduction. Chapter 6. 2014
 * </p>
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class SpectralClustering<U> implements CommunityDetectionAlgorithm<U>
{
    /**
     * The number of clusters we want to find.
     */
    private final int k;

    /**
     * Constructor.
     *
     * @param k The number of clusters we want to find
     */
    public SpectralClustering(int k)
    {
        this.k = k;
    }

    @Override
    public Communities<U> detectCommunities(Graph<U> graph)
    {
        Index<U> index = new FastIndex<>();
        graph.getAllNodes().forEach(index::addObject);

        int vertexCount = Long.valueOf(graph.getVertexCount()).intValue();
        DoubleMatrix2D adjacencyMatrix = graph.getAdjacencyMatrix(EdgeOrientation.UND);

        DoubleMatrix2D degree = new SparseDoubleMatrix2D(vertexCount, vertexCount);
        for (int i = 0; i < vertexCount; ++i)
        {
            // To ensure the presence of eigenvalues, we 
            degree.setQuick(i, i, graph.getNeighbourhoodSize(index.idx2object(i), EdgeOrientation.UND));
        }

        DoubleMatrix2D laplacian = this.laplacian(degree, adjacencyMatrix);
        EigenvalueDecomposition eigenvalue = new EigenvalueDecomposition(laplacian);

        DoubleMatrix2D eigenvectors = eigenvalue.getV();
        DoubleMatrix2D eigenvaluesMatr = eigenvalue.getD();

        List<Tuple2id> eigenvalues = new ArrayList<>();
        for (int i = 0; i < vertexCount; ++i)
        {
            eigenvalues.add(new Tuple2id(i, eigenvaluesMatr.getQuick(i, i)));
        }

        // Sort eigenvalues from smallest to biggest
        eigenvalues.sort(Comparator.comparingDouble(x -> x.v2));

        int length = Math.min(k, vertexCount);
        List<DoubleMatrix1D> vectors = new ArrayList<>();
        for (int i = 0; i < graph.getVertexCount(); ++i)
        {
            DoubleMatrix1D vector = new DenseDoubleMatrix1D(length - 1);
            for (int j = 1; j < length; ++j)
            {
                int col = eigenvalues.get(j).v1;
                // Each column of eigenvaluesMatr represents the eigenvector, and each 
                vector.setQuick(j - 1, eigenvectors.getQuick(i, col));
            }
            vectors.add(vector);
        }

        // Apply kmeans over the resulting vectors.
        KMeans kmeans = new KMeans(this.k);
        Communities<Integer> clusters = kmeans.cluster(vectors, length - 1, true);

        Communities<U> comms = new Communities<>();
        clusters.getCommunities().forEach(c -> {
            comms.addCommunity();
            clusters.getUsers(c).forEach(uIdx -> comms.add(index.idx2object(uIdx), comms.getNumCommunities() - 1));
        });

        return comms;
    }

    protected abstract DoubleMatrix2D laplacian(DoubleMatrix2D degree, DoubleMatrix2D adjacency);


}
