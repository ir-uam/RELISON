/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.simulation;

import java.io.Serializable;

/**
 * A consumer of the iterations produced by a {@link Simulator}, invoked as the simulation runs.
 *
 * <p>This enables <em>streaming</em> simulation: instead of forcing every {@link Iteration} to be retained in a
 * {@link Simulation}, the simulator hands each iteration to a consumer and may then discard it. Consumers that only
 * need running aggregates (e.g. per-iteration metric series) therefore run in memory proportional to a single
 * iteration plus their own accumulators, rather than to the whole simulation. A consumer that <em>does</em> need the
 * full history (interactive inspection, resume) can simply collect the iterations into a {@link Simulation}
 * (see {@code CollectingSimulationConsumer}).</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 */
public interface SimulationConsumer<U extends Serializable, I extends Serializable, P>
{
    /**
     * Called once before the first iteration is produced.
     * @param initialIteration the number of the first iteration that will be produced.
     */
    default void start(int initialIteration)
    {
    }

    /**
     * Called once per iteration, in order, with the freshly produced iteration.
     * @param iteration the iteration.
     */
    void accept(Iteration<U, I, P> iteration);

    /**
     * Called once after the last iteration has been produced.
     */
    default void finish()
    {
    }
}
