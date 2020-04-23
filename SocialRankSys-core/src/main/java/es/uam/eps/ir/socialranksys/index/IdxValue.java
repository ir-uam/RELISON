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

import java.io.Serializable;

/**
 * Class for expressing weights.
 *
 * @param <W> Type of the weights.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class IdxValue<W> implements Comparable<IdxValue<W>>, Serializable, Cloneable
{
    /**
     * Identifier.
     */
    private final int idx;
    /**
     * Value of the weight.
     */
    private final W value;

    /**
     * Constructor.
     *
     * @param idx   Identifier.
     * @param value Value of the weight.
     */
    public IdxValue(int idx, W value)
    {
        this.idx = idx;
        this.value = value;
    }

    @Override
    public int compareTo(IdxValue t)
    {
        return this.idx - t.idx;
    }

    /**
     * Gets the identifier.
     *
     * @return the identifier.
     */
    public int getIdx()
    {
        return this.idx;
    }

    /**
     * Gets the value of the weight.
     *
     * @return the value of the weight.
     */
    public W getValue()
    {
        return this.value;
    }

}
