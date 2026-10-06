/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.utils.datatypes.Pair;
import es.uam.eps.ir.relison.graph.edges.EdgeOrientation;

import es.uam.eps.ir.relison.graph.Graph;
import es.uam.eps.ir.relison.viz.*;
import es.uam.eps.ir.relison.viz.layouts.*;
import es.uam.eps.ir.relison.viz.transforms.ComponentPacking;
import io.javalin.http.Context;
import java.util.*;
import java.util.stream.Collectors;

/** Computes layouts without changing server-side graph topology or attributes. */
public final class LayoutController
{
    private final GraphStore store;

    /** @param store loaded graph registry */
    public LayoutController(GraphStore store) { this.store = Objects.requireNonNull(store); }

    /**
     * Handles POST /api/layout. Coordinates remain client-owned, like existing GUI layouts.
     * @param ctx body with graphId, algorithm, optional params, nodeOrder, positions,
     *            pinnedNodes, shells, partitions, and scores
     */
    public void apply(Context ctx)
    {
        try
        {
            Map<?, ?> body = ctx.bodyAsClass(Map.class);
            if (body == null) throw new IllegalArgumentException("A layout request is required.");
            GraphSession session = store.get(String.valueOf(body.get("graphId")));
            if (session == null)
            {
                ctx.status(404).json(Map.of("error", "Unknown graph id."));
                return;
            }
            ctx.json(compute(session.getGraph(), body));
        }
        catch (IllegalArgumentException error)
        {
            ctx.status(400).json(Map.of("error", error.getMessage() == null ? "Invalid layout request." : error.getMessage()));
        }
    }

    // Kept independent of HTTP for focused request/response contract tests.
    static Map<String, Object> compute(Graph<String> graph, Map<?, ?> body)
    {
        Map<?, ?> params = body.containsKey("params") ? object(body.get("params"), "params") : Collections.emptyMap();
        List<String> nodes = graph.getAllNodes().sorted().collect(Collectors.toList());
        List<String> order = body.containsKey("nodeOrder") ? strings(body.get("nodeOrder"), "nodeOrder") : nodes;
        LayoutRequest.Builder<String> request = LayoutRequest.<String>builder().nodeOrder(order)
            .seed(integer(params, "seed", 0));
        if (body.containsKey("positions")) request.initialPositions(positions(body.get("positions")));
        if (body.containsKey("pinnedNodes")) request.pinnedNodes(new LinkedHashSet<>(strings(body.get("pinnedNodes"), "pinnedNodes")));

        Layout<String> layout;
        String algorithm = String.valueOf(body.get("algorithm"));
        switch (algorithm)
        {
            case "preset": layout = new PresetLayout<>(); break;
            case "circular": layout = new CircularLayout<>(number(params, "radius", Math.max(50, nodes.size() * 8.0)), number(params, "startAngle", 0)); break;
            case "random":
                double side = Math.max(100, Math.sqrt(nodes.size()) * 60);
                layout = new RandomLayout<>(number(params, "width", side), number(params, "height", side));
                break;
            case "grid":
                long columns = integer(params, "columns", 0);
                if (columns < 0 || columns > Integer.MAX_VALUE) throw new IllegalArgumentException("columns is out of range.");
                layout = new GridLayout<>((int) columns, number(params, "spacing", 30));
                break;
            case "shell":
                List<List<String>> shells = new ArrayList<>();
                if (body.containsKey("shells"))
                {
                    Object value = body.get("shells");
                    if (!(value instanceof List)) throw new IllegalArgumentException("shells must be an array of node arrays.");
                    for (Object shell : (List<?>) value) shells.add(strings(shell, "shell"));
                }
                else if (!nodes.isEmpty()) shells.add(nodes);
                layout = new ShellLayout<>(shells, number(params, "spacing", 50));
                break;
            case "concentric":
                if (body.containsKey("scores"))
                {
                    Map<?, ?> raw = object(body.get("scores"), "scores");
                    if (!raw.keySet().equals(new HashSet<>(nodes)))
                        throw new IllegalArgumentException("scores must contain exactly the graph nodes.");
                    Map<String, Double> scores = new HashMap<>();
                    for (String node : nodes) scores.put(node, finite(raw.get(node), "Score for " + node));
                    layout = new ConcentricLayout<>(scores::get, number(params, "spacing", 50));
                }
                else layout = new ConcentricLayout<>(graph::degree, number(params, "spacing", 50));
                break;
            case "radial":
                layout = new RadialLayout<>(root(params),
                    params.containsKey("direction") ? EdgeOrientation.valueOf(String.valueOf(params.get("direction"))) : EdgeOrientation.UND,
                    number(params, "spacing", 50));
                break;
            case "feature-grid":
                layout = new FeatureGridLayout<>(partitions(body.get("partitions")), number(params, "columnSpacing", 150),
                    number(params, "spacing", 50), sweeps(params));
                break;
            case "ego-grid":
                layout = new EgoGridLayout<>(root(params),
                    params.containsKey("direction") ? EdgeOrientation.valueOf(String.valueOf(params.get("direction"))) : EdgeOrientation.UND,
                    number(params, "columnSpacing", 150), number(params, "spacing", 50));
                break;
            case "tree":
                layout = new TreeLayout<>(root(params), number(params, "spacing", 50), number(params, "levelSpacing", 75));
                break;
            case "bipartite":
                if (body.containsKey("partitions"))
                {
                    List<List<String>> parts = partitions(body.get("partitions"));
                    if (parts.size() != 2) throw new IllegalArgumentException("Bipartite layout requires exactly two partitions.");
                    layout = new BipartiteLayout<>(parts.get(0), parts.get(1), number(params, "columnSpacing", 150),
                        number(params, "spacing", 50), sweeps(params));
                }
                else layout = new BipartiteLayout<>(number(params, "columnSpacing", 150), number(params, "spacing", 50), sweeps(params));
                break;
            case "multipartite":
                layout = new MultipartiteLayout<>(partitions(body.get("partitions")), number(params, "columnSpacing", 150),
                    number(params, "spacing", 50), sweeps(params));
                break;
            default: throw new IllegalArgumentException("Unknown layout: " + algorithm);
        }

        if (params.containsKey("packComponents") && !(params.get("packComponents") instanceof Boolean))
            throw new IllegalArgumentException("packComponents must be a boolean.");
        if (Boolean.TRUE.equals(params.get("packComponents")))
            layout = new LayoutPipeline<>(layout, Collections.singletonList(new ComponentPacking<>(number(params, "packingGap", 50))));
        LayoutResult<String> result = layout.compute(graph, request.build());
        Map<String, Object> coordinates = new LinkedHashMap<>();
        result.getPositions().forEach((node, point) -> coordinates.put(node, Map.of("x", point.v1(), "y", point.v2())));
        Bounds2D bounds = result.getBounds();
        return Map.of("algorithm", result.getDiagnostics().getAlgorithmId(), "positions", coordinates,
            "bounds", Map.of("minX", bounds.getMinX(), "minY", bounds.getMinY(), "maxX", bounds.getMaxX(), "maxY", bounds.getMaxY()),
            "termination", result.getDiagnostics().getTermination().name());
    }

    private static List<List<String>> partitions(Object value)
    {
        if (!(value instanceof List)) throw new IllegalArgumentException("partitions must be an array of node arrays.");
        List<List<String>> result = new ArrayList<>();
        for (Object group : (List<?>) value) result.add(strings(group, "partition"));
        return result;
    }

    private static String root(Map<?, ?> params)
    {
        Object value = params.get("root");
        if (value == null) return null;
        if (!(value instanceof String)) throw new IllegalArgumentException("root must be a string node ID.");
        return (String) value;
    }

    private static int sweeps(Map<?, ?> params)
    {
        long value = integer(params, "sweeps", 4);
        if (value < 0 || value > 100) throw new IllegalArgumentException("sweeps must be between 0 and 100.");
        return (int) value;
    }

    private static Map<String, Pair<Double>> positions(Object value)
    {
        Map<String, Pair<Double>> positions = new LinkedHashMap<>();
        object(value, "positions").forEach((key, point) -> {
            if (!(key instanceof String)) throw new IllegalArgumentException("Position keys must be node IDs.");
            Map<?, ?> pair = object(point, "position");
            positions.put((String) key, new Pair<>(finite(pair.get("x"), "x"), finite(pair.get("y"), "y")));
        });
        return positions;
    }

    private static Map<?, ?> object(Object value, String field)
    {
        if (!(value instanceof Map)) throw new IllegalArgumentException(field + " must be an object.");
        return (Map<?, ?>) value;
    }

    private static List<String> strings(Object value, String field)
    {
        if (!(value instanceof List)) throw new IllegalArgumentException(field + " must be a node array.");
        List<String> result = new ArrayList<>();
        for (Object node : (List<?>) value)
        {
            if (!(node instanceof String)) throw new IllegalArgumentException(field + " must contain string node IDs.");
            result.add((String) node);
        }
        return result;
    }

    private static double finite(Object value, String field)
    {
        if (!(value instanceof Number) || !Double.isFinite(((Number) value).doubleValue()))
            throw new IllegalArgumentException(field + " must be a finite number.");
        return ((Number) value).doubleValue();
    }

    private static double number(Map<?, ?> params, String field, double fallback)
    {
        return params.containsKey(field) ? finite(params.get(field), field) : fallback;
    }

    private static long integer(Map<?, ?> params, String field, long fallback)
    {
        if (!params.containsKey(field)) return fallback;
        Object value = params.get(field);
        // Strict decimal parsing rejects fractional numbers and overflow without a lossy double conversion.
        try { return Long.parseLong(value instanceof Number || value instanceof String ? value.toString() : ""); }
        catch (NumberFormatException error) { throw new IllegalArgumentException(field + " must be a 64-bit integer."); }
    }
}
