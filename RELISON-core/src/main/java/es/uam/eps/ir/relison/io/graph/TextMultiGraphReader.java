/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.io.graph;

import org.ranksys.formats.parsing.Parser;

/**
 * Class that reads a multi-graph from a file.
 * <p>
 * For the format,
 *
 * @param <V> the type of the nodes.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @see TextGraphReader
 */
public class TextMultiGraphReader<V> extends TextGraphReader<V>
{
    /**
     * Constructor.
     *
     * @param directed  true if the graph is directed, false otherwise.
     * @param weighted  true if the graph is weighted, false otherwise.
     * @param selfloops true if we allow self loops, false otherwise.
     * @param delimiter file delimiter.
     * @param uParser   parser for reading the nodes.
     */
    public TextMultiGraphReader(boolean directed, boolean weighted, boolean selfloops, String delimiter, Parser<V> uParser)
    {
        super(true, directed, weighted, selfloops, delimiter, uParser);
    }
}
