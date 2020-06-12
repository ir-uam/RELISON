package es.uam.eps.ir.socialranksys.content.index;

import java.io.IOException;

/**
 * Interface for an object that builds an index.
 * @author Pablo Castells
 * @author Javier Sanz-Cruzado.
 */
public interface IndexBuilder<C>
{
    /**
     * Initializes the builder.
     * @param indexPath path containing the index.
     * @throws IOException if something fails while initializing the index.
     */
    void init(String indexPath) throws IOException;

    /**
     * Writes the contents of a user in the index.
     * @param content the published content.
     * @param contentId the user.
     * @throws IOException if something fails while writing the contents.
     * @return the identifier of the new document.
     */
    int indexText(String content, C contentId) throws IOException;

    /**
     * Closes the builder.
     * @param indexPath path containing the index.
     * @throws IOException if something fails while closing the index.
     */
    void close(String indexPath) throws IOException;
}
