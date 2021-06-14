/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.graph.multigraph.edges;


import es.uam.eps.ir.relison.index.IdxValue;

import java.util.ArrayList;
import java.util.List;


/**
 * Class that represents the weight of the edges. Each type is represented as
 * an integer. Value 1.0 is considered a default valid value, and NaN as a default
 * invalid value. Every other value has the interpretation the user wants to give
 * it.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class MultiEdgeWeights extends IdxValue<List<Double>>
{
    /**
     * Constructor
     *
     * @param idx    identifier
     * @param values list of edge weights.
     */
    public MultiEdgeWeights(int idx, List<Double> values)
    {
        super(idx, values);
    }

    /**
     * Constructor
     *
     * @param idx identifier.
     */
    public MultiEdgeWeights(int idx)
    {
        this(idx, new ArrayList<>());
    }

    /**
     * Default value for the error type.
     *
     * @return The default value for the error type.
     */
    public static List<Double> getErrorType()
    {
        return null;
    }

    /**
     * Default valid type value.
     *
     * @param length the number of weights to return.
     *
     * @return The default valid type value.
     */
    public static List<Double> getDefaultValue(int length)
    {
        List<Double> list = new ArrayList<>();
        for (int i = 0; i < length; ++i)
        {
            list.add(getDefaultValue());
        }
        return list;
    }

    /**
     * Gets the default value for a weight.
     *
     * @return the default value for a weight.
     */
    public static double getDefaultValue()
    {
        return 1.0;
    }


}
