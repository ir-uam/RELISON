package es.uam.eps.ir.socialranksys.content.index.lucene;

import org.apache.lucene.index.IndexOptions;

/**
 * Constructor for a Lucene positional index builder.
 * @author Pablo Castells
 */
public class LucenePositionalIndexBuilder<C> extends LuceneBuilder<C>
{
    public LucenePositionalIndexBuilder()
    {
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
    }
}
