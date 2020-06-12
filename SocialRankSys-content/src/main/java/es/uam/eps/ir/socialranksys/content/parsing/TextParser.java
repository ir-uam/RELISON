/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.content.parsing;

/**
 * Interface for text parsers.
 * @author Javier Sanz-Cruzado Puig
 */
public interface TextParser 
{
    /**
     * Given a text, parses and treats it.
     * @param text the original text.
     * @return the treated text.
     */
    String parse(String text);
}
