/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.content.index.lucene;

import es.uam.eps.ir.sonalire.content.index.AbstractIndexBuilder;
import es.uam.eps.ir.sonalire.content.index.Index;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Lucene implementation of an Index Builder
 *
 * @param <C> Type of the contents.
 *
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class LuceneBuilder<C> extends AbstractIndexBuilder<C>
{
    /**
     * Custom type for storing each content.
     */
    protected static FieldType type;
    /**
     * Index writer.
     */
    private IndexWriter builder;
    /**
     * Folder in which to store the index
     */
    private String indexFolder;
    /**
     * Map storing the relation between docids and contents.
     */
    private Int2ObjectMap<C> map;

    /**
     * Constructor.
     */
    public LuceneBuilder()
    {
        type = new FieldType();
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
    }

    @Override
    public void init(String indexPath) throws IOException
    {
        indexFolder = indexPath;
        clear(indexPath);
        CharArraySet set = CharArraySet.copy(EnglishAnalyzer.ENGLISH_STOP_WORDS_SET);
        set.add("RT");

        IndexWriterConfig iwc = new IndexWriterConfig(new StandardAnalyzer(set));
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        builder = new IndexWriter(FSDirectory.open(Paths.get(indexPath)), iwc);
        map = new Int2ObjectOpenHashMap<>();
    }

    @Override
    public void close(String indexPath) throws IOException
    {
        builder.close();
        saveContentMap(indexPath);
    }

    @Override
    public int indexText(String text, C content) throws IOException
    {
        Document doc = new Document();
        Field pathField = new StringField("content", content.toString(), Field.Store.YES);
        doc.add(pathField);
        Field field = new Field("text", text, type);
        doc.add(field);
        builder.addDocument(doc);
        int docId = builder.getDocStats().maxDoc - 1;

        map.put(docId, content);
        return docId;
    }

    @Override
    public Index<C> getCoreIndex() throws IOException
    {
        return new LuceneIndex<>(indexFolder, map);
    }
}
