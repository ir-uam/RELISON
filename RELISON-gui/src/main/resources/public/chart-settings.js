"use strict";

// Chart appearance and palette settings; redrawVisibleCharts is provided by app.js.

function applyChartStyle(style) {
    const valid = ["quiet-scientific", "vibrant-dashboard", "editorial", "glass-panel", "monochrome-accent"];
    const next = valid.includes(style) ? style : "quiet-scientific";
    document.body.dataset.chartStyle = next;
    const selector = $("chart-style-selector");
    if (selector) selector.value = next;
    try { localStorage.setItem("relison-chart-style", next); } catch (e) { /* ignore */ }
    redrawVisibleCharts();
}

function applyChartPalette(palette) {
    const valid = ["balanced", "vibrant", "editorial", "glass", "monochrome"];
    const next = valid.includes(palette) ? palette : "balanced";
    document.body.dataset.chartPalette = next;
    const selector = $("chart-palette-selector");
    if (selector) selector.value = next;
    try { localStorage.setItem("relison-chart-palette", next); } catch (e) { /* ignore */ }
    redrawVisibleCharts();
}

