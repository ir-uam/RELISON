/*
 * Copyright (C) 2019 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
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
import java.io.Writer;

/**
 * Class for writing patterns in different formats.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> Type of the users
 */
public interface InstanceSetWriter<U>
{
    /**
     * Writes a dataset into a file.
     * @param file the name of the file.
     * @param patternSet the pattern set to store.
     * @throws IOException if something fails while writing.
     */
    void write(String file, InstanceSet<U> patternSet) throws IOException;
    
    /**
     * Obtains an string with all the information about the features of a dataset.
     * @param featInfo the feature information.
     * @return a String containing the information about the features.
     */
    String writeFeatureInfo(FeatureInformation featInfo);


    void write(Writer write, FeatureInformation featInfo) throws IOException;

    void write(Writer write, Instance<U> instance) throws IOException;
    
    /**
     * Obtains an string containing the information for a single pattern.
     * @param pattern the pattern.
     * @return an string containing all the necessary information.
     */
    String write(Instance<U> pattern);
}
