/*
 *  Copyright (C) 2016 Information Retrieval Group at Universidad Aut�noma
 *  de Madrid, http://ir.ii.uam.es
 * 
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/**
 * Generalizations of local rerankers, which allow processing several users in a row.
 *
 * These rerankers, given a set of recommendations, sequentially process them one by one. Those
 * recommendations which are processed later are aware of the previously processed recommendations.
 */
package es.uam.eps.ir.socialranksys.links.recommendation.reranking.global.local;
