/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.algorithms.standalone.ir;


import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.UserFastRankingRecommender;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

/**
 * Adaptation of the TF-IDF method of Information Retrieval for user recommendation
 *
 * @param <U> Type of the users
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class VSM<U> extends UserFastRankingRecommender<U>
{   
    /**
     * Target users' inverse document frequency
     */
    private final Int2DoubleMap uIdf;
    /**
     * Candidate users' inverse document frequency
     */
    private final Int2DoubleMap vIdf;
    /**
     * tf-idf vector modules for each user
     */
    private final Int2DoubleMap mod;
    /**
     * Neighborhood selection for the target users.
     */
    private final EdgeOrientation uSel;
    /**
     * Neighborhood selection for the candidate users.
     */
    private final EdgeOrientation vSel;
    /**
     * Constructor.
     * @param graph The original social network graph.
     * @param uSel Neighborhood selection for the target user.
     * @param vSel Neighborhood selection for the candidate user.
     */
    public VSM(FastGraph<U> graph, EdgeOrientation uSel, EdgeOrientation vSel)
    {
        super(graph);
              
        this.uSel = uSel;
        this.vSel = vSel.invertSelection();
        this.uIdf = new Int2DoubleOpenHashMap();
        this.mod = new Int2DoubleOpenHashMap();
        EdgeOrientation uAuxOrient = uSel.invertSelection();
        EdgeOrientation vAuxOrient = vSel.invertSelection();
        
        if(!graph.isDirected() || uSel.equals(vSel))
        {
            this.getAllUidx().forEach(uidx -> uIdf.put(uidx, this.calculateIdf(uidx, uAuxOrient)));
            this.vIdf = uIdf;
        }
        else
        {
            this.vIdf = new Int2DoubleOpenHashMap();
            this.getAllUidx().forEach(uidx -> 
            {
                uIdf.put(uidx, this.calculateIdf(uidx, uAuxOrient));
                vIdf.put(uidx, this.calculateIdf(uidx, vAuxOrient));
            });
        }

        this.getAllUidx().forEach(vidx -> 
        {
            double module = graph.getNeighborhoodWeights(vidx, vSel).mapToDouble(widx -> 
            {
                double val = this.calculateTf(widx.v2)*this.vIdf.get(widx.v1);
                return val*val;
            }).sum();
            this.mod.put(vidx, module);
        });
    }

    /**
     * Compute the term frequency of a node
     * @param weight the frequency value
     * @return the value of the tf
     */
    private double calculateTf(double weight)
    {
        return 1.0 + Math.log(weight)/ Math.log(2.0);
    }
    
    /**
     * Compute the inverse document frequency of a node
     * @param uidx the node
     * @param s the orientation of the neighbors
     * @return the value of the idf
     */
    private double calculateIdf(int uidx, EdgeOrientation s)
    {
        double num = this.getGraph().getNeighborhood(uidx, s).count() + 0.0;
        return Math.log(1.0 + ((double) this.numUsers()) / (num + 1.0))/ Math.log(2.0);
    }
        
    @Override
    public Int2DoubleMap getScoresMap(int uidx) 
    {
        Int2DoubleOpenHashMap scoresMap = new Int2DoubleOpenHashMap();
        scoresMap.defaultReturnValue(0.0);

        
        graph.getNeighborhoodWeights(uidx, uSel).forEach(w -> 
        {            
            int widx = w.v1;
            double uW = this.calculateTf(w.v2)*this.uIdf.get(widx);
            double vW = this.vIdf.get(widx);
            
            graph.getNeighborhoodWeights(widx, vSel).forEach(v -> 
            {
                if(v.v2 == 0) System.err.println(v.v1 + " " + widx + " " + v.v2);
                double val = uW*this.calculateTf(v.v2)*vW;
                scoresMap.addTo(v.v1, val);
            });
        });
                
        scoresMap.replaceAll((vidx, val) -> val/ Math.sqrt(this.mod.get((int) vidx)));
        
        return scoresMap;
    }
}
