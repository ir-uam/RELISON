/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.links.linkprediction.io;

import es.uam.eps.ir.sonalire.utils.datatypes.Pair;
import org.ranksys.core.util.tuples.Tuple2od;
import org.ranksys.formats.parsing.Parser;
import org.ranksys.formats.parsing.Parsers;

import static es.uam.eps.ir.ranksys.core.util.FastStringSplitter.split;

/**
 * Simple format for link prediction: tab-separated user-user-score triplets, by decreasing order of score.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class SimpleLinkPredictionFormat<U> extends TuplesLinkPredictionFormat<U>
{
    /**
     * Constructor.
     * @param uParser a parser for reading the users.
     */
    public SimpleLinkPredictionFormat(Parser<U> uParser)
    {
        this(uParser, false);
    }

    /**
     * Constructor.
     * @param uParser               a parser for reading the users.
     * @param sortByDecreasingScore read the users in decreasing score?
     */
    public SimpleLinkPredictionFormat(Parser<U> uParser, boolean sortByDecreasingScore)
    {
        super(  (u,v,score) ->  String.join("\t", u.toString(), v.toString(), Double.toString(score)),
                line ->
                {
                    CharSequence[] tokens = split(line, '\t', 4);
                    U u = uParser.parse(tokens[0]);
                    U v = uParser.parse(tokens[1]);
                    double score = Parsers.dp.parse(tokens[2]);
                    return new Tuple2od<>(new Pair<>(u,v), score);
                },
                sortByDecreasingScore);
    }
}