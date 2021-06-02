/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.community.io;

import es.uam.eps.ir.sonalire.community.Communities;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Writes a community file.
 * <p>
 * File structure:
 * <p>
 * node community
 * <p>
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class TextCommunitiesWriter<U> implements CommunitiesWriter<U>
{
    /**
     * Delimiter that separates the node and community ids.
     */
    private final String delimiter;

    /**
     * Constructor.
     *
     * @param delimiter Delimiter that separates the node and community ids.
     */
    public TextCommunitiesWriter(String delimiter)
    {
        this.delimiter = delimiter;
    }

    @Override
    public boolean write(Communities<U> comm, String file)
    {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file))))
        {
            int numComm = comm.getNumCommunities();
            for (int i = 0; i < numComm; ++i)
            {
                List<U> users = comm.getUsers(i).collect(Collectors.toCollection(ArrayList::new));
                for (U u : users)
                {
                    bw.write(u + "\t" + i + "\n");
                }
            }

            return true;
        }
        catch (IOException ioe)
        {
            System.err.println("ERROR: An error ocurred while writing the file");
            return false;
        }
    }
}
