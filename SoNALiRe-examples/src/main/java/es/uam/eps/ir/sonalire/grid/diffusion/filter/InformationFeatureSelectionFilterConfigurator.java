/*
 *  Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.sonalire.grid.diffusion.filter;

import es.uam.eps.ir.sonalire.diffusion.data.filter.DataFilter;
import es.uam.eps.ir.sonalire.diffusion.data.filter.InformationFeatureSelectionFilter;
import es.uam.eps.ir.sonalire.grid.Parameters;
import org.ranksys.formats.parsing.Parser;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class for configuring a filter which keeps only a fraction of the item features in a given set, and
 * all the information pieces containing such features.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users
 * @param <I> type of the information pieces.
 * @param <F> Type of the features of the users / information pieces.
 *
 * @see InformationFeatureSelectionFilter
 */
public class InformationFeatureSelectionFilterConfigurator<U extends Serializable,I extends Serializable, F> implements FilterConfigurator<U,I, F>
{
    /**
     * Identifier for the name of the tag parameter.
     */
    private final static String TAGNAME = "tagName";
    /**
     * Identifier for the file containing the tags to maintain.
     */
    private final static String TAGFILE = "tagFile";
    
    /**
     * Parameter parser, for parsing the tags.
     */
    private final Parser<F> parser;
    
    /**
     * Constructor.
     * @param parser a tag parser.
     */
    public InformationFeatureSelectionFilterConfigurator(Parser<F> parser)
    {
        this.parser = parser;
    }
    
    @Override
    public DataFilter<U, I, F> getFilter(Parameters fgr)
    {
        
        Set<F> set = new HashSet<>();
        
        String tagName = fgr.getStringValue(TAGNAME);
        String tagFile = fgr.getStringValue(TAGFILE);
        try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(tagFile))))
        {
            set = br.lines().map(parser::parse).collect(Collectors.toCollection(HashSet::new));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        return new InformationFeatureSelectionFilter<>(set, tagName);
    }
}
