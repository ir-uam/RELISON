/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.content.index.twittomender;

import es.uam.eps.ir.socialranksys.content.ContentVector;
import es.uam.eps.ir.socialranksys.content.index.ContentIndexMode;
import es.uam.eps.ir.socialranksys.content.index.exceptions.WrongModeException;
import es.uam.eps.ir.socialranksys.content.index.weighting.WeightingScheme;
import es.uam.eps.ir.socialranksys.content.parsing.TextParser;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.apache.lucene.search.DocIdSetIterator.NO_MORE_DOCS;


/**
 * Lucene implementation of a Twittomender index.
 * @author Javier Sanz-Cruzado Puig
 */
public class LuceneTwittomenderIndex extends TwittomenderIndex<Long> 
{
    /**
     * Identifier for the user identifier field.
     */
    private final static String USERID = "userId";
    /**
     * Identifier for the content field.
     */
    private final static String CONTENT = "content";
    
    /**
     * Index reader. Object that reads contents from the 
     */
    private IndexReader reader;
    /**
     * Index writer.
     */
    private IndexWriter writer;
    /**
     * Average document length
     */
    private double averageDocLength = 0.0;
    
    /**
     * Constructor.
     * @param route Route of the index.
     * @param created True if the index already exists, false if not.
     */
    public LuceneTwittomenderIndex(String route, boolean created) 
    {

        super(route, created);
    }

    @Override
    public boolean setReadMode() 
    {
        try
        {
            // Close the index writer
            if(this.getMode().equals(ContentIndexMode.WRITE))
                if(this.writer != null)
                    writer.close();

            // Do not change the configuration if it has been already created.
            if(!this.getMode().equals(ContentIndexMode.READ))
            {
                File f = new File(this.getRoute());
                Path pathLocation = Paths.get(this.getRoute());
                Directory dir = SimpleFSDirectory.open(pathLocation);
                this.reader = DirectoryReader.open(dir);

                this.averageDocLength = this.averageUserLength();
            }

            this.setMode(ContentIndexMode.READ);
            return true;
        }
        catch(IOException ioe)
        {
            this.setMode(ContentIndexMode.NONE);
            return false;
        }
    }

    @Override
    public ContentVector<Long> readUser(Long user, WeightingScheme model) throws WrongModeException
    {
        if(!this.isReadModeSet())
            throw new WrongModeException("Index is configured in " + this.getMode() + " mode, instead of " + ContentIndexMode.READ + " mode.");
        
        try
        {
            IndexSearcher isearcher = new IndexSearcher(this.reader);
            Query contentQuery = LongPoint.newExactQuery(USERID, user);
            
            ScoreDoc[] docs = isearcher.search(contentQuery, 1).scoreDocs;
            if(docs.length <= 0)
            {
                return null;
            }
            
            ScoreDoc doc = docs[0];
            return readUser(doc, model);
        }
        catch(IOException ioe)
        {
            return null;
        }
    }
    
    /**
     * Reads the contents of a ScoreDoc representing a user.
     * @param sd the ScoreDoc to read.
     * @param model The weighting scheme model to apply.
     * @return a vector representing the content.
     */
    private ContentVector<Long> readUser(ScoreDoc sd, WeightingScheme model)
    {
        try
        {
            Object2DoubleMap<String> content = new Object2DoubleOpenHashMap<>();
            content.defaultReturnValue(0.0);
            
            long userId = this.reader.document(sd.doc).getField(USERID).numericValue().longValue();
            Terms vector = this.reader.getTermVector(sd.doc, CONTENT);
            if(vector != null)
            {
                double docLength = vector.getSumTotalTermFreq() + 0.0;
                
                TermsEnum termsEnum = vector.iterator();
                
                BytesRef text;
                while((text = termsEnum.next()) != null)
                {
                    String term = text.utf8ToString();
                    
                    int freq = 0;
                    if (termsEnum.seekExact(new BytesRef(term))) 
                        freq = (int) termsEnum.totalTermFreq();
                    
                    Term t = new Term(CONTENT, text);
                    double df = reader.docFreq(t) + 0.0;
                    
                    content.put(term, model.computeWeight(freq + 0.0, df, docLength, this.averageDocLength, reader.numDocs()));
                }
            }
            
            return new ContentVector<>(userId, content);
        }
        catch(IOException ioe)
        {
            return null;
        }
    }

    @Override
    public double averageUserLength() 
    {
        try 
        {
            Terms terms = MultiTerms.getTerms(reader, CONTENT);
            TermsEnum termsIt = terms.iterator();
            int numDocs = this.reader.getDocCount(CONTENT);
            double length = 0.0;
            while(termsIt.next() != null)
            {
                termsIt.seekExact(termsIt.term());
                PostingsEnum postings = termsIt.postings(null);
                while(postings.nextDoc() != NO_MORE_DOCS)
                {
                    double freq = postings.freq();
                    length += freq / (numDocs + 0.0);
                }
                
            }
            
            return length;
            
        } 
        catch (IOException ex) 
        {
            return -1.0;
        }
    }

    @Override
    public boolean setWriteMode(List<String> stopwords) 
    {
        try
        {
            if(this.getMode().equals(ContentIndexMode.READ))
            {
                this.reader.close();
            }
            
            if(!this.getMode().equals(ContentIndexMode.WRITE))
            {
                // Select the stopwords set
                Analyzer analyzer;
                if(stopwords.isEmpty())
                {
                    analyzer = new StandardAnalyzer();
                }
                else
                {
                    CharArraySet stopwordSet = new CharArraySet(stopwords, true);
                    analyzer = new StandardAnalyzer(stopwordSet);
                }

                // Initialize and configure the index writer
                File f = new File(this.getRoute());
                Path pathLocation = Paths.get(this.getRoute());
                Directory dir = SimpleFSDirectory.open(pathLocation);
                IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

                if(this.isCreated())
                {
                    iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
                }
                else
                {
                    iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
                    this.setCreated(true);
                }
                
                this.writer = new IndexWriter(dir, iwc);
                this.setMode(ContentIndexMode.WRITE);
            }
            
            return true;

        }
        catch(IOException ioe)
        {
            this.setMode(ContentIndexMode.NONE);
            return false;
        }    
    }

    @Override
    public boolean writeContent(Long userId, String content, TextParser parser) throws WrongModeException
    {
        if(!this.isWriteModeSet())
            throw new WrongModeException("Index is configured in " + this.getMode() + " mode, instead of " + ContentIndexMode.WRITE + " mode.");
            
        FieldType contentType = new FieldType();
        contentType.setStored(true);
        contentType.setTokenized(true);
        contentType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        contentType.setStoreTermVectors(true);
        
        Document luceneDoc = new Document();
        luceneDoc.add(new LongPoint(USERID, userId));
        luceneDoc.add(new StoredField(USERID, userId));
        luceneDoc.add(new Field(CONTENT, parser.parse(content), contentType));
    
        try
        {
            this.writer.addDocument(luceneDoc);
        }
        catch(IOException ioe)
        {
            return false;
        }
        return true;
    }
    
        
    @Override
    public void close()
    {
        if(this.isReadModeSet())
        {
            try 
            {
                reader.close();
            } 
            catch (IOException ex) 
            {
                Logger.getLogger(LuceneTwittomenderIndex.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else if(this.isWriteModeSet())
        {
            try
            {
                writer.close();
            } 
            catch (IOException ex) 
            {
                Logger.getLogger(LuceneTwittomenderIndex.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        this.setMode(ContentIndexMode.NONE);
    }
    
    
}
