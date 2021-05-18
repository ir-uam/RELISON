/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.data.letor.sampling;

import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Samples the complete set of users at distance two from the user
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 *
 * @param <U> Type of the users.
 */
public class DistanceTwoIndividualSampler<U> extends AbstractIndividualSampler<U>
{
    /**
     * Edge orientation for the neighbors of the origin node.
     */
    private final EdgeOrientation uSel;
    /**
     * Edge orientation for the neighbors of the destination nodes.
     */
    private final EdgeOrientation vSel;
    
    /**
     * Constructor.
     * @param graph the graph.
     * @param uSel  edge orientation for the neighbors of the origin node.
     * @param vSel  edge orientation for the neighbors of the selected nodes.
     */
    public DistanceTwoIndividualSampler(Graph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        super(graph);
        this.uSel = uSel;
        this.vSel = vSel;
    }
    
    @Override
    public Set<U> sampleUsers(U u, Predicate<U> filter)
    {
        Set<U> sample = new HashSet<>();
        
        this.graph.getNeighbourhood(u, uSel).forEach(v -> 
        {
            if(!u.equals(v))
                this.graph.getNeighbourhood(v, vSel.invertSelection()).forEach(w -> 
                {
                    if(!u.equals(w) && filter.test(w))
                        sample.add(w);
                });
        });
        
        return sample;
    }
    
}
