package es.uam.eps.ir.socialranksys.content.search;

import es.uam.eps.ir.socialranksys.content.index.Index;

/**
 * Abstract implementation of a search engine.
 */
public abstract class AbstractSearchEngine implements SearchEngine
{
    /**
     * The index in which to perform the search.
     */
    protected final Index<?> contentIndex;

    /**
     * Constructor.
     * @param index the index in which to perform the search.
     */
    public AbstractSearchEngine(Index<?> index)
    {
        this.contentIndex = index;
    }
}
