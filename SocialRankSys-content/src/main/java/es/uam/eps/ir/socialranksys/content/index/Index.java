package es.uam.eps.ir.socialranksys.content.index;

import es.uam.eps.ir.socialranksys.content.index.structure.PostingsList;

import java.io.IOException;
import java.util.Collection;

/**
 * Interface for a content index.
 * @author Pablo Castells
 *
 * @param <C> type of the content identifier.
 */
public interface Index<C> extends DocumentMap<C>
{
    /**
     * Obtains the posting list of a term.
     * @param term the term to look up.
     * @return the posting list of the term.
     * @throws IOException if something fails while reading the posting list.
     */
    PostingsList getPostings(String term) throws IOException;

    /**
     * Obtains the set of all terms in the index dictionary.
     * @return the collection of terms.
     * @throws IOException if something fails while reading the list of terms.
     */
    Collection<String> getAllTerms() throws IOException;

    /**
     * Obtains the total frequency of a term in the collection.
     * @param term the term to look up.
     * @return the total frequency of the term in the collection.
     * @throws IOException if something fails while reading the frequency.
     */
    long getTotalFreq(String term) throws IOException;

    /**
     * Obtains the number of documents containing the term.
     * @param term the term to look up.
     * @return the number of documents containing the term.
     * @throws IOException if something fails while reading the doc frequency.
     */
    long getDocFreq(String term) throws IOException;
}
