/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package es.uam.eps.ir.relison.io.index;

import es.uam.eps.ir.relison.index.Index;
import es.uam.eps.ir.relison.index.fast.FastIndex;
import org.ranksys.formats.parsing.Parser;

import java.io.*;

/**
 * Reads an index from a file.
 *
 * @param <U> Type of the elements to read in the index.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class FastIndexReader<U> implements IndexReader<U>
{
    /**
     * A parser.
     */
    private final Parser<U> parser;

    /**
     * Constructor.
     *
     * @param parser a parser, for reading the elements in the index.
     */
    public FastIndexReader(Parser<U> parser)
    {
        this.parser = parser;
    }

    @Override
    public Index<U> read(String file)
    {
        try
        {
            InputStream stream = new FileInputStream(file);
            return this.read(stream);
        }
        catch (IOException ioe)
        {
            return null;
        }
    }

    @Override
    public Index<U> read(InputStream stream)
    {
        Index<U> index = new FastIndex<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream)))
        {
            String line;
            while ((line = br.readLine()) != null)
            {
                index.addObject(parser.parse(line));
            }
            return index;
        }
        catch (IOException ioe)
        {
            return null;
        }
    }

}
