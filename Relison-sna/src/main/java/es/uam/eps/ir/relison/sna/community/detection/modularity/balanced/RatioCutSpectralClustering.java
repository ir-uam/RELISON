/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.community.detection.modularity.balanced;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.SparseDoubleMatrix2D;

/**
 * Community detection algorithm for balanced communities. It uses the ratio cut Laplacian.
 *
 * <p>
 * <b>Reference: </b> R. Zafarani, M.A. Abassi, H. Liu. Social Media Mining: An Introduction. Chapter 6. 2014
 * </p>
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class RatioCutSpectralClustering<U> extends SpectralClustering<U>
{

    /**
     * Constructor.
     *
     * @param k The number of clusters we want to find
     */
    public RatioCutSpectralClustering(int k)
    {
        super(k);
    }

    @Override
    protected DoubleMatrix2D laplacian(DoubleMatrix2D degree, DoubleMatrix2D adjacency)
    {
        DoubleMatrix2D laplacian = new SparseDoubleMatrix2D(degree.toArray());
        laplacian.assign(adjacency, (x, y) -> (x - y));
        return laplacian;
    }


}
