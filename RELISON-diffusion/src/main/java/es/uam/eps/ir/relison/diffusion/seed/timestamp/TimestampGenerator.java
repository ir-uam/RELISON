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
 * Assigns a creation timestamp to a seeded information piece. Called once per created piece by a
 * {@link es.uam.eps.ir.relison.diffusion.seed.PieceSeeder seeder}, so different pieces may be given different
 * timestamps (controlling when they become available during the diffusion).
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public interface TimestampGenerator
{
    /**
     * Generates the creation timestamp for the next information piece.
     * @return the timestamp.
     */
    long generate();
}
