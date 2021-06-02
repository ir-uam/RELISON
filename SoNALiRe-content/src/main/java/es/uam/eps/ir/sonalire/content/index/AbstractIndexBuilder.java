package es.uam.eps.ir.sonalire.content.index;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Abstract implementation of an index builder.
 *
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public abstract class AbstractIndexBuilder<C> implements IndexBuilder<C>
{
    /**
     * Obtains the generated index.
     *
     * @return the generated index.
     *
     * @throws IOException if something fails while creating such index.
     */
    public abstract Index<C> getCoreIndex() throws IOException;

    /**
     * Clears the folder containing the index.
     *
     * @param indexFolder the index folder.
     *
     * @throws IOException if something fails while deleting the files.
     */
    protected void clear(String indexFolder) throws IOException
    {
        File dir = new File(indexFolder);
        if (!dir.exists())
        {
            Files.createDirectories(Paths.get(indexFolder));
        }
        else
        {
            File[] files = dir.listFiles();
            if (files != null)
            {
                for (File f : files)
                {
                    if (f.isFile())
                    {
                        f.delete();
                    }
                }
            }
        }
    }

    /**
     * Saves a file containing the relation between indexes and user identifiers.
     *
     * @param indexPath the path of the index.
     *
     * @throws IOException if something fails while writing the map.
     */
    protected void saveContentMap(String indexPath) throws IOException
    {
        Index<C> index = this.getCoreIndex();
        int numDocs = index.numDocs();
        PrintStream out = new PrintStream(indexPath + "/" + Config.PATHS_FILE);
        for (int cidx = 0; cidx < numDocs; ++cidx)
        {
            out.println(index.getContent(cidx).toString());
        }
        out.close();
    }
}
