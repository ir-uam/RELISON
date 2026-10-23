"use strict";

// One analysis workspace for retained diffusion runs. Display changes reuse cached
// responses; only a new run/entity/configuration schedules server work.
const DiffusionWorkspace = (() => {
    const views = {}, cache = new Map(), visible = new Set(), colors = new Map();
    const names = { overview: "Overview", metrics: "Metrics", node: "Nodes", piece: "Pieces", feat: "Features", dist: "Distributions", compare: "Compare" };
    const ids = { node: "diff-traj", piece: "diff-ptraj", feat: "diff-ftraj", dist: "diff-dist" };
    let ready = false, generation = 0;
    const el = (tag, cls, text) => metricWorkspaceElement(tag, cls, text);
    const selectedRuns = () => state.diffusion.runs.filter(run => visible.has(run.id));
    const color = run => /^#[0-9a-f]{6}$/i.test(run.color || "") ? run.color : echartColors().palette[(colors.get(run.id) || 0) % echartColors().palette.length];
    function select(label, entries, parent, onChange) {
        const field = el("label", "diff-analysis-field"), input = el("select");
        for (const [value, text] of entries) input.append(option(value, text));
        field.append(el("span", "", label), input); parent.append(field);
        if (onChange) input.addEventListener("change", onChange);
        return input;
    }
    function button(label, icon, parent, action) {
        const input = metricWorkspaceAction(el("button"), icon, label);
        input.addEventListener("click", action); parent.append(input); return input;
    }
    function table(host, rows) {
        host.replaceChildren();
        if (!rows.length) return;
        const wrap = el("div", "scroll-table diff-analysis-table-wrap"), tbl = el("table", "data diff-analysis-table"), head = el("thead"), body = el("tbody");
        rows.forEach((row, i) => { const tr = el("tr"); row.forEach(value => tr.append(el(i ? "td" : "th", "", typeof value === "number" ? fmt(value) : value ?? "—"))); (i ? body : head).append(tr); });
        tbl.append(head, body);
        wrap.append(tbl); host.append(wrap);
    }
    function exportRows(view) {
        if (!view.rows?.length) return;
        download("diffusion-" + view.key + ".csv", view.rows.map(row => row.map(value => csvCell(typeof value === "number" && !Number.isFinite(value) ? "" : value)).join(",")).join("\n"), "text/csv");
    }
    function panel(key, host, chartId, oldButton) {
        const view = views[key] = { key, host, chartId, datasets: [], rows: [], ticket: 0 };
        const header = el("header", "diff-analysis-header"), actions = el("div", "diff-analysis-actions");
        header.append(el("h2", "", names[key]), actions); host.prepend(header);
        if (chartId) {
            const image = oldButton ? $(oldButton) : null;
            if (image) { metricWorkspaceAction(image, "download", "Download PNG / SVG"); actions.append(image); }
            else button("Download PNG / SVG", "download", actions, () => downloadChartPng($(chartId), "diffusion-" + key + ".png"));
        }
        view.csv = button("Export CSV", "table_view", actions, () => exportRows(view));
        view.controls = el("div", "chart-controls metric-analysis-controls diff-analysis-controls"); header.after(view.controls);
        view.display = el("details", "metric-display-options"); view.display.append(el("summary", "", "Display options"));
        view.displayControls = el("div", "chart-controls diff-analysis-controls"); view.display.append(view.displayControls); view.controls.after(view.display);
        view.note = el("p", "muted diff-analysis-note"); view.note.setAttribute("aria-live", "polite");
        view.summary = el("div", "diff-analysis-summary");
        view.summaryActions = el("div", "diff-analysis-actions"); view.summaryTable = el("div");
        button("Export statistics CSV", "table_view", view.summaryActions, () => exportRows({ key: key + "-statistics", rows: view.summaryRows }));
        view.summary.append(view.summaryActions, view.summaryTable);
        host.append(view.note, view.summary);
        if (chartId) {
            view.style = select("Display", [["lines", "Lines"], ["points", "Points"], ["both", "Lines and points"]], view.displayControls, () => redraw(key));
            view.logX = select("X axis", [["linear", "Linear"], ["log", "Logarithmic"]], view.displayControls, () => redraw(key));
            view.logY = select("Y axis", [["linear", "Linear"], ["log", "Logarithmic"]], view.displayControls, () => redraw(key));
        }
        return view;
    }
    function setup() {
        const pane = $("diff-view-stats").querySelector(".diff-stats-pane");
        $("pane-diffusion").classList.add("diff-analysis-ready");
        document.querySelector('[data-diffsubtab="stats"]').textContent = "Analysis";
        document.querySelector('[data-diffsubtab="metrics"]').hidden = true;
        const nav = pane.querySelector("nav"); nav.replaceChildren(); nav.className = "diff-analysis-tabs";
        nav.setAttribute("role", "tablist"); nav.setAttribute("aria-label", "Diffusion analysis views"); nav.setAttribute("aria-orientation", "horizontal");
        for (const [key, label] of Object.entries(names)) {
            const tab = el("button", "diffstatsview", label); tab.dataset.statsview = key;
            tab.setAttribute("role", "tab"); tab.setAttribute("aria-controls", "diff-stats-view-" + key);
            tab.addEventListener("click", () => switchStatsView(key)); nav.append(tab);
        }
        const picker = el("section", "diff-analysis-runs"); picker.id = "diff-analysis-runs"; nav.after(picker);
        const metricHost = $("diff-view-metrics"); metricHost.id = "diff-stats-view-metrics";
        metricHost.className = "diff-stats-view"; metricHost.hidden = true; pane.append(metricHost);
        // Preserve the legacy ID for integrations and export handlers.
        const legacy = el("div"); legacy.id = "diff-view-metrics"; legacy.hidden = true; $("diff-view-stats").before(legacy);
        for (const key of ["overview", "compare"]) { const host = el("div", "diff-stats-view"); host.id = "diff-stats-view-" + key; host.hidden = true; pane.append(host); }
        panel("overview", $("diff-stats-view-overview"));
        views.overview.display.hidden = true;
        const metric = panel("metrics", metricHost.querySelector(".diffmetrics-pane"), "diff-metrics-chart", "btn-diffmetric-png");
        $("btn-diffmetric-csv").hidden = true; $("diffmetrics-runs").hidden = true;
        metric.host.querySelector(".chart-controls").hidden = true;
        metric.controls.append($("diffmetric-select").parentElement);
        for (const [key, prefix] of Object.entries(ids)) {
            const view = panel(key, $("diff-stats-view-" + key).querySelector(".diff-node-pane"), prefix + "-canvas", "btn-" + prefix + "-png");
            const toolbar = view.host.querySelector(".diff-node-toolbar");
            view.controls.before(toolbar); toolbar.classList.add("diff-analysis-controls");
            if (key !== "dist") {
                view.style.parentElement.hidden = true;
                const style = $(prefix + "-stack");
                style.append(option("percent", "Percentage composition"), option("points", "Points"), option("both", "Lines and points"));
                view.displayControls.prepend(style.parentElement);
            }
            if (key === "node" || key === "piece") {
                view.mode = select("View", [["timeline", "Timeline (single protocol)"], ["multi", "Timeline (multi-protocol)"], ["rankings", "Rankings"]], toolbar, () => { modeControls(key); renderActiveStatsView(); });
                view.singleRun = select("Protocol", [], toolbar, () => renderActiveStatsView());
                view.rankCategory = select("Rank by", ["received", "read", "propagated", "discarded"].map(x => [x, x]), toolbar, () => rankings(key));
                view.featureValue = select("Feature value", [], toolbar, () => key === "node" ? fetchTrajectory() : fetchPieceTrajectory());
                view.orientation = select("Bars", [["vertical", "Vertical"], ["horizontal", "Horizontal (count on X axis)"]], view.displayControls, () => draw(key));
                view.range = select("Show", [["all", "Show all"], ["top", "Top k"], ["bottom", "Bottom k"]], toolbar, () => { modeControls(key); draw(key); }); view.range.value = "top";
                view.limit = el("input"); view.limit.type = "number"; view.limit.min = "1"; view.limit.value = "20";
                const label = el("label", "diff-analysis-field", "k"); label.append(view.limit); toolbar.append(label);
                view.limit.addEventListener("change", () => draw(key));
                modeControls(key);
            }
        }
        const dist = views.dist;
        const feat = views.feat;
        feat.mode = select("View", [["all", "All values"], ["single", "Individual feature (single-protocol)"], ["multi", "Individual feature (multi-protocol)"]], feat.controls, () => { featureControls(); renderActiveStatsView(); });
        feat.singleRun = select("Protocol", [], feat.controls, () => renderActiveStatsView());
        dist.plot = select("Chart", [["categories", "Category counts"], ["shares", "Category shares (%)"], ["histogram", "Numeric histogram"], ["cdf", "Cumulative distribution"], ["exact", "Count per exact value"], ["heatmap", "Heatmap"]], dist.controls, () => renderDistribution());
        dist.orientation = select("Bars", [["vertical", "Vertical"], ["horizontal", "Horizontal (count / share on X axis)"]], dist.displayControls, () => draw("dist"));
        dist.bin = el("input"); dist.bin.type = "number"; dist.bin.min = "0"; dist.bin.step = "any"; dist.bin.placeholder = "Automatic";
        const binLabel = el("label", "diff-analysis-field", "Bin width"); binLabel.append(dist.bin); dist.controls.append(binLabel);
        dist.bin.addEventListener("change", () => draw("dist"));
        dist.heatRun = select("Heatmap run", [], dist.controls, () => draw("dist"));
        const compare = panel("compare", $("diff-stats-view-compare"), "diff-compare-chart");
        compare.style.value = "points";
        const area = el("div", "chart-area"), chart = el("div", "echarts-chart"); chart.id = compare.chartId; area.append(chart); compare.display.after(area);
        compare.mode = select("Compare", [["metrics", "Metric vs. metric"], ["runs", "Run vs. run"], ["delta", "Change from baseline"]], compare.controls, () => renderCompare());
        compare.x = select("X metric", [], compare.controls, () => renderCompare());
        compare.y = select("Y metric", [], compare.controls, () => renderCompare());
        compare.baseline = select("Baseline run", [], compare.controls, () => renderCompare());
        compare.delta = select("Change", [["absolute", "Absolute difference"], ["percent", "Percentage change"]], compare.controls, () => renderCompare());
        const stage = el("div", "diff-analysis-stage"); stage.append(picker);
        for (const key of Object.keys(names)) { const host = $("diff-stats-view-" + key); stage.append(host); }
        pane.append(nav, stage);
        for (const key of ["node", "piece"]) installChartRail(key, views[key].mode, () => { modeControls(key); renderActiveStatsView(); });
        installChartRail("feat", feat.mode, () => { featureControls(); renderActiveStatsView(); });
        installChartRail("dist", views.dist.plot, () => renderDistribution());
        installChartRail("compare", views.compare.mode, () => renderCompare());
        state.diffusion.statsView = "overview";
        for (const key of Object.keys(names)) { $("diff-stats-view-" + key).setAttribute("role", "tabpanel"); }
        ready = true; switchStatsView("overview");
    }
    function installChartRail(key, input, change) {
        const view = views[key], host = $("diff-stats-view-" + key), workspace = el("div", "metric-analysis-workspace");
        const rail = el("nav", "metric-analysis-rail"), stage = el("div", "metric-analysis-stage");
        rail.setAttribute("role", "tablist"); rail.setAttribute("aria-label", names[key] + " chart types"); rail.setAttribute("aria-orientation", "vertical");
        let body = view.host;
        if (body === host) { body = el("section", "metric-analysis-panel"); for (const child of [...host.children]) body.append(child); view.host = body; }
        else body.classList.add("metric-analysis-panel");
        view.typeInput = input; view.typeButtons = [];
        for (const item of input.options) {
            const tab = el("button", "", item.textContent); tab.type = "button"; tab.dataset.chartType = item.value; tab.setAttribute("role", "tab");
            tab.addEventListener("click", () => { input.value = item.value; const result = change(); syncChartRail(key); return result; });
            rail.append(tab); view.typeButtons.push(tab);
        }
        input.parentElement.hidden = true; stage.append(body); workspace.append(rail, stage); host.append(workspace); syncChartRail(key);
    }
    function syncChartRail(key) {
        const view = views[key];
        for (const tab of view.typeButtons || []) {
            const active = tab.dataset.chartType === view.typeInput.value;
            tab.setAttribute("aria-selected", String(active));
            tab.disabled = [...view.typeInput.options].find(option => option.value === tab.dataset.chartType)?.disabled || false;
        }
    }
    function fill(input, entries) {
        const value = input.value; input.replaceChildren(...entries.map(([key, label]) => option(key, label)));
        if (entries.some(([key]) => key === value)) input.value = value;
    }
    function changeSelection(run, checked) {
        if (checked) visible.add(run.id); else visible.delete(run.id);
        renderActiveStatsView();
    }
    function selectionActions(host) {
        for (const [label, action] of [["Select all", () => state.diffusion.runs.forEach(run => visible.add(run.id))], ["Deselect all", () => visible.clear()]]) {
            const btn = el("button", "", label); btn.type = "button";
            btn.addEventListener("click", () => { action(); renderActiveStatsView(); }); host.append(btn);
        }
    }
    function sync() {
        if (!ready) return;
        if (typeof syncDiffPlaybackRuns === "function") syncDiffPlaybackRuns();
        const host = $("diff-analysis-runs"); host.replaceChildren(); host.hidden = state.diffusion.statsView === "overview";
        const choices = el("fieldset", "metric-series-picker"); choices.append(el("legend", "", "Runs"));
        for (const run of state.diffusion.runs) {
            if (!colors.has(run.id)) colors.set(run.id, colors.size);
            const label = el("label", "metric-series-choice"), input = el("input"), swatch = el("span", "legend-swatch");
            input.type = "checkbox"; input.checked = visible.has(run.id); input.setAttribute("aria-label", "Include " + run.label);
            swatch.style.background = color(run); input.addEventListener("change", () => changeSelection(run, input.checked));
            label.append(input, swatch, el("span", "", run.label + (run.archived ? " · Metrics only" : ""))); choices.append(label);
        }
        host.append(choices); const actions = el("div", "diff-analysis-selection-actions"); selectionActions(actions); host.append(actions);
        fill(views.compare.baseline, state.diffusion.runs.map(run => [run.id, run.label]));
        fill(views.dist.heatRun, selectedRuns().map(run => [run.id, run.label]));
        for (const key of ["node", "piece"]) timelineControls(key);
        featureControls();
        for (const key of Object.keys(views)) syncChartRail(key);
    }
    async function deleteRun(run, remove) {
        remove.disabled = true;
        try {
            const response = run.archived ? {} : await api("/api/diffusion/clear", jsonBody({ graphId: state.graphId, runId: run.id }));
            state.diffusion.runs = state.diffusion.runs.filter(item => item !== run); visible.delete(run.id);
            for (const key of cache.keys()) if (key.includes(JSON.stringify(run.id))) cache.delete(key);
            if (run.id === state.diffusion.result?.runId) activateDiffusionAnalysisRun(response.result || null);
            populateDiffMetricSelect(); renderActiveStatsView();
        } catch (error) { setStatus(error.message, "error"); remove.disabled = false; }
    }
    function definition(family, config) {
        if (!config) return "";
        if (typeof config === "string") config = { id: config };
        const label = config.label || state.defs?.[family]?.[config.id]?.label || config.id || "";
        const params = Object.entries(config.params || {}).map(([key, value]) => key + "=" + (typeof value === "object" ? JSON.stringify(value) : String(value))).join(", ");
        return label + (params ? " [" + params + "]" : "");
    }
    function runConfiguration(run) {
        const config = run.config || {}, protocol = config.protocol;
        const old = Object.fromEntries((run.detail || "").split("\n").flatMap(line => { const index = line.indexOf(":"); return index < 0 ? [] : [[line.slice(0, index).trim().toLowerCase(), line.slice(index + 1).trim()]]; }));
        const custom = config.type === "custom" || protocol?.type === "custom" || !!protocol?.selection || !!old.selection;
        const mechanisms = custom ? ["selection", "expiration", "propagation", "update", "sight"].map(key => [key[0].toUpperCase() + key.slice(1), definition(key, protocol?.[key]) || old[key] || "—"]) : [];
        return { preset: custom ? "" : definition("protocol", protocol) || old.protocol || "", mechanisms,
            stop: definition("stop", config.stop) || old.stop || "",
            metrics: (config.metrics || run.metrics || []).map(metric => definition("metric", metric)),
            network: old.recommendation || run.network || (run.recKey ? run.recKey : "Original") };
    }
    function reset() {
        generation++; visible.clear(); colors.clear(); cache.clear();
        for (const view of Object.values(views)) {
            view.ticket++; view.datasets = []; view.rows = [];
            if (view.chartId) clearChart(view.chartId);
            view.summaryRows = []; view.summaryTable?.replaceChildren();
            if (view.summaryActions) view.summaryActions.hidden = true;
        }
        if (ready) { sync(); renderOverview(); }
    }
    function addRun(id) { visible.add(id); colors.set(id, colors.size); sync(); }
    function restoreRuns() {
        for (const run of state.diffusion.runs) {
            run.id ||= crypto.randomUUID(); run.archived = true;
            run.network ||= run.recKey ? "Recommendation" : "Original";
            addRun(run.id);
        }
    }
    function resultRows(view, rows, summary = []) {
        view.rows = rows; view.csv.disabled = rows.length < 2;
        view.summaryRows = summary; view.summaryActions.hidden = summary.length < 2 || view.key === "overview";
        table(view.summaryTable, summary);
    }
    function renderOverview() {
        const view = views.overview; if (!view) return;
        view.note.textContent = state.diffusion.runs.length ? "Select runs for analysis and edit their chart colours." : "Run a simulation to start analysing its results.";
        const actions = el("div", "toolbar diff-analysis-selection-actions"); selectionActions(actions);
        const wrap = el("div", "scroll-table diff-runs-table-wrap"), tbl = el("table", "data diff-runs-table"); tbl.id = "diff-analysis-run-table";
        tbl.setAttribute("aria-label", "Diffusion runs");
        const head = el("thead"), groups = el("tr"), columns = el("tr"), body = el("tbody");
        const selectHead = el("th"), all = el("input"); all.type = "checkbox";
        all.checked = state.diffusion.runs.length > 0 && selectedRuns().length === state.diffusion.runs.length;
        all.indeterminate = selectedRuns().length > 0 && !all.checked;
        all.setAttribute("aria-label", "Select all runs"); all.addEventListener("change", () => {
            if (all.checked) state.diffusion.runs.forEach(run => visible.add(run.id)); else visible.clear(); renderActiveStatsView();
        }); selectHead.append(all); selectHead.setAttribute("rowspan", "2"); groups.append(selectHead);
        for (const label of ["Colour", "Run"]) { const th = el("th", "", label); th.setAttribute("rowspan", "2"); groups.append(th); }
        const configuration = el("th", "", "Configuration"); configuration.setAttribute("colspan", "5"); groups.append(configuration);
        const actionHead = el("th", "act-th", "Delete"); actionHead.setAttribute("rowspan", "2"); groups.append(actionHead);
        for (const label of ["Preset protocol", "Mechanisms", "Stop condition", "Iterations", "Network"]) columns.append(el("th", "", label));
        head.append(groups, columns);
        for (const run of state.diffusion.runs) {
            const config = runConfiguration(run), tr = el("tr"); tr.dataset.runId = run.id;
            const selection = el("td", "diff-run-select"), checkbox = el("input"); checkbox.type = "checkbox"; checkbox.checked = visible.has(run.id);
            checkbox.setAttribute("aria-label", "Include " + run.label); checkbox.addEventListener("change", () => changeSelection(run, checkbox.checked)); selection.append(checkbox);
            const colour = el("td", "diff-run-colour"), picker = el("input"); picker.type = "color"; picker.value = color(run); picker.setAttribute("aria-label", "Chart colour for " + run.label);
            const updateColor = () => {
                if (!/^#[0-9a-f]{6}$/i.test(picker.value)) return;
                run.color = picker.value;
                // Rebuild the source chips, keeping this native colour picker mounted.
                sync(); view.rows = overviewRows();
            };
            picker.addEventListener("input", updateColor);
            picker.addEventListener("change", () => { updateColor(); for (const key of ["metrics", "compare", "node", "piece", "feat", "dist"]) redraw(key); }); colour.append(picker);
            const name = el("td", "diff-run-name"), nameInput = el("input"); nameInput.type = "text"; nameInput.value = run.label;
            nameInput.setAttribute("aria-label", "Name for " + run.label); nameInput.setAttribute("title", "Edit run name"); name.append(nameInput);
            nameInput.addEventListener("change", () => {
                const base = nameInput.value.trim(); if (!base) { nameInput.value = run.label; return; }
                const used = new Set(state.diffusion.runs.filter(item => item !== run).map(item => item.label));
                let label = base, suffix = 2; while (used.has(label)) label = base + " (" + suffix++ + ")";
                run.label = label; sync(); renderOverview();
            });
            nameInput.addEventListener("keydown", event => { if (event.key === "Enter") nameInput.blur(); });
            if (run.archived) name.append(el("span", "diff-analysis-badge", "Metrics only"));
            const preset = el("td", "diff-run-config", config.preset), mechanisms = el("td", "diff-run-config");
            for (const [label, value] of config.mechanisms) { const line = el("div", "diff-run-mechanism"); line.append(el("span", "muted", label), el("span", "", value)); mechanisms.append(line); }
            const removeCell = el("td"), remove = el("button", "row-remove", "✕"); remove.type = "button";
            remove.setAttribute("aria-label", "Delete " + run.label); remove.setAttribute("title", "Delete " + run.label);
            remove.addEventListener("click", () => deleteRun(run, remove)); removeCell.append(remove);
            tr.append(selection, colour, name, preset, mechanisms, el("td", "diff-run-config", config.stop), el("td", "", run.numIterations), el("td", "diff-run-config", config.network), removeCell); body.append(tr);
        }
        tbl.append(head, body); wrap.append(tbl); view.summaryTable.replaceChildren(actions, wrap);
        view.rows = overviewRows(); view.csv.disabled = view.rows.length < 2; view.summaryActions.hidden = true;
    }
    function overviewRows() {
        return [["Selected", "Colour", "Run", "Preset protocol", "Selection", "Expiration", "Propagation", "Update", "Sight", "Stop condition", "Iterations", "Metrics", "Network"], ...state.diffusion.runs.map(run => {
            const config = runConfiguration(run);
            return [visible.has(run.id), color(run), run.label, config.preset, ...["Selection", "Expiration", "Propagation", "Update", "Sight"].map(label => config.mechanisms.find(([name]) => name === label)?.[1] || ""), config.stop, run.numIterations, config.metrics.join("; "), config.network];
        })];
    }
    function renderMetrics() {
        const view = views.metrics; if (!view) return;
        syncChartRail("metrics");
        sync();
        const union = diffMetricUnion(), selected = $("diffmetric-select");
        populateDiffMetricSelect();
        let chart = $(view.chartId);
        if (!chart) { const area = el("div", "chart-area"); chart = el("div", "echarts-chart"); chart.id = view.chartId; area.append(chart); $("diffmetrics-charts").replaceChildren(area); }
        const metric = union.find(item => item.id === selected.value), runs = selectedRuns();
        const series = [], rows = [["Run", "Metric", "Iteration", "Value"]], summary = [["Run", "Final", "Minimum", "Maximum", "Mean"]];
        for (const run of runs) {
            const data = run.metrics.find(item => item.id === metric?.id); if (!data) continue;
            const points = data.values.map((value, i) => [i, value == null ? NaN : value]);
            series.push({ name: run.label, points, color: color(run) });
            points.forEach(([i, value]) => rows.push([run.label, metric.label, i, value]));
            const stats = metricAnalysisSummary(data.values.filter(value => value != null));
            summary.push([run.label, data.values.at(-1), stats.min, stats.max, stats.mean]);
        }
        const missing = runs.filter(run => !run.metrics.some(item => item.id === metric?.id));
        view.note.textContent = missing.length ? "Metric unavailable in: " + missing.map(run => run.label).join(", ") + "." : "";
        $("diffmetrics-hint").textContent = "";
        plot(view, series, "Iteration", metric?.label || "Value"); resultRows(view, rows, summary);
    }
    async function request(url, body, run) {
        if (run.archived) throw new Error("Only metric curves were saved with this session. Run the simulation again for detailed analysis.");
        const payload = { ...body, graphId: state.graphId, runId: run.id }, key = JSON.stringify([url, payload]);
        if (cache.has(key)) return cache.get(key);
        const pending = api(url, jsonBody(payload)).then(response => {
            if (response.cancelled) throw new Error("Analysis cancelled. Select the view again to retry.");
            if (response.error) throw new Error(response.error);
            return response;
        });
        cache.set(key, pending);
        try { return await pending; } catch (error) { if (cache.get(key) === pending) cache.delete(key); throw error; }
    }
    async function load(key, url, body) {
        const view = views[key], ticket = ++view.ticket, epoch = generation;
        if (key === "dist") view.query = body;
        timelineControls(key);
        if (key === "feat") featureControls();
        const single = view.singleRun && (key === "feat" ? view.mode.value !== "multi" : view.mode.value === "timeline");
        const runs = single ? selectedRuns().filter(run => run.id === view.singleRun.value) : selectedRuns();
        if (key === "feat") {
            body = { ...body, allValues: view.mode.value === "all", value: view.mode.value === "all" ? "__all__" : $("diff-ftraj-value").value };
            view.query = body; view.multiple = view.mode.value === "multi";
            if (view.mode.value !== "all" && !body.value) {
                view.datasets = []; clearChart(view.chartId); resultRows(view, []); view.note.textContent = "Choose a feature value."; return;
            }
        }
        const timeline = ["node", "piece"].includes(key) && !isRanking(key);
        if (timeline) {
            const scope = JSON.stringify([body.feature, body.userFeature]);
            if (view.featureScope !== scope) { view.featureScope = scope; view.featureValue.replaceChildren(); }
            body = { ...body }; view.multiple = view.mode.value === "multi";
            if (view.multiple && body.feature) { body.singleValue = true; if (view.featureValue.value) body.value = view.featureValue.value; }
            view.query = body; timelineControls(key);
        }
        view.datasets = []; view.note.textContent = runs.length ? "Computing missing analysis in Processes. You can continue using the app." : "Select at least one run.";
        clearChart(view.chartId); resultRows(view, []);
        const results = await Promise.allSettled(runs.map(async run => ({ run, response: await request(url, body, run) })));
        if (ticket !== view.ticket || epoch !== generation) return;
        view.datasets = results.filter(item => item.status === "fulfilled").map(item => item.value);
        view.errors = results.flatMap((item, i) => item.status === "rejected" ? [runs[i].label + ": " + item.reason.message] : []);
        if (timeline && view.multiple && body.feature) {
            const values = [...new Set(view.datasets.flatMap(({ response }) => response.availableValues || response.values || []))];
            if (body.value != null && !values.includes(body.value)) values.push(body.value);
            values.sort((a, b) => String(a).localeCompare(String(b), undefined, { numeric: true }));
            fill(view.featureValue, values.map(value => [value, value])); view.featureValue.disabled = !values.length;
            const chosen = view.featureValue.value;
            if (chosen && view.datasets.some(({ response }) => Array.isArray(response.availableValues) && response.selectedValue !== chosen)) return load(key, url, { ...body, value: chosen });
        }
        const legacy = { node: "traj", piece: "ptraj", feat: "ftraj", dist: "dist" }[key];
        if (legacy) state.diffusion[legacy] = view.datasets[0]?.response || null;
        draw(key);
    }
    const isRanking = key => views[key]?.mode?.value === "rankings";
    function featureControls() {
        const view = views.feat; if (!view?.mode) return;
        const all = view.mode.value === "all", multiple = view.mode.value === "multi";
        fill(view.singleRun, selectedRuns().map(run => [run.id, run.label]));
        view.singleRun.parentElement.hidden = multiple;
        const value = $("diff-ftraj-value");
        fill(value, featureValueOptions($("diff-ftraj-feature").value).map(value => [value, value]));
        value.parentElement.hidden = all;
        const category = $("diff-ftraj-category"); category.disabled = !all && !multiple; category.parentElement.hidden = !all && !multiple;
        const style = $("diff-ftraj-stack");
        for (const option of style.options) if (["stacked", "percent"].includes(option.value)) option.disabled = multiple;
        if (multiple && ["stacked", "percent"].includes(style.value)) style.value = "lines";
    }
    function timelineControls(key) {
        const view = views[key]; if (!view?.featureValue) return;
        const prefix = ids[key], multiple = view.mode.value === "multi", ranking = isRanking(key);
        if (view.singleRun) {
            fill(view.singleRun, selectedRuns().map(run => [run.id, run.label]));
            view.singleRun.parentElement.hidden = ranking || multiple;
        }
        const base = $(prefix + "-feature").value === (key === "node" ? "pieces" : "users");
        const category = $(prefix + "-category"); category.disabled = base && !multiple; category.parentElement.hidden = ranking || base && !multiple;
        view.featureValue.parentElement.hidden = ranking || !multiple || base;
        if (key === "node") { $(prefix + "-agg").disabled = base; $(prefix + "-agg").parentElement.hidden = ranking || base; }
        const style = $(prefix + "-stack");
        style.parentElement.hidden = ranking;
        for (const option of style.options) if (["stacked", "percent"].includes(option.value)) option.disabled = multiple;
        if (multiple && ["stacked", "percent"].includes(style.value)) style.value = "lines";
    }
    function modeControls(key) {
        const view = views[key], ranking = isRanking(key), prefix = ids[key];
        for (const control of view.host.querySelectorAll(".diff-node-toolbar input, .diff-node-toolbar select")) {
            if (control.id?.startsWith(prefix)) control.closest("label").hidden = ranking;
        }
        view.rankCategory.parentElement.hidden = !ranking; view.range.parentElement.hidden = !ranking;
        view.orientation.parentElement.hidden = !ranking;
        view.limit.parentElement.hidden = !ranking || view.range.value === "all";
        timelineControls(key); syncChartRail(key);
    }
    function rankings(key) {
        modeControls(key); $(ids[key] + "-hint").hidden = true;
        return load(key, "/api/diffusion/rankings", { entity: key === "piece" ? "pieces" : "nodes", category: views[key].rankCategory.value });
    }
    function redraw(key) { if (key === "metrics") renderMetrics(); else if (key === "compare") renderCompare(); else draw(key); }
    function draw(key) {
        const view = views[key]; if (!view) return;
        if (key === "dist") { drawDistribution(view); return; }
        const ranking = isRanking(key);
        if (key === "feat") featureControls(); else if (view.mode) modeControls(key);
        syncChartRail(key);
        if (ranking) { drawRankings(view); return; }
        const style = $(ids[key] + "-stack")?.value || "lines", series = [], rows = [["Run", "Series", "Iteration", "Value"]];
        const summary = [["Run", "Series", "Final", "Minimum", "Maximum", "Mean"]];
        const scoped = ["node", "piece", "feat"].includes(key), multiple = scoped ? (view.multiple ?? selectedRuns().length > 1) : view.datasets.length > 1;
        const query = view.query || {}, category = query.category || (scoped ? $(ids[key] + "-category").value : "received");
        const chosen = key === "feat" ? category : query.feature ? view.featureValue?.value : category;
        const composition = ["stacked", "percent"].includes(style) && !multiple && view.datasets.length === 1;
        for (const { run, response } of view.datasets) {
            const valuesToShow = scoped && multiple ? chosen ? [chosen] : [] : response.values || [];
            const totals = Array.from({ length: response.iterations }, (_, i) => valuesToShow.reduce((sum, value) => sum + (response.series?.[value]?.[i] || 0), 0));
            for (const [index, name] of valuesToShow.entries()) {
                const values = response.series?.[name] || Array(response.iterations || 0).fill(NaN);
                const points = values.map((value, i) => [i, value == null ? NaN : style === "percent" && composition ? totals[i] ? 100 * value / totals[i] : 0 : value]);
                const label = scoped ? multiple ? run.label : name : run.label + " · " + name;
                const palette = echartColors().palette;
                series.push({ name: label, points, color: scoped && !multiple ? palette[index % palette.length] : color(run), dash: index % 5, stack: composition ? run.id : undefined });
                values.forEach((value, i) => rows.push([run.label, name, i, value]));
                const stats = metricAnalysisSummary(values); summary.push([run.label, name, values.at(-1), stats.min, stats.max, stats.mean]);
            }
        }
        const context = key === "feat" ? query.feature + (query.value && query.value !== "__all__" ? " = " + query.value : "") + (multiple || view.mode.value === "all" ? " · " + category : "") + ". " : scoped && (multiple || query.feature) ? category + (query.feature ? " · " + query.feature + (multiple ? " = " + (chosen || "choose a feature value") : "") : "") + ". " : "";
        view.note.textContent = (view.errors || []).join("; ") || context + (multiple && ["stacked", "percent"].includes(style) ? "Select one run for stacked or percentage composition; multiple runs use lines." : style === "percent" ? "Percentages describe the selected series' composition. Categories can overlap; these are not unique-user shares." : "");
        plot(view, series, "Iteration", style === "percent" && composition ? "Composition (%)" : "Count / weight", { style: ["points", "both"].includes(style) ? style : "lines", composition, playhead: true });
        resultRows(view, rows, summary);
    }
    function drawRankings(view) {
        const limit = Math.max(1, Math.floor(Number(view.limit.value) || 20)), range = view.range.value, rows = [["Run", "Entity", "Rank", "Count"]], series = [];
        const selected = view.datasets.map(({ run, response }) => {
            const entries = Object.entries(response.values || {}).filter(([, value]) => Number.isFinite(value)).sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]));
            const shown = range === "all" ? entries : range === "bottom" ? entries.slice().sort((a, b) => a[1] - b[1] || a[0].localeCompare(b[0])).slice(0, limit) : entries.slice(0, limit);
            return { run, entries, shown };
        });
        const labels = [...new Set(selected.flatMap(item => item.shown.map(([id]) => id)))];
        for (const { run, entries } of selected) {
            entries.forEach(([id, count], i) => rows.push([run.label, id, i + 1, count]));
            const values = new Map(entries); series.push({ name: run.label, points: labels.map(id => [id, values.has(id) ? values.get(id) : NaN]), color: color(run) });
        }
        view.note.textContent = (view.errors || []).join("; ") || "Unique " + (view.key === "node" ? "pieces per node" : "users per piece") + " over the whole simulation. " + (range === "all" ? "Showing all entities." : "Chart shows the union of each run's " + range + " " + limit + ".") + " CSV contains all entities.";
        plot(view, series, view.key === "node" ? "Node" : "Piece", "Count", { category: labels, bar: true, horizontal: view.orientation.value === "horizontal", hideLegend: selectedRuns().length < 2 }); resultRows(view, rows);
    }
    function renderCompare() {
        const view = views.compare; if (!view) return;
        syncChartRail("compare");
        const union = diffMetricUnion(); fill(view.x, union.map(item => [item.id, item.label])); fill(view.y, union.map(item => [item.id, item.label]));
        const mode = view.mode.value, baseline = state.diffusion.runs.find(run => run.id === view.baseline.value), series = [], rows = [["Run", "Iteration", "X", "Y"]];
        view.x.parentElement.hidden = mode !== "metrics"; view.baseline.parentElement.hidden = mode === "metrics"; view.delta.parentElement.hidden = mode !== "delta";
        const yLabel = union.find(item => item.id === view.y.value)?.label || "Value", xLabel = union.find(item => item.id === view.x.value)?.label || "Value";
        const missing = [];
        for (const run of selectedRuns()) {
            if (mode !== "metrics" && run === baseline) continue;
            const ys = run.metrics.find(item => item.id === view.y.value)?.values;
            const xs = mode === "metrics" ? run.metrics.find(item => item.id === view.x.value)?.values : baseline?.metrics.find(item => item.id === view.y.value)?.values;
            if (!xs || !ys) { missing.push(run.label); continue; }
            const n = Math.min(xs.length, ys.length), points = [];
            for (let i = 0; i < n; i++) {
                const valid = Number.isFinite(xs[i]) && Number.isFinite(ys[i]);
                const x = mode === "delta" ? i : valid ? xs[i] : NaN;
                const y = !valid ? NaN : mode !== "delta" ? ys[i] : view.delta.value === "percent" ? xs[i] === 0 ? NaN : 100 * (ys[i] - xs[i]) / Math.abs(xs[i]) : ys[i] - xs[i];
                points.push([x, y]); rows.push([run.label, i, x, y]);
            }
            series.push({ name: run.label, points, color: color(run) });
        }
        view.note.textContent = (mode === "metrics" ? "Points pair metrics at the same iteration." : "Runs are paired over their shared iteration range; missing values leave gaps." + (mode === "delta" && view.delta.value === "percent" ? " Percentage change is undefined where the baseline is zero." : "")) + (missing.length ? " Missing metric data: " + missing.join(", ") : "");
        if (!series.length) view.note.textContent += " Select runs with matching metrics; run comparisons need a selected run other than the baseline.";
        plot(view, series, mode === "delta" ? "Iteration" : mode === "metrics" ? xLabel : (baseline?.label || "Baseline") + " · " + yLabel, mode === "delta" ? view.delta.value === "percent" ? "Change (%)" : "Difference · " + yLabel : yLabel);
        resultRows(view, rows);
    }
    function plot(view, series, xName, yName, options = {}) {
        if (!$(view.chartId)) return;
        if (!series.length || !series.some(item => item.points.length)) { clearChart(view.chartId); if (!view.note.textContent) view.note.textContent = "Select runs with suitable data to display this chart."; return; }
        const colors = echartColors(), config = chartBase(colors, ""), categorical = !!options.category, horizontal = !!options.horizontal;
        view.style.disabled = !!options.bar;
        view.logX.disabled = horizontal ? !!options.composition : categorical || !!options.bar; view.logY.disabled = horizontal ? categorical || !!options.bar : !!options.composition;
        const logX = !categorical && !options.bar && view.logX.value === "log", logY = !options.composition && (horizontal ? view.logX : view.logY).value === "log";
        const style = options.style || view.style.value; let omitted = 0;
        config.grid = { left: 80, right: 30, top: 58, bottom: 86, containLabel: true };
        config.legend = { show: !options.hideLegend, type: "scroll", top: 4, textStyle: { color: colors.text }, pageTextStyle: { color: colors.text } };
        const axis = (name, log) => ({ type: log ? "log" : "value", name, nameLocation: "middle", nameGap: 48, scale: true,
            nameTextStyle: { color: colors.text }, axisLabel: { color: colors.muted, formatter: value => fmt(value) }, splitLine: { lineStyle: { color: colors.border, opacity: colors.gridOpacity } } });
        config.xAxis = categorical ? { ...axis(xName, false), type: "category", data: options.category, axisLabel: { color: colors.muted, hideOverlap: true } } : axis(xName, logX);
        config.yAxis = axis(yName, logY); if (options.composition) config.yAxis.min = 0;
        config.dataZoom = [{ type: "inside", filterMode: "none" }, { type: "slider", height: 18, bottom: 10, filterMode: "none" }];
        config.tooltip = { trigger: "item", formatter: item => escapeChartHtml(item.seriesName) + "<br>" + escapeChartHtml(xName) + ": " + escapeChartHtml(String(item.data[0])) + "<br>" + escapeChartHtml(yName) + ": " + escapeChartHtml(fmt(item.data[1])) };
        config.series = series.map(item => ({ name: item.name, type: options.bar ? "bar" : style === "points" ? "scatter" : "line",
            showSymbol: style === "both", symbolSize: 6, connectNulls: false,
            data: item.points.map(([x, y]) => { if ((logX && x <= 0) || (logY && y <= 0)) { omitted++; return [categorical || Number.isFinite(x) && (!logX || x > 0) ? x : null, null]; } return [categorical || Number.isFinite(x) ? x : null, Number.isFinite(y) ? y : null]; }),
            barMaxWidth: 32,
            stack: item.stack, areaStyle: options.composition ? { opacity: 0.5 } : undefined,
            lineStyle: { width: colors.lineWidth, type: ["solid", "dashed", "dotted", [8, 3, 2, 3], [12, 4]][item.dash || 0] }, itemStyle: { color: item.color },
            markLine: options.playhead && !logX ? { silent: true, symbol: "none", data: [{ xAxis: state.diffusion.iteration || 0 }], label: { show: false }, lineStyle: { color: colors.muted, type: "dashed" } } : undefined }));
        if (omitted) view.note.textContent += " " + omitted + " nonpositive points omitted on logarithmic axes; exports retain raw values.";
        if (horizontal) {
            [config.xAxis, config.yAxis] = [config.yAxis, config.xAxis];
            config.yAxis.inverse = true;
            config.dataZoom = [{ type: "inside", yAxisIndex: 0, filterMode: "none" }, { type: "slider", yAxisIndex: 0, width: 18, right: 4, filterMode: "none" }];
            for (const series of config.series) series.data = series.data.map(([value, count]) => [count, value]);
            config.tooltip.formatter = item => escapeChartHtml(item.seriesName) + "<br>" + escapeChartHtml(yName) + ": " + escapeChartHtml(fmt(item.data[0])) + "<br>" + escapeChartHtml(xName) + ": " + escapeChartHtml(String(item.data[1]));
        }
        showEChart(view.chartId, config, names[view.key]);
    }
    function distributionFeatureNumeric(name, user) {
        if (user) {
            if (Object.hasOwn(state.communityData || {}, name)) return false;
            const definition = (state.attrSchema?.node || []).find(item => item.name === name);
            if (definition) return definition.numeric === true || definition.numeric == null && ["int", "integer", "long", "float", "double", "number"].includes(String(definition.type).toLowerCase());
        }
        const values = featureValueOptions((user ? "user:" : "info:") + name);
        return values.length > 0 && values.every(value => String(value).trim() !== "" && Number.isFinite(Number(value)));
    }
    function distributionControls() {
        const view = views.dist; if (!view?.plot) return;
        const heatmap = view.plot.value === "heatmap", numeric = ["histogram", "cdf", "exact"].includes(view.plot.value);
        const features = knownFeatureParams(), type = $("diff-dist-type");
        type.parentElement.hidden = heatmap;
        $("diff-dist-feat-field").hidden = heatmap;
        $("diff-dist-info-field").hidden = !heatmap; $("diff-dist-user-field").hidden = !heatmap;
        if (heatmap) {
            fill($("diff-dist-info"), features.info.map(name => [name, name]));
            fill($("diff-dist-user"), features.user.map(name => [name, name]));
            $("diff-dist-info").disabled = !features.info.length; $("diff-dist-user").disabled = !features.user.length;
        } else {
            const user = type.value === "user";
            const names = (user ? features.user : features.info).filter(name => distributionFeatureNumeric(name, user) === numeric);
            fill($("diff-dist-feature"), names.map(name => [name, name])); $("diff-dist-feature").disabled = !names.length;
        }
        view.heatRun.parentElement.hidden = !heatmap;
        view.bin.parentElement.hidden = view.plot.value !== "histogram";
        view.display.hidden = heatmap;
        view.orientation.parentElement.hidden = heatmap || !["categories", "shares", "histogram"].includes(view.plot.value);
    }
    function renderDistribution() {
        const view = views.dist; if (!view) return;
        distributionControls(); syncChartRail("dist");
        const heatmap = view.plot.value === "heatmap", numeric = ["histogram", "cdf", "exact"].includes(view.plot.value);
        const body = heatmap ? { type: "mixed", infoFeature: $("diff-dist-info").value, userFeature: $("diff-dist-user").value }
            : { type: $("diff-dist-type").value, feature: $("diff-dist-feature").value };
        const valid = heatmap ? body.infoFeature && body.userFeature : body.feature;
        $("diff-dist-hint").hidden = true;
        if (!state.diffusion.result || !valid) {
            view.ticket++; view.datasets = []; clearChart(view.chartId); resultRows(view, []);
            view.note.textContent = !state.diffusion.result ? "Run a simulation to analyse its distributions."
                : heatmap ? "Add an information-piece feature and a user feature to display a heatmap."
                : "No " + (numeric ? "numerical" : "categorical") + " features available for this distribution type.";
            return;
        }
        return load("dist", "/api/diffusion/distribution", body);
    }
    function drawDistribution(view) {
        const datasets = view.datasets, rows = [["Run", "Value", "Count"]], summary = [["Run", "Total weight", "Distinct values", "Mean", "Minimum", "Median", "Maximum"]];
        view.note.textContent = (view.errors || []).join("; ");
        const mixed = view.plot.value === "heatmap";
        distributionControls();
        syncChartRail("dist");
        view.style.disabled = mixed; view.logX.disabled = mixed; view.logY.disabled = mixed;
        if (!datasets.length) { clearChart(view.chartId); if (!view.note.textContent) view.note.textContent = "Select at least one run with available analysis."; resultRows(view, []); return; }
        if (mixed) {
            const selected = datasets.find(item => item.run.id === view.heatRun.value) || datasets[0];
            if (!selected) { clearChart(view.chartId); resultRows(view, []); return; }
            const data = [["Run", "Information value", "User value", "Count"]];
            for (const { run, response } of datasets) {
                if (response.points) for (const [column, row, count] of response.points) data.push([run.label, response.infoValues[row], response.userValues[column], count]);
                else for (let i = 0; i < response.infoValues.length; i++) for (let j = 0; j < response.userValues.length; j++) data.push([run.label, response.infoValues[i], response.userValues[j], response.matrix[i]?.[j] || 0]);
            }
            view.note.textContent += " Mixed features use categorical heatmaps. Choose a selected run above; CSV includes all observed pairs for every selected run. Unobserved pairs have count zero.";
            drawHeatmap(view.chartId, selected.response, color(selected.run)); resultRows(view, data); return;
        }
        const numeric = datasets.length > 0 && datasets.every(item => (item.response.values || []).length > 0 && item.response.values.every(value => String(value.value).trim() !== "" && Number.isFinite(Number(value.value))));
        const mode = view.plot.value;
        if (["histogram", "cdf", "exact"].includes(mode) && !numeric) { clearChart(view.chartId); view.note.textContent += " Select a numeric feature for this chart, or choose Category counts / shares."; resultRows(view, []); return; }
        const labels = [...new Set(datasets.flatMap(item => (item.response.values || []).map(value => String(value.value))))];
        const numericSets = datasets.map(item => {
            const counts = new Map();
            for (const itemValue of item.response.values || []) { const value = Number(itemValue.value); counts.set(value, (counts.get(value) || 0) + Number(itemValue.count)); }
            return [...counts].map(([value, count]) => ({ value, count })).sort((a, b) => a.value - b.value);
        });
        if (mode === "histogram") rows[0] = ["Run", "Bin start (inclusive)", "Bin end (exclusive)", "Count / weight"];
        else if (mode === "cdf") rows[0] = ["Run", "Value", "Cumulative share"];
        else if (mode === "shares") rows[0] = ["Run", "Value", "Count / weight", "Share (%)"];
        let binWidth = Number(view.bin.value), origin = 0, bins = 0;
        if (mode === "histogram" && numeric) {
            const values = numericSets.flatMap(items => items.map(item => item.value));
            if (!view.bin.value) binWidth = metricAnalysisBinSize([values]);
            if (!Number.isFinite(binWidth) || binWidth <= 0) { view.note.textContent = "Bin width must be a positive finite number."; clearChart(view.chartId); resultRows(view, []); return; }
            origin = values.reduce((lo, value) => Math.min(lo, value), Infinity);
            const max = values.reduce((hi, value) => Math.max(hi, value), -Infinity);
            bins = Math.floor((max - origin) / binWidth) + 1;
            if (!Number.isFinite(bins) || bins > 10000) { view.note.textContent = "This width produces more than 10,000 bins. Choose a larger bin width."; clearChart(view.chartId); resultRows(view, []); return; }
        }
        const series = datasets.map(({ run, response }, index) => {
            const values = response.values || [], total = values.reduce((sum, value) => sum + Number(value.count), 0), items = numericSets[index];
            let cumulative = 0, median = NaN, mean = 0;
            if (numeric && total) for (const item of items) { cumulative += item.count; mean += item.value * (item.count / total); if (!Number.isFinite(median) && cumulative >= total / 2) median = item.value; }
            summary.push([run.label, total, numeric ? items.length : values.length, numeric ? mean : "—", numeric ? items[0]?.value : "—", numeric ? median : "—", numeric ? items.at(-1)?.value : "—"]);
            let points;
            if (mode === "histogram") {
                const counts = Array(bins).fill(0);
                for (const item of items) counts[Math.min(bins - 1, Math.floor((item.value - origin) / binWidth))] += item.count;
                points = counts.map((count, i) => [origin + (i + 0.5) * binWidth, count]);
                counts.forEach((count, i) => rows.push([run.label, origin + i * binWidth, origin + (i + 1) * binWidth, count]));
            } else if (mode === "cdf") { let sum = 0; points = items.map(item => [item.value, total ? (sum += item.count) / total : 0]); }
            else if (mode === "exact") points = items.map(item => [item.value, item.count]);
            else { const counts = new Map(values.map(value => [String(value.value), value.count])); points = labels.map(label => [label, mode === "shares" ? total ? 100 * (counts.get(label) || 0) / total : 0 : counts.get(label) || 0]); }
            if (mode === "cdf" || mode === "exact") points.forEach(([value, count]) => rows.push([run.label, value, count]));
            else if (mode !== "histogram") values.forEach(value => rows.push(mode === "shares" ? [run.label, value.value, value.count, total ? 100 * value.count / total : 0] : [run.label, value.value, value.count]));
            return { name: run.label, points, color: color(run) };
        });
        view.note.textContent += " Counts follow RELISON's received-information distribution weights." + (mode === "histogram" ? " Bin width: " + fmt(binWidth) + "; intervals start at " + fmt(origin) + "." : "");
        const bar = ["categories", "shares", "histogram"].includes(mode), horizontal = bar && view.orientation.value === "horizontal";
        let category = ["categories", "shares"].includes(mode) ? labels : undefined;
        if (horizontal && mode === "histogram") {
            category = Array.from({ length: bins }, (_, i) => "[" + fmt(origin + i * binWidth) + ", " + fmt(origin + (i + 1) * binWidth) + ")");
            for (const item of series) item.points = item.points.map(([, count], i) => [category[i], count]);
        }
        plot(view, series, mode === "histogram" && horizontal ? "Bin interval" : "Feature value", mode === "cdf" ? "Cumulative share" : mode === "shares" ? "Share (%)" : "Count / weight", { category, bar, horizontal });
        resultRows(view, rows, summary);
    }
    return { get ready() { return ready; }, setup, reset, sync, addRun, restoreRuns, selectedRuns, renderOverview, renderMetrics, renderCompare, load, draw, rankings, isRanking, timelineControls, featureControls, distributionControls, renderDistribution };
})();
