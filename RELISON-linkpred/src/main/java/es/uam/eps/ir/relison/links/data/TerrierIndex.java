/*
 * Copyright (C) 2020 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es and Terrier Team at University of Glasgow,
 * http://terrierteam.dcs.gla.ac.uk/.
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.data;

import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.utils.datatypes.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.terrier.indexing.Document;
import org.terrier.indexing.FileDocument;
import org.terrier.indexing.tokenisation.Tokeniser;
import org.terrier.querying.Manager;
import org.terrier.querying.ManagerFactory;
import org.terrier.querying.ScoredDocList;
import org.terrier.querying.SearchRequest;
import org.terrier.realtime.memory.MemoryIndex;
import org.terrier.structures.CollectionStatistics;
import org.terrier.utility.ApplicationSetup;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Class for generating Terrier indexes and queries from graphs.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 * @author Craig Macdonald (craig.macdonald@glasgow.ac.uk)
 * @author Iadh Ounis (iadh.ounis@glasgow.ac.uk)
 * @author Pablo Castells (pablo.castells@uam.es)
 */
public class TerrierIndex
{
    /**
     * The Terrier index.
     */
    private final MemoryIndex index;
    /**
     * A structure for storing the different queries.
     */
    private final Map<Integer, String> queries;
    /**
     * Orientation for the "query" users.
     */
    private final EdgeOrientation qSel;
    /**
     * Orientation for the "document" users.
     */
    private final EdgeOrientation dSel;
    /**
     * The graph.
     */
    private final FastGraph<?> graph;
    /**
     * Identifier for the id of the nodes in the index.
     */
    public final static String NODEID = "nodeId";

    public TerrierIndex(FastGraph<?> graph, EdgeOrientation qSel, EdgeOrientation dSel, MemoryIndex memIndex, Map<Integer, String> queries)
    {
        this.graph = graph;
        this.qSel = graph.isDirected() ? qSel : EdgeOrientation.UND;
        this.dSel = graph.isDirected() ? dSel : EdgeOrientation.UND;
        this.index = memIndex;
        this.queries = queries;

        ApplicationSetup.setProperty("querying.processes", "terrierql:TerrierQLParser,"
                + "parsecontrols:TerrierQLToControls,"
                + "parseql:TerrierQLToMatchingQueryTerms,"
                + "matchopql:MatchingOpQLParser,"
                + "applypipeline:ApplyTermPipeline,"
                + "localmatching:LocalManager$ApplyLocalMatching,"
                + "filters:LocalManager$PostFilterProcess");

        // Enable the decorate enhancement
        // These parameters just allow us to obtain the metadata from files.
        ApplicationSetup.setProperty("querying.postfilters.controls", "decorate:org.terrier.querying.SimpleDecorate");
        ApplicationSetup.setProperty("querying.postfilters.order", "org.terrier.querying.SimpleDecorate");
    }

    /**
     * Constructor.
     *
     * @param graph the graph.
     * @param qSel  orientation for the "query" users.
     * @param dSel  orientation for the "document" users.
     */
    public TerrierIndex(FastGraph<?> graph, EdgeOrientation qSel, EdgeOrientation dSel)
    {
        // Initialize the variables.
        this.graph = graph;
        this.qSel = graph.isDirected() ? qSel : EdgeOrientation.UND;
        this.dSel = graph.isDirected() ? dSel : EdgeOrientation.UND;

        // First, setup Terrier.
        ApplicationSetup.setProperty("termpipelines", "NoOp");
        ApplicationSetup.setProperty("indexer.meta.forward.keys", NODEID);
        ApplicationSetup.setProperty("indexer.meta.forward.keylens", "30");
        this.index = new MemoryIndex();
        this.queries = new Int2ObjectOpenHashMap<>();

        // Generate the index and the queries.
        if (graph.isWeighted())
        {
            this.generateWeightedIndex();
        }
        else
        {
            this.generateUnweightedIndex();
        }

        ApplicationSetup.setProperty("querying.processes", "terrierql:TerrierQLParser,"
                + "parsecontrols:TerrierQLToControls,"
                + "parseql:TerrierQLToMatchingQueryTerms,"
                + "matchopql:MatchingOpQLParser,"
                + "applypipeline:ApplyTermPipeline,"
                + "localmatching:LocalManager$ApplyLocalMatching,"
                + "filters:LocalManager$PostFilterProcess");

        // Enable the decorate enhancement
        // These parameters just allow us to obtain the metadata from files.
        ApplicationSetup.setProperty("querying.postfilters.controls", "decorate:org.terrier.querying.SimpleDecorate");
        ApplicationSetup.setProperty("querying.postfilters.order", "org.terrier.querying.SimpleDecorate");
    }

    /**
     * Generates the index and queries for a weighted graph.
     */
    private void generateUnweightedIndex()
    {
        if (!this.graph.isDirected() || qSel == dSel) // If the orientation for the "query" and "document" neighborhoods is the same.
        {
            graph.getAllNodesIds().forEach(uidx ->
            {
                try
                {
                    // First, we generate the strings for the document and the query.
                    String doc = graph.getNeighborhood(uidx, dSel).map(vidx -> vidx + " ").reduce("", (x, y) -> x + y);

                    // Then, we generate the document.
                    Document document = new FileDocument(new StringReader(doc), new HashMap<>(), Tokeniser.getTokeniser());
                    document.getAllProperties().put(NODEID, uidx + "");
                    this.index.indexDocument(document);
                    // And add the corresponding query.
                    this.queries.put(uidx, doc);
                }
                catch (Exception ex)
                {
                    System.err.println("ERROR: Something failed while writing user " + uidx);
                }
            });
        }
        else // generate the query and the document strings separately
        {
            graph.getAllNodesIds().forEach(uidx ->
            {
                try
                {
                    // Generate the document string
                    String doc = graph.getNeighborhood(uidx, dSel).map(vidx -> vidx + " ").reduce("", (x, y) -> x + y);

                    // Generate the query string
                    String query = graph.getNeighborhood(uidx, qSel).map(vidx -> vidx + " ").reduce("", (x, y) -> x + y);

                    // Store both of them.
                    Document document = new FileDocument(new StringReader(doc), new HashMap<>(), Tokeniser.getTokeniser());
                    document.getAllProperties().put(NODEID, uidx + "");
                    this.index.indexDocument(document);
                    this.queries.put(uidx, query);
                }
                catch (Exception ex)
                {
                    System.err.println("ERROR: Something failed while writing user " + uidx);
                }
            });
        }
    }

    /**
     * Generates the index and queries for an unweighted graph.
     */
    private void generateWeightedIndex()
    {
        if (!this.graph.isDirected() || qSel == dSel) // If the orientation for the "query" and "document" neighborhoods is the same.
        {
            graph.getAllNodesIds().forEach(uidx ->
            {
                try
                {
                    // First, we generate the strings for the document and the query.
                    Pair<String> pair = graph.getNeighborhoodWeights(uidx, dSel).map(v ->
                    {
                        int vidx = v.v1();
                        double weight = v.v2();

                        String q = v.v1() + "^" + v.v2() + " ";
                        StringBuilder strBuilder = new StringBuilder();
                        boolean times = weight > 0.0;
                        for (int j = 0; j < weight || times; ++j)
                        {
                            strBuilder.append(vidx);
                            strBuilder.append(" ");
                            times = false;
                        }

                        String d = strBuilder.toString();

                        return new Pair<>(q, d);
                    }).reduce(new Pair<>("", ""), (x, y) -> new Pair<>(x.v1() + y.v1(), x.v2() + y.v2()));

                    String doc = pair.v1();
                    String query = pair.v2();

                    // Then, we generate the document.
                    Document document = new FileDocument(new StringReader(doc), new HashMap<>(), Tokeniser.getTokeniser());
                    document.getAllProperties().put(NODEID, uidx + "");
                    this.index.indexDocument(document);
                    // And add the corresponding query.
                    this.queries.put(uidx, query);
                }
                catch (Exception ex)
                {
                    System.err.println("ERROR: Something failed while writing user " + uidx);
                }
            });
        }
        else // generate the query and the document strings separately
        {
            graph.getAllNodesIds().forEach(uidx ->
            {
                try
                {
                    String doc = graph.getNeighborhoodWeights(uidx, dSel).map(v ->
                    {
                        int vidx = v.v1;
                        String d = "";
                        double weight = v.v2;
                        // We assume that the weight is an integer. If it is not, but
                        // the value is, at least, positive, we sum the weight once.
                        boolean times = weight > 0.0;
                        StringBuilder strBuilder = new StringBuilder();
                        for (int j = 0; j < weight || times; ++j)
                        {
                            strBuilder.append(" ");
                            strBuilder.append(vidx);
                            times = false;
                        }

                        return strBuilder.toString();
                    }).reduce("", (x, y) -> x + y);

                    String query = graph.getNeighborhoodWeights(uidx, qSel).map(v ->
                    {
                        int vidx = v.v1;
                        String q = " " + vidx;
                        if (graph.isWeighted())
                        {
                            q += "^" + v.v2;
                        }
                        return q;
                    }).reduce("", (x, y) -> x + y);

                    Document document = new FileDocument(new StringReader(doc), new HashMap<>(), Tokeniser.getTokeniser());
                    document.getAllProperties().put(NODEID, uidx + "");
                    this.index.indexDocument(document);
                    this.queries.put(uidx, query);

                }
                catch (Exception ex)
                {
                    System.err.println("ERROR: Something failed while writing user " + uidx);
                }
            });
        }
    }

    /**
     * Executes a query over the collection.
     *
     * @param uidx  identifier of the "query" user.
     * @param model identifier of the weighting model.
     * @param c     c value of the weighting model (ex. the b param. in BM25).
     *
     * @return a list of results if everything is OK, null otherwise.
     */
    public ScoredDocList query(int uidx, String model, Optional<Double> c)
    {
        Manager manager = ManagerFactory.from(index.getIndexRef());

        String query = this.queries.get(uidx);
        if (query == null || query.equals("")) return null;

        // First, we configure the search request.
        SearchRequest srq = manager.newSearchRequestFromQuery(query);
        // Specify the model we want to use and its c-value:
        srq.setControl(SearchRequest.CONTROL_WMODEL, model);
        if (c.isPresent())
        {
            srq.setControl("c_set", "true");
            srq.setControl("c", c.get().toString());
        }

        // Enable querying processes
        srq.setControl("terrierql", "on");
        srq.setControl("parsecontrols", "on");
        srq.setControl("parseql", "on");
        srq.setControl("applypipeline", "on");
        srq.setControl("localmatching", "on");
        srq.setControl("filters", "on");

        // Enable post filters -> we are just enabling the gathering of metadata.
        srq.setControl("decorate", "on");

        // Run the query
        manager.runSearchRequest(srq);

        return srq.getResults();
    }

    /**
     * Obtains the statistics of the collection.
     *
     * @return the statistics.
     */
    public CollectionStatistics stats()
    {
        return this.index.getCollectionStatistics();
    }

    /**
     * Obtains the orientation for the "query" users.
     *
     * @return the orientation for the query users.
     */
    public EdgeOrientation getQueryOrientation()
    {
        return qSel;
    }

    /**
     * Obtains the orientation for the "document" users.
     *
     * @return the orientation for the document users.
     */
    public EdgeOrientation getDocumentOrientation()
    {
        return dSel;
    }

    MemoryIndex getMemoryIndex()
    {
        return this.index;
    }

    Map<Integer, String> getQueries()
    {
        return this.queries;
    }
}
