/*
 *  Copyright (C) 2024 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.seed;

import es.uam.eps.ir.relison.diffusion.seed.selector.AllUsersSelector;
import es.uam.eps.ir.relison.diffusion.seed.selector.UserSelector;
import es.uam.eps.ir.relison.diffusion.seed.timestamp.FixedTimestampGenerator;
import es.uam.eps.ir.relison.diffusion.seed.timestamp.TimestampGenerator;
import es.uam.eps.ir.relison.utils.generator.Generator;

import java.io.Serializable;
import java.util.Random;

/**
 * Piece seeder that gives each user a number of information pieces drawn from a Poisson distribution of mean
 * {@code lambda}. This models a population where users produce content at a common average rate but individual counts
 * vary (a more realistic "bursty" alternative to a uniform range). Provide a seed for reproducible datasets.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user / information piece features.
 */
public class PoissonPieceSeeder<U extends Serializable, I extends Serializable, F> extends AbstractPieceSeeder<U, I, F>
{
    /**
     * Mean (and variance) of the Poisson distribution the per-user counts are drawn from.
     */
    private final double lambda;
    /**
     * Random number generator for the per-user counts.
     */
    private final Random rng;

    /**
     * Constructor with a non-reproducible random generator; pieces are created at timestamp 0.
     * @param lambda         mean number of pieces per user (must be non-negative).
     * @param pieceGenerator generator for the information piece identifiers.
     */
    public PoissonPieceSeeder(double lambda, Generator<I> pieceGenerator)
    {
        this(lambda, pieceGenerator, 0L, new Random());
    }

    /**
     * Constructor with a reproducible (seeded) random generator; pieces are created at timestamp 0.
     * @param lambda         mean number of pieces per user (must be non-negative).
     * @param pieceGenerator generator for the information piece identifiers.
     * @param seed           seed for the random number generator.
     */
    public PoissonPieceSeeder(double lambda, Generator<I> pieceGenerator, long seed)
    {
        this(lambda, pieceGenerator, 0L, new Random(seed));
    }

    /**
     * Constructor seeding only a selected subset of the users, with a non-reproducible random generator; pieces are
     * created at timestamp 0.
     * @param lambda         mean number of pieces per user (must be non-negative).
     * @param pieceGenerator generator for the information piece identifiers.
     * @param selector       selects the subset of users that get seeded with pieces.
     */
    public PoissonPieceSeeder(double lambda, Generator<I> pieceGenerator, UserSelector<U> selector)
    {
        this(lambda, pieceGenerator, 0L, new Random(), selector);
    }

    /**
     * Full constructor, seeding all users.
     * @param lambda         mean number of pieces per user (must be non-negative).
     * @param pieceGenerator generator for the information piece identifiers.
     * @param timestamp      creation timestamp assigned to every seeded piece.
     * @param rng            the random number generator to use.
     */
    public PoissonPieceSeeder(double lambda, Generator<I> pieceGenerator, long timestamp, Random rng)
    {
        this(lambda, pieceGenerator, timestamp, rng, new AllUsersSelector<>());
    }

    /**
     * Full constructor with a fixed timestamp, seeding only a selected subset of the users.
     * @param lambda         mean number of pieces per user (must be non-negative).
     * @param pieceGenerator generator for the information piece identifiers.
     * @param timestamp      creation timestamp assigned to every seeded piece.
     * @param rng            the random number generator to use.
     * @param selector       selects the subset of users that get seeded with pieces.
     */
    public PoissonPieceSeeder(double lambda, Generator<I> pieceGenerator, long timestamp, Random rng, UserSelector<U> selector)
    {
        this(lambda, pieceGenerator, new FixedTimestampGenerator(timestamp), rng, selector);
    }

    /**
     * Constructor with a timestamp generator, seeding all users (with a non-reproducible count generator).
     * @param lambda         mean number of pieces per user (must be non-negative).
     * @param pieceGenerator generator for the information piece identifiers.
     * @param timestamps     assigns the creation timestamp of each piece.
     */
    public PoissonPieceSeeder(double lambda, Generator<I> pieceGenerator, TimestampGenerator timestamps)
    {
        this(lambda, pieceGenerator, timestamps, new Random(), new AllUsersSelector<>());
    }

    /**
     * Full constructor: a timestamp generator and a selected subset of users.
     * @param lambda         mean number of pieces per user (must be non-negative).
     * @param pieceGenerator generator for the information piece identifiers.
     * @param timestamps     assigns the creation timestamp of each piece.
     * @param rng            the random number generator to use for the counts.
     * @param selector       selects the subset of users that get seeded with pieces.
     */
    public PoissonPieceSeeder(double lambda, Generator<I> pieceGenerator, TimestampGenerator timestamps, Random rng, UserSelector<U> selector)
    {
        super(pieceGenerator, timestamps, selector);
        this.lambda = Math.max(0.0, lambda);
        this.rng = rng;
    }

    @Override
    protected int numPieces(U user)
    {
        // Knuth's algorithm for sampling from a Poisson distribution.
        double l = Math.exp(-this.lambda);
        int k = 0;
        double p = 1.0;
        do
        {
            ++k;
            p *= this.rng.nextDouble();
        }
        while (p > l);
        return k - 1;
    }
}
