/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.content.index.lucene;

import es.uam.eps.ir.sonalire.content.index.AbstractIndex;
import es.uam.eps.ir.sonalire.content.index.exceptions.NoIndexException;
import es.uam.eps.ir.sonalire.content.index.structure.PostingsList;
import es.uam.eps.ir.sonalire.content.index.structure.lucene.LucenePostingsList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.apache.lucene.index.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.ranksys.formats.parsing.Parser;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Lucene implementation of an index.
 *
 * @param <C> Type of the contents.
 *
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class LuceneIndex<C> extends AbstractIndex<C>
{
    /**
     * Object for accessing the index.
     */
    protected IndexReader index;

    /**
     * General constructor.
     *
     * @param indexFolder folder in which the index is stored.
     * @param cParser     content parser.
     *
     * @throws IOException if something fails while initializing the index.
     */
    public LuceneIndex(String indexFolder, Parser<C> cParser) throws IOException
    {
        try
        {
            index = DirectoryReader.open(FSDirectory.open(Paths.get(indexFolder)));
            loadContents(indexFolder, cParser);
        }
        catch (IndexNotFoundException ex)
        {
            throw new NoIndexException(indexFolder);
        }
    }

    /**
     * Auxiliar constructor.
     *
     * @param indexFolder folder in which the index is stored.
     * @param map         user to id map.
     *
     * @throws IOException if something fails while initializing the index.
     */
    public LuceneIndex(String indexFolder, Int2ObjectMap<C> map) throws IOException
    {
        try
        {
            index = DirectoryReader.open(FSDirectory.open(Paths.get(indexFolder)));
            this.forward = map;
        }
        catch (IndexNotFoundException ex)
        {
            throw new NoIndexException(indexFolder);
        }
    }

    @Override
    public Collection<String> getAllTerms() throws IOException
    {
        List<String> termList = new ArrayList<>();
        TermsEnum terms = MultiTerms.getTerms(index, "text").iterator();
        while (terms.next() != null)
        {
            termList.add(terms.term().utf8ToString());
        }
        return termList;
    }

    @Override
    public int numDocs()
    {
        return index.numDocs();
    }

    @Override
    public long getDocFreq(String term) throws IOException
    {
        return index.docFreq(new Term("text", term));
    }

    @Override
    public long getTotalFreq(String term) throws IOException
    {
        return index.totalTermFreq(new Term("text", term));
    }

    @Override
    public PostingsList getPostings(String term) throws IOException
    {
        TermsEnum terms = MultiTerms.getTerms(index, "text").iterator();
        if (terms.seekExact(new BytesRef(term)))
        {
            return new LucenePostingsList(terms.postings(null), terms.docFreq());
        }
        return new LucenePostingsList(null, 0);
    }
}