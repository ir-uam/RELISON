/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.graph.edges;

import es.uam.eps.ir.ranksys.fast.preference.IdxPref;

/**
 * Class that represents the weight of the edges. Each weight is represented as
 * a double value. Value 1.0 is considered a default valid value, and NaN as a default
 * invalid value. Every other value has the interpretation the user wants to give
 * it.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class EdgeWeight extends IdxPref
{
    /**
     * Constructor.
     *
     * @param idx   Incoming node identifier.
     * @param value weight value.
     */
    public EdgeWeight(int idx, double value)
    {
        super(idx, value);
    }

    /**
     * Constructor. Assigns the default value weight.
     *
     * @param idx Incoming node identifier.
     */
    public EdgeWeight(int idx)
    {
        this(idx, getDefaultValue());
    }

    /**
     * Default value for the error type.
     *
     * @return The default value for the error type.
     */
    public static double getErrorValue()
    {
        return Double.NaN;
    }

    /**
     * Default valid type value.
     *
     * @return The default valid type value.
     */
    public static double getDefaultValue()
    {
        return 1.0;
    }

    /**
     * Given a weight, indicates whether it is the error value.
     * @param weight the weight value.
     * @return true if it is, false otherwise.
     */
    public static boolean isErrorValue(double weight)
    {
        return Double.isNaN(weight);
    }

    /**
     * Given a weight, indicates whether it is the default value.
     * @param weight the weight value.
     * @return true if it is, false otherwise.
     */
    public static boolean isDefaultValue(double weight)
    {
        return weight == 1.0;
    }

}
