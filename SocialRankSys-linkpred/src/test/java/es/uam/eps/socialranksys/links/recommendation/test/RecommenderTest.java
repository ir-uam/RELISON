package es.uam.eps.socialranksys.links.recommendation.test;
/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import es.uam.eps.ir.ranksys.core.Recommendation;
import es.uam.eps.ir.ranksys.fast.index.FastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.FastUserIndex;
import es.uam.eps.ir.ranksys.fast.index.SimpleFastItemIndex;
import es.uam.eps.ir.ranksys.fast.index.SimpleFastUserIndex;
import es.uam.eps.ir.ranksys.rec.Recommender;
import es.uam.eps.ir.socialranksys.graph.edges.EdgeOrientation;
import es.uam.eps.ir.socialranksys.graph.fast.FastDirectedUnweightedGraph;
import es.uam.eps.ir.socialranksys.graph.fast.FastGraph;
import es.uam.eps.ir.socialranksys.links.recommendation.standalone.distance.ShortestDistance;
import es.uam.eps.ir.socialranksys.links.recommendation.standalone.foaf.*;
import es.uam.eps.ir.socialranksys.links.recommendation.standalone.ir.*;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Class that tests some recommenders.
 * @author Javier Sanz-Cruzado Puig
 */
public class RecommenderTest 
{
    
    FastUserIndex<Long> uIndex;
    FastItemIndex<Long> iIndex;
    FastGraph<Long> graph;
    
    public RecommenderTest() 
    {
        List<Long> users = new ArrayList<>();
        users.add(0L);
        users.add(1L);
        users.add(2L);
        users.add(3L);
        users.add(4L);
        
        uIndex = SimpleFastUserIndex.load(users.stream());
        iIndex = SimpleFastItemIndex.load(users.stream());
        
        graph = new FastDirectedUnweightedGraph<>();
        graph.addNode(0L);
        graph.addNode(1L);
        graph.addNode(2L);
        graph.addNode(3L);
        graph.addNode(4L);
        
        graph.addEdge(0L, 1L);
        graph.addEdge(0L, 3L);
        graph.addEdge(1L, 3L);
        graph.addEdge(2L, 0L);
        graph.addEdge(2L, 4L);
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }
    
    /**
     * Popularity algorithm
     */
    @Test
    public void popularity()
    {
        Recommender<Long, Long> rec = new PreferentialAttachment<>(graph, EdgeOrientation.IN);
        Recommendation<Long, Long> res = rec.getRecommendation(2L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(3L))
            {
                assertEquals(2.0, out.v2, 0.0001);
            }
        });
        
        rec = new PreferentialAttachment<>(graph, EdgeOrientation.OUT);
        res = rec.getRecommendation(3L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(2L))
            {
                assertEquals(2.0, out.v2, 0.0001);
            }
        });
    }
    
    /**
     * MCN algorithm
     */
    @Test
    public void mcn()
    {
        Recommender<Long, Long> rec = new MostCommonNeighbors<>(graph, EdgeOrientation.OUT, EdgeOrientation.IN);
        Recommendation<Long, Long> res = rec.getRecommendation(2L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(3L))
            {
                assertEquals(1.0, out.v2, 0.0001);
            }
        });
        
        rec = new MostCommonNeighbors<>(graph, EdgeOrientation.IN, EdgeOrientation.OUT);
        res = rec.getRecommendation(3L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(2L))
            {
                assertEquals(1.0, out.v2, 0.0001);
            }
        });
    }
    
    /**
     * Jaccard algorithm
     */
    @Test
    public void jaccard()
    {
        Recommender<Long, Long> rec = new Jaccard<>(graph, EdgeOrientation.OUT, EdgeOrientation.IN);
        Recommendation<Long, Long> res = rec.getRecommendation(2L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(3L))
            {
                assertEquals(0.3333333333, out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
        
        rec = new Jaccard<>(graph, EdgeOrientation.IN, EdgeOrientation.OUT);
        res = rec.getRecommendation(3L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(2L))
            {
                assertEquals(0.3333333333, out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
    }
    
    /**
     * Salton algorithm
     */
    @Test
    public void salton()
    {
        Recommender<Long, Long> rec = new Cosine<>(graph, EdgeOrientation.OUT, EdgeOrientation.IN);
        Recommendation<Long, Long> res = rec.getRecommendation(2L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(3L))
            {
                assertEquals(0.5, out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
        
        rec = new Cosine<>(graph, EdgeOrientation.IN, EdgeOrientation.OUT);
        res = rec.getRecommendation(3L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(2L))
            {
                assertEquals(0.5, out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
    }
    
    /**
     * Sorensen algorithm
     */
    @Test
    public void sorensen()
    {
        Recommender<Long, Long> rec = new Sorensen<>(graph, EdgeOrientation.OUT, EdgeOrientation.IN);
        Recommendation<Long, Long> res = rec.getRecommendation(2L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(3L))
            {
                assertEquals(0.5, out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
        
        rec = new Sorensen<>(graph, EdgeOrientation.IN, EdgeOrientation.OUT);
        res = rec.getRecommendation(3L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(2L))
            {
                assertEquals(0.5, out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
    }
    
    /**
     * Hub Promoted Index algorithm
     */
    @Test
    public void hpi()
    {
        Recommender<Long, Long> rec = new HubPromotedIndex<>(graph, EdgeOrientation.OUT, EdgeOrientation.IN);
        Recommendation<Long, Long> res = rec.getRecommendation(2L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(3L))
            {
                assertEquals(0.5, out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
        
        rec = new HubPromotedIndex<>(graph, EdgeOrientation.IN, EdgeOrientation.OUT);
        res = rec.getRecommendation(3L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(2L))
            {
                assertEquals(0.5, out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
    }
    
    /**
     * Hub Depressed Index algorithm
     */
    @Test
    public void hdi()
    {
        Recommender<Long, Long> rec = new HubDepressedIndex<>(graph, EdgeOrientation.OUT, EdgeOrientation.IN);
        Recommendation<Long, Long> res = rec.getRecommendation(2L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(3L))
            {
                assertEquals(0.5, out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
        
        rec = new HubDepressedIndex<>(graph, EdgeOrientation.IN, EdgeOrientation.OUT);
        res = rec.getRecommendation(3L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(2L))
            {
                assertEquals(0.5, out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
    }
    
    /**
     * Adamic/Adar algorithm
     */
    @Test
    public void adamic()
    {
        Recommender<Long, Long> rec = new AdamicAdar<>(graph, EdgeOrientation.OUT, EdgeOrientation.IN, EdgeOrientation.UND);
        Recommendation<Long, Long> res = rec.getRecommendation(2L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(3L))
            {
                assertEquals(Math.log(2)/Math.log(5), out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
        
        rec = new AdamicAdar<>(graph, EdgeOrientation.IN, EdgeOrientation.OUT, EdgeOrientation.UND);
        res = rec.getRecommendation(3L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(2L))
            {
                assertEquals(Math.log(2)/Math.log(5), out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
    }
    
    /**
     * Resource Allocation algorithm
     */
    @Test
    public void resalloc()
    {
        Recommender<Long, Long> rec = new ResourceAllocation<>(graph, EdgeOrientation.OUT, EdgeOrientation.IN, EdgeOrientation.UND);
        Recommendation<Long, Long> res = rec.getRecommendation(2L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(3L))
            {
                assertEquals(0.2, out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
        
        rec = new ResourceAllocation<>(graph, EdgeOrientation.IN, EdgeOrientation.OUT, EdgeOrientation.UND);
        res = rec.getRecommendation(3L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(2L))
            {
                assertEquals(0.2, out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
    }
    
    /**
     * Distance algorithm
     */
    @Test
    public void distance()
    {
        Recommender<Long, Long> rec = new ShortestDistance<>(graph, EdgeOrientation.OUT);
        Recommendation<Long, Long> res = rec.getRecommendation(2L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(3L))
            {
                assertEquals(-2.0, out.v2, 1.0); // 1.0 + in the denom. for preventing NaNs
            }
        });
        
        rec = new ShortestDistance<>(graph, EdgeOrientation.IN);
        res = rec.getRecommendation(3L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(2L))
            {
                assertEquals(-2.0, out.v2, 1.0); // 1.0 + in the denom. for preventing NaNs
            }
        });
    }
    
    /**
     * TF-IDF algorithm
     */
    @Test
    public void tfidf()
    {
        Recommender<Long, Long> rec = new VSM<>(graph, EdgeOrientation.OUT, EdgeOrientation.IN);
        Recommendation<Long, Long> res = rec.getRecommendation(2L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(3L))
            {
                double num = (Math.log(7)/Math.log(2) - 1)*(3 - Math.log(3)/Math.log(2));
                double normD = Math.sqrt((3 - Math.log(3)/Math.log(2))*(3 - Math.log(3)/Math.log(2)) + (Math.log(7)/Math.log(2) - 1)*(Math.log(7)/Math.log(2) - 1));
                assertEquals(num/normD, out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
        
        rec = new VSM<>(graph, EdgeOrientation.IN, EdgeOrientation.OUT);
        res = rec.getRecommendation(3L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(2L))
            {
                double num = (Math.log(7)/Math.log(2) - 1)*(3 - Math.log(3)/Math.log(2));
                double normD = Math.sqrt((Math.log(7)/Math.log(2) - 1)*(Math.log(7)/Math.log(2) - 1) + (Math.log(7)/Math.log(2) - 1)*(Math.log(7)/Math.log(2) - 1));
                assertEquals(num/normD, out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
        
        
    }
    
    
    /**
     * BIR algorithm
     */
    @Test
    public void bir()
    {
        Recommender<Long, Long> rec = new BIR<>(graph, EdgeOrientation.OUT, EdgeOrientation.IN);
        Recommendation<Long, Long> res = rec.getRecommendation(2L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(3L))
            {
                assertEquals(Math.log(7) - Math.log(5), out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
        
        rec = new BIR<>(graph, EdgeOrientation.IN, EdgeOrientation.OUT);
        res = rec.getRecommendation(3L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(2L))
            {
                assertEquals(Math.log(3), out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
    }
    
    /**
     * BM25 algorithm
     */
    @Test
    public void bm25()
    {
        Recommender<Long, Long> rec = new BM25<>(graph, EdgeOrientation.OUT, EdgeOrientation.IN, EdgeOrientation.IN, 0.1, 1.0);
        Recommendation<Long, Long> res = rec.getRecommendation(2L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(3L))
            {
                assertEquals(2.0/2.1*(Math.log(7) - Math.log(5)), out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
        
        rec = new BM25<>(graph, EdgeOrientation.IN, EdgeOrientation.OUT, EdgeOrientation.OUT, 0.1,1.0);
        res = rec.getRecommendation(3L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(2L))
            {
                assertEquals(2.0/2.1*(Math.log(3)), out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
    }
    
    /**
     * ExtremeBM25 algorithm
     */
    @Test
    public void ebm25()
    {
        Recommender<Long, Long> rec = new EBM25<>(graph, EdgeOrientation.OUT, EdgeOrientation.IN, EdgeOrientation.IN, 0.1);
        Recommendation<Long, Long> res = rec.getRecommendation(2L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(3L))
            {
                assertEquals((Math.log(7) - Math.log(5))/1.1, out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
        
        rec = new EBM25<>(graph, EdgeOrientation.IN, EdgeOrientation.OUT, EdgeOrientation.OUT, 0.1);
        res = rec.getRecommendation(3L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(2L))
            {
                assertEquals((Math.log(3))/1.1, out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
    }
    
    /**
     * Query Likelihood - Jelinek Mercer smoothing algorithm
     */
    @Test
    public void qljm()
    {
        Recommender<Long, Long> rec = new QLJM<>(graph, EdgeOrientation.OUT, EdgeOrientation.IN, 0.5);
        Recommendation<Long, Long> res = rec.getRecommendation(2L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(3L))
            {
                assertEquals(Math.log(2.25), out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
        
        rec = new QLJM<>(graph, EdgeOrientation.IN, EdgeOrientation.OUT,0.5);
        res = rec.getRecommendation(3L);
        res.getItems().forEach(out -> {
            if(out.v1.equals(2L))
            {
                assertEquals(Math.log(3.5), out.v2, 0.0001); // 1.0 + in the denom. for preventing NaNs
            }
        });
    }

}
