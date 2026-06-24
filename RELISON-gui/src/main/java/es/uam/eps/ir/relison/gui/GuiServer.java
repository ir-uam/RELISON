/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.gui;

import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

import java.awt.Desktop;
import java.net.URI;

/**
 * Entry point of the RELISON GUI. Starts an embedded HTTP server that serves the sigma.js frontend and exposes
 * a small REST API wrapping RELISON's graph IO and metric computation, then opens the default browser.
 *
 * <p>Run with {@code java -jar RELISON-gui.jar [port]} (the port defaults to 7070).</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class GuiServer
{
    /** Default port the server listens on. */
    public static final int DEFAULT_PORT = 7070;

    /**
     * Launches the server.
     * @param args optionally, the port to listen on as the first argument.
     */
    public static void main(String[] args)
    {
        int port = DEFAULT_PORT;
        if (args.length > 0)
        {
            try
            {
                port = Integer.parseInt(args[0]);
            }
            catch (NumberFormatException e)
            {
                System.err.println("Invalid port '" + args[0] + "', using default " + DEFAULT_PORT + ".");
            }
        }

        GraphStore store = new GraphStore();
        GraphController graphController = new GraphController(store);
        MetricController metricController = new MetricController(store);
        CommunityController communityController = new CommunityController(store);
        EditController editController = new EditController(store);
        PathController pathController = new PathController(store);

        Javalin app = Javalin.create(config ->
        {
            config.staticFiles.add(staticFiles ->
            {
                staticFiles.directory = "/public";
                staticFiles.location = Location.CLASSPATH;
            });
            // Allow larger uploads for sizeable edge lists (default is small).
            config.http.maxRequestSize = 256L * 1024L * 1024L;
            config.showJavalinBanner = false;
        });

        // Graph IO.
        app.post("/api/graph/load", graphController::load);
        app.get("/api/graph/{id}", graphController::get);

        // Metrics.
        app.get("/api/metrics", metricController::catalog);
        app.post("/api/metrics/vertex", metricController::vertex);
        app.post("/api/metrics/graph", metricController::graph);
        app.post("/api/metrics/pair", metricController::pair);

        // Communities.
        app.post("/api/communities", communityController::detect);
        app.post("/api/communities/metrics", communityController::metrics);
        app.post("/api/communities/individual", communityController::individual);

        // Shortest paths.
        app.post("/api/paths", pathController::shortestPaths);

        // Graph editing.
        app.post("/api/graph/{id}/node", editController::addNode);
        app.delete("/api/graph/{id}/node/{node}", editController::removeNode);
        app.post("/api/graph/{id}/edge", editController::addEdge);
        app.delete("/api/graph/{id}/edge", editController::removeEdge);

        app.exception(Exception.class, (e, ctx) ->
        {
            e.printStackTrace();
            ctx.status(500).json(java.util.Map.of("error", e.getMessage() == null ? e.toString() : e.getMessage()));
        });

        app.start(port);

        String url = "http://localhost:" + port;
        System.out.println("RELISON GUI running at " + url);
        openBrowser(url);
    }

    /**
     * Attempts to open the system browser at the given URL; failures are non-fatal (the user can open it manually).
     * @param url the URL to open.
     */
    private static void openBrowser(String url)
    {
        try
        {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
            {
                Desktop.getDesktop().browse(new URI(url));
            }
        }
        catch (Exception e)
        {
            System.out.println("Open " + url + " in your browser.");
        }
    }
}
