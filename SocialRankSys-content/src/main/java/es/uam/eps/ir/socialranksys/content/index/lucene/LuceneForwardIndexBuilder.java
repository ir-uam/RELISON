package es.uam.eps.ir.socialranksys.content.index.lucene;

/**
 * Lucene implementation of a builder for a forward index.
 * @author Pablo Castells
 */
public class LuceneForwardIndexBuilder<C> extends LuceneBuilder<C>
{
    /**
     * Constructor.
     */
    public LuceneForwardIndexBuilder()
    {
        type.setStoreTermVectors (true);
    }
}
