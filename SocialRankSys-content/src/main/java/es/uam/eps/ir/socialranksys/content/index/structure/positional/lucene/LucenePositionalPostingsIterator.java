package es.uam.eps.ir.socialranksys.content.index.structure.positional.lucene;

import es.uam.eps.ir.socialranksys.content.index.structure.Posting;
import es.uam.eps.ir.socialranksys.content.index.structure.lucene.LucenePostingsIterator;
import es.uam.eps.ir.socialranksys.content.index.structure.positional.PositionalPosting;
import org.apache.lucene.index.PostingsEnum;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author pablo
 */
public class LucenePositionalPostingsIterator extends LucenePostingsIterator
{
    PostingsEnum positionPostings;

    public LucenePositionalPostingsIterator(PostingsEnum p1, PostingsEnum p2) throws IOException {
        super(p1);
        positionPostings = p2;
    }

    public Posting next() {
        super.next();
        try {
            positionPostings.nextDoc();
            List<Integer> positions = new ArrayList<>(positionPostings.freq());
            for (int i = 0; i < positionPostings.freq(); i++)
                positions.add(positionPostings.nextPosition());
            return new PositionalPosting(positionPostings.docID(), positionPostings.freq(), positions);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
