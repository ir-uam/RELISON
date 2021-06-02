/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.graph.multigraph;

import java.util.List;


/**
 * Class for expressing weights
 *
 * @param <I> Type of the identifiers.
 * @param <W> Type of the different weight values.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Weights<I, W>
{
    /**
     * Identifier
     */
    private final I idx;
    /**
     * List of values
     */
    private final List<W> weights;

    /**
     * Constructor.
     *
     * @param idx     Identifier
     * @param weights List of values.
     */
    public Weights(I idx, List<W> weights)
    {
        this.idx = idx;
        this.weights = weights;
    }

    /**
     * Gets the identifier of the weight
     *
     * @return the identifier of the weight.
     */
    public I getIdx()
    {
        return this.idx;
    }

    /**
     * Gets the different values for the weight.
     *
     * @return a list containing the values.
     */
    public List<W> getValue()
    {
        return this.weights;
    }

}
