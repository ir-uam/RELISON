/*
 *  Copyright (C) 2022 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es / Terrier Team at University of Glasgow,
 *  http://http://terrierteam.dcs.gla.ac.uk
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.linkprediction.io;

import es.uam.eps.ir.relison.links.linkprediction.Prediction;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import org.jooq.lambda.function.Function3;
import org.ranksys.core.util.tuples.Tuple2od;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Format for writing the link predictions. This format writes tuples in the format
 * <p>
 * origin_node, destination_node, score
 * </p>
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzadopuig@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class TuplesLinkPredictionFormat<U> implements LinkPredictionFormat<U>
{
    /**
     * Function for setting triplets into text.
     */
    private final Function3<U,U,Double, String> tupleWriter;
    /**
     * Function for transforming a read tuple into a ((node1, node2), value) triplet.
     */
    private final Function<String, Tuple2od<Pair<U>>> tupleReader;
    /**
     * True if we want to sort the tuples by decreasing score, false otherwise.
     */
    private final boolean sortByDecreasingScore;

    /**
     * Constructor.
     * @param tupleWriter tuple writer.
     * @param tupleReader tuple reader.
     */
    public TuplesLinkPredictionFormat(Function3<U,U,Double, String> tupleWriter, Function<String, Tuple2od<Pair<U>>> tupleReader)
    {
        this(tupleWriter, tupleReader, false);
    }

    /**
     * Constructor.
     * @param tupleWriter tuple writer.
     * @param tupleReader tuple reader.
     * @param sortByDecreasingScore true if we want to sort the tuples by decreasing score, false otherwise.
     */
    public TuplesLinkPredictionFormat(Function3<U,U,Double,String> tupleWriter, Function<String, Tuple2od<Pair<U>>> tupleReader, boolean sortByDecreasingScore)
    {
        this.tupleReader = tupleReader;
        this.tupleWriter = tupleWriter;
        this.sortByDecreasingScore = sortByDecreasingScore;
    }

    @Override
    public LinkPredictionFormat.Writer<U> getWriter(OutputStream out)
    {
        return new Writer(out);
    }

    @Override
    public LinkPredictionFormat.Reader<U> getReader(InputStream in)
    {
        return new Reader(in);
    }

    /**
     * The reader for the format.
     */
    protected class Reader implements LinkPredictionFormat.Reader<U>
    {
        /**
         * An input stream.
         */
        private final InputStream in;

        /**
         * Constructor.
         * @param in input stream where to read the link prediction.
         */
        public Reader(InputStream in)
        {
            this.in = in;
        }

        @Override
        public Prediction<U> read()
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            List<Tuple2od<Pair<U>>> rec = new ArrayList<>();
            reader.lines().map(tupleReader).forEach(rec::add);

            if(sortByDecreasingScore) rec.sort(Comparator.comparingDouble(Tuple2od::v2));
            return new Prediction<>(rec);
        }
    }

    /**
     * Writer for the format.
     */
    protected class Writer implements LinkPredictionFormat.Writer<U>
    {
        /**
         * A buffered writer.
         */
        private final BufferedWriter writer;

        /**
         * Constructor.
         * @param out an output stream where link prediction is written.
         */
        public Writer(OutputStream out)
        {
            this.writer = new BufferedWriter(new OutputStreamWriter(out));
        }

        @Override
        public void write(Prediction<U> prediction) throws IOException
        {
            List<Tuple2od<Pair<U>>> pred = prediction.getPrediction();

            for(Tuple2od<Pair<U>> link : pred)
            {
                String line = tupleWriter.apply(link.v1.v1(),link.v1.v2(),link.v2);
                writer.write(line);
                writer.newLine();
            }
        }

        @Override
        public void close() throws IOException
        {
            writer.close();
        }
    }
}