/*
 *  Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.content.index.lucene;

import org.apache.lucene.index.IndexOptions;

/**
 * Constructor for a Lucene positional index builder.
 *
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class LucenePositionalIndexBuilder<C> extends LuceneBuilder<C>
{
    /**
     * Constructor.
     */
    public LucenePositionalIndexBuilder()
    {
        type.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
    }
}
