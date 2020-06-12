package es.uam.eps.ir.socialranksys.content.index.individual;

import es.uam.eps.ir.socialranksys.content.index.AbstractIndex;
import es.uam.eps.ir.socialranksys.content.index.Config;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.ranksys.formats.parsing.Parser;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * Abstract implementation of a content index.
 * @param <C> type of the contents.
 * @param <U> type of the users.
 */
public abstract class AbstractIndividualContentIndex<C,U> extends AbstractIndex<C> implements IndividualContentIndex<C,U>
{
    /**
     * Mapping from identifiers to contents.
     */
    protected Int2ObjectMap<U> contentsToUsers;

    @Override
    public U getUser(int docID)
    {
        return contentsToUsers.get(docID);
    }

    /**
     * Loads the users.
     * @param indexFolder folder in which the index is stored.
     * @param uParser user parser.
     * @throws IOException if something fails while reading the users.
     */
    public void loadUsers(String indexFolder, Parser<U> uParser) throws IOException
    {
        forward = new Int2ObjectOpenHashMap<>();
        backward = new Object2IntOpenHashMap<>();
        File f = new File(indexFolder + "/" + Config.PATHS_FILE);
        if(!f.exists()) return;
        Scanner scn = new Scanner(f);
        int numDocs = this.numDocs();

        for(int cidx = 0; cidx < numDocs; ++cidx)
        {
            U user = uParser.parse(scn.nextLine());
            contentsToUsers.put(cidx, user);
        }
    }
}
