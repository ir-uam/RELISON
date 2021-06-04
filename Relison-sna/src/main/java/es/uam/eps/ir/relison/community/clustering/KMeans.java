/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.community.clustering;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import es.uam.eps.ir.ranksys.core.util.Stats;
import es.uam.eps.ir.relison.community.Communities;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Implementation of the k-means clustering algorithm.
 *
 * <p>
 * <b>Reference:</b> D. MacKay. An example inference task: clustering. Information Theory, Inference and Learning Algorithms, Chapter 20, pp. 284-292 (2003)
 * </p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class KMeans
{
    /**
     * Variation threshold.
     */
    private static final double THRESHOLD = 0.0001;
    /**
     * The number of desired clusters.
     */
    private final int k;

    /**
     * Constructor.
     *
     * @param k The number of desired clusters.
     */
    public KMeans(int k)
    {
        this.k = k;
    }

    /**
     * Obtains a partition of a group of examples in k groups.
     *
     * @param vectors   The examples.
     * @param length    Length of the vectors.
     * @param normalize True if the vectors have to be normalized, false if not.
     *
     * @return The partition of the examples in groups. If k is smaller than 1, an empty set
     *         of communities is returned.
     */
    public Communities<Integer> cluster(List<DoubleMatrix1D> vectors, int length, boolean normalize)
    {
        Communities<Integer> comms = new Communities<>();
        if (this.k == 1) // If we only look for a single group of communities.
        {
            comms.addCommunity();
            for (int i = 0; i < vectors.size(); ++i)
            {
                comms.add(i, comms.getNumCommunities() - 1);
            }
        }
        else if (this.k > vectors.size()) // Too many clusters -> Each point is a separate cluster.
        {
            for (int i = 0; i < vectors.size(); ++i)
            {
                comms.addCommunity();
                comms.add(i, comms.getNumCommunities() - 1);
            }
        }
        else if (this.k > 1) // If we look for more than one community
        {
            List<DoubleMatrix1D> normVectors;

            // If it is indicated, normalize the vectors, so that each coordinate has mean equal to 0
            // and variance equal to 1.
            if (normalize)
            {
                normVectors = this.normalize(vectors, length);
            }
            else
            {
                normVectors = vectors;
            }

            // Initialize.
            List<Integer> groups = new ArrayList<>();
            DoubleMatrix2D distance = new DenseDoubleMatrix2D(vectors.size(), k);
            List<DoubleMatrix1D> clusterVectors = new ArrayList<>();

            // Randomly initialize the weights of the vectors. To do that, we randomly select k different points
            // of data.
            Set<Integer> selectedInitialVectors = new HashSet<>();
            Random rnd = new Random();
            while (selectedInitialVectors.size() < k)
            {
                selectedInitialVectors.add(rnd.nextInt(k));
            }

            List<Integer> initialIndexes = new ArrayList<>(selectedInitialVectors);
            for (int i = 0; i < k; ++i)
            {
                DoubleMatrix1D vector = new DenseDoubleMatrix1D(length);
                DoubleMatrix1D auxVector = normVectors.get(initialIndexes.get(i));
                for (int j = 0; j < length; ++j)
                {
                    vector.setQuick(j, auxVector.getQuick(j));
                }
                clusterVectors.add(vector);
            }

            // Initialize the belonging to each group. Each element belongs to the same group.
            for (int i = 0; i < normVectors.size(); ++i)
            {
                groups.add(0);
            }

            // Old value of the target function
            double oldJ;
            // New value of the target function
            double newJ = Double.POSITIVE_INFINITY;
            do
            {
                oldJ = newJ;
                newJ = 0.0;

                // Compute the objective function J, and the distances between items and clusters.
                for (int i = 0; i < normVectors.size(); ++i)
                {
                    int auxI = i;
                    for (int j = 0; j < k; ++j)
                    {
                        int auxJ = j;
                        // Euclidean distance
                        double dist = IntStream.range(0, length).mapToDouble(l -> normVectors.get(auxI).getQuick(l) * clusterVectors.get(auxJ).getQuick(l)).sum();

                        // Store the distance
                        distance.setQuick(i, j, dist);

                        // If item i belongs to cluster j
                        if (groups.get(i).equals(j))
                        {
                            newJ += dist;
                        }
                    }
                }

                // If the algorithm has not converged
                if (Math.abs(oldJ - newJ) > THRESHOLD)
                {
                    // Expectation step -> update the community partition, fixing the vector representation.
                    for (int i = 0; i < normVectors.size(); ++i)
                    {
                        int auxI = i;
                        int min = 0;
                        double minDist = Double.POSITIVE_INFINITY;
                        for (int j = 0; j < k; ++j)
                        {
                            int auxJ = j;
                            // Compute the euclidean distance between the centroid of the cluster and the example.
                            double dist = IntStream.range(0, length).mapToDouble(l -> Math.pow(normVectors.get(auxI).getQuick(l) - clusterVectors.get(auxJ).getQuick(l), 2.0)).sum();
                            dist = Math.sqrt(dist);

                            if (dist < minDist)
                            {
                                min = j;
                                minDist = dist;
                            }
                        }

                        groups.set(i, min);
                    }

                    // Maximization step -> Update the vector representation for the partition, fixing the partition
                    for (int i = 0; i < k; ++i)
                    {
                        DoubleMatrix1D vector = new DenseDoubleMatrix1D(length);
                        int counter = 0;
                        for (int j = 0; j < normVectors.size(); ++j)
                        {
                            if (groups.get(j).equals(i))
                            {
                                vector.assign(normVectors.get(j), Double::sum);
                                counter++;
                            }
                        }

                        if (counter > 0)
                        {
                            for (int j = 0; j < length; ++j)
                            {
                                vector.setQuick(j, vector.getQuick(j) / (counter + 0.0));
                            }
                        }
                        clusterVectors.set(i, vector);
                    }
                }
            }
            while (Math.abs(oldJ - newJ) > THRESHOLD);

            // Store the community partition
            for (int i = 0; i < k; ++i)
            {
                comms.addCommunity();
            }
            for (int i = 0; i < normVectors.size(); ++i)
            {
                int comm = groups.get(i);
                comms.add(i, comm);
            }
        }

        // If k < 1, an empty set of communities is returned

        return comms;
    }

    /**
     * Given a group of vectors, normalizes them, so each coordinate has mean equal to 0
     * and variance equal to 1.
     *
     * @param vectors The vectors to normalize.
     * @param length  The length of the vectors.
     *
     * @return the normalized vectors.
     */
    public List<DoubleMatrix1D> normalize(List<DoubleMatrix1D> vectors, int length)
    {
        List<Stats> stats = new ArrayList<>();

        // First, we compute the mean and standard deviation of each coordinate.
        for (int i = 0; i < length; ++i)
        {
            stats.add(new Stats());
        }

        for (DoubleMatrix1D vector : vectors)
        {
            for (int i = 0; i < length; ++i)
            {
                stats.get(i).accept(vector.getQuick(i));
            }
        }

        // Standardize the vectors, so the mean of each coordinate is 0, and the 
        // standard deviation is equal to 1.
        List<DoubleMatrix1D> normed = new ArrayList<>();
        for (DoubleMatrix1D vector : vectors)
        {
            DoubleMatrix1D normedVector = new DenseDoubleMatrix1D(length);
            for (int i = 0; i < length; ++i)
            {
                double mean = stats.get(i).getMean();
                double stdev = stats.get(i).getStandardDeviation();
                if (stdev > 0)
                {
                    normedVector.setQuick(i, (vector.getQuick(i) - mean) / stdev);
                }
                else
                {
                    normedVector.setQuick(i, 0.0);
                }
            }
            normed.add(vector);
        }

        return normed;
    }
}
