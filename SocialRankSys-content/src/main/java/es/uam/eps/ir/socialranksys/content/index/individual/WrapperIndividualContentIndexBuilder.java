package es.uam.eps.ir.socialranksys.content.index.individual;

import es.uam.eps.ir.socialranksys.content.index.AbstractIndexBuilder;
import es.uam.eps.ir.socialranksys.content.index.Index;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.IOException;

/**
 * Individual content index builder wrapping a simple one.
 *
 * @param <C> type of the contents.
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class WrapperIndividualContentIndexBuilder<C, U> extends AbstractIndividualContentIndexBuilder<C, U>
{
    /**
     * A builder.
     */
    private final AbstractIndexBuilder<C> builder;

    /**
     * Map storing the relation between contents and users.
     */
    private final Int2ObjectMap<U> userMap;

    /**
     * Constructor.
     *
     * @param builder an index builder.
     */
    public WrapperIndividualContentIndexBuilder(AbstractIndexBuilder<C> builder)
    {
        this.builder = builder;
        this.userMap = new Int2ObjectOpenHashMap<>();
    }

    @Override
    public Index<C> getCoreIndex() throws IOException
    {
        return this.getCoreIndividualIndex();
    }

    @Override
    protected IndividualContentIndex<C, U> getCoreIndividualIndex() throws IOException
    {
        return new WrapperIndividualContentIndex<>(this.builder.getCoreIndex(), userMap);
    }

    @Override
    public void indexText(String content, C contentId, U userId) throws IOException
    {
        int docId = this.indexText(content, contentId);
        this.userMap.put(docId, userId);
    }

    @Override
    public void init(String indexPath) throws IOException
    {
        this.builder.init(indexPath);
        this.userMap.clear();
    }

    @Override
    public int indexText(String content, C contentId) throws IOException
    {
        return this.builder.indexText(content, contentId);
    }

    @Override
    public void close(String indexPath) throws IOException
    {
        this.builder.close(indexPath);
        this.saveUserContentMap(indexPath);
    }
}
