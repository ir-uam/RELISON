/*
 * Copyright (C) 2017 Information Retrieval Group at Universidad Aut�noma
 * de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.content.index;


import es.uam.eps.ir.socialranksys.content.Content;
import es.uam.eps.ir.socialranksys.content.ContentVector;
import es.uam.eps.ir.socialranksys.content.TermData;
import es.uam.eps.ir.socialranksys.content.index.exceptions.WrongModeException;
import es.uam.eps.ir.socialranksys.content.index.weighting.WeightingScheme;
import es.uam.eps.ir.socialranksys.content.parsing.TextParser;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.apache.lucene.search.DocIdSetIterator.NO_MORE_DOCS;

/**
 * Lucene wrapper for a content index.
 * @author Javier Sanz-Cruzado Puig
 */
public class LuceneContentIndex extends ContentIndex<Long,Long>
{
    /**
     * Reads data from the Lucene index.
     */
    private IndexReader reader;
    /**
     * Writes data into the Lucene index.
     */
    private IndexWriter writer;
    /**
     * Analyzer for parsing text.
     */
    private final Analyzer analyzer;

    /**
     * Average document length
     */
    private double averageDocLength = 0.0;
    /**
     * Document length
     */
    private final Int2DoubleMap docLength;
    
    /**
     * Name of the index field which stores the identifier of the content.
     */
    private final static String CONTENTID = "contentId";
    /**
     * Name of the index field which stores the identifier of the user.
     */
    private final static String USERID = "userId";
    /**
     * Name of the index field which stores the timestamp.
     */
    private final static String TIMESTAMP = "timestamp";
    /**
     * Name of the index field which stores the term vectors.
     */
    private final static String CONTENT = "content";
    
    
    
    /**
     * Constructor.
     * @param route Route to the index. 
     * @param created Indicates if the index already exists (true) or not (false)
     */
    public LuceneContentIndex(String route, boolean created) 
    {
        super(route, created);
        
        if(this.getStopwords().isEmpty())
        {
            analyzer = new StandardAnalyzer();
        }
        else
        {
            CharArraySet stopwordSet = new CharArraySet(this.getStopwords(), true);
            analyzer = new StandardAnalyzer(stopwordSet);
        }
       
        docLength = new Int2DoubleOpenHashMap();
        docLength.defaultReturnValue(0.0);
    }
    
    /**
     * Constructor.
     * @param route Route to the index.
     * @param created Indicates if the index already exists (true) or not (false)
     * @param stopwords List of stopwords.
     */
    public LuceneContentIndex(String route, boolean created, List<String> stopwords)
    {
        super(route, created, stopwords);
        
        if(this.getStopwords().isEmpty())
        {
            analyzer = new StandardAnalyzer();
        }
        else
        {
            CharArraySet stopwordSet = new CharArraySet(this.getStopwords(), true);
            analyzer = new StandardAnalyzer(stopwordSet);
        }
        
        docLength = new Int2DoubleOpenHashMap();
        docLength.defaultReturnValue(0.0);

    }
    
    /**
     * Constructor.
     * @param route Route to the index.
     * @param created Indicates if the index already exists (true) or not (false)
     * @param stopwordsFile Route to a file containing the stopwords.
     */
    public LuceneContentIndex(String route, boolean created, String stopwordsFile)
    {
        super(route, created, stopwordsFile);
        
        if(this.getStopwords().isEmpty())
        {
            analyzer = new StandardAnalyzer();
        }
        else
        {
            CharArraySet stopwordSet = new CharArraySet(this.getStopwords(), true);
            analyzer = new StandardAnalyzer(stopwordSet);
        }
        
        docLength = new Int2DoubleOpenHashMap();
        docLength.defaultReturnValue(0.0);

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

                // Read the document length, and then, store the value of the average.
                this.averageDocLength = this.averageDocLength();
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
    public ContentVector<Long> readContent(Long contentId, WeightingScheme model) throws WrongModeException
    {
        if(!this.isReadModeSet())
            throw new WrongModeException("Index is configured in " + this.getMode() + " mode, instead of " + ContentIndexMode.READ + " mode.");
        
        try
        {
            IndexSearcher isearcher = new IndexSearcher(this.reader);
            Query contentQuery = LongPoint.newExactQuery(CONTENTID, contentId);

            ScoreDoc[] docs = isearcher.search(contentQuery, 1).scoreDocs;
            if(docs.length <= 0)
                return null;
            
            ScoreDoc doc = docs[0];
            return readDocument(doc, model);
            
        }
        catch(IOException ioe)
        {
            return null;
        }
    }
    
    /**
     * Reads a document (content) from the index.
     * @param sd the document in the Lucene index.
     * @param model Model for computing the value of each coordinate.
     * @return a map relating the terms of the content with their TF-IDF score.
     */
    private ContentVector<Long> readDocument(ScoreDoc sd, WeightingScheme model)
    {
        try
        {
            // Initialize the vector
            Object2DoubleMap<String> content = new Object2DoubleOpenHashMap<>();
            content.defaultReturnValue(0.0);
            
            // Obtain the content identifier.
            long contentId = this.reader.document(sd.doc).getField(CONTENTID).numericValue().longValue();

            Terms vector = this.reader.getTermVector(sd.doc, CONTENT);
            // Read the term vector
            if(vector != null)
            {
                double docL = this.docLength.get(sd.doc);

                TermsEnum termsEnum = vector.iterator();

                BytesRef text;
                while((text = termsEnum.next()) != null)
                {
                    String term = text.utf8ToString();
                    termsEnum.seekExact(text);
                    
                    //PostingsEnum postEnum = termsEnum.postings(null);
                    //postEnum.advance(sd.doc);                  
                    //int freq = (int) postEnum.freq();
                    
                    long freq = termsEnum.totalTermFreq();
                    Term t = new Term(CONTENT, text);
                    double df = reader.docFreq(t) + 0.0;

                    content.put(term, model.computeWeight(freq + 0.0, df, docL, this.averageDocLength, reader.numDocs()));
                
                }
            }
            
            return new ContentVector<>(contentId, content);
        }
        catch(IOException ioe)
        {
            return null;
        }
    }

    @Override
    public Map<Long, ContentVector<Long>> readUser(Long userId, WeightingScheme model) throws WrongModeException 
    {
        if(!this.isReadModeSet())
            throw new WrongModeException("Index is configured in " + this.getMode() + " mode, instead of " + ContentIndexMode.READ + " mode.");
        
        Long2ObjectMap<ContentVector<Long>> contents = new Long2ObjectOpenHashMap<>();
        contents.defaultReturnValue(null);
        
        try 
        {
            if(this.reader == null)
            {
                return null;
            }
            
            // Search by query and date
            IndexSearcher isearcher = new IndexSearcher(this.reader);
            Query userQuery = LongPoint.newExactQuery(USERID, userId);
            
           
            // Read the results (all the results)
            ScoreDoc[] docs = isearcher.search(userQuery, reader.numDocs()).scoreDocs;
            
            for(ScoreDoc doc : docs)
            {
                ContentVector<Long> content = this.readDocument(doc, model);
                assert content != null;
                contents.put(content.getId().longValue(), content);
            }
            
            return contents;
        } 
        catch (IOException ex) 
        {
            return null;
        }
    }

    @Override
    public Map<Long, ContentVector<Long>> readUserDateRange(Long userId, long minTimestamp, long maxTimestamp, WeightingScheme model) throws WrongModeException 
    {
        if(!this.isReadModeSet())
            throw new WrongModeException("Index is configured in " + this.getMode() + " mode, instead of " + ContentIndexMode.READ + " mode.");
        
        Long2ObjectMap<ContentVector<Long>> contents = new Long2ObjectOpenHashMap<>();
        contents.defaultReturnValue(null);
        
        try 
        {
            if(this.reader == null)
            {
                return null;
            }
            
            // Search by query and date
            IndexSearcher isearcher = new IndexSearcher(this.reader);
            Query userQuery = LongPoint.newExactQuery(USERID, userId);
            Query dateQuery = LongPoint.newRangeQuery("date", minTimestamp, maxTimestamp);
            
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.add(new BooleanClause(userQuery, BooleanClause.Occur.MUST));
            builder.add(new BooleanClause(dateQuery, BooleanClause.Occur.MUST));
            
            // Read the results (all the results)
            ScoreDoc[] docs = isearcher.search(builder.build(), reader.numDocs()).scoreDocs;
            
            for(ScoreDoc doc : docs)
            {
                ContentVector<Long> content = this.readDocument(doc, model);
                assert content != null;
                contents.put(content.getId().longValue(), content);
            }
            
            return contents;
        } 
        catch (IOException ex) 
        {
            return null;
        }
    }

    @Override
    public Map<Long, Map<Long, ContentVector<Long>>> readDateRange(long minTimestamp, long maxTimestamp, WeightingScheme model) throws WrongModeException 
    {
        if(!this.isReadModeSet())
            throw new WrongModeException("Index is configured in " + this.getMode() + " mode, instead of " + ContentIndexMode.READ + " mode.");
        
        Long2ObjectMap<Map<Long,ContentVector<Long>>> contents = new Long2ObjectOpenHashMap<>();
        Supplier<Map<Long,ContentVector<Long>>> sup = () -> 
        {
            Long2ObjectMap<ContentVector<Long>> map = new Long2ObjectOpenHashMap<>();
            map.defaultReturnValue(null);
            return map;
        };
        contents.defaultReturnValue(sup.get());
        
        try
        {
            if(this.reader == null)
            {
                return null;
            }
            
            // Search by query and date
            IndexSearcher isearcher = new IndexSearcher(this.reader);
            Query dateQuery = LongPoint.newRangeQuery("date", minTimestamp, maxTimestamp);
                        
            // Read the results (all the results)
            ScoreDoc[] docs = isearcher.search(dateQuery, reader.numDocs()).scoreDocs;
            
            for(ScoreDoc doc : docs)
            {
                long userId = reader.document(doc.doc).getField(USERID).numericValue().longValue();
                if(contents.containsKey(userId))
                {
                    contents.put(userId, new Long2ObjectOpenHashMap<>());
                }
                ContentVector<Long> content = this.readDocument(doc, model);
                assert content != null;
                contents.get(userId).put(content.getId(), content);
            }
            
            return contents;
        } 
        catch (IOException ex) 
        {
            return null;
        }
    }

    
    @Override
    public double averageDocLength() 
    {
        try 
        {
            this.docLength.clear();
            
            
            Terms terms = MultiTerms.getTerms(reader, CONTENT);
            TermsEnum termsIt = terms.iterator();
            int numDocs = this.reader.getDocCount(CONTENT);
            
            for(int i = 0; i < numDocs; ++i)
            {
                docLength.put(i, 0.0);
            }
            
            double length = 0.0;
            while(termsIt.next() != null)
            {
                termsIt.seekExact(termsIt.term());
                PostingsEnum postings = termsIt.postings(null);
                int doc;
                while((doc = postings.nextDoc()) != NO_MORE_DOCS)
                {
                    double freq = postings.freq();
                    docLength.put(doc, docLength.get(doc) + freq);
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
    public boolean setWriteMode() 
    {
        try
        {
            if(this.getMode().equals(ContentIndexMode.READ))
            {
                this.reader.close();
            }
            
            if(!this.getMode().equals(ContentIndexMode.WRITE))
            {
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
    public boolean writeContent(Long user, Content<Long, String> content, TextParser parser) throws WrongModeException
    {
        if(!this.isWriteModeSet())
            throw new WrongModeException("Index is configured in " + this.getMode() + " mode, instead of " + ContentIndexMode.WRITE + " mode.");

        FieldType contentType = new FieldType();
        contentType.setStored(true);
        contentType.setTokenized(true);
        contentType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        contentType.setStoreTermVectors(true);

        Document luceneDoc = new Document();
        luceneDoc.add(new LongPoint(CONTENTID, content.getContentId()));
        luceneDoc.add(new StoredField(CONTENTID, content.getContentId()));
        luceneDoc.add(new LongPoint(USERID, user));
        luceneDoc.add(new StoredField(USERID, user));
        luceneDoc.add(new LongPoint(TIMESTAMP, content.getTimestamp()));
        luceneDoc.add(new StoredField(TIMESTAMP, content.getTimestamp()));
        luceneDoc.add(new Field(CONTENT, parser.parse(content.getContent()), contentType));
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
                Logger.getLogger(LuceneContentIndex.class.getName()).log(Level.SEVERE, null, ex);
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
                Logger.getLogger(LuceneContentIndex.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        this.setMode(ContentIndexMode.NONE);
    }

    @Override
    public Stream<String> getAllTerms() throws WrongModeException
    {
        if(!this.isReadModeSet())
                throw new WrongModeException("Index is configured in " + this.getMode() + " mode, instead of " + ContentIndexMode.READ + " mode.");

        try 
        {
            List<String> terms = new ArrayList<>();
            TermsEnum termEnum = MultiTerms.getTerms(reader, CONTENT).iterator();
            while(termEnum.next() != null)
            {
                terms.add(termEnum.term().utf8ToString());
            }
            
            return terms.stream();
        } 
        catch (IOException ex) 
        {
            return Stream.empty();
        }
    }

    @Override
    public Stream<TermData<Long,Long>> getContents(String term, WeightingScheme model) throws WrongModeException
    {
        if(!this.isReadModeSet())
                throw new WrongModeException("Index is configured in " + this.getMode() + " mode, instead of " + ContentIndexMode.READ + " mode.");
        
        try 
        {
            TermsEnum terms = MultiTerms.getTerms(reader, CONTENT).iterator();
            terms.seekExact(new BytesRef(term));
            PostingsEnum postings = terms.postings(null);
            
            List<TermData<Long,Long>> data = new ArrayList<>();
            
            double df = this.reader.docFreq(new Term(CONTENT, term));
            int doc;
            while((doc = postings.nextDoc()) != NO_MORE_DOCS)
            {
                double tf = postings.freq();
                double docL = this.docLength.get(doc);
                double score = model.computeWeight(tf, df, docL, averageDocLength, reader.numDocs() + 0.0);
                
                long tweetId = this.reader.document(doc).getField(CONTENTID).numericValue().longValue();
                long userId = this.reader.document(doc).getField(USERID).numericValue().longValue();
                long timestamp = this.reader.document(doc).getField(TIMESTAMP).numericValue().longValue();
                
                data.add(new TermData<>(userId, tweetId, timestamp, score));
            }
            
            return data.stream();
        } 
        catch (IOException ex) 
        {
            return Stream.empty();
        }
        
    }
}