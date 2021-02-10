/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.content.search;

import es.uam.eps.ir.socialranksys.content.index.Index;
import es.uam.eps.ir.socialranksys.content.index.freq.FreqVector;
import es.uam.eps.ir.socialranksys.content.index.freq.TermFreq;
import es.uam.eps.ir.socialranksys.content.index.freq.impl.ImplFreqVector;
import es.uam.eps.ir.socialranksys.content.index.structure.Posting;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.ranksys.formats.parsing.Parsers;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

/**
 * Search engine using the vector space model.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * <p>
 * <b>Reference:</b> G. Salton, A. Wong, C. Yang. A vector space model for automatic indexing. Communications of the ACM 18(11), pp. 613-618 (1975)
 * </p>
 */
public class VSMSearchEngine extends AbstractSearchEngine
{
    /**
     * Modules
     */
    Int2DoubleOpenHashMap mods = new Int2DoubleOpenHashMap();

    /**
     * Constructor.
     *
     * @param index index.
     */
    public VSMSearchEngine(Index<?> index)
    {
        super(index);

        try // First, we precompute the modules.
        {
            int numDocs = index.numDocs();
            for (String term : index.getAllTerms())
            {
                long docFreq = index.getDocFreq(term);
                for (Posting p : index.getPostings(term))
                {
                    int userId = p.getDocID();
                    if (!mods.containsKey(userId))
                    {
                        mods.put(userId, 0.0);
                    }
                    mods.addTo(userId, Math.pow(tfidf(p.getFreq(), docFreq, numDocs), 2));
                }
            }
        }
        catch (IOException ioe)
        {
            System.err.println("Something failed while reading the index.");
        }
    }

    /**
     * Constructor.
     *
     * @param index   index.
     * @param modules a file containing the precomputed modules.
     */
    public VSMSearchEngine(Index<?> index, String modules)
    {
        super(index);

        // Load the modules.
        try
        {
            File f = new File(modules);
            if (!f.exists())
            {
                return;
            }
            Scanner scn = new Scanner(f);
            int numDocs = index.numDocs();
            for (int docID = 0; docID < numDocs; docID++)
            {
                mods.put(docID, Parsers.dp.parse(scn.nextLine()).doubleValue());
            }
            scn.close();
        }
        catch (IOException ioe)
        {
            System.err.println("Something failed while reading the modules");
        }
    }

    /**
     * Computes the tf-idf of the value.
     *
     * @param freq    frequency of the term.
     * @param df      number of documents containing the term.
     * @param numDocs total number of documents
     *
     * @return the tf-idf value of the
     */
    public static double tfidf(double freq, double df, double numDocs)
    {
        double auxTF = 0.0;
        double auxIDF = 0.0;
        if (freq > 0.0)
        {
            auxTF = 1.0 + Math.log(freq) / Math.log(2.0);
        }
        if (df > 0.0)
        {
            auxIDF = Math.log(1.0 + (numDocs + 0.0) / (df + 1.0)) / Math.log(2.0);
        }
        return auxTF * auxIDF;
    }

    @Override
    public Map<Integer, Double> search(FreqVector vector) throws IOException
    {
        // First, find the number of documents in the index.
        double numDocs = contentIndex.numDocs() + 0.0;
        Int2DoubleOpenHashMap map = new Int2DoubleOpenHashMap();
        map.defaultReturnValue(0.0);

        double currentMod = 0.0;

        // For each term in the "query":
        for (TermFreq freq : vector)
        {
            // Obtain the tfidf value.
            String term = freq.getTerm();
            double tf = freq.getFreq() + 0.0;
            double df = contentIndex.getDocFreq(term) + 0.0;
            double tfidf = VSMSearchEngine.tfidf(tf, df, numDocs);

            // For the different postings containing the term, find the scalar product.
            contentIndex.getPostings(term).forEach(posting ->
                                                   {
                                                       double auxtfidf = VSMSearchEngine.tfidf(posting.getFreq() + 0.0, df, numDocs);
                                                       map.addTo(posting.getDocID(), tfidf * auxtfidf);
                                                   });

            currentMod += Math.pow(tfidf, 2.0);
        }

        double finalMod = Math.sqrt(currentMod);

        // Divide by the corresponding modules.
        map.forEach((userId, value) ->
                    {
                        int uidx = userId;
                        double mod = mods.getOrDefault(uidx, 1.0);
                        map.put(uidx, value / (Math.sqrt(mod) * finalMod));
                    });

        return map;
    }

    @Override
    public Map<Integer, Double> search(String query) throws IOException
    {
        return this.search(new ImplFreqVector(query));
    }
}
