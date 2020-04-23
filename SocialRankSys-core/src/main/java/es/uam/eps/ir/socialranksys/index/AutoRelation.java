/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.index;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Interface for defining the relation of a set of objects with themselves.
 *
 * @param <W> Type of the weights.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public interface AutoRelation<W> extends Relation<W>
{
    /**
     * Gets the total number of elements of the first item in the relation.
     *
     * @return The number of different elements of the first item in the relation.
     */
    @Override
    default int numSecond()
    {
        return this.numFirst();
    }

    /**
     * Gets the total number of elements of the second item related to a first item.
     *
     * @param firstIdx Identifier of the first item.
     *
     * @return the second item count.
     */
    @Override
    default int numSecond(int firstIdx)
    {
        return this.numFirst(firstIdx);
    }

    /**
     * Gets all the elements of the second type.
     *
     * @return a stream containing all the elements.
     */
    @Override
    default Stream<Integer> getAllSecond()
    {
        return this.getAllFirst();
    }

    @Override
    default boolean addSecondItem(int secondIdx)
    {
        return addFirstItem(secondIdx);
    }

    /**
     * Removes an element. The identifiers of all remaining elements will
     * be reduced by 1.
     *
     * @param idx The identifier of the element.
     *
     * @return true if everything went OK, false otherwise.
     */
    boolean remove(int idx);

    /**
     * Obtains the elements in an autorelation which are related to none.
     *
     * @return an int stream containing the indexes of the items which are related to none.
     */
    IntStream getIsolated();

    /**
     * Checks whether an element is related to someone or not.
     *
     * @param idx The identifier of the element.
     *
     * @return true if the node is isolated, false otherwise.
     */
    default boolean isIsolated(int idx)
    {
        return !this.hasFirsts(idx) && !this.hasSeconds(idx);
    }
}
