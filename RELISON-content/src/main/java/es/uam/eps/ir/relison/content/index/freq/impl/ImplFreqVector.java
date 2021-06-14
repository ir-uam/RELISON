/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.content.index.freq.impl;

import es.uam.eps.ir.relison.content.index.freq.FreqVector;
import es.uam.eps.ir.relison.content.index.freq.TermFreq;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import java.util.Iterator;

/**
 * Implementation of a frequency vector
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class ImplFreqVector implements FreqVector
{
    /**
     * The frequencies for the different terms.
     */
    private final Object2LongOpenHashMap<String> vector;

    /**
     * Constructor. Builds a frequency vector from text.
     *
     * @param text the text.
     */
    public ImplFreqVector(String text)
    {
        vector = new Object2LongOpenHashMap<>();
        String[] sep = text.split("\\s+");
        vector.defaultReturnValue(0L);
        for (String term : sep)
        {
            vector.addTo(term, 1L);
        }
    }

    @Override
    public long size()
    {
        return vector.size();
    }

    @Override
    public long getFreq(String term)
    {
        return vector.getLong(term);
    }

    @Override
    public Iterator<TermFreq> iterator()
    {
        return vector.object2LongEntrySet().stream().map(x -> (TermFreq) new ImplTermFreq(x.getKey(), x.getLongValue())).iterator();
    }
}
