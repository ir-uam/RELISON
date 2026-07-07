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
        AttributeController attributeController = new AttributeController(store);
        RecommendationController recommendationController = new RecommendationController(store);
        DiffusionController diffusionController = new DiffusionController(store);

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
        app.post("/api/communities/global", communityController::globalMetric);
        app.post("/api/communities/individual", communityController::individual);

        // Recommendation / link prediction.
        app.get("/api/recommendation/catalog", recommendationController::catalog);
        app.post("/api/recommendation/run", recommendationController::run);
        app.post("/api/recommendation/clear", recommendationController::clear);

        // Information diffusion.
        app.get("/api/diffusion/catalog", diffusionController::catalog);
        app.post("/api/diffusion/run", diffusionController::run);
        app.post("/api/diffusion/state", diffusionController::state);
        app.post("/api/diffusion/clear", diffusionController::clear);
        app.post("/api/diffusion/pieces", diffusionController::savePieces);
        app.post("/api/diffusion/pieces/get", diffusionController::getPieces);

        // Shortest paths.
        app.post("/api/paths", pathController::shortestPaths);

        // Graph editing.
        app.post("/api/graph/{id}/node", editController::addNode);
        app.post("/api/graph/{id}/node/rename", editController::renameNode);
        app.delete("/api/graph/{id}/node/{node}", editController::removeNode);
        app.post("/api/graph/{id}/edge", editController::addEdge);
        app.delete("/api/graph/{id}/edge", editController::removeEdge);

        // Node / edge attributes.
        app.post("/api/graph/{id}/attributes/nodes", attributeController::uploadNodes);
        app.post("/api/graph/{id}/attributes/edges", attributeController::uploadEdges);
        app.post("/api/graph/{id}/attributes/define", attributeController::define);
        app.post("/api/graph/{id}/attributes/remove", attributeController::remove);
        app.post("/api/graph/{id}/attributes/node", attributeController::setNode);
        app.post("/api/graph/{id}/attributes/edge", attributeController::setEdge);

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
