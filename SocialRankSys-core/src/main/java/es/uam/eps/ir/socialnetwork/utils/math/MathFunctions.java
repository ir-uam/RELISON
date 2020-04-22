/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.utils.math;

import java.util.function.DoubleUnaryOperator;

/**
 * Mathematical functions.
 * @author Javier Sanz-Cruzado Puig
 */
public class MathFunctions
{
    /**
     * Computes the sigmoid of a number.
     */
    public static DoubleUnaryOperator sigmoid = (double d) -> 1.0/(1.0 + Math.exp(-d));
}
