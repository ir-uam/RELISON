/*
 *  Copyright (C) 2024 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.seed.timestamp;

/**
 * Assigns the same, fixed creation timestamp to every seeded information piece. With a single timestamp, all pieces
 * are available from the first iteration and the dataset's set of distinct timestamps stays minimal.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class FixedTimestampGenerator implements TimestampGenerator
{
    /**
     * The timestamp assigned to every piece.
     */
    private final long timestamp;

    /**
     * Constructor.
     * @param timestamp the timestamp assigned to every piece.
     */
    public FixedTimestampGenerator(long timestamp)
    {
        this.timestamp = timestamp;
    }

    @Override
    public long generate()
    {
        return this.timestamp;
    }
}
