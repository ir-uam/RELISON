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

public class TuplesLinkPredictionFormat<U> implements LinkPredictionFormat<U>
{

    private final Function3<U,U,Double, String> tupleWriter;
    private final Function<String, Tuple2od<Pair<U>>> tupleReader;
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
     * @param sortByDecreasingScore read tuples by decreasing score?
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