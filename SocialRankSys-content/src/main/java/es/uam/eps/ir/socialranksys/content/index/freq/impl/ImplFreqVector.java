package es.uam.eps.ir.socialranksys.content.index.freq.impl;

import es.uam.eps.ir.socialranksys.content.index.freq.FreqVector;
import es.uam.eps.ir.socialranksys.content.index.freq.TermFreq;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import java.util.Iterator;

/**
 * Implementation of a frequency vector
 * @author Javier Sanz-Cruzado
 */
public class ImplFreqVector implements FreqVector
{
    /**
     * The frequencies for the different terms.
     */
    private final Object2LongOpenHashMap<String> vector;

    /**
     * Constructor. Builds a frequency vector from text.
     * @param text the text.
     */
    public ImplFreqVector(String text)
    {
        vector = new Object2LongOpenHashMap<>();
        String[] sep = text.split("\\s+");
        vector.defaultReturnValue(0L);
        for(String term : sep)
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
        return vector.object2LongEntrySet().stream().map(x -> (TermFreq) new ImplTermFreq(x.getKey(),x.getLongValue())).iterator();
    }
}
