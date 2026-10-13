"use strict";

let CHART_PNG_REQUEST = null;
const CHART_PNG_PIXEL_RATIO = 2;

function chartPngAxisScale(axis, zooms, coordinate) {
    const count = axis.data?.length || 0;
    let scale = 1;
    for (const zoom of zooms) {
        if (!categoryZoomTargets(zoom)[coordinate]) continue;
        const start = Number(zoom.start), end = Number(zoom.end);
        if (axis.type === "category" && count) {
            const categoryIndex = (value, fallback) => {
                if (value == null) return fallback;
                if (typeof value === "number") return value;
                const index = axis.data.findIndex(item => String(item?.value ?? item) === String(value));
                return index < 0 ? fallback : index;
            };
            const lo = categoryIndex(zoom.startValue, Number.isFinite(start) ? Math.round(start * (count - 1) / 100) : 0);
            const hi = categoryIndex(zoom.endValue, Number.isFinite(end) ? Math.round(end * (count - 1) / 100) : count - 1);
            const bands = axis.boundaryGap !== false;
            const full = bands ? count : Math.max(1, count - 1);
            const visible = bands ? Math.max(1, hi - lo + 1) : Math.max(1, hi - lo);
            scale = Math.max(scale, full / visible);
        } else if (Number.isFinite(start) && Number.isFinite(end) && end > start) {
            scale = Math.max(scale, 100 / (end - start));
        }
    }
    return scale;
}

function fullChartPngPlan(chart) {
    const option = chart.getOption();
    const grid = Array.isArray(option.grid) ? option.grid[0] : option.grid || {};
    const width = chart.getWidth(), height = chart.getHeight();
    const margin = (value, dimension) => typeof value === "string" && value.endsWith("%")
        ? parseFloat(value) * dimension / 100 : Number(value) || 0;
    const left = margin(grid.left, width), right = margin(grid.right, width);
    const top = margin(grid.top, height), bottom = margin(grid.bottom, height);
    const axes = { x: option.xAxis?.[0] || {}, y: option.yAxis?.[0] || {} };
    const scaleX = chartPngAxisScale(axes.x, option.dataZoom || [], "x");
    const scaleY = chartPngAxisScale(axes.y, option.dataZoom || [], "y");
    const plotWidth = Math.max(1, width - left - right), plotHeight = Math.max(1, height - top - bottom);
    const exportWidth = Math.ceil(left + right + plotWidth * scaleX);
    const exportHeight = Math.ceil(top + bottom + plotHeight * scaleY);
    option.grid = [{ ...grid, left, right, top, bottom }];
    option.dataZoom = [];
    option.toolbox = [];
    option.animation = false;
    // Use the full source data while retaining current overlays and legend selections.
    option.series = (option.series || []).map((series, i) => ({ ...series,
        data: chart.__relisonOption?.series?.[i]?.data ?? series.data, animation: false, progressive: 0 }));
    for (const coordinate of ["x", "y"]) {
        const scale = coordinate === "x" ? scaleX : scaleY;
        const size = coordinate === "x" ? plotWidth * scale : plotHeight * scale;
        option[coordinate + "Axis"] = (option[coordinate + "Axis"] || []).map(axis => ({ ...axis,
            nameTextStyle: { ...axis.nameTextStyle, width: size },
            ...(axis.type !== "category" && axis.interval == null
                ? { splitNumber: Math.max(2, Math.round((axis.splitNumber || 5) * scale)) } : {}) }));
    }
    const pixelWidth = exportWidth * CHART_PNG_PIXEL_RATIO, pixelHeight = exportHeight * CHART_PNG_PIXEL_RATIO;
    return { option, width: exportWidth, height: exportHeight, pixelWidth, pixelHeight,
        tooLarge: pixelWidth > 32767 || pixelHeight > 32767 || pixelWidth * pixelHeight > 64000000 };
}

function downloadChartPng(chartId, filename) {
    const chart = typeof chartId === "string" ? ECHARTS.get(chartId)
        : (chartId && typeof chartId.getDataURL === "function" ? chartId : ECHARTS.get(chartId?.id));
    if (!chart || chart.isDisposed() || !chart.getWidth() || !chart.getHeight()) {
        setStatus("Nothing to download yet — draw the chart first.", "error"); return;
    }
    CHART_PNG_REQUEST = { chart, filename: filename || "chart.png" };
    const plan = fullChartPngPlan(chart);
    $("chart-png-current-size").textContent = Math.round(chart.getWidth() * CHART_PNG_PIXEL_RATIO) + " × "
        + Math.round(chart.getHeight() * CHART_PNG_PIXEL_RATIO) + " pixels";
    $("chart-png-full-size").textContent = plan.pixelWidth + " × " + plan.pixelHeight + " pixels";
    $("chart-png-full").disabled = plan.tooLarge;
    $("chart-png-limit").hidden = !plan.tooLarge;
    $("chart-png-dialog").showModal();
}

async function exportChartPng(mode) {
    const request = CHART_PNG_REQUEST;
    $("chart-png-dialog").close();
    CHART_PNG_REQUEST = null;
    if (!request || request.chart.isDisposed()) return;
    let exportChart = null, host = null;
    try {
        const settings = { type: "png", pixelRatio: CHART_PNG_PIXEL_RATIO,
            backgroundColor: echartColors().background, excludeComponents: ["dataZoom"] };
        let url;
        if (mode === "full") {
            const plan = fullChartPngPlan(request.chart);
            if (plan.tooLarge) throw new Error("The complete chart is too large for PNG at this zoom. Zoom out and try again.");
            host = document.createElement("div");
            host.style.cssText = "position:fixed;left:-100000px;top:0;pointer-events:none;";
            host.style.width = plan.width + "px"; host.style.height = plan.height + "px";
            document.body.appendChild(host);
            exportChart = echarts.init(host, null, { renderer: "canvas", useDirtyRect: false, devicePixelRatio: 1 });
            exportChart.setOption(plan.option, { notMerge: true, lazyUpdate: false });
            // Give the independent chart one paint before capturing its complete surface.
            await new Promise(resolve => requestAnimationFrame(resolve));
            url = exportChart.getDataURL(settings);
        } else url = request.chart.getDataURL(settings);
        if (!url || !url.startsWith("data:image/png")) throw new Error("Could not create the PNG image at this size.");
        const blob = await (await fetch(url)).blob();
        const filename = mode === "full" ? request.filename.replace(/(?:\.png)?$/i, "-full.png") : request.filename;
        download(filename, blob, "image/png");
    } catch (error) { setStatus("Chart export failed: " + error.message, "error"); }
    finally { if (exportChart) exportChart.dispose(); if (host) host.remove(); }
}

function setupChartPngExport() {
    $("chart-png-current").addEventListener("click", () => exportChartPng("current"));
    $("chart-png-full").addEventListener("click", () => exportChartPng("full"));
    $("chart-png-cancel").addEventListener("click", () => $("chart-png-dialog").close());
    $("chart-png-dialog").addEventListener("close", () => { CHART_PNG_REQUEST = null; });
}
