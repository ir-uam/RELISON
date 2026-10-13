"use strict";

window.relisonNetworkSvg = {
    labelSettings(fallback = {}) {
        const settings = { ...fallback };
        for (const kind of ["node", "edge"]) {
            for (const [suffix, key] of [["show", "Show"], ["prop", "Prop"], ["size", "Size"], ["color", "Color"], ["color-mode", "ColorMode"], ["font", "Font"], ["attribute", "Attribute"]]) {
                const control = document.getElementById(kind + "-label-" + suffix);
                if (control) settings[kind + key] = suffix === "show" || suffix === "prop" ? control.checked
                    : suffix === "size" ? Math.max(1, Number(control.value) || (kind === "node" ? 12 : 9)) : control.value;
            }
        }
        return settings;
    },

    nodeLabel(graph, id, settings) {
        const attributes = graph.getNodeAttributes(id);
        const selection = settings.nodeAttribute || "";
        const value = selection === "id" ? id : selection.startsWith("attr:")
            ? attributes.attrs?.[selection.slice(5)] : attributes.attrs?.label ?? attributes.label;
        return value == null || value === "" ? String(id) : typeof value === "object" ? JSON.stringify(value) : String(value);
    },

    // Measure all vector geometry and text, then translate without changing scale.
    async fit(source, legend) {
        if (document.fonts?.ready) await document.fonts.ready;
        const svg = new DOMParser().parseFromString(source, "image/svg+xml").documentElement;
        if (svg.localName !== "svg" || svg.querySelector("parsererror")) throw new Error("Could not read the network SVG.");
        const background = [...svg.children].find(child => child.localName === "rect");
        const artwork = document.createElementNS("http://www.w3.org/2000/svg", "g");
        [...svg.children].filter(child => child !== background && child.localName !== "defs").forEach(child => artwork.appendChild(child));
        svg.appendChild(artwork);
        svg.style.cssText = "position:fixed;left:-100000px;top:0;visibility:hidden;pointer-events:none;overflow:visible;";
        document.body.appendChild(svg);
        try {
            // SVG text has no native label box. Preserve the selected fill,
            // but outline low-contrast labels so they remain readable on export.
            const luminance = color => {
                const rgb = color.match(/[\d.]+/g);
                if (!rgb || rgb.length < 3) return null;
                const channels = rgb.slice(0, 3).map(value => {
                    const channel = Number(value) / 255;
                    return channel <= 0.04045 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
                });
                return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722;
            };
            const backgroundLuminance = background ? luminance(getComputedStyle(background).fill) : null;
            if (backgroundLuminance != null) artwork.querySelectorAll("text").forEach(label => {
                const labelLuminance = luminance(getComputedStyle(label).fill);
                if (labelLuminance == null) return;
                const contrast = (Math.max(labelLuminance, backgroundLuminance) + 0.05) / (Math.min(labelLuminance, backgroundLuminance) + 0.05);
                if (contrast >= 3) return;
                label.setAttribute("stroke", backgroundLuminance > 0.5 ? "#1c1d20" : "#ffffff");
                label.setAttribute("stroke-width", "2");
                label.setAttribute("stroke-linejoin", "round");
                label.setAttribute("paint-order", "stroke fill");
            });
            const bounds = artwork.getBBox();
            if (![bounds.x, bounds.y, bounds.width, bounds.height].every(Number.isFinite)) throw new Error("Invalid network export bounds.");
            let stroke = 0;
            artwork.querySelectorAll("[stroke-width]").forEach(element => { stroke = Math.max(stroke, Number(element.getAttribute("stroke-width")) || 0); });
            const padding = Math.ceil(Math.max(24, stroke / 2 + 12));
            const sidebar = legend && legend.type !== "none" ? 260 : 0;
            const width = Math.max(1, Math.ceil(bounds.width + 2 * padding + sidebar));
            const height = Math.max(sidebar ? 300 : 1, Math.ceil(bounds.height + 2 * padding));
            artwork.setAttribute("transform", "translate(" + (padding + sidebar - bounds.x) + " " + (padding - bounds.y) + ")");
            svg.setAttribute("width", width); svg.setAttribute("height", height);
            svg.setAttribute("viewBox", "0 0 " + width + " " + height);
            if (background) { background.setAttribute("x", 0); background.setAttribute("y", 0); background.setAttribute("width", width); background.setAttribute("height", height); }
            svg.removeAttribute("style");
            return new XMLSerializer().serializeToString(svg);
        } finally { svg.remove(); }
    },

    // Generate labels from the complete active snapshot, bypassing live label caps.
    labels(points, links, config, settings = this.labelSettings(window.relisonCosmographLabelOpts)) {
        const E = window.relisonGeographicExport.element, group = E("g");
        const labelColor = (kind, underlying, defaultColor) => {
            const mode = settings[kind + "ColorMode"] || "default";
            return mode === "single" ? (settings[kind + "Color"] || defaultColor)
                : mode === kind ? (underlying || defaultColor) : defaultColor;
        };
        const text = (content, x, y, fontSize, fontFamily, color, centered = false) => {
            group.appendChild(E("text", { x, y, "font-size": fontSize, "font-family": fontFamily || "sans-serif",
                fill: color || "#ffffff", "text-anchor": centered ? "middle" : "start" }, content));
        };
        if (settings.edgeShow) links.forEach(link => {
            if (link.label == null || link.label === "") return;
            const s = points.get(link.source), t = points.get(link.target);
            if (!s || !t) return;
            let x = (s.x + t.x) / 2, y = (s.y + t.y) / 2;
            if (config.curvedLinks) {
                const bend = (config.curvedLinkControlPointDistance ?? 0.25) * (config.curvedLinkWeight ?? 0.8) / 2;
                x -= (t.y - s.y) * bend; y += (t.x - s.x) * bend;
            }
            const fontSize = settings.edgeProp ? Math.max(5, (link.width || 1) * ((Number(settings.edgeSize) || 9) / 2)) : Number(settings.edgeSize) || 9;
            text(link.label, x, y, fontSize, settings.edgeFont, labelColor("edge", link.color, "#ffffff"), true);
        });
        if (settings.nodeShow) points.forEach(({ x, y, radius, point }) => {
            if (point.label == null || point.label === "") return;
            const fontSize = settings.nodeProp ? Math.max(6, (point.size / 2 || 4) * ((Number(settings.nodeSize) || 13) / 8)) : Number(settings.nodeSize) || 13;
            text(point.label, x + radius + 3, y + fontSize / 3, fontSize, settings.nodeFont, labelColor("node", point.color, "#ffffff"));
        });
        return new XMLSerializer().serializeToString(group);
    },
};

function setupNetworkSvgExport() {
    const dialog = $("network-svg-dialog");
    $("network-svg-current").addEventListener("click", () => { dialog.close(); exportSvg(false); });
    $("network-svg-full").addEventListener("click", () => { dialog.close(); exportSvg(true); });
    $("network-svg-cancel").addEventListener("click", () => dialog.close());
}
