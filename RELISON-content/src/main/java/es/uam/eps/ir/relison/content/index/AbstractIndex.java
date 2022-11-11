/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.content.index;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.ranksys.formats.parsing.Parser;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * Abstract implementation of an index.
 *
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public abstract class AbstractIndex<C> implements Index<C>
{
    /**
     * Mapping from identifiers to contents.
     */
    protected Int2ObjectMap<C> forward;
    /**
     * Mapping from contents to identifiers.
     */
    protected Object2IntMap<C> backward;

    @Override
    public C getContent(int docID)
    {
        return forward.get(docID);
    }

    @Override
    public int getContentId(C path)
    {
        return backward.getInt(path);
    }

    /**
     * Loads the users.
     *
     * @param indexFolder folder in which the index is stored.
     * @param cParser     user parser.
     *
     * @throws IOException if something fails while reading the users.
     */
    public void loadContents(String indexFolder, Parser<C> cParser) throws IOException
    {
        forward = new Int2ObjectOpenHashMap<>();
        backward = new Object2IntOpenHashMap<>();
        File f = new File(indexFolder + "/" + Config.PATHS_FILE);
        if (!f.exists())
        {
            return;
        }
        Scanner scn = new Scanner(f);
        int numDocs = this.numDocs();

        for (int uidx = 0; uidx < numDocs; ++uidx)
        {
            C user = cParser.parse(scn.nextLine());
            forward.put(uidx, user);
            backward.put(user, uidx);
        }
    }
}
