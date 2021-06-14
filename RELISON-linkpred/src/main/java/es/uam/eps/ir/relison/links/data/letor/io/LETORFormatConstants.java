/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.data.letor.io;

/**
 * Constant formats for learning to rank files.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 */
public class LETORFormatConstants
{
    /**
     * Query identifiers.
     */
    public static final String QID = "qid:";
    /**
     * Document identifiers.
     */
    public static final String DOCID = "docid:";
    /**
     * Comments starting characters.
     */
    public static final String COMMENT = "#";
    /**
     * Field separator.
     */
    public static final String SEPARATOR = " ";
    /**
     * Identifier separator.
     */
    public static final String IDSEP = ":";
}
