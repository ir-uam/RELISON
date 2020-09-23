package es.uam.eps.ir.socialranksys.content.index.individual;

import es.uam.eps.ir.socialranksys.content.index.AbstractIndex;
import es.uam.eps.ir.socialranksys.content.index.Config;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.ranksys.formats.parsing.Parser;

import java.io.File;
import java.io.IOException;
import java.util.List;
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

    protected Object2ObjectMap<U, IntList> usersToContents;

    @Override
    public U getUser(int docID)
    {
        return contentsToUsers.get(docID);
    }

    @Override
    public List<Integer> getContents(U user)
    {
        return usersToContents.containsKey(user) ? this.usersToContents.get(user) : new IntArrayList();
    }

    /**
     * Saves a file containing the relation between indexes and user identifiers.
     * @param indexPath the path of the index.
     * @throws IOException if something fails while writing the map.
     */
    protected void loadUserContentMap(String indexPath, Parser<U> uParser) throws IOException
    {
        this.contentsToUsers = new Int2ObjectOpenHashMap<>();
        this.usersToContents = new Object2ObjectOpenHashMap<>();

        File f = new File(indexPath + "/" + Config.POSTINGS_FILE);
        if(!f.exists()) return;
        Scanner scn = new Scanner(f);

        int docId = 0;
        while(scn.hasNext())
        {
            U user = uParser.parse(scn.nextLine());
            contentsToUsers.put(docId, user);
            if(!usersToContents.containsKey(user))
                usersToContents.put(user, new IntArrayList());
            usersToContents.get(user).add(docId);
            docId++;
        }
        scn.close();
    }
}
