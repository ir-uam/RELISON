/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialnetwork.graph.generator;

import es.uam.eps.ir.socialnetwork.graph.Graph;

/**
 * Generates different graphs.
 *
 * @param <V> Type of the vertices.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface GraphGenerator<V>
{
    /**
     * Configures the generator.
     *
     * @param configuration An array containing the configuration parameters.
     */
    void configure(Object... configuration);

    /**
     * Generates a graph.
     *
     * @return the generated graph.
     *
     * @throws GeneratorNotConfiguredException The generator is not configured.
     * @throws GeneratorBadConfiguredException The generator parameters are incorretct.
     */
    Graph<V> generate() throws GeneratorNotConfiguredException, GeneratorBadConfiguredException;
}
