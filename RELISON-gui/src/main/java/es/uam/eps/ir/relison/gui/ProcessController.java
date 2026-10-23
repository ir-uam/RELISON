package es.uam.eps.ir.relison.gui;

import io.javalin.http.Handler;
import es.uam.eps.ir.relison.utils.ComputationMonitor;
import java.util.*;

/** HTTP adapters keep preparation, computation, publication and cleanup in one job. */
final class ProcessController
{
    private final JobManager jobs;
    ProcessController(JobManager jobs) { this.jobs = jobs; }

    Handler track(String kind, Handler handler) {
        return ctx -> {
            Map<?, ?> body = ctx.bodyAsClass(Map.class);
            String graphId = String.valueOf(body.get("graphId"));
            String requested = ctx.header("X-Relison-Job-ID");
            String id = requested == null ? UUID.randomUUID().toString() : UUID.fromString(requested).toString();
            Map<String, Object> details = new LinkedHashMap<>();
            for (String key : List.of("metric", "algorithm", "partitionId", "attribute", "params", "type", "mode", "withRecommendation", "recommendationKey", "onlyLinks", "runId", "node", "piece", "feature", "category", "entity"))
                if (body.get(key) != null) details.put(key, body.get(key));
            String label = String.valueOf(body.containsKey("metric") ? body.get("metric")
                    : body.containsKey("algorithm") ? body.get("algorithm") : kind);
            ctx.header("X-Relison-Job-ID", id);
            try {
                jobs.run(id, graphId, kind, label, details, () -> {
                    ComputationMonitor.current().phase("Running " + label);
                    handler.handle(ctx);
                    if (ctx.status().getCode() >= 400) JobManager.fail(ctx.result());
                    return null;
                });
            } catch (JobCancelledException error) {
                ctx.status(200).json(Map.of("cancelled", true, "jobId", id));
            }
        };
    }

    Handler mutation(Handler handler) {
        return ctx -> {
            String graphId = ctx.pathParamMap().get("id");
            if (graphId == null) graphId = String.valueOf(ctx.bodyAsClass(Map.class).get("graphId"));
            if (!jobs.tryMutation(graphId, () -> handler.handle(ctx)))
                ctx.status(409).json(Map.of("error", "A process is using this graph. Wait for it to finish or cancel it in Processes before editing."));
        };
    }
}
