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
import es.uam.eps.ir.relison.graph.Graph;

import java.io.Serializable;

/**
 * Seeds a diffusion dataset with synthetic content: given a social network, it creates a number of information pieces
 * "owned" (created) by each user, so a simulation can run over any network without external information-piece files.
 *
 * <p>Implementations differ in <em>how many</em> pieces each user gets (a fixed count, a random count, …); the id of
 * each piece is produced by a {@link es.uam.eps.ir.relison.utils.generator.Generator} and the pieces carry no
 * features (feature files can be added afterwards if needed).</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 *
 * @param <U> type of the users.
 * @param <I> type of the information pieces.
 * @param <F> type of the user / information piece features.
 */
public interface PieceSeeder<U extends Serializable, I extends Serializable, F>
{
    /**
     * Builds a diffusion dataset by seeding the users of a network with synthetic information pieces.
     * @param graph the social network whose nodes are the users to seed.
     * @return the diffusion data (users, generated pieces and the ownership relation between them), with no features.
     */
    Data<U, I, F> seed(Graph<U> graph);
}
