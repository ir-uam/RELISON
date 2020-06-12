package es.uam.eps.ir.socialranksys.content.index.individual;

import es.uam.eps.ir.socialranksys.content.index.IndexBuilder;

import java.io.IOException;

/**
 * Individual content index builder.
 * @param <C> type of the contents.
 * @param <U> type of the users.
 */
public interface IndividualContentIndexBuilder<C,U> extends IndexBuilder<C>
{
    /**
     * Writes the contents of a user in the index.
     * @param content the published content.
     * @param contentId the user.
     * @throws IOException if something fails while writing the contents.
     */
    void indexText(String content, C contentId, U userId) throws IOException;
}
