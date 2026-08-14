/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.diffusion.data.Data;
import es.uam.eps.ir.relison.diffusion.io.backup.BinarySimulationWriter;
import es.uam.eps.ir.relison.diffusion.simulation.Iteration;
import es.uam.eps.ir.relison.diffusion.simulation.Simulation;
import es.uam.eps.ir.relison.diffusion.simulation.SimulationConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A {@link SimulationConsumer} that streams each iteration to a binary file as it is produced (via
 * {@link BinarySimulationWriter}) and keeps only a compact per-iteration summary (the propagating and newly-informed
 * users, for the time-scrubbing canvas) in memory.
 *
 * <p>The full {@link Simulation} is therefore never retained, so the peak memory of a run is proportional to a single
 * iteration plus the summaries, rather than to the whole simulation. The per-node / per-piece / distribution endpoints
 * read the iterations back from the file on demand (see {@link DiffusionResult#eachIteration}).</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public final class StreamingSimulationConsumer implements SimulationConsumer<String, String, String>
{
    private final Data<String, String, String> data;
    private final String file;
    private final BinarySimulationWriter<String, String, String> writer = new BinarySimulationWriter<>();
    private final List<DiffusionResult.IterationSummary> summaries = new ArrayList<>();
    private boolean ok;

    /**
     * @param data the diffusion data (used to resolve identifiers when writing).
     * @param file the path of the binary file the iterations are streamed to.
     */
    public StreamingSimulationConsumer(Data<String, String, String> data, String file)
    {
        this.data = data;
        this.file = file;
    }

    @Override
    public void start(int initialIteration)
    {
        this.ok = writer.initialize(file);
        // Fail loudly rather than silently producing an empty file (which would leave the per-node / per-piece plots
        // blank while the metrics still worked).
        if (!this.ok) throw new IllegalStateException("Could not open the simulation backing file: " + file);
    }

    @Override
    public void accept(Iteration<String, String, String> iteration)
    {
        if (!ok) return;
        // Cooperative cancellation: the simulation runs on a worker thread; when the Stop button interrupts it, bail
        // out between iterations so the run halts promptly instead of finishing the whole simulation in the background.
        if (Thread.currentThread().isInterrupted())
            throw new java.util.concurrent.CancellationException("Diffusion simulation cancelled.");
        // Wrap the single iteration in a throwaway one-iteration simulation so the binary writer's per-iteration
        // format can be reused; nothing is retained once it has been written.
        Simulation<String, String, String> single = new Simulation<>(data, iteration.getIterationNumber());
        single.addIteration(iteration);
        writer.writeIteration(single, iteration.getIterationNumber());

        List<String> propagating = iteration.getPropagatingUsers().collect(Collectors.toList());
        List<String> newlyInformed = iteration.getReceivingUsers().collect(Collectors.toList());
        summaries.add(new DiffusionResult.IterationSummary(propagating, newlyInformed));
    }

    @Override
    public void finish()
    {
        writer.close();
    }

    /** @return the compact per-iteration summaries collected while streaming. */
    public List<DiffusionResult.IterationSummary> getSummaries()
    {
        return summaries;
    }

    /** @return the number of iterations written. */
    public int getNumIterations()
    {
        return summaries.size();
    }
}
