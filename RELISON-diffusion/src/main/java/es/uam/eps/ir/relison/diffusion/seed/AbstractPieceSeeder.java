/*
 *  Copyright (C) 2024 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.diffusion.seed;

import es.uam.eps.ir.relison.diffusion.data.Data;
import es.uam.eps.ir.relison.diffusion.data.Information;
import es.uam.eps.ir.relison.diffusion.seed.selector.AllUsersSelector;
import es.uam.eps.ir.relison.diffusion.seed.selector.UserSelector;
import es.uam.eps.ir.relison.diffusion.seed.timestamp.FixedTimestampGenerator;
import es.uam.eps.ir.relison.diffusion.seed.timestamp.TimestampGenerator;
import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.index.Index;
import es.uam.eps.ir.relison.index.Relation;
import es.uam.eps.ir.relison.index.fast.FastIndex;
import es.uam.eps.ir.relison.index.fast.FastWeightedPairwiseRelation;
import es.uam.eps.ir.relison.utils.generator.Generator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Base class for the {@link PieceSeeder piece seeders}: it performs the common assembly of the diffusion dataset and
 * delegates the only varying decision — <em>how many</em> pieces each user creates — to {@link #numPieces(U)}.
 *
 * <p>Two further, independent decisions are delegated to injected strategies: <em>which</em> users get seeded (a
 * {@link es.uam.eps.ir.relison.diffusion.seed.selector.UserSelector}, all users by default) and the creation
 * <em>timestamp</em> of each piece (a {@link TimestampGenerator}, a single fixed timestamp of 0 by default). A fixed
 * timestamp makes every piece available from the first iteration; a spread of timestamps staggers when pieces enter
 * the diffusion.</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user / information piece features.
 */
public abstract class AbstractPieceSeeder<U extends Serializable, I extends Serializable, F> implements PieceSeeder<U, I, F>
{
    /**
     * Generates the (unique) identifiers of the created information pieces.
     */
    protected final Generator<I> pieceGenerator;
    /**
     * Assigns the creation timestamp of each seeded piece (a single fixed timestamp by default).
     */
    protected final TimestampGenerator timestamps;
    /**
     * Selects the subset of users that get seeded with pieces (all users by default).
     */
    protected final UserSelector<U> selector;

    /**
     * Constructor. Every seeded piece is created at timestamp 0, and all users are seeded.
     * @param pieceGenerator generator for the information piece identifiers.
     */
    protected AbstractPieceSeeder(Generator<I> pieceGenerator)
    {
        this(pieceGenerator, new FixedTimestampGenerator(0L), new AllUsersSelector<>());
    }

    /**
     * Constructor with a fixed timestamp for every piece. All users are seeded.
     * @param pieceGenerator generator for the information piece identifiers.
     * @param timestamp      creation timestamp assigned to every seeded piece.
     */
    protected AbstractPieceSeeder(Generator<I> pieceGenerator, long timestamp)
    {
        this(pieceGenerator, new FixedTimestampGenerator(timestamp), new AllUsersSelector<>());
    }

    /**
     * Constructor with a fixed timestamp for every piece.
     * @param pieceGenerator generator for the information piece identifiers.
     * @param timestamp      creation timestamp assigned to every seeded piece.
     * @param selector       selects the subset of users that get seeded with pieces.
     */
    protected AbstractPieceSeeder(Generator<I> pieceGenerator, long timestamp, UserSelector<U> selector)
    {
        this(pieceGenerator, new FixedTimestampGenerator(timestamp), selector);
    }

    /**
     * Constructor with a timestamp generator. All users are seeded.
     * @param pieceGenerator generator for the information piece identifiers.
     * @param timestamps     assigns the creation timestamp of each piece.
     */
    protected AbstractPieceSeeder(Generator<I> pieceGenerator, TimestampGenerator timestamps)
    {
        this(pieceGenerator, timestamps, new AllUsersSelector<>());
    }

    /**
     * Full constructor.
     * @param pieceGenerator generator for the information piece identifiers.
     * @param timestamps     assigns the creation timestamp of each piece.
     * @param selector       selects the subset of users that get seeded with pieces.
     */
    protected AbstractPieceSeeder(Generator<I> pieceGenerator, TimestampGenerator timestamps, UserSelector<U> selector)
    {
        this.pieceGenerator = pieceGenerator;
        this.timestamps = timestamps;
        this.selector = selector;
    }

    /**
     * The number of information pieces the given user creates. Called once per user, in user index order; negative
     * values are treated as zero.
     * @param user the user.
     * @return the number of pieces the user owns.
     */
    protected abstract int numPieces(U user);

    @Override
    public Data<U, I, F> seed(Graph<U> graph)
    {
        // Start the piece identifiers from a clean state so each produced dataset is self-contained.
        this.pieceGenerator.reset();

        // Every node of the network is a user (so all users take part in the diffusion); only the selected subset,
        // however, is seeded with pieces.
        Index<U> users = new FastIndex<>();
        graph.getAllNodes().forEach(users::addObject);
        Set<U> seeded = this.selector.select(graph);

        Index<I> pieces = new FastIndex<>();
        Map<Integer, Information<I>> infoMap = new HashMap<>();
        List<int[]> ownership = new ArrayList<>();   // (userIdx, pieceIdx) pairs

        for (int uidx = 0; uidx < users.numObjects(); ++uidx)
        {
            U user = users.idx2object(uidx);
            if (!seeded.contains(user)) continue;
            int k = Math.max(0, this.numPieces(user));
            for (int j = 0; j < k; ++j)
            {
                I pieceId = this.pieceGenerator.generate();
                int iidx = pieces.addObject(pieceId);
                infoMap.put(iidx, new Information<>(pieceId, this.timestamps.generate()));
                ownership.add(new int[]{uidx, iidx});
            }
        }

        Relation<Integer> userInfo = new FastWeightedPairwiseRelation<>();
        IntStream.range(0, users.numObjects()).forEach(userInfo::addFirstItem);
        IntStream.range(0, pieces.numObjects()).forEach(userInfo::addSecondItem);
        for (int[] o : ownership) userInfo.addRelation(o[0], o[1], 1);

        return new Data<>(graph, users, pieces, infoMap, userInfo);
    }
}
