package es.uam.eps.ir.socialranksys.content.search;

import es.uam.eps.ir.socialranksys.content.index.freq.FreqVector;

import java.io.IOException;
import java.util.Map;

/**
 * Interface defining the methods for a search engine.
 */
public interface SearchEngine
{
    /**
     * Searches from a term frequency vector.
     * @param termFreq the term frequency vector.
     * @return a map containing the results of the search.
     * @throws IOException if something fails while searching.
     */
    Map<Integer, Double> search(FreqVector termFreq) throws IOException;

    /**
     * Searches from a query.
     * @param query the query.
     * @return a map containing the results of the search.
     * @throws IOException if something fails while searching.
     */
    Map<Integer, Double> search(String query) throws IOException;
}
