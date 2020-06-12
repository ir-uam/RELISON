package es.uam.eps.ir.socialranksys.content.index.structure.positional;

import es.uam.eps.ir.socialranksys.content.index.structure.Posting;

import java.util.Iterator;
import java.util.List;

/**
 * Positional posting implementation.
 * @author Pablo Castells
 */
public class PositionalPosting extends Posting implements Iterable<Integer>
{
    /**
     * List of positions.
     */
    protected List<Integer> positions;

    /**
     * Constructor.
     * @param id identifier of the document.
     * @param f frequency of appearance.
     * @param pos position list.
     */
    public PositionalPosting(int id, long f, List<Integer> pos) {
        super(id, f);
        positions = pos;
    }

    /**
     * Obtains an iterator over the positions.
     * @return the iterator.
     */
    @Override
    public Iterator<Integer> iterator() {
        return new PositionsIterator(positions);
    }

    /**
     * Obtains the position list.
     * @return the position list.
     */
    public List<Integer> getPositions() { return this.positions; }
}
