package es.uam.eps.ir.socialranksys.content.index.lucene;

import es.uam.eps.ir.socialranksys.content.index.structure.PostingsList;
import es.uam.eps.ir.socialranksys.content.index.structure.positional.lucene.LucenePositionalPostingsList;
import org.apache.lucene.index.MultiTerms;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.ranksys.formats.parsing.Parser;

import java.io.IOException;

/**
 * Lucene implementation of a positional index.
 * @author Pablo Castells
 * @author Javier Sanz-Cruzado
 */
public class LucenePositionalIndex<C> extends LuceneIndex<C>
{
    public LucenePositionalIndex(String path, Parser<C> cParser) throws IOException
    {
        super(path, cParser);
    }

    public PostingsList getPostings(String term) throws IOException
    {
        TermsEnum terms = MultiTerms.getTerms(index, "text").iterator();
        terms.seekExact(new BytesRef(term));
        return new LucenePositionalPostingsList(terms.postings(null), terms.postings(null, PostingsEnum.ALL), terms.docFreq());
    }
}
