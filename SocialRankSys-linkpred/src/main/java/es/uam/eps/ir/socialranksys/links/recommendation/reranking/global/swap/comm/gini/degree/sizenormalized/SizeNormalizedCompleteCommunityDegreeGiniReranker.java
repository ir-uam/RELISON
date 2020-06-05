/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap.comm.gini.degree.sizenormalized;

import es.uam.eps.ir.socialranksys.community.Communities;
import es.uam.eps.ir.socialranksys.graph.Graph;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.swap.comm.gini.degree.CommunityDegreeGiniReranker;
import es.uam.eps.ir.socialranksys.utils.datatypes.Pair;

/**
 * Reranks a recommendation by improving the complete community Degree Gini Index of the 
 * different communities in a community graph, normalized by the maximum possible degree of 
 * the community.
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class SizeNormalizedCompleteCommunityDegreeGiniReranker<U> extends CommunityDegreeGiniReranker<U>
{
    /**
     * Constructor.
     * @param lambda Establishes the trade-off between the value of the Gini Index and the original score
     * @param cutoff The number of recommended items for each user.
     * @param norm true if the score and the Gini Index value have to be normalized.
     * @param rank true if the normalization is by ranking position, false if it is by score
     * @param graph The user graph.
     * @param communities A relation between communities and users in the graph.
     * @param orientation The orientation of the community degree to take.
     * @param autoloops true if autoloops are allowed, false if they are not.
     * @param outer true if we want to force links to go outside communities.
     */
    public SizeNormalizedCompleteCommunityDegreeGiniReranker(double lambda, int cutoff, boolean norm, boolean rank, Graph<U> graph, Communities<U> communities, boolean autoloops, boolean outer, EdgeOrientation orientation)
    {
        super(lambda, cutoff, norm, rank, graph, communities, autoloops, true, outer, orientation);
    }
    
    /**
     * Constructor. By default, this does not consider promoting links outside communities.
     * @param lambda Establishes the trade-off between the value of the Gini Index and the original score
     * @param cutoff The number of recommended items for each user.
     * @param norm true if the score and the Gini Index value have to be normalized.
     * @param rank true if the normalization is by ranking position, false if it is by score
     * @param graph The user graph.
     * @param communities A relation between communities and users in the graph.
     * @param orientation The orientation of the community degree to take.
     * @param autoloops true if autoloops are allowed, false if they are not.
     */
    public SizeNormalizedCompleteCommunityDegreeGiniReranker(double lambda, int cutoff, boolean norm, boolean rank, Graph<U> graph, Communities<U> communities, boolean autoloops, EdgeOrientation orientation) 
    {
        super(lambda, cutoff, norm, rank, graph, communities, autoloops, true, false, orientation);
    }
    
    
    @Override
    protected Pair<Double> computeAddValue(Integer userComm, Integer recommComm)
    {
        double userCommSize = this.communities.getCommunitySize(userComm) + 0.0;
        double recommCommSize = this.communities.getCommunitySize(recommComm) + 0.0;
        double numUsers = this.graph.getVertexCount() - (autoloops ? 0.0 : 1.0);
        
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
        double numUsers = this.graph.getVertexCount() - (autoloops ? 0.0 : 1.0);
        
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
