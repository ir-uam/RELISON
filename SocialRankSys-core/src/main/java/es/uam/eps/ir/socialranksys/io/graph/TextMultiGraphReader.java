package es.uam.eps.ir.socialranksys.io.graph;

import org.ranksys.formats.parsing.Parser;

public class TextMultiGraphReader<V> extends TextGraphReader<V>
{
    public TextMultiGraphReader(boolean directed, boolean weighted, boolean selfloops, String delimiter, Parser<V> uParser)
    {
        super(true, directed, weighted, selfloops, delimiter, uParser);
    }
}
