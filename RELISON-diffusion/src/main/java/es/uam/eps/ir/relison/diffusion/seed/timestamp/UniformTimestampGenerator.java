/*
 *  Copyright (C) 2024 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.seed.timestamp;

import java.util.Random;

/**
 * Assigns each seeded information piece a creation timestamp drawn uniformly at random from a {@code [min, max]}
 * range (both inclusive), so pieces become available at spread-out points during the diffusion. Provide a seed for
 * reproducibility.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class UniformTimestampGenerator implements TimestampGenerator
{
    /**
     * Minimum timestamp (inclusive).
     */
    private final long min;
    /**
     * Maximum timestamp (inclusive).
     */
    private final long max;
    /**
     * Random number generator.
     */
    private final Random rng;

    /**
     * Constructor with a non-reproducible random generator.
     * @param min the minimum timestamp (inclusive).
     * @param max the maximum timestamp (inclusive).
     */
    public UniformTimestampGenerator(long min, long max)
    {
        this(min, max, new Random());
    }

    /**
     * Constructor with a reproducible (seeded) random generator.
     * @param min  the minimum timestamp (inclusive).
     * @param max  the maximum timestamp (inclusive).
     * @param seed the seed for the random number generator.
     */
    public UniformTimestampGenerator(long min, long max, long seed)
    {
        this(min, max, new Random(seed));
    }

    /**
     * Constructor with a given random generator.
     * @param min the minimum timestamp (inclusive).
     * @param max the maximum timestamp (inclusive).
     * @param rng the random number generator.
     */
    public UniformTimestampGenerator(long min, long max, Random rng)
    {
        this.min = Math.min(min, max);
        this.max = Math.max(min, max);
        this.rng = rng;
    }

    @Override
    public long generate()
    {
        if (this.max <= this.min) return this.min;
        // Uniform in [min, max] (both inclusive). Assumes a sensible timestamp range (no overflow of max - min + 1).
        return this.min + (long) (this.rng.nextDouble() * (this.max - this.min + 1));
    }
}
