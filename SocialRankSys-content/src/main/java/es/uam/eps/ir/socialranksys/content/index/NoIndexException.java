package es.uam.eps.ir.socialranksys.content.index;

import java.io.IOException;

/**
 * Exception for the case when an index does not exist
 * @author Pablo Castells
 */
public class NoIndexException extends IOException
{
    /**
     * The route where the index has been searched.
     */
    private final String folder;

    /**
     * Constructor.
     * @param f the folder.
     */
    public NoIndexException (String f)
    {
        folder = f;
    }

    /**
     * Obtains the folder which has been searched and does not contain an index.
     * @return the folder.
     */
    public String getFolder()
    {
        return folder;
    }
}
