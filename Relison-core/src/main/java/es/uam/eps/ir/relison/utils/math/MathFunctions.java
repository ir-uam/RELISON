/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.utils.math;

import java.util.function.DoubleUnaryOperator;

/**
 * Mathematical functions.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class MathFunctions
{
    /**
     * Computes the sigmoid of a number.
     */
    public static DoubleUnaryOperator sigmoid = (double d) -> 1.0 / (1.0 + Math.exp(-d));
}
