/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.updateable.ir;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.links.recommendation.updateable.UserFastRankingUpdateableRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import org.jooq.lambda.tuple.Tuple3;

import java.util.OptionalDouble;
import java.util.stream.Stream;

/**
 * Updateable adaptation of the BM-25 Information Retrieval Algorithm for user recommendation. Uses a term-based implementation.
 * <p>
 * <b>Reference: </b> K. Sparck Jones, S. Walker, S.E. Robertson. A Probabilistic Model of Information Retrieval: Development and Comparative Experiments.
 * Information Processing and Management 36. February 2000, pp. 779-808 (part 1), pp. 809-840 (part 2).
 * </p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 *
 * @param <U> type of the users.
 */
public class UpdateableBM25<U> extends UserFastRankingUpdateableRecommender<U>
{
    /**
     * Parameter that tunes the effect of the neighborhood size. Between 0 and 1.
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
     * Neighborhood sizes for each user.
     */
    private final Int2DoubleOpenHashMap size;

    private final Int2DoubleOpenHashMap wLengths;
    /**
     * Constructor.
     * @param graph graph
     * @param uSel  selection of the neighbours of the target user
     * @param vSel  selection of the neighbours of the candidate user
     * @param dlSel selection of the neighbours for the document length
     * @param b     tunes the effect of the neighborhood size. Between 0 and 1.
     * @param k     parameter of the algorithm.
     */
    public UpdateableBM25(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel, EdgeOrientation dlSel, double b, double k)
    {
        super(graph);
        
        this.dlSel = dlSel;
        this.b = b;
        this.k = k;
        this.size = new Int2DoubleOpenHashMap();
        this.wLengths = new Int2DoubleOpenHashMap();
        this.numUsers = graph.getVertexCount();
        
        this.uSel = uSel;
        this.vSel = vSel.invertSelection();

        OptionalDouble opt = this.getAllUidx().mapToDouble(vidx ->
        {
             double rsjV = graph.getNeighborhood(vidx, this.vSel).count();
             this.wLengths.put(vidx, rsjV);

             double val = graph.getNeighborhoodWeights(vidx, dlSel).mapToDouble(widx -> widx.v2).sum();
             this.size.put(vidx, val);
             return val;
        }).average();

        this.avgSize = opt.isPresent() ? opt.getAsDouble() : 0.0;
    }

    @Override
    public Int2DoubleMap getScoresMap(int uidx) 
    {
        Int2DoubleOpenHashMap scoresMap = new Int2DoubleOpenHashMap();
        scoresMap.defaultReturnValue(0.0);

        if(Double.isFinite(k))
        {
            graph.getNeighborhood(uidx, uSel).forEach(widx ->
            {
                double wLen = this.wLengths.get(widx.intValue());
                double rsjW = (this.numUsers - wLen + 0.5)/(wLen + 0.5);

                graph.getNeighborhoodWeights(widx, vSel).forEach(vidx ->
                {
                    double weight = vidx.v2;
                    double s = this.size.get(vidx.v1);
                    double num = (this.k + 1.0)*weight*rsjW;
                    double den = this.k*(1.0-b+((b*s/avgSize))) + weight;

                    scoresMap.addTo(vidx.v1, num/den);
                });
            });
        }
        else
        {
            graph.getNeighborhood(uidx, uSel).forEach(widx ->
            {
                double wLen = this.wLengths.get(widx.intValue());
                double rsjW = (this.numUsers - wLen + 0.5)/(wLen + 0.5);

                graph.getNeighborhoodWeights(widx, vSel).forEach(vidx ->
                {
                    double weight = vidx.v2;
                    double s = this.size.get(vidx.v1);
                    double num = weight*rsjW;
                    double den = (1.0-b+((b*s/avgSize)));

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
            int uidx = this.graph.object2idx(u);
            this.wLengths.put(uidx, 0.0);
            this.size.put(uidx, 0.0);
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

            if(this.graph.containsVertex(u) && this.graph.containsVertex(v)) // Just update weights
            {
                int uidx = this.user2uidx(u);
                int vidx = this.user2uidx(v);

                boolean containsEdge = this.graph.containsEdge(uidx, vidx);
                double oldw = this.graph.getEdgeWeight(uidx, vidx);
                double incr = val - oldw;

                // We first update the RSJ value.
                if(!containsEdge)
                {
                    if(!graph.isDirected() || !this.vSel.equals(EdgeOrientation.IN))
                    {
                        this.wLengths.addTo(uidx, 1.0);
                    }

                    if(!graph.isDirected() || !this.vSel.equals(EdgeOrientation.OUT))
                    {
                        this.wLengths.addTo(vidx, 1.0);
                    }
                }

                // And, then, the length:
                if(!graph.isDirected() || !dlSel.equals(EdgeOrientation.IN))
                {
                    this.size.addTo(uidx, incr);
                    this.avgSize += incr/(this.numUsers+0.0);
                }

                if(!graph.isDirected() || !dlSel.equals(EdgeOrientation.OUT))
                {
                    this.size.addTo(vidx, incr);
                    this.avgSize += incr/(this.numUsers+0.0);
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

            // We first update the RSJ value.
            if(!containsEdge)
            {
                if(!graph.isDirected() || !this.vSel.equals(EdgeOrientation.IN))
                {
                    this.wLengths.addTo(uidx, 1.0);
                }

                if(!graph.isDirected() || !this.vSel.equals(EdgeOrientation.OUT))
                {
                    this.wLengths.addTo(vidx, 1.0);
                }
            }

            // And, then, the length:
            if(!graph.isDirected() || !dlSel.equals(EdgeOrientation.IN))
            {
                this.size.addTo(uidx, incr);
                this.avgSize += incr/(this.numUsers+0.0);
            }

            if(!graph.isDirected() || !dlSel.equals(EdgeOrientation.OUT))
            {
                this.size.addTo(vidx, incr);
                this.avgSize += incr/(this.numUsers+0.0);
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
