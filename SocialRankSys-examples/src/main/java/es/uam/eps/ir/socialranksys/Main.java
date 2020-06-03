/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

/**
 * Class for applying the main methods to the executions.
 * @author Javier Sanz-Cruzado Puig
 */
public class Main
{
    /**
     * Main method. Executes the main method in the class specified by the first
     * argument with the rest of execution arguments.
     *
     * @param args Arguments to select the class whose main method's execute and
     * arguments to execute it.
     */
    public static void main(String[] args)
    {
        try
        {
            String main = args[0];
            System.out.println(main);
            String[] executionArgs = Arrays.copyOfRange(args, 1, args.length);
            Class[] argTypes = {executionArgs.getClass()};
            Object[] passedArgs = {executionArgs};
            Class.forName(main).getMethod("main", argTypes).invoke(null, passedArgs);
        }
        catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex)
        {
            System.err.println("The execution arguments were not right");
            ex.printStackTrace();
        }
    }
}