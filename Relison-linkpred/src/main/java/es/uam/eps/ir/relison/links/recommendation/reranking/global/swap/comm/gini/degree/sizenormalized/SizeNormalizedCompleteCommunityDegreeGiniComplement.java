/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.global.swap.comm.gini.degree.sizenormalized;

import es.uam.eps.ir.relison.community.Communities;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.links.recommendation.reranking.global.swap.comm.gini.degree.AbstractCommunityDegreeGiniComplement;
import es.uam.eps.ir.relison.links.recommendation.reranking.normalizer.Normalizer;
import es.uam.eps.ir.relison.utils.datatypes.Pair;

import java.util.function.Supplier;

/**
 * Swap reranker for promoting the balance in the degree distribution for the different
 * communities. It considers both links between communities and inside communities.
 *
 * Degrees are normalized by the maximum possible degree of the community.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> Type of the users.
 */
public class SizeNormalizedCompleteCommunityDegreeGiniComplement<U> extends AbstractCommunityDegreeGiniComplement<U>
{
    /**
     * Constructor.
     * @param lambda        establishes the trade-off between the value of the Gini Index and the original score
     * @param cutoff        the maximum number of contacts in the definitive rankings.
     * @param norm          the normalization scheme.
     * @param graph         the original graph.
     * @param communities   the relation between communities and users in the graph.
     * @param selfloops     true if selfloops are allowed, false if they are not.
     * @param outer         true if we want to force links to go outside communities.
     * @param orientation   the orientation of the community degree to take.
     */
    public SizeNormalizedCompleteCommunityDegreeGiniComplement(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities, boolean selfloops, boolean outer, EdgeOrientation orientation)
    {
        super(lambda, cutoff, norm, graph, communities, selfloops, true, outer, orientation);
    }

    /**
     * Constructor. By default, this constructor does not force links to go outside communities.
     * @param lambda        establishes the trade-off between the value of the Gini Index and the original score
     * @param cutoff        the maximum number of contacts in the definitive rankings.
     * @param norm          the normalization scheme.
     * @param graph         the original graph.
     * @param communities   the relation between communities and users in the graph.
     * @param selfloops     true if selfloops are allowed, false if they are not.
     * @param orientation   the orientation of the community degree to take.
     */
    public SizeNormalizedCompleteCommunityDegreeGiniComplement(double lambda, int cutoff, Supplier<Normalizer<U>> norm, Graph<U> graph, Communities<U> communities, boolean selfloops, EdgeOrientation orientation)
    {
        super(lambda, cutoff, norm, graph, communities, selfloops, true, false, orientation);
    }
    
    @Override
    protected Pair<Double> computeAddValue(Integer userComm, Integer recommComm)
    {
        double userCommSize = this.communities.getCommunitySize(userComm) + 0.0;
        double recommCommSize = this.communities.getCommunitySize(recommComm) + 0.0;
        double numUsers = this.graph.getVertexCount() - (selfloops ? 0.0 : 1.0);
        
        if(!this.graph.isDirected())
        {
            if(userComm.equals(recommComm))
            {
                
                return new Pair<>(1.0/(userCommSize*numUsers),0.0);
            }
            else
            {
                return new Pair<>(1.0/(userCommSize*numUsers),1.0/(recommCommSize*numUsers));
            }
        }
        else if(this.orientation.equals(EdgeOrientation.OUT))
        {
            return new Pair<>(1.0/(userCommSize*numUsers),0.0);
        }
        else if(this.orientation.equals(EdgeOrientation.IN))
        {
            return new Pair<>(0.0,1.0/(recommCommSize*numUsers));
        }
        else
        {
            return new Pair<>(1.0/(userCommSize*numUsers),1.0/(recommCommSize*numUsers));
        }
    }
    
    @Override
    protected Pair<Double> computeDelValue(Integer userComm, Integer delComm)
    {
        return this.computeAddValue(userComm, delComm);
    }
    
    @Override
    protected double communityValue(int userComm, double degree)
    {
        double sizeComm = this.communities.getCommunitySize(userComm) + 0.0;
        double numUsers = this.graph.getVertexCount() - (selfloops ? 0.0 : 1.0);
        
        if(this.graph.isDirected() && this.orientation.equals(EdgeOrientation.UND))
        {
            return degree/(2.0*sizeComm*numUsers);
        }
        else
        {
            return degree/(sizeComm*numUsers);
        }
    }
}
