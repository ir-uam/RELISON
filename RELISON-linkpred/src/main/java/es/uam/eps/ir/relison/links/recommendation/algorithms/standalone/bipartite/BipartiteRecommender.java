/* 
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.recommendation.algorithms.standalone.bipartite;

import es.uam.eps.ir.relison.graph.DirectedGraph;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.graph.generator.EmptyGraphGenerator;
import es.uam.eps.ir.relison.graph.generator.GraphGenerator;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.relison.links.recommendation.UserFastRankingRecommender;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract class which represents a bipartite recommender. In those recommenders, there are two types 
 * of users: authorities and hubs. Hubs only have outgoing links, and authorities only have incoming links.
 *
 * @param <U> type of the users.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public abstract class BipartiteRecommender<U> extends UserFastRankingRecommender<U>
{

    /**
     * Execution mode: true if we want to recommend authorities, false if not.
     */
    protected final boolean mode;
    /**
     * A map between the hubs identifiers and their true representation (the node in 
     * the original graph).
     */
    protected final Map<Long,U> hubs;
    /**
     * A map between the authorities identifiers and their true representation (the node in 
     * the original graph).
     */    
    protected final Map<Long,U> authorities;
    /**
     * The bipartite representation of the graph.
     */
    protected final DirectedGraph<Long> bipartiteGraph;
    
    /**
     * Constructor.
     * @param graph the graph.
     * @param mode  true if we want to recommend authorities, false if we want to recommmend hubs.
     */
    public BipartiteRecommender(FastGraph<U> graph, boolean mode)
    {
        super(graph);
        this.mode = mode;
        this.hubs = new HashMap<>();
        this.authorities = new HashMap<>();
        
        this.bipartiteGraph = this.computeBipartiteGraph();
    }

    /**
     * Computes the bipartite graph from the original network.
     * @return the bipartite graph.
     */
    private DirectedGraph<Long> computeBipartiteGraph()
    {
        try 
        {
            GraphGenerator<Long> gg = new EmptyGraphGenerator<>();
            gg.configure(true, this.getGraph().isWeighted());
            DirectedGraph<Long> bipartite = (DirectedGraph<Long>) gg.generate();
            Map<U, Long> authAux = new HashMap<>();
            this.getGraph().getAllNodes().forEach(u -> 
            {
                long uIdx = bipartite.getVertexCount();
                this.hubs.put(uIdx,u);
                bipartite.addNode(uIdx);
                if(this.getGraph().getAdjacentNodesCount(u) > 0)
                {
                    this.getGraph().getAdjacentNodes(u).forEach(v -> 
                    {
                        long vIdx;
                        if(authAux.containsKey(v))
                        {
                            vIdx = authAux.get(v);
                        }
                        else
                        {
                            vIdx = bipartite.getVertexCount();
                            this.authorities.put(vIdx,v);
                            authAux.put(v,vIdx);
                            bipartite.addNode(vIdx);
                        }
                        
                        bipartite.addEdge(uIdx, vIdx);
                    });
                }
            });
            

            return bipartite;
        } catch (GeneratorNotConfiguredException | GeneratorBadConfiguredException ex) {
            return null;
        }
    }
}
