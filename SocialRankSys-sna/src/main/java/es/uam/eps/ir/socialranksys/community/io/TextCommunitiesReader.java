/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.community.io;

import es.uam.eps.ir.socialranksys.community.Communities;
import org.ranksys.formats.parsing.Parser;
import org.ranksys.formats.parsing.Parsers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads a file containing the community structure.
 * <p>
 * File structure:
 * <p>
 * node community
 * <p>
 *
 * @param <U> type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class TextCommunitiesReader<U> implements CommunitiesReader<U>
{
    /**
     * Delimiter separating node identifier and community.
     */
    private final String delimiter;
    /**
     * Parser for the users.
     */
    private final Parser<U> parser;

    /**
     * Constructor.
     *
     * @param delimiter delimiter separating node identifier and community identifier.
     * @param parser    user parser for reading the users.
     */
    public TextCommunitiesReader(String delimiter, Parser<U> parser)
    {
        this.delimiter = delimiter;
        this.parser = parser;
    }

    @Override
    public Communities<U> read(String file)
    {
        Map<Integer, Integer> visitedComms = new HashMap<>();
        Communities<U> communities = new Communities<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file))))
        {
            br.lines().forEach((line) ->
            {
                String[] split = line.split(delimiter);
                U user = parser.parse(split[0]);
                Integer comm = Parsers.ip.parse(split[1]);

                if (!visitedComms.containsKey(comm))
                {
                    visitedComms.put(comm, visitedComms.size());
                    communities.addCommunity();
                }

                communities.add(user, visitedComms.get(comm));
            });

            return communities;
        }
        catch (IOException ioe)
        {
            System.err.println("ERROR: An error ocurred while reading the file");
            return null;
        }
    }
}
