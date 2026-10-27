"use strict";

// Convert the notebook's already-sanitized rendered text into a native PDF document.
const ReportPdf = (() => {
    const pageSizes = { A3: [841.89, 1190.55], A4: [595.28, 841.89], A5: [419.53, 595.28],
        LETTER: [612, 792], LEGAL: [612, 1008] };
    const allowedImage = src => /^data:image\/(?:png|jpeg);base64,[a-z0-9+/=\s]+$/i.test(src || "");
    const safeColour = value => {
        const probe = document.createElement("span");
        probe.style.color = value || "";
        return probe.style.color || undefined;
    };

    async function imageData(element) {
        const src = element.getAttribute("src");
        if (!src) return null;
        return new Promise(resolve => {
            const image = new Image(), timer = setTimeout(() => resolve(null), 10000);
            image.crossOrigin = "anonymous";
            image.onload = () => {
                clearTimeout(timer);
                try {
                    const canvas = document.createElement("canvas");
                    canvas.width = image.naturalWidth; canvas.height = image.naturalHeight;
                    if (!canvas.width || !canvas.height) return resolve(null);
                    canvas.getContext("2d").drawImage(image, 0, 0);
                    resolve(canvas.toDataURL("image/png"));
                } catch (_) { resolve(null); }
            };
            image.onerror = () => { clearTimeout(timer); resolve(null); };
            image.src = src;
        });
    }

    function inline(node, inherited = {}) {
        if (node.nodeType === 3) {
            const text = node.nodeValue.replace(/\s+/g, " ");
            return text ? [{ text, ...inherited }] : [];
        }
        if (node.nodeType !== 1) return [];
        const tag = node.tagName.toLowerCase(), style = { ...inherited };
        if (tag === "br") return [{ text: "\n", ...style }];
        if (tag === "img") return [{ text: node.getAttribute("alt") || "[Image]", ...style }];
        if (tag === "strong" || tag === "b") style.bold = true;
        if (tag === "em" || tag === "i") style.italics = true;
        if (tag === "u") style.decoration = "underline";
        if (["s", "del", "strike"].includes(tag)) style.decoration = "lineThrough";
        if (tag === "code") style.background = "#eeeeee";
        if (node.style?.color) style.color = safeColour(node.style.color);
        if (/^\d+(?:\.\d+)?px$/.test(node.style?.fontSize || "")) style.fontSize = Math.max(6, Math.min(54, parseFloat(node.style.fontSize) * 0.75));
        if (tag === "a") {
            const href = node.getAttribute("href") || "";
            const match = href.match(/^#relison-report-cell-([1-9]\d*)$/);
            if (match && !node.classList.contains("report-reference-missing")) style.linkToDestination = `report-cell-${match[1]}`;
            else if (/^(https?:|mailto:)/i.test(href)) style.link = href;
            if (style.link || style.linkToDestination) { style.color = "#185bb5"; style.decoration = "underline"; }
        }
        return [...node.childNodes].flatMap(child => inline(child, style));
    }

    function contentBlocks(root, images, fit) {
        const result = [];
        const textBlock = (node, extra = {}) => ({ text: inline(node), margin: [0, 0, 0, 7], ...extra });
        const imageBlock = node => {
            const image = images.get(node);
            return image ? { image, fit, margin: [0, 4, 0, 8] }
                : { text: `[Image unavailable${node.getAttribute("alt") ? ": " + node.getAttribute("alt") : ""}]`, italics: true, color: "#666666", margin: [0, 0, 0, 8] };
        };
        function append(node, output) {
            if (node.nodeType === 3) {
                if (node.nodeValue.trim()) output.push({ text: node.nodeValue.replace(/\s+/g, " "), margin: [0, 0, 0, 7] });
                return;
            }
            if (node.nodeType !== 1) return;
            const tag = node.tagName.toLowerCase();
            if (/^h[1-6]$/.test(tag)) {
                const size = [20, 17, 15, 13, 12, 11][Number(tag[1]) - 1];
                output.push(textBlock(node, { bold: true, fontSize: size, margin: [0, 9, 0, 6] }));
            } else if (tag === "p") {
                let parts = [];
                const flush = () => { if (parts.length) { output.push({ text: parts, margin: [0, 0, 0, 7] }); parts = []; } };
                for (const child of node.childNodes) {
                    if (child.nodeType === 1 && child.tagName.toLowerCase() === "img") { flush(); output.push(imageBlock(child)); }
                    else parts.push(...inline(child));
                }
                flush();
            } else if (tag === "img") output.push(imageBlock(node));
            else if (tag === "pre") output.push({ text: node.textContent.replace(/\n$/, ""), fontSize: 9, background: "#f1f2f4", margin: [0, 4, 0, 8] });
            else if (tag === "ul" || tag === "ol") {
                const items = Array.from(node.children).filter(child => child.tagName.toLowerCase() === "li").map(li => {
                    const blocks = [], fragments = [];
                    for (const child of li.childNodes) {
                        if (child.nodeType === 1 && ["ul", "ol", "p", "pre", "table", "blockquote"].includes(child.tagName.toLowerCase())) {
                            if (fragments.length) { blocks.push({ text: fragments.splice(0) }); }
                            append(child, blocks);
                        } else fragments.push(...inline(child));
                    }
                    if (fragments.length) blocks.push({ text: fragments });
                    return { stack: blocks.length ? blocks : [{ text: "" }] };
                });
                if (items.length) output.push({ [tag]: items, margin: [16, 0, 0, 8], ...(tag === "ol" && Number(node.getAttribute("start")) > 0 ? { start: Number(node.getAttribute("start")) } : {}) });
            } else if (tag === "blockquote") {
                const blocks = []; [...node.childNodes].forEach(child => append(child, blocks));
                if (blocks.length) output.push({ stack: blocks, margin: [16, 4, 0, 8], color: "#555555" });
            } else if (tag === "table") {
                const rows = Array.from(node.querySelectorAll("tr")).map(row => Array.from(row.children).filter(cell => ["TH", "TD"].includes(cell.tagName)));
                const width = Math.max(0, ...rows.map(row => row.length));
                if (width) {
                    const body = rows.map(row => Array.from({ length: width }, (_, i) => ({ text: row[i] ? inline(row[i]) : [], bold: row[i]?.tagName === "TH" })));
                    output.push({ table: { headerRows: node.querySelector("thead") ? 1 : 0, widths: Array(width).fill("*"), body },
                        layout: "lightHorizontalLines", margin: [0, 4, 0, 9] });
                }
            } else if (tag === "hr") output.push({ canvas: [{ type: "line", x1: 0, y1: 0, x2: fit[0], y2: 0, lineWidth: 0.5, lineColor: "#aaaaaa" }], margin: [0, 5, 0, 10] });
            else if (["div", "section", "article"].includes(tag)) [...node.childNodes].forEach(child => append(child, output));
            else output.push(textBlock(node));
        }
        [...root.childNodes].forEach(node => append(node, result));
        return result;
    }

    async function buildDefinition(model, pageSize, orientation) {
        if (!pageSizes[pageSize] || !["portrait", "landscape"].includes(orientation)) throw new Error("Choose a valid page size and orientation.");
        const [w, h] = orientation === "landscape" ? [...pageSizes[pageSize]].reverse() : pageSizes[pageSize];
        const fit = [w - 96, h - 150], content = [{ text: model.title || "Untitled report", fontSize: 22, bold: true, margin: [0, 0, 0, 18] }];
        let unavailableImages = 0;
        for (const cell of model.cells) {
            if (cell.type === "text") {
                const root = document.createElement("div"); root.innerHTML = cell.html;
                const images = new Map();
                for (const image of Array.from(root.querySelectorAll("img"))) {
                    const data = await imageData(image); images.set(image, data);
                    if (!data) unavailableImages++;
                }
                content.push(...contentBlocks(root, images, fit));
            } else {
                const blocks = [];
                if (cell.type === "graph" && allowedImage(cell.snapshot)) blocks.push({ image: cell.snapshot, fit, margin: [0, 6, 0, 6] });
                else blocks.push({ text: cell.placeholder, color: "#777777", italics: true, margin: [0, 6, 0, 6] });
                if (cell.label && cell.caption) blocks.push({ id: `report-cell-${cell.id}`, text: [{ text: `${cell.label}. `, bold: true }, { text: cell.caption }],
                    fontSize: 9, color: "#555555", margin: [0, 0, 0, 14] });
                // Keep ordinary captions with their image; very long captions remain splittable.
                if (blocks.length === 2 && cell.caption.length <= 120) content.push({ stack: blocks, unbreakable: true });
                else content.push(...blocks);
            }
        }
        return { definition: { pageSize, pageOrientation: orientation, pageMargins: [48, 48, 48, 48],
            defaultStyle: { font: "Roboto", fontSize: 11, lineHeight: 1.2 }, content }, unavailableImages };
    }

    async function exportPdf(model, pageSize, orientation) {
        if (!globalThis.pdfMake?.createPdf) throw new Error("The PDF exporter is unavailable.");
        const { definition, unavailableImages } = await buildDefinition(model, pageSize, orientation);
        let timer;
        const blob = await Promise.race([
            new Promise(resolve => pdfMake.createPdf(definition).getBlob(resolve)),
            new Promise((_, reject) => { timer = setTimeout(() => reject(new Error("PDF generation timed out.")), 30000); }),
        ]).finally(() => clearTimeout(timer));
        if (!blob || !blob.size) throw new Error("Could not build the PDF.");
        const url = URL.createObjectURL(blob), link = document.createElement("a");
        const filename = (model.title || "report").trim().replace(/[\\/:*?"<>|\x00-\x1f]/g, "_").replace(/[. ]+$/g, "").slice(0, 100) || "report";
        link.href = url; link.download = `${filename}.pdf`; document.body.appendChild(link); link.click(); link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
        return { unavailableImages };
    }
    return { buildDefinition, exportPdf };
})();
