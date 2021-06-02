/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.content.index.exceptions;

import java.io.IOException;

/**
 * Exception for the case when an index does not exist
 *
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class NoIndexException extends IOException
{
    /**
     * The route where the index has been searched.
     */
    private final String folder;

    /**
     * Constructor.
     *
     * @param f the folder.
     */
    public NoIndexException(String f)
    {
        folder = f;
    }

    /**
     * Obtains the folder which has been searched and does not contain an index.
     *
     * @return the folder.
     */
    public String getFolder()
    {
        return folder;
    }
}
