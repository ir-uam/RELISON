/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.gui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of the networks loaded in the GUI, keyed by session identifier.
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class GraphStore
{
    /** The currently loaded sessions, indexed by their identifier. */
    private final Map<String, GraphSession> sessions = new ConcurrentHashMap<>();

    /**
     * Registers a session under a freshly generated identifier.
     * @param session the session to store; its id is ignored in favour of the generated one.
     * @return the generated identifier.
     */
    public String register(GraphSession session)
    {
        sessions.put(session.getId(), session);
        return session.getId();
    }

    /**
     * Generates a new, unique session identifier.
     * @return the identifier.
     */
    public String newId()
    {
        return UUID.randomUUID().toString();
    }

    /**
     * Retrieves a session.
     * @param id the session identifier.
     * @return the session, or {@code null} if no session with that id exists.
     */
    public GraphSession get(String id)
    {
        return sessions.get(id);
    }

    /**
     * Removes a session.
     * @param id the session identifier.
     */
    public void remove(String id)
    {
        sessions.remove(id);
    }
}
