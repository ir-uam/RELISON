/* Standalone SVG snapshots and PNG rasterization; no live DOM capture. */
(() => {
    const NS = "http://www.w3.org/2000/svg";
    function element(tag, attributes = {}, text) {
        const node = document.createElementNS(NS, tag);
        for (const [key, value] of Object.entries(attributes)) if (value !== undefined && value !== null) node.setAttribute(key, String(value));
        if (text !== undefined) node.textContent = String(text);
        return node;
    }
    function root(width, height) {
        const svg = element("svg", { xmlns: NS, width, height, viewBox: `0 0 ${width} ${height}` });
        svg.appendChild(element("rect", { width, height, fill: "#e2e8f0" })); return svg;
    }
    function credit(svg, width, height, content) {
        const boxWidth = Math.min(width, content.length * 6 + 12);
        svg.appendChild(element("rect", { x: width - boxWidth, y: Math.max(0, height - 22), width: boxWidth, height: 22, fill: "white", opacity: .95 }));
        svg.appendChild(element("text", { x: width - 6, y: height - 7, "text-anchor": "end", "font-family": "sans-serif", "font-size": 11, fill: "#334155" }, content));
    }
    function save(blob, filename) {
        const url = URL.createObjectURL(blob), link = document.createElement("a");
        link.href = url; link.download = filename; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
    }
    async function exportImage(build, format, filename) {
        const svg = await build();
        if (!svg) return false;
        const blob = new Blob([new XMLSerializer().serializeToString(svg)], { type: "image/svg+xml;charset=utf-8" });
        if (format === "svg") { save(blob, filename); return true; }
        const url = URL.createObjectURL(blob);
        try {
            const image = new Image();
            await new Promise((resolve, reject) => { image.onload = resolve; image.onerror = () => reject(new Error("Could not rasterize the map SVG.")); image.src = url; });
            const canvas = document.createElement("canvas");
            canvas.width = Number(svg.getAttribute("width")); canvas.height = Number(svg.getAttribute("height"));
            canvas.getContext("2d").drawImage(image, 0, 0);
            const png = await new Promise(resolve => canvas.toBlob(resolve, "image/png"));
            if (!png) throw new Error("Could not encode the map PNG.");
            save(png, filename); return true;
        } finally { URL.revokeObjectURL(url); }
    }
    function legend(svg, width, height, data, placement = null) {
        if (!data || data.type === "none") return 0;
        if (data.type === "group") {
            const legends = data.legends.filter(item => item && item.type !== "none").slice(0, 2);
            let top = 12;
            legends.forEach((item, index) => {
                const remaining = legends.length - index;
                const available = (height - top - 12 - (remaining - 1) * 12) / remaining;
                top += legend(svg, width, available + 24, item, { top, id: String(index) }) + 12;
            });
            return top - 12;
        }
        const boxWidth = Math.min(230, placement?.width ?? width - 24);
        if (boxWidth < 60 || height < 100) return 0;
        const entries = [...(data.entries || [])];
        if (data.hasMissing) entries.push({ value: "(none)", color: "#888888" });
        const maxRows = Math.max(1, Math.floor((height - 100) / 20));
        const rows = entries.slice(0, maxRows), omitted = entries.length - rows.length;
        const description = [];
        if (data.type === "text") {
            let line = "";
            for (const word of String(data.description || "").split(/\s+/)) {
                if (line && (line + " " + word).length > Math.max(10, Math.floor((boxWidth - 20) / 7))) { description.push(line); line = ""; }
                line += (line ? " " : "") + word;
            }
            if (line) description.push(line);
            const limit = Math.max(1, Math.floor((height - 58) / 16));
            if (description.length > limit) {
                description.length = limit;
                description[limit - 1] += "…";
            }
        }
        const boxHeight = data.type === "numeric" ? 72 + (data.hasMissing ? 20 : 0)
            : data.type === "text" ? 34 + description.length * 16 : 34 + rows.length * 20 + (omitted ? 20 : 0);
        const group = element("g", { transform: `translate(12 ${placement?.top ?? 12})`, "font-family": "sans-serif", "font-size": 12, fill: "#0f172a" });
        group.appendChild(element("rect", { width: boxWidth, height: boxHeight, rx: 5, fill: "white", opacity: .95, stroke: "#94a3b8" }));
        const title = String(data.title || "Node colour");
        const titleLimit = Math.max(5, Math.floor((boxWidth - 20) / 7));
        group.appendChild(element("text", { x: 10, y: 21 }, title.length > titleLimit ? title.slice(0, titleLimit - 1) + "…" : title));
        if (data.type === "text") {
            description.forEach((line, index) => group.appendChild(element("text", { x: 10, y: 40 + index * 16 }, line)));
        } else if (data.type === "numeric") {
            const gradientId = "geographic-legend-gradient" + (placement ? "-" + placement.id : "");
            const defs = element("defs"), gradient = element("linearGradient", { id: gradientId });
            gradient.appendChild(element("stop", { offset: "0%", "stop-color": data.low }));
            gradient.appendChild(element("stop", { offset: "100%", "stop-color": data.high }));
            defs.appendChild(gradient); group.appendChild(defs);
            group.appendChild(element("rect", { x: 10, y: 32, width: boxWidth - 20, height: 12, fill: `url(#${gradientId})` }));
            group.appendChild(element("text", { x: 10, y: 61 }, Number(data.min).toLocaleString()));
            group.appendChild(element("text", { x: boxWidth - 10, y: 61, "text-anchor": "end" }, Number(data.max).toLocaleString()));
            if (data.hasMissing) {
                group.appendChild(element("rect", { x: 10, y: 72, width: 12, height: 3, fill: "#888888" }));
                group.appendChild(element("text", { x: 30, y: 80 }, "(none)"));
            }
        } else {
            rows.forEach((entry, index) => {
                const y = 34 + index * 20, label = String(entry.label ?? entry.value);
                group.appendChild(element("rect", { x: 10, y: data.scope === "Edges" ? y + 4 : y - 1,
                    width: data.scope === "Edges" ? 18 : 12, height: data.scope === "Edges" ? 3 : 12, fill: entry.color, stroke: "#94a3b8" }));
                const labelLimit = Math.max(3, Math.floor((boxWidth - 42) / 7));
                group.appendChild(element("text", { x: 34, y: y + 10 }, label.length > labelLimit ? label.slice(0, labelLimit - 1) + "…" : label));
            });
            if (omitted) group.appendChild(element("text", { x: 10, y: boxHeight - 10 }, "+ " + omitted + " more categories"));
        }
        svg.appendChild(group);
        return boxHeight;
    }
    function withLegend(source, data) {
        if (!data || data.type === "none") return source;
        const svg = new DOMParser().parseFromString(source, "image/svg+xml").documentElement;
        legend(svg, Number(svg.getAttribute("width")), Number(svg.getAttribute("height")), data);
        return new XMLSerializer().serializeToString(svg);
    }
    async function canvasLegend(canvas, data, scale = 1) {
        if (!data || data.type === "none") return;
        const width = canvas.width / scale, height = canvas.height / scale;
        const svg = element("svg", { xmlns: NS, width, height, viewBox: `0 0 ${width} ${height}` });
        legend(svg, width, height, data);
        const url = URL.createObjectURL(new Blob([new XMLSerializer().serializeToString(svg)], { type: "image/svg+xml" }));
        try {
            const image = new Image();
            await new Promise((resolve, reject) => { image.onload = resolve; image.onerror = () => reject(new Error("Could not render the export legend.")); image.src = url; });
            canvas.getContext("2d").drawImage(image, 0, 0, canvas.width, canvas.height);
        } finally { URL.revokeObjectURL(url); }
    }
    window.relisonGeographicExport = { element, root, credit, legend, withLegend, canvasLegend, exportImage };
})();
