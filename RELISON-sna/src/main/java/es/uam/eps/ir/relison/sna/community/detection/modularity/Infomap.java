/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.community.detection.modularity;

import es.uam.eps.ir.relison.utils.ComputationMonitor;

import es.uam.eps.ir.relison.sna.community.Communities;
import es.uam.eps.ir.relison.sna.community.detection.CommunityDetectionAlgorithm;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.index.Index;
import es.uam.eps.ir.relison.index.fast.FastIndex;
import es.uam.eps.ir.relison.io.graph.GraphWriter;
import es.uam.eps.ir.relison.io.graph.PajekGraphWriter;
import org.ranksys.formats.parsing.Parsers;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

/**
 * Community detection algorithm using the Infomap algorithm.
 * Uses the Infomap C++ implementation by Rosvall and Bergstrom (<a href="http://www.mapequation.org/code.html">link</a>), downloaded at 06 November 2018.
 *
 * <p>
 * <b>Reference:</b>  M. Rosvall and C. Bergstrom. Maps of random walks on complex networks reveal community structure. Proceedings of the National Academy of Sciences 105(4), pp. 1118-1123 (2008)
 * </p>
 *
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 * @author Sofía Marina Pepa (sofia.marinapepa@gmail.com)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class Infomap<U extends Serializable> implements CommunityDetectionAlgorithm<U>
{
    /**
     * Download link for the Infomap C++ library.
     */
    private final static String DOWNLOADLINK = "http://www.mapequation.org/code.html#Download-and-compile";
    /**
     * Pre-fixed random seed
     */
    private final static int RANDOMSEED = 345234;
    /**
     * Pre-fixed number of trials.
     */
    private final static int NUMTRIALS = 10;
    /**
     * Random seed.
     */
    public final int seed;
    /**
     * Executable path.
     */
    private String exec;
    /**
     * Temporary path where we want to store the auxiliar networks.
     */
    private final String temp;
    /**
     * Number of trials.
     */
    private final int numTrials;

    /**
     * Full constructor.
     *
     * @param exec      location of the executable
     * @param temp      location of temporal files.
     * @param numTrials number of trials before obtaining the communities.
     * @param seed      the random seed.
     */
    public Infomap(String exec, String temp, int numTrials, int seed)
    {
        this.exec = exec;

        this.temp = temp;
        this.numTrials = numTrials;
        this.seed = seed;
    }

    /**
     * Constructor. Sets the random seed at a prefixed value.
     *
     * @param exec      location of the executable.
     * @param temp      location of the temporal files.
     * @param numTrials number of trials before obtaining the communities.
     */
    public Infomap(String exec, String temp, int numTrials)
    {
        this(exec, temp, numTrials, RANDOMSEED);
    }

    /**
     * Constructor. Sets the number of trials and the random seed at a prefixed value.
     *
     * @param exec path to the Infomap executable for directed networks.
     * @param temp Temporary path where we want to store the auxiliar networks.
     */
    public Infomap(String exec, String temp)
    {
        this(exec, temp, NUMTRIALS, RANDOMSEED);
    }

    /**
     * Constructor. Sets the number of trials and the random seed at a prefixed value.
     * It copies the executable from the resources folder into the temporary files
     * folder.
     *
     * @param temp temporary path where we want to store all the auxiliar networks.
     */
    public Infomap(String temp)
    {
        this(null, temp, NUMTRIALS, RANDOMSEED);
    }

    /**
     * Constructor. Sets the number of trials and the random seed at a prefixed value.
     * It copies the executable from the resources folder into the temporary files
     * folder.
     *
     * @param temp      temporary path where we want to store all the auxiliar networks.
     * @param numTrials number of trials before obtaining the communities.
     */
    public Infomap(String temp, int numTrials)
    {
        this(null, temp, numTrials, RANDOMSEED);
    }

    /**
     * Constructor. Sets the number of trials and the random seed at a prefixed value.
     * It copies the executable from the resources folder into the temporary files
     * folder.
     *
     * @param temp      temporary path where we want to store all the auxiliar networks.
     * @param numTrials number of trials before obtaining the communities.
     * @param seed      the random seed.
     */
    public Infomap(String temp, int numTrials, int seed)
    {
        this(null, temp, numTrials, seed);
    }

    @Override
    public Communities<U> detectCommunities(Graph<U> graph)
    {
        ComputationMonitor.current().phase("Preparing Infomap");
        boolean bundled = exec == null;
        java.nio.file.Path directory;
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Path.of(temp));
            directory = java.nio.file.Files.createTempDirectory(java.nio.file.Path.of(temp), "infomap-");
        } catch (IOException error) { throw new UncheckedIOException(error); }
        String path = directory.toString(), network = "tmpNet";
        try {
            if (bundled) {
                try (InputStream input = getClass().getClassLoader().getResourceAsStream("Infomap")) {
                    if (input == null) throw new IOException("Bundled Infomap executable is unavailable");
                    java.nio.file.Path executable = directory.resolve("Infomap");
                    java.nio.file.Files.copy(input, executable);
                    executable.toFile().setExecutable(true);
                    exec = executable.toString();
                }
            }
            ComputationMonitor.current().checkCancelled();
            GraphWriter<U> writer = new PajekGraphWriter<>();
            if (!writer.write(graph, path + "/" + network + ".net"))
                throw new IOException("Could not write Infomap input graph");
            return detectCommunities(graph, network, path);
        } catch (IOException error) { throw new UncheckedIOException(error); }
        finally {
            if (bundled) exec = null;
            File[] generated = directory.toFile().listFiles();
            if (generated != null) for (File file : generated) file.delete();
            directory.toFile().delete();
        }
    }

    /**
     * Method that calls the Infomap community detector, and executes it.
     *
     * @param graph   the network graph.
     * @param network a name of a file containing the network.
     * @param path    the path where the network is contained.
     *
     * @return the communities
     */
    private Communities<U> detectCommunities(Graph<U> graph, String network, String path)
    {
        Communities<U> comms = new Communities<>();
        Index<U> index = new FastIndex<>();
        graph.getAllNodes().forEach(index::addObject);

        Process child = null;
        try
        {
            String command = exec;
            // In case the command is null (the executable does not exist, this fails and returns null communities).
            if (command == null)
            {
                return null;
            }

            ComputationMonitor monitor = ComputationMonitor.current();
            monitor.phase("Running Infomap");
            child = new ProcessBuilder(exec, path + "/" + network + ".net", path + "/",
                    "--input-format", "pajek", "--seed", Integer.toString(seed),
                    graph.isDirected() ? "--directed" : "--undirected", "--num-trials", Integer.toString(numTrials),
                    "--silent", "--clu", "--map")
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            while (!child.waitFor(200, java.util.concurrent.TimeUnit.MILLISECONDS)) monitor.checkCancelled();
            monitor.checkCancelled();
            if (child.exitValue() != 0) throw new IOException("Infomap exited with code " + child.exitValue());

            System.out.println("Communities detected");

            Map<Integer, Integer> aux = new HashMap<>();
            // Read the file containing the communities.
            FileInputStream clu = new FileInputStream(path + "/" + network + ".clu");

            try (BufferedReader br = new BufferedReader(new InputStreamReader(clu)))
            {
                String line = br.readLine(); // Remove the first line.
                line = br.readLine(); // Remove the second line
                while ((line = br.readLine()) != null)
                {
                    ComputationMonitor.current().checkCancelled();
                    String[] split = line.split("\\s+");
                    int userId = Parsers.ip.parse(split[0]) - 1;
                    int commId = Parsers.ip.parse(split[1]);

                    if (!aux.containsKey(commId))
                    {
                        aux.put(commId, aux.size());
                        comms.addCommunity();
                    }
                    comms.add(index.idx2object(userId), aux.get(commId));
                }
            }

            System.out.println("Communities found");

            // Return the communities
            return comms;
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt(); throw new java.util.concurrent.CancellationException("Infomap cancelled");
        } catch (IOException ex) { throw new UncheckedIOException(ex); }
        finally {
            if (child != null && child.isAlive()) {
                child.descendants().forEach(process -> process.destroyForcibly());
                child.destroyForcibly();
                boolean interrupted = Thread.interrupted();
                while (child.isAlive()) {
                    try { child.waitFor(); } catch (InterruptedException ex) { interrupted = true; }
                }
                if (interrupted) Thread.currentThread().interrupt();
            }
        }
    }


}
