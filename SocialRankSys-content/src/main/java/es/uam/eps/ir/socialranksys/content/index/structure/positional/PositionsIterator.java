package es.uam.eps.ir.socialranksys.content.index.structure.positional;

import java.util.Iterator;
import java.util.List;

/**
 * Iterator over a list of positions
 * @author Pablo Castells
 * @author Javier Sanz-Cruzado
 */
public class PositionsIterator implements Iterator<Integer>
{
    /**
     * The position list.
     */
    List<Integer> positions;
    /**
     * Pointers.
     */
    int up, down;

    /**
     * Obtains a list of positions.
     * @param pos the list of positions.
     */
    public PositionsIterator(List<Integer> pos) {
        positions = pos;
    }

    /**
     * Indicates whether the list has a next position.
     * @return true if it has, false otherwise.
     */
    public boolean hasNext() {
        return up < positions.size();
    }

    /**
     * The next position in the position list.
     * @return the position.
     */
    public Integer next() {
        return positions.get(up++);
    }

    /**
     * Obtains the first element after a given position.
     * @param pos the position.
     * @return the next element after a given position.
     */
    public Integer nextAfter(int pos) {
        while (hasNext() && positions.get(up) <= pos) up++;
        if (hasNext()) return positions.get(up);
        else return Integer.MAX_VALUE;
    }

    /**
     * Obtains the first element before a given position.
     * @param pos the position.
     * @return the next element before a given position.
     */
    public Integer nextBefore(int pos) {
        while (down < positions.size() - 1 && positions.get(down+1) <= pos) down++;
        /* if (down < positions.size()) */ return positions.get(down);
//        else return -1;
    }
}
