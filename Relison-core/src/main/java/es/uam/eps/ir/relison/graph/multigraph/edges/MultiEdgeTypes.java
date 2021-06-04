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
 * Class that represents the type of the multiedges. Each type is represented as
 * an integer. Value 0 is considered a default valid value, and -1 as a default
 * invalid value. Every other value has the interpretation the user wants to give
 * it.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class MultiEdgeTypes extends IdxValue<List<Integer>>
{
    /**
     * Constructor
     *
     * @param idx   user identifier
     * @param value list of values
     */
    public MultiEdgeTypes(int idx, List<Integer> value)
    {
        super(idx, value);
    }

    /**
     * Constructor
     *
     * @param idx user identifier
     */
    public MultiEdgeTypes(int idx)
    {
        this(idx, new ArrayList<>());
    }

    /**
     * Default value for the error type.
     *
     * @return The default value for the error type.
     */
    public static List<Integer> getErrorType()
    {
        return null;
    }


    public static List<Integer> getDefaultValue(int length)
    {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < length; ++i)
        {
            list.add(getDefaultValue());
        }
        return list;
    }

    /**
     * Default valid type value.
     *
     * @return The default valid type value.
     */
    public static int getDefaultValue()
    {
        return 0;
    }


}
