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
 * Piece seeder that gives each user a number of information pieces drawn uniformly at random from a
 * {@code [min, max]} range (both inclusive). Provide a seed to the random generator for reproducible datasets.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user / information piece features.
 */
public class UniformRandomPieceSeeder<U extends Serializable, I extends Serializable, F> extends AbstractPieceSeeder<U, I, F>
{
    /**
     * Minimum number of pieces a user may create (inclusive).
     */
    private final int min;
    /**
     * Maximum number of pieces a user may create (inclusive).
     */
    private final int max;
    /**
     * Random number generator for the per-user counts.
     */
    private final Random rng;

    /**
     * Constructor with a non-reproducible random generator; pieces are created at timestamp 0.
     * @param min            minimum number of pieces a user may create (inclusive, treated as zero if negative).
     * @param max            maximum number of pieces a user may create (inclusive).
     * @param pieceGenerator generator for the information piece identifiers.
     */
    public UniformRandomPieceSeeder(int min, int max, Generator<I> pieceGenerator)
    {
        this(min, max, pieceGenerator, 0L, new Random());
    }

    /**
     * Constructor with a reproducible (seeded) random generator; pieces are created at timestamp 0.
     * @param min            minimum number of pieces a user may create (inclusive, treated as zero if negative).
     * @param max            maximum number of pieces a user may create (inclusive).
     * @param pieceGenerator generator for the information piece identifiers.
     * @param seed           seed for the random number generator.
     */
    public UniformRandomPieceSeeder(int min, int max, Generator<I> pieceGenerator, long seed)
    {
        this(min, max, pieceGenerator, 0L, new Random(seed));
    }

    /**
     * Constructor seeding only a selected subset of the users, with a non-reproducible random generator; pieces are
     * created at timestamp 0.
     * @param min            minimum number of pieces a user may create (inclusive, treated as zero if negative).
     * @param max            maximum number of pieces a user may create (inclusive).
     * @param pieceGenerator generator for the information piece identifiers.
     * @param selector       selects the subset of users that get seeded with pieces.
     */
    public UniformRandomPieceSeeder(int min, int max, Generator<I> pieceGenerator, UserSelector<U> selector)
    {
        this(min, max, pieceGenerator, 0L, new Random(), selector);
    }

    /**
     * Full constructor, seeding all users.
     * @param min            minimum number of pieces a user may create (inclusive, treated as zero if negative).
     * @param max            maximum number of pieces a user may create (inclusive).
     * @param pieceGenerator generator for the information piece identifiers.
     * @param timestamp      creation timestamp assigned to every seeded piece.
     * @param rng            the random number generator to use.
     */
    public UniformRandomPieceSeeder(int min, int max, Generator<I> pieceGenerator, long timestamp, Random rng)
    {
        this(min, max, pieceGenerator, timestamp, rng, new AllUsersSelector<>());
    }

    /**
     * Full constructor with a fixed timestamp, seeding only a selected subset of the users.
     * @param min            minimum number of pieces a user may create (inclusive, treated as zero if negative).
     * @param max            maximum number of pieces a user may create (inclusive).
     * @param pieceGenerator generator for the information piece identifiers.
     * @param timestamp      creation timestamp assigned to every seeded piece.
     * @param rng            the random number generator to use.
     * @param selector       selects the subset of users that get seeded with pieces.
     */
    public UniformRandomPieceSeeder(int min, int max, Generator<I> pieceGenerator, long timestamp, Random rng, UserSelector<U> selector)
    {
        this(min, max, pieceGenerator, new FixedTimestampGenerator(timestamp), rng, selector);
    }

    /**
     * Constructor with a timestamp generator, seeding all users (with a non-reproducible count generator).
     * @param min            minimum number of pieces a user may create (inclusive, treated as zero if negative).
     * @param max            maximum number of pieces a user may create (inclusive).
     * @param pieceGenerator generator for the information piece identifiers.
     * @param timestamps     assigns the creation timestamp of each piece.
     */
    public UniformRandomPieceSeeder(int min, int max, Generator<I> pieceGenerator, TimestampGenerator timestamps)
    {
        this(min, max, pieceGenerator, timestamps, new Random(), new AllUsersSelector<>());
    }

    /**
     * Full constructor: a timestamp generator and a selected subset of users.
     * @param min            minimum number of pieces a user may create (inclusive, treated as zero if negative).
     * @param max            maximum number of pieces a user may create (inclusive).
     * @param pieceGenerator generator for the information piece identifiers.
     * @param timestamps     assigns the creation timestamp of each piece.
     * @param rng            the random number generator to use for the counts.
     * @param selector       selects the subset of users that get seeded with pieces.
     */
    public UniformRandomPieceSeeder(int min, int max, Generator<I> pieceGenerator, TimestampGenerator timestamps, Random rng, UserSelector<U> selector)
    {
        super(pieceGenerator, timestamps, selector);
        this.min = Math.max(0, min);
        this.max = Math.max(this.min, max);
        this.rng = rng;
    }

    @Override
    protected int numPieces(U user)
    {
        return this.min + this.rng.nextInt(this.max - this.min + 1);
    }
}
