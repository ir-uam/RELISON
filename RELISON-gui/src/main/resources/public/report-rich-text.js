"use strict";

// Native editing keeps typing, caret movement and undo in the browser. Serialization is sanitized separately.
const ReportRichText = (() => {
    function capture(element) {
        const selection = window.getSelection();
        if (!selection.rangeCount || !element.contains(selection.anchorNode) || !element.contains(selection.focusNode)) return null;
        const range = selection.getRangeAt(0), prefix = range.cloneRange();
        prefix.selectNodeContents(element); prefix.setEnd(range.startContainer, range.startOffset);
        const start = prefix.toString().length;
        return { start, end: start + range.toString().length };
    }

    function restore(element, saved) {
        const range = document.createRange();
        range.selectNodeContents(element); range.collapse(false);
        if (saved) {
            const walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT);
            const nodes = []; let node, length = 0;
            while ((node = walker.nextNode())) { nodes.push({ node, start: length }); length += node.textContent.length; }
            const point = offset => {
                offset = Math.max(0, Math.min(length, offset));
                const item = nodes.find(item => offset <= item.start + item.node.textContent.length);
                return item && { node: item.node, offset: offset - item.start };
            };
            const start = point(saved.start), end = point(saved.end);
            if (start && end) { range.setStart(start.node, start.offset); range.setEnd(end.node, end.offset); }
        }
        const selection = window.getSelection(); selection.removeAllRanges(); selection.addRange(range);
    }

    function format(element, command, value, saved) {
        element.focus({ preventScroll: true });
        restore(element, saved);
        document.execCommand("styleWithCSS", false, false);
        const native = { bold: "bold", italic: "italic", underline: "underline", strike: "strikeThrough",
            bullet: "insertUnorderedList", numbered: "insertOrderedList", font: "fontName", colour: "foreColor" };
        if (native[command]) document.execCommand(native[command], false, value || null);
        else if (command === "heading") document.execCommand("formatBlock", false, Number(value) ? `h${value}` : "p");
        else if (command === "quote" || command === "code") document.execCommand("formatBlock", false, command === "quote" ? "blockquote" : "pre");
        else if (command === "size") {
            document.execCommand("fontSize", false, "7");
            element.querySelectorAll('font[size="7"]').forEach(font => {
                font.removeAttribute("size"); font.style.fontSize = `${Number(value)}px`;
            });
        } else if (command === "link") {
            const url = window.prompt("Link URL", "https://example.com");
            if (url && ReportMarkdown.safeUrl(url)) {
                if (window.getSelection().isCollapsed) {
                    const link = document.createElement("a"); link.href = url; link.textContent = url;
                    document.execCommand("insertHTML", false, ReportMarkdown.sanitize(link.outerHTML));
                } else document.execCommand("createLink", false, url);
            }
        }
        element.dispatchEvent(new Event("input", { bubbles: true }));
    }

    function insert(element, html, text) {
        element.focus({ preventScroll: true });
        const escape = value => value.replace(/[&<>]/g, ch => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;" })[ch]);
        const content = html ? ReportMarkdown.sanitize(html) : escape(text || "").replace(/\r?\n/g, "<br>");
        document.execCommand("insertHTML", false, content);
    }

    function insertReference(element, id, label, saved) {
        element.focus({ preventScroll: true });
        restore(element, saved);
        const link = document.createElement("a");
        link.setAttribute("href", `#relison-report-cell-${id}`);
        link.dataset.reportReference = String(id);
        link.textContent = label;
        document.execCommand("insertHTML", false, link.outerHTML);
    }

    return { capture, restore, format, insert, insertReference };
})();
