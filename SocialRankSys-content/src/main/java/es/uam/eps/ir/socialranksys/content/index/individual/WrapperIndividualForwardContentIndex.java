package es.uam.eps.ir.socialranksys.content.index.individual;

import es.uam.eps.ir.socialranksys.content.index.ForwardIndex;
import es.uam.eps.ir.socialranksys.content.index.freq.FreqVector;
import es.uam.eps.ir.socialranksys.content.index.structure.PostingsList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.ranksys.formats.parsing.Parser;

import java.io.IOException;
import java.util.Collection;

/**
 * Individual content index builder wrapping a simple one.
 * @param <C> type of the contents.
 * @param <U> type of the users.
 */
public class WrapperIndividualForwardContentIndex<C,U> extends AbstractIndividualContentIndex<C,U> implements ForwardIndex<C>
{
    /**
     * The internal index.
     */
    private final ForwardIndex<C> index;

    /**
     * Constructor.
     * @param indexFolder route to the index.
     * @param index the index.
     * @param uParser a user parser.
     * @throws IOException if something fails while loading the content to user map.
     */
    public WrapperIndividualForwardContentIndex(String indexFolder, ForwardIndex<C> index, Parser<U> uParser) throws IOException
    {
        this.index = index;
        this.loadUserContentMap(indexFolder, uParser);
    }

    /**
     * Constructor.
     * @param index the index.
     * @param userMap the content to user map.
     */
    public WrapperIndividualForwardContentIndex(ForwardIndex<C> index, Int2ObjectMap<U> userMap)
    {
        this.index = index;
        this.contentsToUsers = userMap;
    }

    @Override
    public PostingsList getPostings(String term) throws IOException
    {
        return index.getPostings(term);
    }

    @Override
    public Collection<String> getAllTerms() throws IOException
    {
        return index.getAllTerms();
    }

    @Override
    public long getTotalFreq(String term) throws IOException
    {
        return index.getTotalFreq(term);
    }

    @Override
    public long getDocFreq(String term) throws IOException
    {
        return index.getDocFreq(term);
    }

    @Override
    public int numDocs()
    {
        return index.numDocs();
    }

    @Override
    public FreqVector getContentVector(int contentId) throws IOException
    {
        return index.getContentVector(contentId);
    }

    @Override
    public long getTermFreq(String term, int contentId) throws IOException
    {
        return index.getTermFreq(term, contentId);
    }
}
