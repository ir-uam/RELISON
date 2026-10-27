"use strict";

// Markdown is the source of truth. Only a small set of text styles may survive HTML rendering.
const ReportMarkdown = (() => {
    const fonts = [["system-ui", "Default font"], ["Arial", "Arial"], ["Georgia", "Georgia"],
        ["Times New Roman", "Times New Roman"], ["Verdana", "Verdana"], ["monospace", "Monospace"]];
    const escape = value => String(value).replace(/[&<>"']/g, ch =>
        ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch]);
    const fontKey = value => value.replace(/["']/g, "").trim().toLowerCase();
    const approvedFont = value => fonts.find(([font]) => fontKey(font) === fontKey(value))?.[0];

    function safeUrl(value, image = false) {
        if (!value || /[\u0000-\u001f\u007f]/.test(value)) return false;
        if (image && /^data:image\/(?:png|jpeg|gif|webp);base64,[a-z0-9+/=\s]+$/i.test(value)) return true;
        const scheme = value.trim().match(/^([a-z][a-z0-9+.-]*):/i)?.[1]?.toLowerCase();
        return scheme ? ["http", "https", ...(image ? [] : ["mailto"])].includes(scheme) : true;
    }

    function sanitize(html) {
        if (!globalThis.DOMPurify?.sanitize) return escape(html);
        const fragment = DOMPurify.sanitize(String(html), {
            RETURN_DOM_FRAGMENT: true,
            ALLOWED_TAGS: ["p", "br", "strong", "b", "em", "i", "u", "s", "del", "code", "pre", "blockquote",
                "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li", "a", "hr", "table", "thead", "tbody",
                "tr", "th", "td", "span", "sup", "sub", "img", "div", "font", "strike"],
            ALLOWED_ATTR: ["href", "title", "style", "start", "align", "src", "alt", "face", "color", "size"],
            ALLOW_DATA_ATTR: false, ALLOW_ARIA_ATTR: false,
        });
        for (const font of fragment.querySelectorAll("font")) {
            const span = document.createElement("span");
            span.style.cssText = font.style.cssText;
            if (font.hasAttribute("color")) span.style.color = font.getAttribute("color");
            const family = approvedFont(font.getAttribute("face") || "");
            if (family) span.style.fontFamily = family;
            if (!span.style.fontSize && /^[1-7]$/.test(font.getAttribute("size") || ""))
                span.style.fontSize = [10, 12, 14, 16, 18, 24, 32][Number(font.getAttribute("size")) - 1] + "px";
            span.append(...font.childNodes);
            font.replaceWith(span);
        }
        for (const pre of fragment.querySelectorAll("pre")) if (!pre.querySelector("code")) {
            const code = document.createElement("code");
            code.textContent = pre.textContent;
            pre.replaceChildren(code);
        }
        for (const element of fragment.querySelectorAll("[style]")) {
            const colour = element.style.color, font = approvedFont(element.style.fontFamily);
            const size = element.style.fontSize;
            element.removeAttribute("style");
            if (colour && !/(?:var|env|url|expression)\s*\(/i.test(colour)) element.style.color = colour;
            if (font) element.style.fontFamily = font;
            if (/^\d+(?:\.\d+)?px$/.test(size) && parseFloat(size) >= 8 && parseFloat(size) <= 72) element.style.fontSize = size;
        }
        for (const element of fragment.querySelectorAll("[align]"))
            if (!["left", "center", "right"].includes(element.getAttribute("align"))) element.removeAttribute("align");
        for (const link of fragment.querySelectorAll("a")) {
            if (!safeUrl(link.getAttribute("href"))) link.removeAttribute("href");
            else if (!/^#relison-report-cell-[1-9]\d*$/.test(link.getAttribute("href"))) {
                link.setAttribute("target", "_blank"); link.setAttribute("rel", "noopener noreferrer");
            }
        }
        for (const image of fragment.querySelectorAll("img"))
            if (!safeUrl(image.getAttribute("src"), true)) image.removeAttribute("src");
        const container = document.createElement("div");
        container.append(fragment);
        return container.innerHTML;
    }

    function render(source) {
        if (!globalThis.marked?.parse || !globalThis.DOMPurify?.sanitize) return `<p>${escape(source)}</p>`;
        return sanitize(marked.parse(String(source), { gfm: true, breaks: false, async: false }));
    }

    let converter;
    function fromHtml(html) {
        if (!converter) {
            converter = new TurndownService({ headingStyle: "atx", bulletListMarker: "-", codeBlockStyle: "fenced", emDelimiter: "*" });
            converter.use(turndownPluginGfm.gfm);
            converter.keep(["u", "sup", "sub"]);
            converter.addRule("reportReference", {
                filter: node => node.nodeName === "A" && /^#relison-report-cell-[1-9]\d*$/.test(node.getAttribute("href") || ""),
                replacement: (_content, node) => `[reference](${node.getAttribute("href")})`,
            });
            converter.addRule("textStyles", { filter: node => node.nodeName === "SPAN" && node.hasAttribute("style"),
                replacement: (_content, node) => node.outerHTML });
        }
        return converter.turndown(sanitize(html));
    }

    function format(source, start, end, command, value = "") {
        source = String(source);
        start = Math.max(0, Math.min(source.length, Number(start) || 0));
        end = Math.max(start, Math.min(source.length, Number(end) || 0));
        const replace = (from, to, text, selectionStart = 0, selectionEnd = text.length) =>
            ({ text: source.slice(0, from) + text + source.slice(to), start: from + selectionStart, end: from + selectionEnd });
        function wrap(before, after, placeholder) {
            const selected = source.slice(start, end);
            if (selected.startsWith(before) && selected.endsWith(after) && selected.length >= before.length + after.length)
                return replace(start, end, selected.slice(before.length, -after.length));
            if (source.slice(start - before.length, start) === before && source.slice(end, end + after.length) === after)
                return replace(start - before.length, end + after.length, selected);
            const [, leading, content, trailing] = selected.match(/^(\s*)([\s\S]*?)(\s*)$/);
            const text = content || placeholder;
            const replacement = leading + before + text + after + trailing;
            return replace(start, end, replacement, leading.length + before.length, leading.length + before.length + text.length);
        }
        if (command === "bold") return wrap("**", "**", "bold text");
        if (command === "italic") return wrap("*", "*", "italic text");
        if (command === "underline") return wrap("<u>", "</u>", "underlined text");
        if (command === "strike") return wrap("~~", "~~", "text");
        if (command === "colour" && /^#[0-9a-f]{6}$/i.test(value)) return wrap(`<span style="color:${value}">`, "</span>", "text");
        if (command === "font" && approvedFont(value)) return wrap(`<span style="font-family:${approvedFont(value)}">`, "</span>", "text");
        if (command === "size" && /^\d+$/.test(value) && Number(value) >= 8 && Number(value) <= 72)
            return wrap(`<span style="font-size:${value}px">`, "</span>", "text");
        if (command === "code") {
            const text = source.slice(start, end);
            if (text.includes("\n")) {
                const fence = "`".repeat(Math.max(3, ...[...text.matchAll(/`+/g)].map(match => match[0].length + 1)));
                const before = (start && source[start - 1] !== "\n" ? "\n" : "") + fence + "\n";
                return replace(start, end, before + text + "\n" + fence + "\n", before.length, before.length + text.length);
            }
            if (text.includes("`")) {
                const delimiter = "`".repeat(Math.max(...[...text.matchAll(/`+/g)].map(match => match[0].length)) + 1);
                return wrap(delimiter + " ", " " + delimiter, "code");
            }
            return wrap("`", "`", "code");
        }
        if (command === "link") {
            const text = source.slice(start, end) || "link text", url = "https://example.com";
            return replace(start, end, `[${text}](${url})`, text.length + 3, text.length + 3 + url.length);
        }
        if (["heading", "quote", "bullet", "numbered"].includes(command)) {
            const from = start ? source.lastIndexOf("\n", start - 1) + 1 : 0;
            const last = end > start && source[end - 1] === "\n" ? end - 1 : end;
            const newline = source.indexOf("\n", last), to = newline < 0 ? source.length : newline;
            const lines = source.slice(from, to).split("\n");
            const remove = command === "quote" && lines.every(line => !line.trim() || /^>\s?/.test(line))
                || command === "bullet" && lines.every(line => !line.trim() || /^-\s/.test(line));
            let number = 0;
            const result = lines.map(line => {
                if (command === "heading") {
                    const level = Number(value);
                    if (!Number.isInteger(level) || level < 0 || level > 6) return line;
                    const text = line.replace(/^ {0,3}#{1,6}\s+/, "");
                    return level ? "#".repeat(level) + " " + text : text;
                }
                if (command === "quote") return remove ? line.replace(/^>\s?/, "") : "> " + line;
                if (!line.trim()) return line;
                const text = line.replace(/^\s*(?:[-+*]|\d+[.)])\s+/, "");
                return remove ? text : command === "numbered" ? `${++number}. ${text}` : "- " + text;
            }).join("\n");
            return replace(from, to, result);
        }
        return { text: source, start, end };
    }

    return { render, sanitize, fromHtml, safeUrl, format, fonts };
})();
