"use strict";

// In-memory report documents are independent of graph/GUI state and .relison session serialization.
// Network snapshots are supplied by the workspace; metrics and file operations are connected separately.
const ReportNotebook = (() => {
    const types = {
        text: { label: "Text", icon: "notes", description: "Write headings, explanations and observations." },
        graph: { label: "Graph snapshot", icon: "hub", description: "Capture the current network view." },
        table: { label: "Top-k table", icon: "table_chart", description: "Rank nodes by a metric." },
        chart: { label: "Metric chart", icon: "bar_chart", description: "Configure a chart or distribution." },
    };
    const metrics = [["degree", "Degree"], ["pagerank", "PageRank"], ["betweenness", "Betweenness"],
        ["closeness", "Closeness"], ["clustering", "Local clustering coefficient"]];
    const orders = [["descending", "Highest first"], ["ascending", "Lowest first"], ["node", "Node ID"]];
    const reports = [];
    let nextReportId = 1, draggedCellId = null;
    let root, onExit, onCaptureGraph, report = null, selected = null, picker = false, preview = false, message = "", optionsLayout = "side";
    let leftCollapsed = false, rightCollapsed = false, panelResize = null;
    const panelWidths = { left: 170, right: 270 };
    const panelScroll = { left: 0, right: 0 };
    const textSelections = new Map();
    const textModes = new Map();
    let editingTextKey = null;
    const textKey = cell => `${report.id}:${cell.id}`;

    const escape = value => String(value ?? "").replace(/[&<>"']/g, ch =>
        ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch]);
    const icon = name => `<span class="material-symbols-outlined" aria-hidden="true">${name}</span>`;
    const labelOf = (options, value) => options.find(option => option[0] === value)?.[1] || value;
    const findCell = id => report?.cells.find(cell => cell.id === Number(id));
    const cellLabel = cell => `${report.cells.indexOf(cell) + 1}. ${types[cell.type].label}`;

    function createCell(type) {
        const options = {};
        if (type === "text") Object.assign(options, { text: "" });
        else {
            options.caption = "";
            if (type !== "graph") Object.assign(options, { metric: "degree", orientation: "total", damping: "0.85", iterations: "100", order: "descending",
                ...(type === "table" ? { k: "10", ranks: true } : { chart: "bar", axis: "vertical", bins: "10" }) });
        }
        return { id: report.nextId++, type, options, draft: { ...options } };
    }

    function startNew() {
        editingTextKey = null;
        report = { id: nextReportId++, title: "Untitled report", cells: [], nextId: 1, modified: false };
        reports.push(report);
        const cell = createCell("text");
        report.cells.push(cell);
        selected = cell.id;
        preview = false;
        picker = false;
        message = "New report. Add cells and configure their options.";
        renderEditor();
        root.querySelector("[data-report-title]").focus();
        root.querySelector("[data-report-title]").select();
    }

    function showSelection() {
        editingTextKey = null;
        stopPanelResize();
        resetDrag();
        picker = false;
        root.innerHTML = `<section class="report-selection" aria-labelledby="report-selection-title">
            <button type="button" class="report-back" data-report-action="exit">${icon("arrow_back")} Back to workspace</button>
            <div class="report-selection-heading">${icon("lab_profile")}<h1 id="report-selection-title" tabindex="-1">Reports</h1>
                <p>Build a notebook of text, network views, charts and tables.</p></div>
            <div class="report-choices">
                <button type="button" class="report-choice" data-report-action="load" disabled>
                    ${icon("upload_file")}<strong>Load report</strong><span>Open a saved report.</span><small>Coming later</small></button>
                <button type="button" class="report-choice" data-report-action="new">
                    ${icon("note_add")}<strong>New report</strong><span>Start a notebook and configure its cells.</span></button>
            </div>
            ${reports.length ? `<section class="report-open-reports" aria-labelledby="report-open-title"><h2 id="report-open-title">Open reports</h2>
                <p class="muted">Your reports are kept while this page is open.</p>
                <div class="report-open-list">${reports.map(document => `<button type="button" data-report-open="${document.id}">
                    ${icon("description")}<span><strong>${escape(document.title || "Untitled report")}</strong>
                    <small>${document.cells.length} ${document.cells.length === 1 ? "cell" : "cells"} · Unsaved draft${document === report ? " · Current report" : ""}</small></span>${icon("arrow_forward")}</button>`).join("")}</div></section>` : ""}
        </section>`;
    }

    function field(name, label, value, extra = "") {
        const type = extra.includes("type=") ? "" : 'type="text"';
        return `<label class="field"><span>${label}</span><input ${type} name="${name}" value="${escape(value)}" ${extra}></label>`;
    }
    function select(name, label, options, value, dynamic = false) {
        return `<label class="field"><span>${label}</span><select name="${name}" ${dynamic ? 'data-report-dynamic="true"' : ""}>
            ${options.map(([key, text]) => `<option value="${key}" ${key === value ? "selected" : ""}>${text}</option>`).join("")}</select></label>`;
    }
    function check(name, label, checked) {
        return `<label class="inline-check"><input type="checkbox" name="${name}" ${checked ? "checked" : ""}> ${label}</label>`;
    }

    function textToolbar(cell) {
        const enabled = cell?.type === "text", disabled = enabled ? "" : "disabled";
        const mode = enabled ? textModes.get(textKey(cell)) || "visual" : "visual";
        const tools = [["bold", "Bold (Ctrl+B)", "format_bold"], ["italic", "Italic (Ctrl+I)", "format_italic"],
            ["underline", "Underline (Ctrl+U)", "format_underlined"], ["strike", "Strikethrough", "strikethrough_s"],
            ["bullet", "Bulleted list", "format_list_bulleted"], ["numbered", "Numbered list", "format_list_numbered"],
            ["quote", "Block quote", "format_quote"], ["code", "Code", "code"], ["link", "Insert link", "link"]];
        const labels = captionLabels();
        return `<div class="report-format-bar" ${preview ? "hidden" : ""}><div class="report-text-toolbar" role="group" aria-label="Text formatting">
                <div class="report-text-modes" role="group" aria-label="Text editing mode">${["visual", "markdown"].map(value => `<button type="button" data-report-text-mode="${value}" aria-pressed="${mode === value}" ${disabled}>${value === "visual" ? "Visual" : "Markdown"}</button>`).join("")}</div>
                ${tools.map(([command, label, symbol]) => `<button type="button" data-report-format="${command}" aria-label="${label}" title="${label}" ${disabled}>${icon(symbol)}</button>`).join("")}
                <select data-report-format="heading" aria-label="Heading level" ${disabled}><option value="">Heading</option><option value="0">Paragraph</option>${[1, 2, 3, 4, 5, 6].map(level => `<option value="${level}">Heading ${level}</option>`).join("")}</select>
                <select data-report-format="font" aria-label="Font family" ${disabled}><option value="">Font</option>${ReportMarkdown.fonts.map(([value, label]) => `<option value="${value}">${label}</option>`).join("")}</select>
                <select data-report-format="size" aria-label="Font size" ${disabled}><option value="">Size</option>${[12, 14, 16, 18, 24, 32].map(size => `<option value="${size}">${size} px</option>`).join("")}</select>
                <select data-report-insert-reference aria-label="Insert caption reference" ${!enabled || !labels.size ? "disabled" : ""}><option value="">Insert reference</option>${report.cells.filter(item => labels.has(item.id)).map(item => `<option value="${item.id}">${escape(labels.get(item.id))}: ${escape(item.options.caption.trim())}</option>`).join("")}</select>
                <label class="report-text-colour"><span>Colour</span><input type="color" value="#4f9dff" data-report-format="colour" aria-label="Text colour" ${disabled}></label>
            </div></div>`;
    }

    function textEditor(cell) {
        const mode = textModes.get(textKey(cell)) || "visual";
        return `<div class="report-markdown-editor report-wide" data-report-text-cell="${cell.id}">
            <div class="report-rich-text report-markdown" contenteditable="true" role="textbox" aria-label="Cell text" aria-multiline="true" data-report-rich-text data-report-editor-location="cell" data-report-text-key="${textKey(cell)}:visual" data-placeholder="Write your text here…" spellcheck="true" ${mode !== "visual" ? "hidden" : ""}>${renderText(cell.draft.text)}</div>
            <textarea name="text" rows="7" data-report-markdown data-report-editor-location="cell" data-report-text-key="${textKey(cell)}:markdown" aria-label="Markdown source" spellcheck="true" placeholder="Write Markdown here…" ${mode !== "markdown" ? "hidden" : ""}>${escape(cell.draft.text)}</textarea>
        </div>`;
    }

    function setTextMode(mode) {
        const cell = findCell(selected);
        if (!cell || cell.type !== "text") return;
        textModes.set(textKey(cell), mode);
        if (editingTextKey !== textKey(cell)) { editText(cell); return; }
        const editor = root.querySelector(`[data-report-text-cell="${cell.id}"]`);
        const rich = editor.querySelector("[data-report-rich-text]"), source = editor.querySelector("[data-report-markdown]");
        const previousSelection = !rich.hidden && ReportRichText.capture(rich);
        if (previousSelection) textSelections.set(rich.dataset.reportTextKey, previousSelection);
        rich.hidden = mode !== "visual"; source.hidden = mode !== "markdown";
        if (mode === "visual") rich.innerHTML = renderText(cell.draft.text);
        root.querySelectorAll("[data-report-text-mode]").forEach(button => button.setAttribute("aria-pressed", String(button.dataset.reportTextMode === mode)));
        const target = mode === "visual" ? rich : source;
        target.focus({ preventScroll: true });
        if (mode === "visual") ReportRichText.restore(rich, textSelections.get(rich.dataset.reportTextKey));
    }

    function formatText(control, command, value) {
        const cell = findCell(selected);
        if (!cell || cell.type !== "text") return;
        if (editingTextKey !== textKey(cell)) editText(cell);
        const editor = root.querySelector(`[data-report-text-cell="${cell.id}"]`);
        const rich = editor?.querySelector("[data-report-rich-text]");
        if (rich && !rich.hidden) {
            ReportRichText.format(rich, command, value, ReportRichText.capture(rich) || textSelections.get(rich.dataset.reportTextKey));
            return;
        }
        const textarea = editor?.querySelector("[data-report-markdown]");
        if (!textarea) return;
        const scrollTop = textarea.scrollTop;
        const result = ReportMarkdown.format(textarea.value, textarea.selectionStart, textarea.selectionEnd, command, value);
        textarea.value = result.text;
        textarea.focus({ preventScroll: true });
        textarea.setSelectionRange(result.start, result.end);
        textarea.scrollTop = scrollTop;
        textarea.dispatchEvent(new Event("input", { bubbles: true }));
    }

    function optionsForm(cell) {
        const d = cell.draft;
        if (cell.type === "text") {
            return '<p class="muted">Edit text directly in the notebook. Choose Visual or Markdown in the top toolbar.</p>';
        }
        let fields = "";
        if (cell.type === "graph") {
            fields += '<p class="report-wide muted">Capture from the Network view. Adjust its layout, camera and appearance there. The captured image stays in this report until you replace it.</p>';
        } else {
            fields += select("metric", "Node metric", metrics, d.metric, true);
            if (d.metric === "degree") fields += select("orientation", "Degree orientation", [["total", "Total"], ["in", "Incoming"], ["out", "Outgoing"]], d.orientation);
            if (d.metric === "pagerank") {
                fields += field("damping", "Damping factor", d.damping, 'type="number" min="0" max="1" step="0.01" required');
                fields += field("iterations", "Maximum iterations", d.iterations, 'type="number" min="1" step="1" required');
            }
            if (cell.type === "table") {
                fields += field("k", "Number of nodes (k)", d.k, 'type="number" min="1" step="1" required');
                fields += select("order", "Order", orders, d.order);
                fields += check("ranks", "Show rank column", d.ranks);
            } else {
                fields += select("chart", "Chart type", [["bar", "Bar chart"], ["histogram", "Histogram"]], d.chart, true);
                if (d.chart === "histogram") fields += field("bins", "Number of bins", d.bins, 'type="number" min="1" step="1" required');
                else {
                    fields += select("order", "Node order", orders, d.order);
                    fields += select("axis", "Orientation", [["vertical", "Vertical"], ["horizontal", "Horizontal"]], d.axis);
                }
            }
        }
        fields += field("caption", "Caption (optional)", d.caption);
        return `<form class="report-cell-options" data-report-form="${cell.id}">
            <h3>Cell options · ${types[cell.type].label}</h3><div class="report-options-grid">${fields}</div>
            <div class="report-cell-actions"><button type="submit" class="primary">Apply options</button>
                <span class="muted">${cell.type === "graph" ? "Update the snapshot caption." : "Configuration only · output is not generated yet."}</span></div>
        </form>`;
    }

    function captionLabels() {
        let figures = 0, tables = 0;
        const labels = new Map();
        for (const cell of report.cells) {
            if (!cell.options.caption?.trim()) continue;
            if (cell.type === "table") labels.set(cell.id, `Table ${++tables}`);
            else if (cell.type === "graph" || cell.type === "chart") labels.set(cell.id, `Figure ${++figures}`);
        }
        return labels;
    }

    function renderText(source, labels = captionLabels()) {
        const container = document.createElement("div");
        container.innerHTML = ReportMarkdown.render(source);
        for (const link of container.querySelectorAll('a[href^="#relison-report-cell-"]')) {
            const match = link.getAttribute("href").match(/^#relison-report-cell-([1-9]\d*)$/);
            if (!match) continue;
            const id = Number(match[1]), label = labels.get(id);
            link.textContent = label || "Missing reference";
            link.setAttribute("data-report-reference", String(id));
            link.classList.toggle("report-reference-missing", !label);
            link.title = label ? `Go to ${label}` : "The caption is missing or its cell was deleted.";
            link.removeAttribute("target");
            link.removeAttribute("rel");
        }
        return container.innerHTML;
    }

    function insertReference(id) {
        const cell = findCell(selected), label = captionLabels().get(Number(id));
        if (!cell || cell.type !== "text" || !label) return;
        if (editingTextKey !== textKey(cell)) editText(cell);
        const editor = root.querySelector(`[data-report-text-cell="${cell.id}"]`);
        const rich = editor?.querySelector("[data-report-rich-text]");
        if (rich && !rich.hidden) {
            ReportRichText.insertReference(rich, Number(id), label, ReportRichText.capture(rich) || textSelections.get(rich.dataset.reportTextKey));
        } else {
            const textarea = editor?.querySelector("[data-report-markdown]");
            if (!textarea) return;
            const text = `[reference](#relison-report-cell-${Number(id)})`;
            textarea.setRangeText(text, textarea.selectionStart, textarea.selectionEnd, "end");
            textarea.focus({ preventScroll: true });
            textarea.dispatchEvent(new Event("input", { bubbles: true }));
        }
    }

    function caption(cell, labels) {
        const label = labels.get(cell.id);
        return label ? `<figcaption class="report-cell-caption"><strong>${label}.</strong> ${escape(cell.options.caption)}</figcaption>` : "";
    }

    function cellOutput(cell, labels) {
        const o = cell.options;
        if (cell.type === "text") {
            if (!o.text.trim()) return `<p class="report-empty-text muted">${preview ? "No text yet." : "Click here or choose Edit text to write."}</p>`;
            return `<div class="report-text-content report-markdown">${renderText(o.text, labels)}</div>`;
        }
        if (cell.type === "graph") return `<figure class="report-cell-figure">${cell.snapshot
            ? `<img class="report-graph-snapshot" src="${escape(cell.snapshot.dataUrl)}" width="${cell.snapshot.width}" height="${cell.snapshot.height}" alt="Captured network view">`
            : `<div class="report-output-placeholder">${icon("hub")}<span>No snapshot captured</span><small>Capture a view from the Network window.</small></div>`}
            ${caption(cell, labels)}</figure>`;
        let summary;
        if (cell.type === "table") summary = `${labelOf(metrics, o.metric)} · ${labelOf(orders, o.order)} · ${o.k} nodes`;
        else summary = `${labelOf(metrics, o.metric)} · ${o.chart === "histogram" ? `Histogram · ${o.bins} bins` : `Bar chart · ${o.axis}`}`;
        return `<figure class="report-cell-figure"><div class="report-output-placeholder">${icon(types[cell.type].icon)}<span>${types[cell.type].label} output</span>
            <small>${escape(summary)}</small><small>Not generated yet</small></div>
            ${caption(cell, labels)}</figure>`;
    }

    function cellBody(cell, labels) {
        if (cell.type === "graph" && !preview) return `${cellOutput(cell, labels)}<div class="report-cell-actions">
            <button type="button" class="primary" data-report-action="capture-graph" ${onCaptureGraph ? "" : "disabled"}>${icon("photo_camera")} ${cell.snapshot ? "Replace snapshot" : "Capture from network"}</button>
            <span class="muted">Opens Network; capture there to return to this cell.</span></div>`;
        if (cell.type !== "text" || preview) return cellOutput(cell, labels);
        if (editingTextKey === textKey(cell)) return `<div class="report-direct-text-editor">${textEditor(cell)}
            <div class="report-cell-actions"><button type="button" class="primary" data-report-action="done-text" aria-keyshortcuts="Control+Enter Meta+Enter">Done</button>
            <span class="muted">Use the top toolbar to format text or switch to Markdown. Ctrl+Enter to finish.</span></div></div>`;
        return `<div class="report-text-view" data-report-edit-text="${cell.id}" title="Click to edit text">${cellOutput(cell, labels)}</div>`;
    }

    function editText(cell) {
        if (!cell || cell.type !== "text" || preview) return;
        selected = cell.id;
        editingTextKey = textKey(cell);
        renderEditor();
        const target = root.querySelector(`[data-report-cell="${cell.id}"] [data-report-rich-text]:not([hidden]), [data-report-cell="${cell.id}"] [data-report-markdown]:not([hidden])`);
        target.focus({ preventScroll: true });
        if (target.matches("[data-report-rich-text]")) ReportRichText.restore(target, textSelections.get(target.dataset.reportTextKey));
        target.scrollIntoView({ block: "nearest" });
    }

    function finishText(cell) {
        if (!cell || cell.type !== "text") return;
        cell.options.text = cell.draft.text;
        report.modified = true;
        editingTextKey = null;
        message = "Text updated.";
        renderEditor();
        root.querySelector(`[data-report-cell="${cell.id}"] [data-report-action="edit-text"]`)?.focus({ preventScroll: true });
    }

    function renderEditor(focusName) {
        root.querySelectorAll("[data-report-markdown], [data-report-rich-text]").forEach(source => {
            const selection = source.matches("[data-report-rich-text]") ? ReportRichText.capture(source)
                : { start: source.selectionStart, end: source.selectionEnd };
            if (selection) textSelections.set(source.dataset.reportTextKey, { ...selection, top: source.scrollTop });
        });
        const scrollPositions = [[".report-outline .report-panel-content", "left"], [".report-document"], [".report-inspector .report-panel-content", "right"]].map(([selector, side]) => {
            const element = root.querySelector(selector);
            const top = element && !element.hidden ? element.scrollTop : side ? panelScroll[side] : 0;
            if (side) panelScroll[side] = top;
            return { selector, top };
        });
        stopPanelResize();
        resetDrag();
        report.selectedCellId = selected;
        const current = findCell(selected);
        const labels = captionLabels();
        root.innerHTML = `<section class="report-editor ${preview ? "report-preview" : ""}" data-options-layout="${optionsLayout}" data-left-collapsed="${leftCollapsed}" data-right-collapsed="${rightCollapsed}" style="--report-outline-width:${panelWidths.left}px;--report-inspector-width:${panelWidths.right}px" aria-label="Report notebook">
            <header class="report-editor-toolbar"><div class="report-editor-heading">
                <button type="button" class="report-back" data-report-action="selection">${icon("arrow_back")} Reports</button>
                <h1>Report editor</h1><span class="muted">Draft · not saved to a file</span></div>
                <div class="report-toolbar-actions"><button type="button" data-report-action="preview" aria-pressed="${preview}">${icon(preview ? "edit" : "visibility")}${preview ? "Edit report" : "Preview"}</button>
                    <button type="button" disabled title="Saving reports will be added later">Save report</button>
                    <button type="button" data-report-action="export">Export PDF</button>
                    <button type="button" disabled title="Cell execution will be added later">${icon("refresh")} Rerun all</button></div>
                ${textToolbar(current)}</header>
            <div class="report-context"><span>Graph snapshots from Network</span><span class="muted">Tables and charts: cell options only</span>
                <label class="report-options-position" ${preview ? "hidden" : ""}>Cell options <select data-report-options-layout aria-label="Cell options position"><option value="side" ${optionsLayout === "side" ? "selected" : ""}>Side panel</option><option value="inline" ${optionsLayout === "inline" ? "selected" : ""}>Inline</option></select></label></div>
            <div class="report-notebook-layout">
                <aside class="report-outline ${leftCollapsed ? "report-panel-collapsed" : ""}" aria-label="Report contents">
                    <div class="report-panel-heading"><h2>Contents</h2><button type="button" data-report-action="toggle-left" aria-controls="report-outline-content" aria-expanded="${!leftCollapsed}" aria-label="${leftCollapsed ? "Expand" : "Collapse"} contents panel" title="${leftCollapsed ? "Expand" : "Collapse"} contents panel">${icon(leftCollapsed ? "chevron_right" : "chevron_left")}</button></div>
                    <span class="report-panel-collapsed-label" aria-hidden="true">Contents</span>
                    <div id="report-outline-content" class="report-panel-content" ${leftCollapsed ? "hidden" : ""}>
                    ${report.cells.map(cell => `<button type="button" data-report-select="${cell.id}" aria-pressed="${cell.id === selected}">${cellLabel(cell)}</button>`).join("")}
                    <button type="button" class="report-add" data-report-action="add" aria-expanded="${picker}" aria-controls="report-cell-picker">${icon("add")} Add cell</button></div>
                    ${leftCollapsed ? "" : panelDivider("left")}</aside>
                <div class="report-document"><div class="report-paper">
                    ${preview ? `<h2 class="report-document-title">${escape(report.title || "Untitled report")}</h2>` : `<label class="report-title-label"><span class="report-sr-only">Report title</span><input class="report-document-title" data-report-title value="${escape(report.title)}" maxlength="160" placeholder="Untitled report"></label>`}
                    <p class="report-document-meta muted">Analysis report · ${report.cells.length} ${report.cells.length === 1 ? "cell" : "cells"}</p>
                    ${report.cells.map((cell, i) => `<article class="report-cell" id="relison-report-cell-${cell.id}" tabindex="-1" data-report-cell="${cell.id}" data-selected="${cell.id === selected}" aria-label="${cellLabel(cell)}">
                        <button type="button" class="report-cell-drag-handle" draggable="true" data-report-drag="${cell.id}" ${preview ? "hidden" : ""} ${report.cells.length < 2 ? "disabled" : ""} aria-label="Drag to reorder ${cellLabel(cell)}" title="Drag to reorder; the arrow buttons also move this cell">${icon("drag_indicator")}</button>
                        <div class="report-cell-heading"><button type="button" data-report-select="${cell.id}" aria-pressed="${cell.id === selected}">${icon(types[cell.type].icon)} ${i + 1} / ${types[cell.type].label}</button>
                            <div class="report-cell-tools">${cell.type === "text" ? `<button type="button" data-report-action="edit-text" aria-label="Edit text in ${cellLabel(cell)}">${icon("edit")} Edit text</button>` : ""}
                                <button type="button" data-report-action="up" ${i === 0 ? "disabled" : ""} aria-label="Move ${cellLabel(cell)} up">${icon("arrow_upward")}</button>
                                <button type="button" data-report-action="down" ${i === report.cells.length - 1 ? "disabled" : ""} aria-label="Move ${cellLabel(cell)} down">${icon("arrow_downward")}</button>
                                <button type="button" data-report-action="duplicate" aria-label="Duplicate ${cellLabel(cell)}">${icon("content_copy")}</button>
                                <button type="button" data-report-action="delete" aria-label="Delete ${cellLabel(cell)}">${icon("delete")}</button></div></div>
                        ${cellBody(cell, labels)}
                        ${optionsLayout === "inline" && cell.id === selected && !preview ? optionsForm(cell) : ""}
                    </article>`).join("")}
                    ${!report.cells.length ? '<p class="report-empty-text muted">Your report is empty. Add a cell to get started.</p>' : ""}
                    <div class="report-insert"><button type="button" data-report-action="add" aria-expanded="${picker}" aria-controls="report-cell-picker">${icon("add")} Add cell</button></div>
                    <section id="report-cell-picker" class="report-cell-picker" aria-labelledby="report-cell-picker-title" ${picker && !preview ? "" : "hidden"}>
                        <h2 id="report-cell-picker-title">Add cell</h2><div class="report-cell-types">${Object.entries(types).map(([type, def]) => `<button type="button" data-report-add="${type}">${icon(def.icon)}<strong>${def.label}</strong><span>${def.description}</span></button>`).join("")}</div>
                        <button type="button" class="report-back" data-report-action="cancel-add">Cancel</button></section>
                </div></div>
                ${optionsLayout === "side" && !preview ? `<aside class="report-inspector ${rightCollapsed ? "report-panel-collapsed" : ""}" aria-label="Cell options">
                    <div class="report-panel-heading"><h2>Cell options</h2><button type="button" data-report-action="toggle-right" aria-controls="report-inspector-content" aria-expanded="${!rightCollapsed}" aria-label="${rightCollapsed ? "Expand" : "Collapse"} cell options panel" title="${rightCollapsed ? "Expand" : "Collapse"} cell options panel">${icon(rightCollapsed ? "chevron_left" : "chevron_right")}</button></div>
                    <span class="report-panel-collapsed-label" aria-hidden="true">Cell options</span>
                    <div id="report-inspector-content" class="report-panel-content" ${rightCollapsed ? "hidden" : ""}>${current ? optionsForm(current) : '<p class="muted">Add a cell to get started.</p>'}</div>
                    ${rightCollapsed ? "" : panelDivider("right")}</aside>` : ""}
            </div><p class="report-editor-message" role="status">${escape(message)}</p>
            <dialog class="report-export-dialog" aria-labelledby="report-export-title" aria-describedby="report-export-description">
                <h2 id="report-export-title">Export report as PDF</h2>
                <p id="report-export-description">Choose the page size and orientation for this report. PDF text uses the bundled Roboto fonts.</p>
                <label class="field"><span>Page size</span><select data-report-pdf-size>
                    <option value="A4">A4 (210 × 297 mm)</option><option value="LETTER">Letter (8.5 × 11 in)</option>
                    <option value="LEGAL">Legal (8.5 × 14 in)</option><option value="A3">A3 (297 × 420 mm)</option><option value="A5">A5 (148 × 210 mm)</option>
                </select></label>
                <label class="field"><span>Orientation</span><select data-report-pdf-orientation><option value="portrait">Portrait</option><option value="landscape">Landscape</option></select></label>
                <p class="report-export-status" role="status"></p>
                <div class="report-export-actions"><button type="button" data-report-action="cancel-export">Cancel</button>
                    <button type="button" class="primary" data-report-action="download-pdf">Download PDF</button></div>
            </dialog>
        </section>`;
        for (const position of scrollPositions) {
            const element = root.querySelector(position.selector);
            if (element) element.scrollTop = position.top;
        }
        syncPanelSizes();
        root.querySelectorAll("[data-report-markdown], [data-report-rich-text]").forEach(element => {
            const selection = textSelections.get(element.dataset.reportTextKey);
            if (!selection) return;
            if (element.matches("[data-report-rich-text]")) { if (!element.hidden) ReportRichText.restore(element, selection); }
            else element.setSelectionRange(selection.start, selection.end);
            element.scrollTop = selection.top || 0;
        });
        if (focusName) root.querySelector(`[name="${focusName}"]`)?.focus();
    }

    function panelDivider(side) {
        return `<div class="report-panel-divider" data-report-resize="${side}" role="separator" tabindex="0" aria-orientation="vertical" aria-label="Resize ${side === "left" ? "contents" : "cell options"} panel" aria-valuemin="${side === "left" ? 140 : 220}" aria-valuemax="${side === "left" ? 420 : 560}" aria-valuenow="${panelWidths[side]}" title="Drag to resize; use Left and Right arrows with the keyboard"></div>`;
    }

    function panelLimits(side) {
        const layout = root.querySelector(".report-notebook-layout");
        const other = root.querySelector(side === "left" ? ".report-inspector" : ".report-outline");
        const min = side === "left" ? 140 : 220;
        const max = Math.max(min, Math.min(side === "left" ? 420 : 560,
            Math.floor(window.innerWidth * (side === "left" ? 0.3 : 0.35)),
            (layout?.clientWidth || 0) - (other?.getBoundingClientRect().width || 0) - 280));
        return { min, max };
    }

    function syncPanelSizes() {
        root.querySelectorAll("[data-report-resize]").forEach(divider => {
            const side = divider.dataset.reportResize, limits = panelLimits(side);
            const width = Math.round(divider.parentElement.getBoundingClientRect().width);
            divider.setAttribute("aria-valuemin", limits.min);
            divider.setAttribute("aria-valuemax", limits.max);
            divider.setAttribute("aria-valuenow", width);
            divider.setAttribute("aria-valuetext", `${width} pixels`);
        });
    }

    function setPanelWidth(side, value) {
        const limits = panelLimits(side);
        panelWidths[side] = Math.round(Math.max(limits.min, Math.min(limits.max, value)));
        root.querySelector(".report-editor").style.setProperty(side === "left" ? "--report-outline-width" : "--report-inspector-width", `${panelWidths[side]}px`);
        syncPanelSizes();
    }

    function stopPanelResize() {
        const resize = panelResize;
        panelResize = null;
        root?.classList.remove("report-panel-resizing");
        if (resize?.divider.hasPointerCapture(resize.pointerId)) resize.divider.releasePointerCapture(resize.pointerId);
    }

    function selectCell(id) {
        if (selected !== Number(id)) editingTextKey = null;
        selected = Number(id);
        renderEditor();
        root.querySelector(`[data-report-cell="${selected}"]`)?.scrollIntoView({ block: "nearest", behavior: "instant" });
    }

    function clearDropIndicator() {
        root?.querySelectorAll(".report-drop-before, .report-drop-after").forEach(element =>
            element.classList.remove("report-drop-before", "report-drop-after"));
    }

    function resetDrag() {
        draggedCellId = null;
        clearDropIndicator();
        root?.querySelectorAll(".report-cell-dragging").forEach(element => element.classList.remove("report-cell-dragging"));
    }

    // Use the pointer's position between cell centres, including the space above/below the document.
    // The outline is a compact drop target as well, useful when a report contains tall cells.
    function dropPosition(event) {
        const container = event.target.closest(".report-paper, .report-document, .report-outline");
        if (!container || !root.contains(container)) return null;
        const outline = container.classList.contains("report-outline");
        if (outline && leftCollapsed) return null;
        const elements = [...container.querySelectorAll(outline ? "[data-report-select]" : "[data-report-cell]")]
            .filter(element => Number(outline ? element.dataset.reportSelect : element.dataset.reportCell) !== draggedCellId);
        if (!elements.length) return null;
        const next = elements.find(element => {
            const bounds = element.getBoundingClientRect();
            return event.clientY < bounds.top + bounds.height / 2;
        });
        return { beforeId: next ? Number(outline ? next.dataset.reportSelect : next.dataset.reportCell) : null,
            indicator: next || elements.at(-1), after: !next };
    }

    function onDragStart(event) {
        const handle = event.target.closest("[data-report-drag]");
        if (!handle || handle.disabled || preview || !event.dataTransfer) return;
        const cell = findCell(handle.dataset.reportDrag);
        if (!cell) return;
        draggedCellId = cell.id;
        event.dataTransfer.effectAllowed = "move";
        // A private type prevents dropping a handle from inserting IDs into editable text fields.
        event.dataTransfer.setData("application/x-relison-report-cell", String(cell.id));
        const element = handle.closest("[data-report-cell]");
        const bounds = element.getBoundingClientRect();
        event.dataTransfer.setDragImage(element, Math.max(0, event.clientX - bounds.left), Math.max(0, event.clientY - bounds.top));
        element.classList.add("report-cell-dragging");
    }

    function onDragOver(event) {
        if (draggedCellId == null || preview) return;
        const position = dropPosition(event);
        clearDropIndicator();
        if (!position) return;
        event.preventDefault();
        event.dataTransfer.dropEffect = "move";
        position.indicator.classList.add(position.after ? "report-drop-after" : "report-drop-before");
    }

    function onDrop(event) {
        const rich = event.target.closest("[data-report-rich-text]");
        if (rich && draggedCellId == null) {
            event.preventDefault();
            ReportRichText.insert(rich, event.dataTransfer?.getData("text/html"), event.dataTransfer?.getData("text/plain"));
            return;
        }
        if (draggedCellId == null || preview) return;
        const position = dropPosition(event), cell = findCell(draggedCellId);
        if (!position || !cell) { resetDrag(); return; }
        event.preventDefault();
        const from = report.cells.indexOf(cell);
        const reordered = report.cells.filter(candidate => candidate !== cell);
        const to = position.beforeId == null ? reordered.length : reordered.findIndex(candidate => candidate.id === position.beforeId);
        resetDrag();
        if (to < 0 || from === to) return;
        reordered.splice(to, 0, cell);
        report.cells = reordered;
        report.modified = true;
        selected = cell.id;
        message = `Moved ${types[cell.type].label} cell to position ${to + 1}.`;
        selectCell(cell.id);
        root.querySelector(`[data-report-drag="${cell.id}"]`)?.focus({ preventScroll: true });
    }

    function onClick(event) {
        const reference = event.target.closest("[data-report-reference]");
        if (reference && root.contains(reference)) {
            event.preventDefault();
            if (!reference.closest("[data-report-rich-text]") && !reference.classList.contains("report-reference-missing")) {
                const target = root.querySelector(`[data-report-cell="${reference.dataset.reportReference}"]`);
                target?.scrollIntoView({ block: "center" });
                target?.focus({ preventScroll: true });
            }
            return;
        }
        const textView = event.target.closest("[data-report-edit-text]");
        if (textView && !event.target.closest("a")) { editText(findCell(textView.dataset.reportEditText)); return; }
        const button = event.target.closest("button");
        if (!button || button.disabled) return;
        if (button.dataset.reportTextMode) { setTextMode(button.dataset.reportTextMode); return; }
        if (button.dataset.reportFormat) { formatText(button, button.dataset.reportFormat); return; }
        if (button.dataset.reportOpen) {
            editingTextKey = null;
            report = reports.find(document => document.id === Number(button.dataset.reportOpen));
            selected = findCell(report.selectedCellId)?.id ?? report.cells[0]?.id ?? null;
            preview = false;
            picker = false;
            message = "Report reopened.";
            renderEditor();
            root.querySelector("[data-report-title]").focus();
            return;
        }
        if (button.dataset.reportSelect) { selectCell(button.dataset.reportSelect); return; }
        if (button.dataset.reportAdd) {
            const cell = createCell(button.dataset.reportAdd);
            report.cells.push(cell);
            report.modified = true;
            picker = false;
            message = `${types[cell.type].label} cell added.`;
            selectCell(cell.id);
            root.querySelector('[data-report-form] input, [data-report-form] select')?.focus();
            return;
        }
        const action = button.dataset.reportAction;
        if (!action) return;
        if (action === "exit") { onExit?.(); return; }
        if (action === "new") { startNew(); return; }
        if (action === "toggle-left" || action === "toggle-right") {
            if (action === "toggle-left") leftCollapsed = !leftCollapsed;
            else rightCollapsed = !rightCollapsed;
            renderEditor();
            root.querySelector(`[data-report-action="${action}"]`).focus({ preventScroll: true });
            return;
        }
        if (action === "selection") { showSelection(); root.querySelector("h1").focus(); return; }
        if (action === "export") { root.querySelector(".report-export-dialog").showModal(); return; }
        if (action === "cancel-export") { root.querySelector(".report-export-dialog").close(); return; }
        if (action === "download-pdf") { exportPdf(); return; }
        if (action === "preview") { preview = !preview; picker = false; renderEditor(); root.querySelector('[data-report-action="preview"]').focus(); return; }
        if (action === "add" || action === "cancel-add") {
            picker = action === "add" ? !picker : false;
            renderEditor();
            const target = root.querySelector(picker ? "[data-report-add]" : ".report-insert button");
            target.focus();
            if (picker) target.scrollIntoView({ block: "nearest" });
            return;
        }
        const cell = findCell(button.closest("[data-report-cell]")?.dataset.reportCell);
        if (!cell) return;
        if (action === "capture-graph") {
            onCaptureGraph?.({ reportId: report.id, cellId: cell.id, reportTitle: report.title });
            return;
        }
        if (action === "edit-text") { editText(cell); return; }
        if (action === "done-text") { finishText(cell); return; }
        const index = report.cells.indexOf(cell);
        if (action === "up" && index > 0) [report.cells[index - 1], report.cells[index]] = [cell, report.cells[index - 1]];
        else if (action === "down" && index < report.cells.length - 1) [report.cells[index + 1], report.cells[index]] = [cell, report.cells[index + 1]];
        else if (action === "duplicate") {
            const copy = { id: report.nextId++, type: cell.type, options: { ...cell.options }, draft: { ...cell.draft },
                ...(cell.snapshot ? { snapshot: { ...cell.snapshot } } : {}) };
            report.cells.splice(index + 1, 0, copy);
            selected = copy.id;
        } else if (action === "delete") {
            report.cells.splice(index, 1);
            if (selected === cell.id) selected = report.cells[Math.min(index, report.cells.length - 1)]?.id ?? null;
        } else return;
        editingTextKey = null;
        report.modified = true;
        message = action === "delete" ? "Cell deleted." : action === "duplicate" ? "Cell duplicated." : "Cell moved.";
        renderEditor();
        const target = root.querySelector(`[data-report-cell="${selected}"] [data-report-select]`) || root.querySelector(".report-insert button");
        target.focus();
    }

    async function exportPdf() {
        const dialog = root.querySelector(".report-export-dialog"), status = dialog.querySelector(".report-export-status");
        const buttons = [...dialog.querySelectorAll("button")], labels = captionLabels();
        const model = { title: report.title, cells: report.cells.map(cell => ({ id: cell.id, type: cell.type,
            ...(cell.type === "text" ? { html: renderText(cell.options.text, labels) } : {
                label: labels.get(cell.id), caption: cell.options.caption,
                snapshot: cell.snapshot?.dataUrl,
                placeholder: cell.type === "graph" ? "No snapshot captured." : `${types[cell.type].label} output is not generated yet.`,
            }) })) };
        buttons.forEach(button => { button.disabled = true; });
        status.textContent = "Preparing PDF…";
        try {
            const { unavailableImages } = await ReportPdf.exportPdf(model, dialog.querySelector("[data-report-pdf-size]").value,
                dialog.querySelector("[data-report-pdf-orientation]").value);
            message = unavailableImages ? `PDF downloaded. ${unavailableImages} image(s) could not be included.` : "PDF downloaded.";
            if (dialog.isConnected) { dialog.close(); root.querySelector(".report-editor-message").textContent = message; }
        } catch (error) {
            if (dialog.isConnected) status.textContent = `Could not export PDF: ${error.message}`;
        } finally {
            buttons.forEach(button => { button.disabled = false; });
        }
    }

    function updateText(cell, value, input) {
        cell.draft.text = value;
        if (editingTextKey === textKey(cell)) cell.options.text = value;
        const rendered = renderText(value);
        root.querySelectorAll(`[data-report-text-cell="${cell.id}"]`).forEach(editor => {
            const source = editor.querySelector("[data-report-markdown]"), rich = editor.querySelector("[data-report-rich-text]");
            if (source && source !== input && source.value !== value) source.value = value;
            if (rich && rich !== input) rich.innerHTML = rendered;
        });
        report.modified = true;
    }

    function onInput(event) {
        const input = event.target;
        if (input.matches("[data-report-title]")) {
            report.title = input.value;
            report.modified = true;
            return;
        }
        const cell = findCell(input.closest("[data-report-form]")?.dataset.reportForm
            || input.closest("[data-report-text-cell]")?.dataset.reportTextCell);
        if (cell && input.matches("[data-report-rich-text]")) {
            if (!event.isComposing) {
                const selection = ReportRichText.capture(input);
                if (selection) textSelections.set(input.dataset.reportTextKey, selection);
                updateText(cell, ReportMarkdown.fromHtml(input.innerHTML), input);
            }
            return;
        }
        if (!cell || !input.name) return;
        cell.draft[input.name] = input.type === "checkbox" ? input.checked : input.value;
        if (input.matches("[data-report-markdown]")) {
            updateText(cell, input.value, input);
        }
        report.modified = true;
    }

    function init(element, options = {}) {
        root = element;
        onExit = options.onExit;
        onCaptureGraph = options.onCaptureGraph;
        root.addEventListener("click", onClick);
        root.addEventListener("input", onInput);
        root.addEventListener("compositionend", onInput);
        document.addEventListener("selectionchange", () => {
            const rich = root.querySelector("[data-report-rich-text]:not([hidden])");
            const selection = rich && ReportRichText.capture(rich);
            if (selection) textSelections.set(rich.dataset.reportTextKey, selection);
        });
        root.addEventListener("paste", event => {
            const rich = event.target.closest("[data-report-rich-text]");
            if (!rich) return;
            event.preventDefault();
            ReportRichText.insert(rich, event.clipboardData?.getData("text/html"), event.clipboardData?.getData("text/plain"));
        });
        root.addEventListener("pointerdown", event => {
            if (event.target.closest("button[data-report-format]")) { event.preventDefault(); return; }
            const divider = event.target.closest("[data-report-resize]");
            if (!divider || event.button !== 0) return;
            event.preventDefault();
            divider.focus({ preventScroll: true });
            panelResize = { side: divider.dataset.reportResize, divider, pointerId: event.pointerId,
                x: event.clientX, width: divider.parentElement.getBoundingClientRect().width };
            divider.setPointerCapture(event.pointerId);
            root.classList.add("report-panel-resizing");
        });
        root.addEventListener("pointermove", event => {
            if (!panelResize || event.pointerId !== panelResize.pointerId) return;
            const delta = (event.clientX - panelResize.x) * (panelResize.side === "left" ? 1 : -1);
            setPanelWidth(panelResize.side, panelResize.width + delta);
        });
        for (const type of ["pointerup", "pointercancel", "lostpointercapture"]) root.addEventListener(type, event => {
            if (panelResize?.pointerId === event.pointerId) stopPanelResize();
        });
        root.addEventListener("keydown", event => {
            if (event.target.matches('[data-report-editor-location="cell"]') && event.key === "Enter" && (event.ctrlKey || event.metaKey)) {
                event.preventDefault(); event.stopPropagation();
                finishText(findCell(event.target.closest("[data-report-text-cell]").dataset.reportTextCell));
                return;
            }
            if (event.target.matches("[data-report-markdown], [data-report-rich-text]") && (event.ctrlKey || event.metaKey) && !event.altKey) {
                const command = { b: "bold", i: "italic", u: "underline" }[event.key.toLowerCase()];
                if (command) { event.preventDefault(); event.stopPropagation(); formatText(event.target, command); return; }
            }
            const divider = event.target.closest("[data-report-resize]");
            if (!divider || !["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key)) return;
            event.preventDefault();
            event.stopPropagation();
            const side = divider.dataset.reportResize, limits = panelLimits(side);
            const delta = (event.key === "ArrowRight" ? 16 : -16) * (side === "left" ? 1 : -1);
            setPanelWidth(side, event.key === "Home" ? limits.min : event.key === "End" ? limits.max
                : divider.parentElement.getBoundingClientRect().width + delta);
        });
        window.addEventListener("resize", syncPanelSizes);
        root.addEventListener("dragstart", onDragStart);
        root.addEventListener("dragover", onDragOver);
        root.addEventListener("drop", onDrop);
        root.addEventListener("dragend", resetDrag);
        root.addEventListener("dragleave", event => {
            if (!root.contains(event.relatedTarget)) clearDropIndicator();
        });
        root.addEventListener("change", event => {
            if (event.target.matches("[data-report-insert-reference]")) {
                const value = event.target.value;
                event.target.value = "";
                if (value) insertReference(value);
                return;
            }
            if (event.target.dataset.reportFormat) {
                if (event.target.value !== "") formatText(event.target, event.target.dataset.reportFormat, event.target.value);
                if (event.target.tagName === "SELECT") event.target.value = "";
                return;
            }
            if (event.target.matches("[data-report-options-layout]")) {
                optionsLayout = event.target.value;
                renderEditor();
                root.querySelector("[data-report-options-layout]").focus();
                return;
            }
            onInput(event);
            if (event.target.dataset.reportDynamic) renderEditor(event.target.name);
        });
        root.addEventListener("submit", event => {
            const form = event.target.closest("[data-report-form]");
            if (!form) return;
            event.preventDefault();
            if (!form.reportValidity()) return;
            const cell = findCell(form.dataset.reportForm);
            cell.options = { ...cell.draft };
            cell.draft = { ...cell.options };
            report.modified = true;
            message = "Cell options applied.";
            renderEditor();
            root.querySelector("[data-report-form] button[type=submit]").focus();
        });
        showSelection();
    }

    function setGraphSnapshot(target, snapshot) {
        const document = reports.find(item => item.id === target.reportId);
        const cell = document?.cells.find(item => item.id === target.cellId && item.type === "graph");
        if (!cell || !/^data:image\/png;base64,[A-Za-z0-9+/=]+$/.test(snapshot?.dataUrl)
            || !Number.isInteger(snapshot.width) || snapshot.width <= 0 || !Number.isInteger(snapshot.height) || snapshot.height <= 0) return false;
        cell.snapshot = { dataUrl: snapshot.dataUrl, width: snapshot.width, height: snapshot.height,
            capturedAt: snapshot.capturedAt, renderer: snapshot.renderer };
        document.modified = true;
        return true;
    }

    function returnToCell(target) {
        const document = reports.find(item => item.id === target.reportId);
        if (!document) return false;
        report = document;
        selected = findCell(target.cellId)?.id ?? report.cells[0]?.id ?? null;
        editingTextKey = null;
        picker = false;
        preview = false;
        message = "Returned from Network.";
        renderEditor();
        const cell = root.querySelector(`[data-report-cell="${selected}"]`);
        cell?.scrollIntoView({ block: "nearest" });
        cell?.querySelector('[data-report-action="capture-graph"]')?.focus({ preventScroll: true });
        return true;
    }

    return { init, showSelection, setGraphSnapshot, returnToCell };
})();
