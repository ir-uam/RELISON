package es.uam.eps.ir.socialranksys.content.index;

import java.io.IOException;

/**
 * Document map containing information about the documents in an index.
 * @author Pablo Castells
 * @author Javier Sanz-Cruzado
 */
public interface DocumentMap<C>
{
    /**
     * Obtains the user corresponding to an identifier.
     * @param contentId document identifier.
     * @return the content corresponding to such identifier.
     * @throws IOException if something fails while reading the value.
     */
    C getContent(int contentId) throws IOException;

    /**
     * Obtains the identifier of a content in the index.
     * @param content the content to look up.
     * @return the identifier of the content.
     */
    int getContentId(C content);

    /**
     * Obtains the number of contents.
     * @return the number of contents.
     */
    int numDocs();
}
