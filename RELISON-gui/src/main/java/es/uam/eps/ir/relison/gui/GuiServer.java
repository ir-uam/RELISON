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
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;

import java.awt.Desktop;
import java.io.PrintStream;
import java.net.URI;

/**
 * Entry point of the RELISON GUI. Starts an embedded HTTP server that serves the sigma.js frontend and exposes
 * a small REST API wrapping RELISON's graph IO and metric computation, then opens the default browser.
 *
 * <p>Run with {@code java -jar RELISON-gui.jar [port] [--debug]} (the port defaults to 7070). With {@code --debug}
 * the standard output / error is captured and can be inspected from a console in the frontend.</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public class GuiServer
{
    /** Default port the server listens on. */
    public static final int DEFAULT_PORT = 7070;

    /**
     * Launches the server.
     * @param args optionally, the port to listen on, and/or {@code --debug} to enable the output-capturing console.
     */
    public static void main(String[] args)
    {
        int port = DEFAULT_PORT;
        boolean debug = false;
        for (String arg : args)
        {
            if ("--debug".equals(arg))
            {
                debug = true;
            }
            else
            {
                try
                {
                    port = Integer.parseInt(arg);
                }
                catch (NumberFormatException e)
                {
                    System.err.println("Invalid argument '" + arg + "', ignoring.");
                }
            }
        }

        // In debug mode, capture stdout/stderr into a ring buffer the frontend can display. Done first so every
        // subsequent message (including the "running at" line below) is captured.
        final boolean debugEnabled = debug;
        final LogCapture logCapture = debug ? new LogCapture(5000) : null;
        final PrintStream realOut = System.out;   // the real console stdout, before it is wrapped by the capture below
        if (logCapture != null) LogCapture.install(logCapture);

        GraphStore store = new GraphStore();
        // Shared manager for long-running, cancellable computations (metrics, community detection, recommendation,
        // diffusion). Handlers run their heavy work through it so the Stop buttons can interrupt them.
        JobManager jobs = new JobManager();
        GraphController graphController = new GraphController(store);
        MetricController metricController = new MetricController(store, jobs);
        CommunityController communityController = new CommunityController(store, jobs);
        EditController editController = new EditController(store);
        PathController pathController = new PathController(store);
        AttributeController attributeController = new AttributeController(store);
        RecommendationController recommendationController = new RecommendationController(store, jobs);
        DiffusionController diffusionController = new DiffusionController(store, jobs);
        SessionController sessionController = new SessionController(store);

        Javalin app = Javalin.create(config ->
        {
            config.staticFiles.add(staticFiles ->
            {
                staticFiles.directory = "/public";
                staticFiles.location = Location.CLASSPATH;
            });
            // Allow larger uploads for sizeable edge lists (default is small).
            config.http.maxRequestSize = 256L * 1024L * 1024L;
            //config.showJavalinBanner = false;
            // Trace every request to the console (method, path, resulting status code and time). The debug console's
            // own polling endpoints are skipped so it does not spam itself. These request traces are tagged as the
            // "http" stream (shown green) so they stand out from ordinary application stdout (shown white).
            config.requestLogger.http((ctx, ms) ->
            {
                String path = ctx.path();
                if ("/api/logs".equals(path) || "/api/config".equals(path)) return;
                String qs = ctx.queryString();
                String url = (qs == null || qs.isEmpty()) ? path : path + "?" + qs;
                String reqArgs = requestArgs(ctx);
                String line = ctx.method() + " " + url + " -> " + ctx.status().getCode()
                        + " (" + String.format("%.1f", ms) + " ms)" + (reqArgs.isEmpty() ? "" : " " + reqArgs);
                realOut.println(line);                                       // to the real console
                if (logCapture != null) logCapture.addLine("http", line);   // tagged so the web console shows it green
            });

            config.routes.get("/api/config", ctx -> ctx.json(java.util.Map.of("debug", debugEnabled)));
            config.routes.get("/api/logs", ctx ->
            {
                if (logCapture == null)
                {
                    ctx.json(java.util.Map.of("lines", java.util.List.of(), "cursor", 0, "enabled", false));
                    return;
                }
                long cursor = 0;
                try { cursor = Long.parseLong(ctx.queryParam("since")); }
                catch (NumberFormatException | NullPointerException ignored) { /* default 0 */ }
                java.util.Map<String, Object> out = logCapture.since(cursor);
                out.put("enabled", true);
                ctx.json(out);
            });

            // Whole-session save / restore (.relison files).
            config.routes.post("/api/session/save", sessionController::save);
            config.routes.post("/api/session/load", sessionController::load);

            // Graph IO.
            config.routes.post("/api/graph/load", graphController::load);
            config.routes.post("/api/graph/generate", graphController::generate);
            config.routes.get("/api/graph/{id}", graphController::get);

            // Metrics.
            config.routes.get("/api/metrics", metricController::catalog);
            config.routes.post("/api/metrics/vertex", metricController::vertex);
            config.routes.post("/api/metrics/graph", metricController::graph);
            config.routes.post("/api/metrics/pair", metricController::pair);

            // Communities.
            config.routes.post("/api/communities", communityController::detect);
            config.routes.post("/api/communities/global", communityController::globalMetric);
            config.routes.post("/api/communities/individual", communityController::individual);

            // Recommendation / link prediction.
            config.routes.get("/api/recommendation/catalog", recommendationController::catalog);
            config.routes.post("/api/recommendation/run", recommendationController::run);
            config.routes.post("/api/recommendation/clear", recommendationController::clear);
            // Evaluation against an uploaded held-out test set.
            config.routes.get("/api/recommendation/eval-catalog", recommendationController::evalCatalog);
            config.routes.post("/api/graph/{id}/recommendation/test", recommendationController::uploadTest);
            config.routes.post("/api/recommendation/evaluate", recommendationController::evaluate);

            // Information diffusion.
            config.routes.get("/api/diffusion/catalog", diffusionController::catalog);
            config.routes.post("/api/diffusion/run", diffusionController::run);
            config.routes.post("/api/diffusion/state", diffusionController::state);
            config.routes.post("/api/diffusion/trajectory", diffusionController::trajectory);
            config.routes.post("/api/diffusion/piece-trajectory", diffusionController::pieceTrajectory);
            config.routes.post("/api/diffusion/feature-trajectory", diffusionController::featureTrajectory);
            config.routes.post("/api/diffusion/distribution", diffusionController::distribution);
            config.routes.post("/api/diffusion/clear", diffusionController::clear);
            config.routes.post("/api/diffusion/pieces", diffusionController::savePieces);
            config.routes.post("/api/diffusion/pieces/get", diffusionController::getPieces);
            config.routes.post("/api/diffusion/seed", diffusionController::seed);
            config.routes.post("/api/diffusion/real-propagated", diffusionController::saveRealPropagated);
            config.routes.post("/api/diffusion/real-propagated/get", diffusionController::getRealPropagated);

            // Cancel a running computation (metrics / communities / recommendation / diffusion). Served on a separate
            // request thread from the one blocked in the computation, so it can interrupt it.
            config.routes.post("/api/jobs/cancel", ctx ->
            {
                java.util.Map<?, ?> body = ctx.bodyAsClass(java.util.Map.class);
                Object graphId = body.get("graphId");
                Object kind = body.get("kind");
                boolean cancelled = graphId != null && kind != null && jobs.cancel(graphId + ":" + kind);
                ctx.json(java.util.Map.of("cancelled", cancelled));
            });

            // Shortest paths.
            config.routes.post("/api/paths", pathController::shortestPaths);

            // Graph editing.
            config.routes.post("/api/graph/{id}/node", editController::addNode);
            config.routes.post("/api/graph/{id}/node/rename", editController::renameNode);
            config.routes.delete("/api/graph/{id}/node/{node}", editController::removeNode);
            config.routes.post("/api/graph/{id}/edge", editController::addEdge);
            config.routes.delete("/api/graph/{id}/edge", editController::removeEdge);
            // Bulk variants (one request for many edges), backing "add all recommended links" and its undo.
            config.routes.post("/api/graph/{id}/edges", editController::addEdges);
            config.routes.delete("/api/graph/{id}/edges", editController::removeEdges);

            // Node / edge attributes.
            config.routes.post("/api/graph/{id}/attributes/nodes", attributeController::uploadNodes);
            config.routes.post("/api/graph/{id}/attributes/edges", attributeController::uploadEdges);
            config.routes.post("/api/graph/{id}/attributes/define", attributeController::define);
            config.routes.post("/api/graph/{id}/attributes/remove", attributeController::remove);
            config.routes.post("/api/graph/{id}/attributes/node", attributeController::setNode);
            config.routes.post("/api/graph/{id}/attributes/edge", attributeController::setEdge);
            config.routes.exception(Exception.class, (e, ctx) ->
            {
                e.printStackTrace();
                ctx.status(500).json(java.util.Map.of("error", e.getMessage() == null ? e.toString() : e.getMessage()));
            });
        });

        // Runtime configuration + debug console (only meaningful when launched with --debug).




        app.start(port);

        String url = "http://localhost:" + port;
        System.out.println("RELISON GUI running at " + url);
        openBrowser(url);
    }

    /**
     * A one-line, truncated representation of a request's body for the trace log. Multipart uploads (potentially large
     * binary files) are summarised instead of dumped, and long bodies are cut off. Query-string arguments are logged
     * separately (appended to the URL), so this only covers the request body.
     * @param ctx the request context.
     * @return {@code "body=…"}, {@code "[multipart upload]"}, or an empty string when there is no body.
     */
    private static String requestArgs(Context ctx)
    {
        String contentType = ctx.contentType();
        if (contentType != null && contentType.toLowerCase().contains("multipart/form-data"))
        {
            return "[multipart upload]";
        }
        String body;
        try { body = ctx.body(); }
        catch (Exception e) { return ""; }
        if (body == null || body.isEmpty()) return "";
        body = body.replaceAll("\\s+", " ").trim();
        if (body.isEmpty()) return "";
        int max = 600;
        if (body.length() > max) body = body.substring(0, max) + "… (" + body.length() + " chars)";
        return "body=" + body;
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
