package es.uam.eps.ir.socialranksys.content.index.structure;

/**
 * Interface for posting lists
 * @author Pablo Castells
 */
public interface PostingsList extends Iterable<Posting>
{
    /**
     * The size of the list.
     * @return the size.
     */
    int size();
}
