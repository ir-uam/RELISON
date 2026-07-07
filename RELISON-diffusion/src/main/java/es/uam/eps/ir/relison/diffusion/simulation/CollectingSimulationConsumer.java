/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.simulation;

import es.uam.eps.ir.relison.diffusion.data.Data;
import es.uam.eps.ir.relison.diffusion.io.backup.BinarySimulationWriter;

import java.io.Serializable;

/**
 * A {@link SimulationConsumer} that retains every iteration in a {@link Simulation}, reproducing the classic
 * (non-streaming) behaviour of {@link Simulator#simulate(String)}. Use this consumer when the full history is
 * needed afterwards (interactive inspection, resume, or adding metrics post-hoc). It also reproduces the periodic
 * binary backup that the simulator used to perform inline.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 */
public class CollectingSimulationConsumer<U extends Serializable, I extends Serializable, P> implements SimulationConsumer<U, I, P>
{
    /** The simulation being assembled. */
    private final Simulation<U, I, P> simulation;
    /** File to back the simulation up to (hourly), or {@code null} to disable backups. */
    private final String backup;
    /** Wall-clock time of the last backup. */
    private long lastBackup;

    /**
     * Constructor.
     * @param data          the simulation data.
     * @param initialNumber the number of the first iteration.
     * @param backup        file to back the simulation up to every hour, or {@code null} to disable backups.
     */
    public CollectingSimulationConsumer(Data<U, I, P> data, int initialNumber, String backup)
    {
        this.simulation = new Simulation<>(data, initialNumber);
        this.backup = backup;
    }

    @Override
    public void start(int initialIteration)
    {
        this.lastBackup = System.currentTimeMillis();
    }

    @Override
    public void accept(Iteration<U, I, P> iteration)
    {
        this.simulation.addIteration(iteration);

        // Each hour of simulation, store a backup to prevent losing a long run.
        if (this.backup != null && (System.currentTimeMillis() - this.lastBackup) > 3600L * 1000L)
        {
            BinarySimulationWriter<U, I, P> bsw = new BinarySimulationWriter<>();
            bsw.initialize(this.backup);
            bsw.writeSimulation(this.simulation);
            bsw.close();
            this.lastBackup = System.currentTimeMillis();
        }
    }

    /**
     * Obtains the assembled simulation.
     * @return the simulation with every produced iteration.
     */
    public Simulation<U, I, P> getSimulation()
    {
        return this.simulation;
    }
}
