"use strict";

let CATEGORY_LABEL_CONTEXT = null;

function categoryLabelMetrics(axis, textStyle) {
    if (!CATEGORY_LABEL_CONTEXT) CATEGORY_LABEL_CONTEXT = document.createElement("canvas").getContext("2d");
    const style = { ...textStyle, ...axis.axisLabel };
    const fontSize = style.fontSize || 12, lineHeight = style.lineHeight || Math.ceil(fontSize * 1.4);
    CATEGORY_LABEL_CONTEXT.font = (style.fontWeight || "normal") + " " + fontSize + "px " + (style.fontFamily || "sans-serif");
    let width = 0, lines = 1;
    (axis.data || []).forEach((item, index) => {
        const value = item && typeof item === "object" ? item.value : item;
        const formatter = axis.axisLabel?.formatter;
        const label = typeof formatter === "function" ? formatter(value, index)
            : typeof formatter === "string" ? formatter.replace(/\{value\}/g, String(value)) : value;
        const parts = String(label ?? "").split("\n");
        lines = Math.max(lines, parts.length);
        parts.forEach(part => { width = Math.max(width, Math.ceil(CATEGORY_LABEL_CONTEXT.measureText(part).width) + 4); });
    });
    return { width, lines, lineHeight };
}

function categoryZoomTargets(zoom) {
    const hasIndex = value => value === 0 || (Array.isArray(value) && value.includes(0));
    return { x: hasIndex(zoom.xAxisIndex) || (zoom.xAxisIndex == null && zoom.yAxisIndex == null), y: hasIndex(zoom.yAxisIndex) };
}

function categoryZoomRange(zoom) {
    const range = {};
    for (const key of ["start", "end", "startValue", "endValue", "rangeMode"]) if (zoom?.[key] != null) range[key] = zoom[key];
    return range;
}

// Use ECharts' own sliders and inside zoom. Restrict the category window to the
// number of full labels that fits, so panning exposes every category without overlap.
function prepareCategoryChartLayout(el, option, chart) {
    const rect = el.getBoundingClientRect();
    const axes = ["x", "y"].filter(axis => option[axis + "Axis"]?.type === "category");
    const all = document.body.dataset.chartLabels === "all" && axes.length > 0;
    const current = chart?.getOption().dataZoom || [];
    if (!all) {
        const layout = responsiveChartLayout(option, rect.width, rect.height);
        if (chart?.__categoryLabelMode === "all") {
            layout.dataZoom = (option.dataZoom || []).map((zoom, i) => {
                const targets = categoryZoomTargets(zoom);
                const categorical = axes.some(axis => targets[axis]);
                const native = current.find(item => item.id === "relison-base-zoom-" + i);
                return { ...zoom, ...(categorical ? chart.__categoryLabelZoom?.[i] : categoryZoomRange(native)),
                    ...layout.dataZoom?.[i] };
            });
            layout.__replaceZoom = true;
        }
        if (chart) { chart.__categoryLabelMode = "automatic"; chart.__categoryLabelZoom = null; }
        return layout;
    }
    if (chart && chart.__categoryLabelMode !== "all") {
        chart.__categoryLabelZoom = (option.dataZoom || []).map((zoom, i) => categoryZoomRange(current[i]));
    }
    const colors = echartColors();
    // Keep any numeric-axis zoom controls. Give each category axis its own native
    // controls, because X and Y may need different label capacities.
    const dataZoom = (option.dataZoom || []).flatMap((zoom, i) => {
        const targets = categoryZoomTargets(zoom);
        const xNumeric = targets.x && !axes.includes("x"), yNumeric = targets.y && !axes.includes("y");
        if (!xNumeric && !yNumeric) return [];
        const native = chart?.__categoryLabelMode === "all"
            ? current.find(item => item.id === "relison-base-zoom-" + i) : current[i];
        return [{ ...zoom, ...categoryZoomRange(native), id: "relison-base-zoom-" + i,
            xAxisIndex: xNumeric ? 0 : [], yAxisIndex: yNumeric ? 0 : [] }];
    });
    axes.forEach(axis => {
        const settings = { [axis + "AxisIndex"]: 0, filterMode: "none" };
        dataZoom.push(
            { ...settings, id: "relison-labels-" + axis + "-inside", type: "inside" },
            { ...settings, id: "relison-labels-" + axis + "-slider", type: "slider",
                orient: axis === "y" ? "vertical" : "horizontal",
                borderColor: colors.border, textStyle: { color: colors.muted },
                fillerColor: colors.accent + "33", handleStyle: { color: colors.accent } }
        );
    });
    const layout = responsiveChartLayout({ ...option, dataZoom }, rect.width, rect.height);
    if (axes.includes("y") && option.visualMap && rect.width >= 520) {
        layout.grid.right += 28;
        dataZoom.find(zoom => zoom.id === "relison-labels-y-slider").right = 72;
    }
    const metrics = {};
    axes.forEach(axis => { metrics[axis] = categoryLabelMetrics(option[axis + "Axis"], option.textStyle); });
    const capacities = {};
    if (metrics.y) {
        const labelWidth = Math.max(32, Math.min(metrics.y.width, 240, rect.width * 0.35));
        layout.grid.left = Math.ceil(labelWidth + (option.yAxis.name ? 40 : 16));
        layout.yAxis.nameGap = labelWidth + 14;
        layout.yAxis.axisLabel = { ...option.yAxis.axisLabel, interval: 0, hideOverlap: false,
            showMinLabel: true, showMaxLabel: true, rotate: 0, width: labelWidth,
            overflow: "breakAll", lineHeight: metrics.y.lineHeight };
        capacities.y = Math.max(28, metrics.y.lineHeight * metrics.y.lines * Math.max(1, Math.ceil(metrics.y.width / labelWidth)) + 12);
    }
    const plotWidth = Math.max(40, rect.width - layout.grid.left - layout.grid.right);
    if (metrics.x) {
        capacities.x = Math.max(48, Math.min(220, metrics.x.width + 20));
        const labelWidth = Math.max(16, Math.min(capacities.x - 16, plotWidth - 12));
        const labelHeight = metrics.x.lineHeight * metrics.x.lines * Math.max(1, Math.ceil(metrics.x.width / labelWidth));
        layout.grid.bottom += labelHeight - (layout.xAxis.nameGap - 20);
        layout.xAxis.nameGap = labelHeight + 20;
        layout.xAxis.axisLabel = { ...option.xAxis.axisLabel, interval: 0, hideOverlap: false,
            showMinLabel: true, showMaxLabel: true, rotate: 0, width: labelWidth,
            overflow: "breakAll", lineHeight: metrics.x.lineHeight };
    }
    const plotHeight = Math.max(40, rect.height - layout.grid.top - layout.grid.bottom);
    dataZoom.forEach(zoom => {
        if (zoom.type !== "slider") return;
        Object.assign(zoom, zoom.orient === "vertical"
            ? { top: layout.grid.top, bottom: layout.grid.bottom, right: option.visualMap && rect.width >= 520 ? 72 : 8, width: 16 }
            : { left: layout.grid.left, right: layout.grid.right, bottom: 8 + (option.visualMap && rect.width < 520 ? 52 : 0), height: 16 });
    });
    axes.forEach(axis => {
        const count = option[axis + "Axis"].data?.length || 0;
        const visible = Math.max(1, Math.min(count, Math.floor((axis === "x" ? plotWidth : plotHeight) / capacities[axis])));
        const native = current.find(zoom => zoom.id === "relison-labels-" + axis + "-slider");
        const nativeStart = Number(native?.startValue), nativeEnd = Number(native?.endValue);
        const span = native && Number.isFinite(nativeStart) && Number.isFinite(nativeEnd)
            ? Math.min(visible, Math.max(1, nativeEnd - nativeStart + 1)) : visible;
        const start = Math.max(0, Math.min(Math.max(0, count - span), Number.isFinite(nativeStart) ? nativeStart : 0));
        const end = Math.min(Math.max(0, count - 1), start + span - 1);
        dataZoom.filter(zoom => zoom.id.startsWith("relison-labels-" + axis + "-")).forEach(zoom => Object.assign(zoom, {
            rangeMode: ["value", "value"], startValue: start, endValue: Math.max(start, end),
            minValueSpan: 0, maxValueSpan: visible - 1
        }));
        layout[axis + "Axis"].boundaryGap = true;
        layout[axis + "Axis"].axisTick = { ...option[axis + "Axis"].axisTick, interval: 0 };
    });
    layout.dataZoom = dataZoom;
    layout.__replaceZoom = true;
    if (chart) chart.__categoryLabelMode = "all";
    return layout;
}

function resizeEChartLayout(chart) {
    if (!chart.__relisonOption) { chart.resize(); return; }
    const layout = prepareCategoryChartLayout(chart.getDom(), chart.__relisonOption, chart);
    const replaceZoom = layout.__replaceZoom;
    delete layout.__replaceZoom;
    chart.resize();
    chart.setOption(layout, { silent: true, ...(replaceZoom ? { replaceMerge: ["dataZoom"] } : {}) });
}
