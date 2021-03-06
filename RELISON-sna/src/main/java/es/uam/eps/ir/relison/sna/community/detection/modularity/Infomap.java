/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.sna.community.detection.modularity;

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
        boolean modexec = false;



        // First, we configure an auxiliar path for storing the Pajek graph.
        String network = "tmpNet", path = temp;
        new File(path).mkdir();

        // If exec does not exist, then, we copy it from the resources file:
        if(exec == null)
        {
            try
            {
                InputStream stream = getClass().getClassLoader().getResourceAsStream("Infomap");
                assert stream != null;
                stream = new BufferedInputStream(stream);

                File outputfile = new File(temp + File.separator + "Infomap");
                outputfile.setReadable(true, false);
                outputfile.setWritable(true, false);
                outputfile.setExecutable(true, false);

                OutputStream out = new BufferedOutputStream(new FileOutputStream(outputfile));

                byte[] buffer = new byte[1024];
                int lengthRead;
                while((lengthRead = stream.read(buffer)) > 0)
                {
                    out.write(buffer, 0, lengthRead);
                    out.flush();
                }
                this.exec = temp + File.separator + "Infomap";
                modexec = true;
                out.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        // Write the Pajek graph.
        GraphWriter<U> graphWriter = new PajekGraphWriter<>();
        graphWriter.write(graph, path + "/" + network + ".net");

        System.out.println("File written");

        Communities<U> communities = this.detectCommunities(graph, network, path);
        // Detect communities
        if(modexec)
        {
            exec = null;
        }
        return communities;
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

        try
        {
            String command = exec;
            // In case the command is null (the executable does not exist, this fails and returns null communities).
            if (command == null)
            {
                return null;
            }

            // First, define the execution command
            command += " " + path + "/" + network + ".net " + (path + "/");
            command += " --input-format pajek";
            command += " --seed " + this.seed;
            command += graph.isDirected() ? " --directed" : " --undirected";
            command += " --num-trials " + this.numTrials;
            command += " --silent --clu --map";

            System.out.println(command);
            Process child = Runtime.getRuntime().exec(command);

            Scanner sc = new Scanner(child.getInputStream());
            while (sc.hasNext())
            {
                sc.nextLine();
            }
            child.waitFor();

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

            // Delete the auxiliar files generated by the algorithm.
            File f = new File(path);
            for (File file : Objects.requireNonNull(f.listFiles()))
            {
                file.delete();
            }
            f.delete();

            // Return the communities
            return comms;
        }
        catch (IOException | InterruptedException ex)
        {
            return null;
        }
    }


}
