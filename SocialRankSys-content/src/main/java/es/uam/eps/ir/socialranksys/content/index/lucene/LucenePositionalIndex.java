/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
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
 *
 * @param <C> type of the contents.
 *
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Javier Sanz-Cruzado
 */
public class LucenePositionalIndex<C> extends LuceneIndex<C>
{
    /**
     * Constructor.
     *
     * @param path    Path to the index.
     * @param cParser content parser.
     *
     * @throws IOException if something fails while reading the index.
     */
    public LucenePositionalIndex(String path, Parser<C> cParser) throws IOException
    {
        super(path, cParser);
    }

    @Override
    public PostingsList getPostings(String term) throws IOException
    {
        TermsEnum terms = MultiTerms.getTerms(index, "text").iterator();
        terms.seekExact(new BytesRef(term));
        return new LucenePositionalPostingsList(terms.postings(null), terms.postings(null, PostingsEnum.ALL), terms.docFreq());
    }
}
