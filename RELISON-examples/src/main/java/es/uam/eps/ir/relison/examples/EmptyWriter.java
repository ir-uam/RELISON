/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.examples;

import es.uam.eps.ir.ranksys.core.Recommendation;
import org.ranksys.formats.rec.RecommendationFormat;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

/**
 * Class for storing / reading recommendations on RAM memory.
 * @param <U> Type of the users.
 * @param <I> Type of the items
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 */
public class EmptyWriter<U, I> implements RecommendationFormat.Writer<U, I>, RecommendationFormat.Reader<U, I>
{
    /**
     * List for storing the recommendations.
     */
    private final ConcurrentLinkedQueue<Recommendation<U, I>> recs;

    /**
     * Constructor.
     */
    public EmptyWriter()
    {
        this.recs = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void write(Recommendation<U, I> recommendation)
    {
        try
        {
            this.recs.add(recommendation);
        }
        catch (ArrayIndexOutOfBoundsException array)
        {
            System.err.println("WHAT?");
        }

    }

    @Override
    public void close()
    {
        // Does not do anything since it is not a file.
    }

    @Override
    public Stream<Recommendation<U, I>> readAll()
    {
        // just returns the stream of the list.
        return recs.stream();
    }
}
