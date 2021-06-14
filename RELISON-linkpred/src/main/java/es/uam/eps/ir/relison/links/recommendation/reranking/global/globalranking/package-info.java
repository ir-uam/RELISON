/*
 * Copyright (C) 2021 Information Retrieval Group at Universidad Autónoma
 * de Madrid, http://ir.ii.uam.es
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/**
 * Definition and implementation of reranking algorithms for enhancing global properties of
 * the system.
 *
 * Given a set of recommendations for different users, these rerankers iteratively choose the user-item
 * pair that maximizes a given objective function. A user can only be selected as long as the ranking
 * does not have enough items.
 */
package es.uam.eps.ir.relison.links.recommendation.reranking.global.globalranking;