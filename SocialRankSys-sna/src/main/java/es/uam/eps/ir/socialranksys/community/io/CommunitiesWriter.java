/* 
 * Copyright (C) 2018 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.community.io;

import es.uam.eps.ir.socialranksys.community.Communities;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Writes a community file
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class CommunitiesWriter<U> 
{
    /**
     * Writes the communities into a file.
     * @param comm The communities
     * @param file The file.
     * @param delimiter The delimiter that separates different values to retrieve.
     * @return True if everything went ok, false if not.
     */
    public boolean write(Communities<U> comm, String file, String delimiter)
    {
        try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file))))
        {
            int numComm = comm.getNumCommunities();
            for(int i = 0; i < numComm; ++i)
            {
                List<U> users = comm.getUsers(i).collect(Collectors.toCollection(ArrayList::new));
                for(U u : users)
                {
                    bw.write(u + "\t" + i  + "\n");
                }
            }
            
            return true;
        }
        catch(IOException ioe)
        {
            System.err.println("ERROR: An error ocurred while writing the file");
            return false;
        }
    }
}
