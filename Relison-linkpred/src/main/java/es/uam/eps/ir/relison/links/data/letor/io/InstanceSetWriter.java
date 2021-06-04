/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.data.letor.io;

import es.uam.eps.ir.relison.links.data.letor.FeatureInformation;
import es.uam.eps.ir.relison.links.data.letor.Instance;
import es.uam.eps.ir.relison.links.data.letor.InstanceSet;

import java.io.IOException;
import java.io.Writer;

/**
 * Class for writing patterns in different formats.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 *
 * @param <U> Type of the users
 */
public interface InstanceSetWriter<U>
{
    /**
     * Writes a dataset into a file.
     * @param file          the name of the file.
     * @param instanceSet   the instance set to store.
     * @throws IOException  if something fails while writing.
     */
    void write(String file, InstanceSet<U> instanceSet) throws IOException;
    
    /**
     * Obtains an string with all the information about the features of a dataset.
     * @param featInfo the feature information.
     * @return a String containing the information about the features.
     */
    String writeFeatureInfo(FeatureInformation featInfo);

    /**
     * Writes the feature information of a file.
     * @param write     the writer.
     * @param featInfo  feature information to write.
     * @throws IOException if something fails while writing.
     */
    void write(Writer write, FeatureInformation featInfo) throws IOException;

    /**
     * Writes an individual instance into a file.
     * @param write     the writer.
     * @param instance  the instance to write.
     * @throws IOException if something fails while writing.
     */
    void write(Writer write, Instance<U> instance) throws IOException;
    
    /**
     * Obtains an string containing the information for a single instance.
     * @param instance the instance.
     * @return an string containing all the necessary information.
     */
    String write(Instance<U> instance);
}
