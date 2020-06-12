package es.uam.eps.ir.socialranksys.content.index.individual;

import es.uam.eps.ir.socialranksys.content.index.AbstractIndexBuilder;
import es.uam.eps.ir.socialranksys.content.index.Config;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Abstract implementation of an individual content index.
 *
 * @author Javier Sanz-Cruzado
 *
 * @param <C> Type of the contents
 * @param <U> Type of the users.
 */
public abstract class AbstractIndividualContentIndexBuilder<C,U> extends AbstractIndexBuilder<C> implements IndividualContentIndexBuilder<C,U>
{
    /**
     * Obtains the generated index.
     * @return the generated index.
     * @throws IOException if something fails while creating such index.
     */
    protected abstract IndividualContentIndex<C,U> getCoreIndividualIndex() throws IOException;

    /**
     * Saves a file containing the relation between indexes and user identifiers.
     * @param indexPath the path of the index.
     * @throws IOException if something fails while writing the map.
     */
    protected void saveUserContentMap(String indexPath) throws IOException
    {
        IndividualContentIndex<C,U> index = this.getCoreIndividualIndex();
        int numDocs = index.numDocs();
        PrintStream out = new PrintStream(indexPath + "/" + Config.POSTINGS_FILE);
        for(int cidx = 0; cidx < numDocs; ++cidx)
        {
            out.println(index.getUser(cidx).toString());
        }
        out.close();
    }
}
