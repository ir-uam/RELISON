/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.utils.datatypes;

import java.util.Collection;

/**
 * Class that represents a vector of real values.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Vector
{
    /**
     * Length of the vector
     */
    private final int length;
    /**
     * Values of the vector
     */
    private final double[] vector;
    /**
     * Norm of the vector
     */
    private double norm;

    /**
     * Constructor. Sets each coordinate to zero.
     *
     * @param length The length of the vector.
     */
    public Vector(int length)
    {
        this.length = length;
        this.vector = new double[length];
        for (int i = 0; i < length; ++i)
        {
            vector[i] = 0.0;
        }
        norm = 0.0;
    }

    /**
     * Constructor. Initializes the whole vector.
     *
     * @param length  The lenght of the vector.
     * @param vectors The coordinates of the vector.
     */
    public Vector(int length, double[] vectors)
    {
        this.length = length;
        this.vector = new double[length];
        System.arraycopy(vectors, 0, vector, 0, length);

        norm = this.norm();

    }

    /**
     * Computes the centroid of a group of vectors.
     *
     * @param vectors      the collection of vectors.
     * @param vectorLength the length of the vectors in the collection.
     *
     * @return a vector if everything is OK, null if not.
     */
    public static Vector centroid(Collection<Vector> vectors, int vectorLength)
    {
        if (vectorLength < 1) // If the length of the vector is impossible
        {
            return null;
        }

        // If the vector collection is empty, return a basic vector
        if (vectors == null || vectors.isEmpty())
        {
            return new Vector(vectorLength);
        }

        Vector vector = vectors.stream().reduce(new Vector(vectorLength), Vector::sum);
        double size = vectors.size() + 0.0;
        for (int i = 0; i < vectorLength; ++i)
        {
            vector.set(i, vector.get(i) / size);
        }

        return vector;
    }

    /**
     * Obtains the length of the vector.
     *
     * @return the length of the vector.
     */
    public int getLength()
    {
        return length;
    }

    /**
     * Obtains the values for all the coordinates of the vector.
     *
     * @return an array containing the coordinates of the vector.
     */
    public double[] getVector()
    {
        return vector;
    }

    /**
     * Obtains the norm of the vector.
     *
     * @return the norm of the vector.
     */
    public double getNorm()
    {
        return norm;
    }

    /**
     * Obtains the i-th coordinate of the vector
     *
     * @param i The coordinate to obtain
     *
     * @return If the coordinate was correct (in the range), the coordinate. NaN if not.
     */
    public double get(int i)
    {
        if (i < 0 || i >= this.length)
        {
            return Double.NaN;
        }
        else
        {
            return this.vector[i];
        }
    }

    /**
     * Sets the value of the i-th coordinate.
     *
     * @param i     The coordinate to modify.
     * @param value The new value.
     *
     * @return true if the coordinate was correctly modified (if the index is in range), false if not.
     */
    public boolean set(int i, double value)
    {
        if (i < 0 || i >= this.length)
        {
            return false;
        }
        this.vector[i] = value;
        norm = this.norm();
        return true;
    }

    /**
     * Returns a vector whose coordinates are opposite of the coordinates of the current vector
     *
     * @return a vector whose coordinates are opposite of the coordinates of the current vector
     */
    public Vector negate()
    {
        double[] v = new double[this.length];
        for (int i = 0; i < this.length; ++i)
        {
            v[i] = -this.vector[i];
        }

        return new Vector(this.length, v);
    }

    /**
     * Sums a vector to the current one.
     *
     * @param vector The new vector.
     *
     * @return A vector that sums both vectors if their lenght is the same, null if not.
     */
    public Vector sum(Vector vector)
    {
        if (this.length != vector.getLength())
        {
            return null;
        }

        double[] v = new double[this.length];

        for (int i = 0; i < this.length; ++i)
        {
            v[i] = this.vector[i] + vector.vector[i];
        }

        return new Vector(this.length, v);


    }

    /**
     * Computes the norm of the vector.
     *
     * @return the norm of the vector.
     */
    private double norm()
    {
        double aux = 0.0;

        for (int i = 0; i < this.length; ++i)
        {
            aux += this.vector[i] * this.vector[i];
        }

        return Math.sqrt(aux);
    }

    /**
     * Compute the scalar product between this vector and another one.
     *
     * @param vector The vector.
     *
     * @return the scalar product value if the length is the same, NaN if not.
     */
    public double scalarProd(Vector vector)
    {
        if (this.length != vector.getLength())
        {
            return Double.NaN;
        }

        double sum = 0.0;
        for (int i = 0; i < this.length; ++i)
        {
            sum += this.vector[i] * vector.vector[i];
        }

        return sum;
    }

    /**
     * Computes the cosine similarity between this vector and another one.
     *
     * @param vector The vector.
     *
     * @return the cosine similarity if the length is the same, NaN if not.
     */
    public double cosineSimilarity(Vector vector)
    {
        return this.scalarProd(vector) / (this.norm * vector.norm);
    }
}
