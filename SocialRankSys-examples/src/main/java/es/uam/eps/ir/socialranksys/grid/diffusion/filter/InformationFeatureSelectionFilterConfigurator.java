/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.grid.diffusion.filter;

import es.uam.eps.ir.socialranksys.diffusion.data.filter.DataFilter;
import es.uam.eps.ir.socialranksys.diffusion.data.filter.InformationFeatureSelectionFilter;
import org.ranksys.formats.parsing.Parser;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Configures a Tag Selection Filter.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <P> type of the parameters.
 */
public class InformationFeatureSelectionFilterConfigurator<U extends Serializable,I extends Serializable,P> implements FilterConfigurator<U,I,P>
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
    private final Parser<P> parser;
    
    /**
     * Constructor.
     * @param parser A tag parser. 
     */
    public InformationFeatureSelectionFilterConfigurator(Parser<P> parser)
    {
        this.parser = parser;
    }
    
    @Override
    public DataFilter<U, I, P> getFilter(FilterParamReader fgr)
    {
        
        Set<P> set = new HashSet<>();
        
        String tagName = fgr.getParams().getStringValue(TAGNAME);
        String tagFile = fgr.getParams().getStringValue(TAGFILE);
        try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(tagFile))))
        {
            set = br.lines().map(parser::parse).collect(Collectors.toCollection(HashSet::new));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        return new InformationFeatureSelectionFilter<>(set, tagName);
    }
}
