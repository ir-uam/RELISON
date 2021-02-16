/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.io.graph;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.index.Index;
import es.uam.eps.ir.socialranksys.index.fast.FastIndex;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Writes the graph in the Pajek format. Note: this format does support
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class PajekGraphWriter<U extends Serializable> implements GraphWriter<U>
{

    @Override
    public boolean write(Graph<U> graph, String file)
    {
        try
        {
            return this.write(graph, new FileOutputStream(file));
        }
        catch (FileNotFoundException ex)
        {
            return false;
        }

    }

    @Override
    public boolean write(Graph<U> graph, OutputStream file)
    {
        return this.write(graph, file, true, false);
    }

    @Override
    public boolean write(Graph<U> graph, String file, boolean writeWeights, boolean writeTypes)
    {
        try
        {
            return this.write(graph, new FileOutputStream(file));
        }
        catch (FileNotFoundException ex)
        {
            return false;
        }
    }

    @Override
    public boolean write(Graph<U> graph, OutputStream file, boolean writeWeights, boolean writeTypes)
    {
        if (writeTypes)
        {
            throw new UnsupportedOperationException("ERROR: Pajek format does not support writing types");
        }
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(file)))
        {
            Index<U> index = new FastIndex<>();
            graph.getAllNodes().forEach(index::addObject);

            bw.write("*Vertices " + graph.getVertexCount() + "\n");
            for (int l = 0; l < graph.getVertexCount(); ++l)
            {
                bw.write((l + 1) + " \"" + index.idx2object(l) + "\"\n");
            }

            bw.write("*Edges " + graph.getEdgeCount());
            graph.getAllNodes().forEach(u ->
            {
                int uidx = index.object2idx(u);
                graph.getAdjacentNodesWeights(u).forEach(v ->
                {
                    int vidx = index.object2idx(v.getIdx());
                    try
                    {
                        if (writeWeights)
                        {
                            bw.write("\n" + (uidx + 1) + " " + (vidx + 1) + " " + v.getValue());
                        }
                        else
                        {
                            bw.write("\n" + (uidx + 1) + " " + (vidx + 1));
                        }
                    }
                    catch (IOException ex)
                    {
                        Logger.getLogger(PajekGraphWriter.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
            });

            return true;
        }
        catch (IOException ioe)
        {
            return false;
        }
    }

}
