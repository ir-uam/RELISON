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
 * Simple text parser that converts a text into lower case.
 * @author Javier Sanz-Cruzado Puig
 */
public class ToLowerParser implements TextParser
{

    @Override
    public String parse(String text) 
    {
        return text.toLowerCase();
    }
    
}
