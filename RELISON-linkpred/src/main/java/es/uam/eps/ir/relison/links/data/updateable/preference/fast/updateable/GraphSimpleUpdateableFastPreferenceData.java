/* 
 * Copyright (C) 2017 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.links.data.updateable.preference.fast.updateable;


import es.uam.eps.ir.ranksys.core.preference.IdPref;
import es.uam.eps.ir.ranksys.fast.preference.IdxPref;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;
import es.uam.eps.ir.relison.graph.edges.EdgeType;
import es.uam.eps.ir.relison.graph.fast.FastGraph;
import es.uam.eps.ir.relison.graph.generator.EmptyGraphGenerator;
import es.uam.eps.ir.relison.graph.generator.GraphGenerator;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorBadConfiguredException;
import es.uam.eps.ir.relison.graph.generator.exception.GeneratorNotConfiguredException;
import es.uam.eps.ir.relison.links.data.GraphIndex;
import es.uam.eps.ir.relison.links.data.updateable.index.fast.FastUpdateableGraphIndex;
import org.jooq.lambda.function.Function4;
import org.jooq.lambda.tuple.Tuple3;
import org.jooq.lambda.tuple.Tuple4;

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Implementation of a fast updateable version of preference data for social network
 * analysis, based on social network graphs.
 * 
 * @param <U> type of the users
 * 
 * @author Javier Sanz-Cruzado Puig (javier.sanz-cruzado@uam.es)
 * @author Saúl Vargas (saul.vargas@uam.es)
 */
public class GraphSimpleUpdateableFastPreferenceData<U> extends StreamsAbstractFastUpdateablePreferenceData<U, U> implements FastUpdateablePointWisePreferenceData<U, U>, Serializable
{
    /**
     * The number of preferences.
     */
    private int numPreferences;
    
    /**
     * The graph.
     */
    private FastGraph<U> graph;

    /**
     * Constructor with predetermined IdxPref to IdPref converter.
     * @param fastGraph the fast graph.
     */
    protected GraphSimpleUpdateableFastPreferenceData(FastGraph<U> fastGraph)
    {
        this(fastGraph, new FastUpdateableGraphIndex<>(fastGraph),
             (Function<IdxPref, IdPref<U>> & Serializable) p -> new IdPref<>(fastGraph.getIndex().idx2object(p.v1), p.v2),
             (Function<IdxPref, IdPref<U>> & Serializable) p -> new IdPref<>(fastGraph.getIndex().idx2object(p.v1),p.v2));
    }

    /**
     * Constructor with custom IdxPref to IdPref converter.
     *
     * @param fastGraph the fast graph.
     * @param index the index.

     * @param uPrefFun user IdxPref to IdPref converter
     * @param iPrefFun item IdxPref to IdPref converter
     */
    protected GraphSimpleUpdateableFastPreferenceData(FastGraph<U> fastGraph, FastUpdateableGraphIndex<U> index,
                                                      Function<IdxPref, IdPref<U>> uPrefFun, Function<IdxPref, IdPref<U>> iPrefFun)
    {
        super(index, index, uPrefFun, iPrefFun);
        this.numPreferences = Long.valueOf(fastGraph.getEdgeCount()).intValue();
        this.graph = fastGraph;
    }

    @Override
    public int numUsers(int iidx) 
    {
        return this.graph.getNeighborhood(iidx, EdgeOrientation.IN).reduce(0, (a, b) -> a + 1);
    }

    @Override
    public int numItems(int uidx) 
    {
        return this.graph.getNeighborhood(uidx, EdgeOrientation.OUT).reduce(0, (a,b) -> a + 1);
    }

    @Override
    public Stream<IdxPref> getUidxPreferences(int uidx) 
    {
        Stream<IdxPref> stream = this.graph.getNeighborhoodWeights(uidx, EdgeOrientation.OUT);
        return (stream != null) ? stream : Stream.empty();
    }

    @Override
    public Stream<IdxPref> getIidxPreferences(int iidx) 
    {
        Stream<IdxPref> stream = this.graph.getNeighborhoodWeights(iidx, EdgeOrientation.IN);
        return (stream != null) ? stream : Stream.empty();
    }

    @Override
    public int numPreferences() 
    {
        return numPreferences;
    }

    @Override
    public IntStream getUidxWithPreferences() 
    {
        return this.getAllUidx().filter(uidx -> this.graph.getNeighborhood(uidx, EdgeOrientation.OUT) != null);
    }

    @Override
    public IntStream getIidxWithPreferences() 
    {
        return this.getAllIidx().filter(iidx -> this.graph.getNeighborhood(iidx, EdgeOrientation.IN) != null);

    }

    @Override
    public int numUsersWithPreferences() 
    {
        return this.getUidxWithPreferences().reduce(0, (a,b) -> a + 1);
    }

    @Override
    public int numItemsWithPreferences() 
    {
        return this.getIidxWithPreferences().reduce(0, (a,b) -> a + 1);
    }

    @Override
    public Optional<IdxPref> getPreference(int uidx, int iidx) 
    {
        double edgew = this.graph.getEdgeWeight(uidx, iidx);
        if(!this.graph.containsEdge(uidx, iidx)) return Optional.empty();
        return Optional.of(new IdxPref(iidx,edgew));
    }

    @Override
    public Optional<? extends IdPref<U>> getPreference(U u, U i) 
    {
        Optional<? extends IdxPref> pref = getPreference(user2uidx(u), item2iidx(i));
        return pref.map(uPrefFun);
    }

    @Override
    protected void updateRating(int uidx, int iidx, double rating)
    {
        if(this.graph.containsEdge(uidx,iidx))
        {
            this.graph.updateEdgeWeight(uidx, iidx, rating);
        }
        
        if(this.graph.addEdge(uidx, iidx, rating, EdgeType.getDefaultValue()))
        {
            this.numPreferences++;
        }
    }

    @Override
    protected void updateDelete(int uidx, int iidx)
    {
        if(this.graph.removeEdge(this.uidx2user(uidx), this.uidx2user(iidx)))
        {
            this.numPreferences--;
        }
        
    }

    @Override
    public int addItem(U u) 
    {
        this.graph.addNode(u);
        return this.user2uidx(u);
    }

    @Override
    public int addUser(U u) 
    {
        this.graph.addNode(u);
        return this.user2uidx(u);
    }
        
    /**
     * Loads the preferences from a file.
     * @param <U> Type of the users.
     * @param graph the graph.
     * @return the corresponding preference data.
     */
    public static <U> GraphSimpleUpdateableFastPreferenceData<U> load(FastGraph<U> graph)
    {
        return new GraphSimpleUpdateableFastPreferenceData<>(graph);
    }
    
    /**
     * Loads a SimpleFastPreferenceData from a stream of user-item-value triples.
     *
     * @param <U> user type
     * @param tuples stream of user-item-value triples
     * @param uIndex user index
     * @param directed indicates if the graph is directed or undirected
     * @param weighted indicates if the graph is weighted or unweighted
     * @return an instance of SimpleFastPreferenceData containing the data from the input stream
     */
    public static <U> GraphSimpleUpdateableFastPreferenceData<U> load(Stream<Tuple3<U, U, Double>> tuples, GraphIndex<U> uIndex, boolean directed, boolean weighted)
    {
        return load(tuples.map(t -> t.concat((Void) null)),
                (uidx, iidx, v, o) -> new IdxPref(iidx, v),
                uIndex,
                (Function<IdxPref, IdPref<U>> & Serializable) p -> new IdPref<>(uIndex.uidx2user(p)),
                directed, weighted);
    }

    /**
     * Loads a SimpleFastPreferenceData from a stream of user-item-value-other tuples. It can accomodate other information, thus you need to provide sub-classes of IdxPref IdPref accomodating for this new information.
     *
     * @param <U> user type
     * @param <O> additional information type
     * @param tuples stream of user-item-value-other tuples
     * @param uIdxPrefFun converts a tuple to a user IdxPref
     * @param uIndex user index
     * @param uIdPrefFun user IdxPref to IdPref converter
     * @param directed indicates if the graph is directed or undirected
     * @param weighted indicates if the graph is weighted or unweighted
     * @return an instance of SimpleFastPreferenceData containing the data from the input stream
     */
    public static <U, O> GraphSimpleUpdateableFastPreferenceData<U> load(Stream<Tuple4<U, U, Double, O>> tuples,
            Function4<Integer, Integer, Double, O, ? extends IdxPref> uIdxPrefFun,
            GraphIndex<U> uIndex,
            Function<IdxPref, IdPref<U>> uIdPrefFun,
            boolean directed, boolean weighted) 
    {
        try 
        {
            GraphGenerator<U> ggen = new EmptyGraphGenerator<>();
            ggen.configure(directed, weighted);
            FastGraph<U> graph = (FastGraph<U>) ggen.generate();
            
            uIndex.getAllUidx().forEach(i -> graph.addNode(uIndex.uidx2user(i)));
            
            tuples.forEach(t ->
            {
                int uidx = uIndex.user2uidx(t.v1);
                int vidx = uIndex.user2uidx(t.v2);
                
                IdxPref idpref = uIdxPrefFun.apply(uidx, vidx, t.v3, t.v4);
                graph.addEdge(t.v1, t.v2, idpref.v2);
            });
            
            return load(graph);
        }
        catch (GeneratorNotConfiguredException | GeneratorBadConfiguredException ex)
        {
            return null;
        }
    }
}