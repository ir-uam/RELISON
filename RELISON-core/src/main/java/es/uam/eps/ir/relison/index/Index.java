/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.index;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Generic index.
 *
 * @param <I> The type of indexed objects.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface Index<I> extends ReducedIndex<I>
{
    /**
     * Checks whether the index contains a given object.
     *
     * @param i The object to check.
     *
     * @return true if the index contains the object, false if not.
     */
    boolean containsObject(I i);

    /**
     * Index size.
     *
     * @return the number of objects in the index.
     */
    int numObjects();

    /**
     * A stream of all the objects in the index.
     *
     * @return the stream.
     */
    Stream<I> getAllObjects();

    /**
     * A stream of all ids.
     *
     * @return all ids.
     */
    IntStream getAllObjectsIds();

    /**
     * Adds an object to the index.
     *
     * @param i Object to add.
     *
     * @return the index of the added object.
     */
    int addObject(I i);

    /**
     * Removes an object from the index.
     *
     * @param i The object to remove.
     *
     * @return the index of the removed object, -1 if it did not exist.
     */
    int removeObject(I i);

    /**
     * Replaces an object by another one, keeping the same identifier (so any index-based structures, such as the
     * edges of a graph, remain valid).
     *
     * @param oldObj the object to replace.
     * @param newObj the new object.
     *
     * @return the (unchanged) identifier of the object, or -1 if {@code oldObj} is absent or {@code newObj} already
     *         exists.
     */
    default int renameObject(I oldObj, I newObj)
    {
        throw new UnsupportedOperationException("Renaming objects is not allowed");
    }
}
