/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.content.index.individual;

import es.uam.eps.ir.relison.content.index.AbstractIndexBuilder;
import es.uam.eps.ir.relison.content.index.Config;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Abstract implementation of an individual content index.
 *
 * @param <C> Type of the contents
 * @param <U> Type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class AbstractIndividualContentIndexBuilder<C, U> extends AbstractIndexBuilder<C> implements IndividualContentIndexBuilder<C, U>
{
    /**
     * Obtains the generated index.
     *
     * @return the generated index.
     *
     * @throws IOException if something fails while creating such index.
     */
    protected abstract IndividualContentIndex<C, U> getCoreIndividualIndex() throws IOException;

    /**
     * Saves a file containing the relation between indexes and user identifiers.
     *
     * @param indexPath the path of the index.
     *
     * @throws IOException if something fails while writing the map.
     */
    protected void saveUserContentMap(String indexPath) throws IOException
    {
        IndividualContentIndex<C, U> index = this.getCoreIndividualIndex();
        int numDocs = index.numDocs();
        PrintStream out = new PrintStream(indexPath + "/" + Config.POSTINGS_FILE);
        for (int cidx = 0; cidx < numDocs; ++cidx)
        {
            out.println(index.getUser(cidx).toString());
        }
        out.close();
    }
}
