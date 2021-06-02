/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.utils.generator;

/**
 * Generator examples.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Generators
{
    /**
     * Integer generator.
     */
    public static Generator<Integer> intgen = new Generator<>()
    {
        /**
         * Previous generated element
         */
        int prev = -1;

        @Override
        public Integer generate()
        {
            ++prev;
            return prev;
        }

        @Override
        public void reset()
        {
            prev = -1;
        }

        @Override
        public void reset(Integer val)
        {
            prev = val;
        }
    };

    /**
     * Long generator.
     */
    public static Generator<Long> longgen = new Generator<>()
    {
        /**
         * Previous generated element
         */
        long prev = -1L;

        @Override
        public Long generate()
        {
            ++prev;
            return prev;
        }

        @Override
        public void reset()
        {
            prev = -1L;
        }

        @Override
        public void reset(Long val)
        {
            prev = val;
        }
    };

    /**
     * Float generator.
     */
    public static Generator<Float> floatgen = new Generator<>()
    {
        /**
         * Previous generated element
         */
        float prev = -1f;

        @Override
        public Float generate()
        {
            ++prev;
            return prev;
        }

        @Override
        public void reset()
        {
            prev = -1f;
        }

        @Override
        public void reset(Float val)
        {
            prev = val;
        }

    };

    /**
     * Double generator.
     */
    public static Generator<Double> doublegen = new Generator<>()
    {
        /**
         * Previous generated element
         */
        double prev = -1.0;

        @Override
        public Double generate()
        {
            ++prev;
            return prev;
        }

        @Override
        public void reset()
        {
            prev = -1.0;
        }

        @Override
        public void reset(Double val)
        {
            prev = val;
        }

    };

    /**
     * String generator.
     */
    public static Generator<String> stringgen = new Generator<>()
    {
        /**
         * Previous generated element
         */
        int prev = -1;

        @Override
        public String generate()
        {
            ++prev;
            return "" + prev;
        }

        @Override
        public void reset()
        {
            prev = -1;
        }

        @Override
        public void reset(String val)
        {
            prev = Integer.parseInt(val);
        }

    };
}
