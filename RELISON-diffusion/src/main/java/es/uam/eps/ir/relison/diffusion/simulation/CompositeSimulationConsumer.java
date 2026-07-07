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
import java.util.List;

/**
 * A {@link SimulationConsumer} that forwards every callback to several delegate consumers, so a single simulation
 * pass can, for example, both collect the iterations into a {@link Simulation} and fold them into metric
 * accumulators.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 */
public class CompositeSimulationConsumer<U extends Serializable, I extends Serializable, P> implements SimulationConsumer<U, I, P>
{
    /** The delegate consumers, invoked in order. */
    private final List<SimulationConsumer<U, I, P>> consumers;

    /**
     * Constructor.
     * @param consumers the delegate consumers, invoked in order for every callback.
     */
    @SafeVarargs
    public CompositeSimulationConsumer(SimulationConsumer<U, I, P>... consumers)
    {
        this.consumers = List.of(consumers);
    }

    @Override
    public void start(int initialIteration)
    {
        this.consumers.forEach(c -> c.start(initialIteration));
    }

    @Override
    public void accept(Iteration<U, I, P> iteration)
    {
        this.consumers.forEach(c -> c.accept(iteration));
    }

    @Override
    public void finish()
    {
        this.consumers.forEach(SimulationConsumer::finish);
    }
}
