/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms;

import org.jblas.DoubleMatrix;
import org.jblas.Solve;

/**
 * Checks whether we can use the JBLAS library, or we have to use the COLT one
 * to perform matrix-based recommendations.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class MatrixChecker
{
    /**
     * Indicates whether we have checked this or not.
     */
    public static boolean checked = false;
    /**
     * True if we can use the JBLAS library, false otherwise.
     */
    public static boolean fast = true;

    /**
     * Initializes the check.
     */
    public static void init()
    {
        if(!checked) // We first check whether we can use the JBLAS Library or not.
        {
            try
            {
                DoubleMatrix checkMatrix = DoubleMatrix.eye(2);
                DoubleMatrix eye = DoubleMatrix.eye(2);
                Solve.solve(checkMatrix, eye);
            }
            catch(UnsatisfiedLinkError error)
            {
                fast = false;
            }
            finally
            {
                checked = true;
            }
        }
    }
}