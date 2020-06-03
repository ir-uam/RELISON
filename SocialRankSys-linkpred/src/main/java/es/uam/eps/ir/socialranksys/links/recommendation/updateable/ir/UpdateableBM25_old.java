/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.updateable.ir;

import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.updateable.UserFastRankingUpdateableRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.jooq.lambda.tuple.Tuple3;

import java.util.stream.Stream;

/**
 * Adaptation of the BM-25 Information Retrieval Algorithm for user recommendation. Uses a term-based implementation.
 * 
 * Sparck Jones, K., Walker, S., Roberton S.E. A Probabilistic Model of Information Retrieval: Development and Comparative Experiments. 
 * Information Processing and Management 36. February 2000, pp. 779-808 (part 1), pp. 809-840 (part 2).
 * 
 * @author Javier Sanz-Cruzado Puig
 * @param <U> type of the users
 */
public class UpdateableBM25_old<U> extends UserFastRankingUpdateableRecommender<U>
{
    /**
     * Parameter that tunes the effect of the neighborhood size. Between 0 and 1
     */
    private final double b;
    /**
     * Parameter that tunes the effect of the term frequency on the formula.
     */
    private final double k;
    /**
     * Neighborhood selection for the target users.
     */
    private final EdgeOrientation uSel;
        /**
     * Neighborhood selection for the candidate users.
     */
    private final EdgeOrientation vSel;
    /**
     * Neighbour selection for the document length
     */
    private final EdgeOrientation dlSel;
    /**
     * Average size of the neighborhood of the candidate nodes.
     */
    private double avgSize;
    /**
     * Number of users in the network.
     */
    private long numUsers;
    /**
     * Robertson-Sparck-Jones formula values for each user.
     */
    private final Int2DoubleOpenHashMap rsj;
    /**
     * Neighborhood sizes for each user.
     */
    private final Int2DoubleOpenHashMap size;

    private final Int2DoubleOpenHashMap wLengths;
    /**
     * Constructor.
     * @param graph Graph
     * @param uSel Selection of the neighbours of the target user
     * @param vSel Selection of the neighbours of the candidate user
     * @param dlSel Selection of the neighbours for the document length
     * @param b Tunes the effect of the neighborhood size. Between 0 and 1.
     * @param k parameter of the algorithm.
     */
    public UpdateableBM25_old(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel, EdgeOrientation dlSel, double b, double k)
    {
        super(graph);
        
        this.dlSel = dlSel;
        this.b = b;
        this.k = k;
        this.rsj = new Int2DoubleOpenHashMap();
        this.size = new Int2DoubleOpenHashMap();
        this.wLengths = new Int2DoubleOpenHashMap();
        this.numUsers = graph.getVertexCount();
        
        this.uSel = uSel;
        this.vSel = vSel.invertSelection();
        this.avgSize = this.getAllUidx().mapToDouble(vidx -> 
        {
            // Compute RSJ
            double rsjV = graph.getNeighborhood(vidx, this.vSel).count();
            this.wLengths.put(vidx, rsjV);
            rsjV = Math.log((numUsers - rsjV + 0.5)/(rsjV + 0.5));
            this.rsj.put(vidx, rsjV);
            
            
            // Compute size
            double val = graph.getNeighborhoodWeights(vidx, dlSel).mapToDouble(widx -> widx.v2).sum();

            this.size.put(vidx, val);
            return val;
        }).average().getAsDouble();
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx) 
    {
        Int2DoubleOpenHashMap scoresMap = new Int2DoubleOpenHashMap();
        scoresMap.defaultReturnValue(0.0);

        
        if(Double.isFinite(this.k))
        {
            graph.getNeighborhood(uidx, uSel).forEach(widx -> 
            {
                double rsjW = this.rsj.get((int) widx);
                graph.getNeighborhoodWeights(widx, vSel).forEach(vidx -> 
                {
                    double weight = vidx.v2;
                    double s = this.size.get(vidx.v1);
                    
                    double num = (this.k + 1.0)*weight*rsjW;
                    double den = this.k*(1-b + (b*s/avgSize)) + weight;
                    
                    scoresMap.addTo(vidx.v1, num/den);
                });
            });
        }
        else
        {
            graph.getNeighborhood(uidx, uSel).forEach(widx -> 
            {
                double rsjW = this.rsj.get((int) widx);
                graph.getNeighborhoodWeights(widx, vSel).forEach(vidx -> 
                {
                    double weight = vidx.v2;
                    double s = this.size.get(vidx.v1);
                    
                    double num = weight*rsjW;
                    double den = (1-b + (b*s/avgSize));
                    
                    scoresMap.addTo(vidx.v1, num/den);
                });
            });
        }

        return scoresMap;
    }
    
    
    @Override
    public void updateAddUser(U u)
    {
        if(!this.graph.containsVertex(u))
        {
            this.graph.addNode(u);
            this.rsj.keySet().stream().forEach(widx -> this.rsj.addTo(widx, Math.log(1.0 + 1.0/(this.numUsers - this.wLengths.get((int) widx) + 0.5))));
            
            int uidx = this.user2uidx(u);
            
            this.rsj.put(uidx, Math.log((numUsers + 1.5)/0.5));
            this.wLengths.put(uidx, 0.0);
            this.size.put(uidx, 0.0);
            
            // Update the average size
            this.avgSize *= (numUsers + 0.0)/(numUsers + 1.0);
            this.numUsers += 1;
        }
    }
    
    @Override
    public void updateAddItem(U u)
    {
        updateAddUser(u);
    }
    
    /**
     * Re-trains the recommender after receiving a set of preferences
     * @param newPrefs the new preferences
     */
    @Override
    public void update(Stream<Tuple3<U,U,Double>> newPrefs)
    {
        newPrefs.forEach(triplet -> 
        {
            U u = triplet.v1;
            U v = triplet.v2;
            double val = triplet.v3;
            
            
            // If it is not necessary to add a user to the network
            if(this.graph.containsVertex(u) && this.graph.containsVertex(v)) // Just update weights
            {
                int uidx = this.user2uidx(u);
                int vidx = this.user2uidx(v);

                boolean containsEdge = this.graph.containsEdge(uidx, vidx);
                double oldw = this.graph.getEdgeWeight(uidx, vidx);
                double incr = val - oldw;
            
                // Update the RSJ of the corresponding nodes.
                if(!this.vSel.equals(EdgeOrientation.IN) && !containsEdge)
                {
                    double numAdj = this.wLengths.get(uidx);
                    double increase = Math.log(1.0 - 1.0/(this.numUsers - numAdj + 0.5));
                    increase = increase - Math.log(1.0 + 1.0/(numAdj + 0.5));
                    this.rsj.addTo(uidx, increase);

                    this.wLengths.addTo(uidx, 1.0);
                }

                if(!this.vSel.equals(EdgeOrientation.OUT) && !containsEdge)
                {
                    double numInc = this.wLengths.get(vidx);
                    double increase = Math.log(1.0 - 1.0/(this.numUsers - numInc + 0.5));
                    increase -= Math.log(1.0 + 1.0/(numInc+0.5));
                    this.rsj.addTo(vidx, increase);                
                    this.wLengths.addTo(vidx, 1.0);
                }

                // Update the individual sizes and the average size.
                if(!graph.isDirected() || !dlSel.equals(EdgeOrientation.IN))
                {
                    this.size.addTo(uidx, incr);
                    this.avgSize += incr/(this.numUsers + 0.0);
                }

                if(!graph.isDirected() || !dlSel.equals(EdgeOrientation.OUT))
                {
                    this.size.addTo(vidx, incr);
                    this.avgSize += incr/(this.numUsers + 0.0);
                }

                if(containsEdge) this.graph.updateEdgeWeight(uidx, vidx, val);
                else this.graph.addEdge(u, v, val);
            }
        });
                
    }

    @Override
    public void update(U u, U v, double val) 
    {
        if(this.graph.containsVertex(u) && this.graph.containsVertex(v)) // Just update weights
        {
            int uidx = this.user2uidx(u);
            int vidx = this.user2uidx(v);

            boolean containsEdge = this.graph.containsEdge(uidx, vidx);
            double oldw = this.graph.getEdgeWeight(uidx, vidx);
            double incr = val - oldw;

            // Update the RSJ of the corresponding nodes.
            if(!this.vSel.equals(EdgeOrientation.IN) && !containsEdge)
            {
                double numAdj = this.wLengths.get(uidx);
                double increase = Math.log(1.0 - 1.0/(this.numUsers - numAdj + 0.5));
                increase = increase - Math.log(1.0 + 1.0/(numAdj + 0.5));
                this.rsj.addTo(uidx, increase);

                this.wLengths.addTo(uidx, 1.0);
            }

            if(!this.vSel.equals(EdgeOrientation.OUT) && !containsEdge)
            {
                double numInc = this.wLengths.get(vidx);
                double increase = Math.log(1.0 - 1.0/(this.numUsers - numInc + 0.5));
                increase -= Math.log(1.0 + 1.0/(numInc+0.5));
                this.rsj.addTo(vidx, increase);                
                this.wLengths.addTo(vidx, 1.0);
            }

            // Update the individual sizes and the average size.
            if(!graph.isDirected() || !dlSel.equals(EdgeOrientation.IN))
            {
                this.size.addTo(uidx, incr);
                this.avgSize += incr/(this.numUsers + 0.0);
            }

            if(!graph.isDirected() || !dlSel.equals(EdgeOrientation.OUT))
            {
                this.size.addTo(vidx, incr);
                this.avgSize += incr/(this.numUsers + 0.0);
            }

            if(containsEdge) this.graph.updateEdgeWeight(uidx, vidx, val);
            else this.graph.addEdge(u, v, val);
        }
    }

    @Override
    public void updateDelete(U u, U u2, double val)
    {
        //TODO
    }
}
