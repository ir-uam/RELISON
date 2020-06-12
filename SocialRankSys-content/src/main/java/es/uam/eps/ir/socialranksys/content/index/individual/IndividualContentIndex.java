package es.uam.eps.ir.socialranksys.content.index.individual;

import es.uam.eps.ir.socialranksys.content.index.Index;

/**
 * Index that stores individual pieces for each user.
 * @param <C> type of the content identifiers.
 * @param <U> type of the user identifiers.
 */
public interface IndividualContentIndex<C,U> extends Index<C>
{
    /**
     * Given a content identifier, obtains the user that created it.
     * @param contentId the content identifier.
     * @return the creator of the content.
     */
    U getUser(int contentId);
}
