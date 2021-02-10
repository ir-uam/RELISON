/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.letor.io;

import es.uam.eps.ir.socialranksys.links.data.letor.FeatureInformation;
import es.uam.eps.ir.socialranksys.links.data.letor.Instance;
import es.uam.eps.ir.socialranksys.links.data.letor.InstanceSet;

import java.io.IOException;
import java.util.List;

/**
 * Interface for reading a pattern set for link prediction / contact recommendation.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 *
 * @param <U> Type of the users.
 */
public interface InstanceSetReader<U> 
{
    /**
     * Given a file, reads a pattern set.
     * @param file the file.
     * @return the pattern set which has been read.
     * @throws IOException if something fails while reading.
     */
    InstanceSet<U> read(String file) throws IOException;
    
    /**
     * Reads the header of the file into an object containing all
     * information about features.
     * @param header the header lines.
     * @return an object containing information about features.
     */
    FeatureInformation readHeader(List<String> header);
    
    /**
     * Reads an individual pattern.
     * @param line the line string containing the pattern.
     * @param numFeats the number of features.
     * @return the pattern.
     */
    Instance<U> readInstance(String line, int numFeats);
}
