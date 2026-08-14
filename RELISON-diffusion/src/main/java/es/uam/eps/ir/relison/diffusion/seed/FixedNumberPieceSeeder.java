/*
 *  Copyright (C) 2024 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.seed;

import es.uam.eps.ir.relison.diffusion.seed.selector.UserSelector;
import es.uam.eps.ir.relison.diffusion.seed.timestamp.TimestampGenerator;
import es.uam.eps.ir.relison.utils.generator.Generator;

import java.io.Serializable;

/**
 * Piece seeder that gives <b>every</b> user the same, fixed number of information pieces. This is the deterministic
 * baseline seeder (the random seeders draw the per-user count from a distribution instead).
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user / information piece features.
 */
public class FixedNumberPieceSeeder<U extends Serializable, I extends Serializable, F> extends AbstractPieceSeeder<U, I, F>
{
    /**
     * Number of pieces created by each user.
     */
    private final int numPieces;

    /**
     * Constructor. Every piece is created at timestamp 0.
     * @param numPieces      number of pieces each user creates (negative values are treated as zero).
     * @param pieceGenerator generator for the information piece identifiers.
     */
    public FixedNumberPieceSeeder(int numPieces, Generator<I> pieceGenerator)
    {
        super(pieceGenerator);
        this.numPieces = numPieces;
    }

    /**
     * Constructor.
     * @param numPieces      number of pieces each user creates (negative values are treated as zero).
     * @param pieceGenerator generator for the information piece identifiers.
     * @param timestamp      creation timestamp assigned to every seeded piece.
     */
    public FixedNumberPieceSeeder(int numPieces, Generator<I> pieceGenerator, long timestamp)
    {
        super(pieceGenerator, timestamp);
        this.numPieces = numPieces;
    }

    /**
     * Constructor seeding only a selected subset of the users. Pieces are created at timestamp 0.
     * @param numPieces      number of pieces each selected user creates (negative values are treated as zero).
     * @param pieceGenerator generator for the information piece identifiers.
     * @param selector       selects the subset of users that get seeded with pieces.
     */
    public FixedNumberPieceSeeder(int numPieces, Generator<I> pieceGenerator, UserSelector<U> selector)
    {
        super(pieceGenerator, 0L, selector);
        this.numPieces = numPieces;
    }

    /**
     * Full constructor seeding only a selected subset of the users.
     * @param numPieces      number of pieces each selected user creates (negative values are treated as zero).
     * @param pieceGenerator generator for the information piece identifiers.
     * @param timestamp      creation timestamp assigned to every seeded piece.
     * @param selector       selects the subset of users that get seeded with pieces.
     */
    public FixedNumberPieceSeeder(int numPieces, Generator<I> pieceGenerator, long timestamp, UserSelector<U> selector)
    {
        super(pieceGenerator, timestamp, selector);
        this.numPieces = numPieces;
    }

    /**
     * Constructor with a timestamp generator, seeding all users.
     * @param numPieces      number of pieces each user creates (negative values are treated as zero).
     * @param pieceGenerator generator for the information piece identifiers.
     * @param timestamps     assigns the creation timestamp of each piece.
     */
    public FixedNumberPieceSeeder(int numPieces, Generator<I> pieceGenerator, TimestampGenerator timestamps)
    {
        super(pieceGenerator, timestamps);
        this.numPieces = numPieces;
    }

    /**
     * Full constructor: a timestamp generator and a selected subset of users.
     * @param numPieces      number of pieces each selected user creates (negative values are treated as zero).
     * @param pieceGenerator generator for the information piece identifiers.
     * @param timestamps     assigns the creation timestamp of each piece.
     * @param selector       selects the subset of users that get seeded with pieces.
     */
    public FixedNumberPieceSeeder(int numPieces, Generator<I> pieceGenerator, TimestampGenerator timestamps, UserSelector<U> selector)
    {
        super(pieceGenerator, timestamps, selector);
        this.numPieces = numPieces;
    }

    @Override
    protected int numPieces(U user)
    {
        return this.numPieces;
    }
}
