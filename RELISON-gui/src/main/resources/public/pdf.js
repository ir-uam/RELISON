/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * MiniPDF — a tiny, dependency-free PDF writer, just enough to render the RELISON report directly to a PDF file
 * (headings, key/value blocks, bordered data tables and JPEG images) laid out over A4 pages with automatic
 * pagination. It emits a valid PDF 1.4 document using the base-14 Helvetica fonts (no font embedding) and DCTDecode
 * (JPEG) image XObjects, so the whole thing is a few hundred lines with no external libraries.
 *
 * Coordinates follow PDF conventions: origin at the bottom-left, units in points (72 per inch). The layout cursor
 * `y` tracks the top of the next element and decreases down the page; `newPage()` resets it to the top margin.
 */
(function () {
    "use strict";

    // ---- byte helpers -----------------------------------------------------

    // A growable buffer of bytes that accepts either Latin-1 strings (PDF syntax, ASCII/WinAnsi) or raw byte arrays
    // (JPEG streams), tracking the running length so object byte offsets can be recorded for the xref table.
    function ByteBuf() { this.chunks = []; this.length = 0; }
    ByteBuf.prototype.push = function (data) {
        let bytes;
        if (typeof data === "string") {
            bytes = new Uint8Array(data.length);
            for (let i = 0; i < data.length; i++) bytes[i] = data.charCodeAt(i) & 0xff;
        } else {
            bytes = data;
        }
        this.chunks.push(bytes);
        this.length += bytes.length;
        return this;
    };
    ByteBuf.prototype.toUint8Array = function () {
        const out = new Uint8Array(this.length);
        let off = 0;
        for (const c of this.chunks) { out.set(c, off); off += c.length; }
        return out;
    };

    function b64ToBytes(b64) {
        const bin = atob(b64);
        const bytes = new Uint8Array(bin.length);
        for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
        return bytes;
    }

    // Map the handful of non-ASCII characters the report uses onto plain ASCII. WinAnsi high bytes would render
    // slightly prettier, but ASCII is unambiguous in every viewer and survives any encoding hiccup — which matters,
    // because a wrong replacement here deletes the character silently rather than failing loudly (an em dash marks an
    // undefined metric value, so losing it would misreport that result as blank).
    // Anything else outside the byte range degrades to '?' so the PDF stays well-formed.
    const UNI = {
        "\u2026": "...",           // ellipsis
        "\u2022": "-",             // bullet
        "\u2013": "-",             // en dash
        "\u2014": "-",             // em dash
        "\u2018": "'",             // left single quote
        "\u2019": "'",             // right single quote
        "\u201C": "\"",            // left double quote
        "\u201D": "\"",            // right double quote
        "\u2192": "->",            // right arrow
        "\u00A0": " "              // non-breaking space
    };
    function sanitize(str) {
        str = String(str == null ? "" : str);
        let out = "";
        for (const ch of str) {
            if (UNI[ch] != null) { out += UNI[ch]; continue; }
            const code = ch.codePointAt(0);
            out += code <= 0xff ? ch : "?";
        }
        return out;
    }
    // Escape a (already sanitized) string for a PDF literal string: backslash-escape \ ( ) and control chars.
    function escLit(str) {
        return str.replace(/[\\()]/g, "\\$&").replace(/\r/g, "").replace(/\n/g, " ");
    }
    function n(v) { return (Math.round(v * 100) / 100).toString(); }

    // ---- document ---------------------------------------------------------

    function MiniPDF(opts) {
        opts = opts || {};
        this.pageW = opts.pageW || 595.28;   // A4 portrait
        this.pageH = opts.pageH || 841.89;
        this.margin = opts.margin || 48;
        this.x = this.margin;
        this.contentW = this.pageW - 2 * this.margin;
        this.pages = [];     // array of content-op strings, one per page
        this.cur = null;     // current page's op array
        this.y = 0;
        this.images = [];    // { name, w, h, bytes }
        this._imgByUrl = new Map();
        this._meas = document.createElement("canvas").getContext("2d");
        this.newPage();
    }

    // -- primitives ---------------------------------------------------------

    MiniPDF.prototype._op = function (s) { this.cur.push(s); };

    MiniPDF.prototype._measure = function (text, size, bold) {
        this._meas.font = (bold ? "bold " : "") + size + "px Helvetica, Arial, sans-serif";
        return this._meas.measureText(text).width;
    };

    // Word-wrap sanitized text to a maximum width, hard-breaking any single word that is itself too long.
    MiniPDF.prototype._wrap = function (text, maxW, size, bold) {
        text = sanitize(text);
        const self = this;
        const lines = [];
        const pushWord = function (line, word) {
            if (self._measure(word, size, bold) <= maxW) return word ? (line ? line + " " + word : word) : line;
            // The word alone overflows: flush the current line, then break the word by characters.
            if (line) { lines.push(line); line = ""; }
            let chunk = "";
            for (const ch of word) {
                if (self._measure(chunk + ch, size, bold) > maxW && chunk) { lines.push(chunk); chunk = ch; }
                else chunk += ch;
            }
            return chunk;
        };
        let line = "";
        for (const word of text.split(/\s+/)) {
            if (!word) continue;
            if (!line) { line = pushWord("", word); continue; }
            if (this._measure(line + " " + word, size, bold) <= maxW) line += " " + word;
            else { lines.push(line); line = pushWord("", word); }
        }
        if (line) lines.push(line);
        return lines.length ? lines : [""];
    };

    MiniPDF.prototype._fill = function (rgb) { this._op(n(rgb[0]) + " " + n(rgb[1]) + " " + n(rgb[2]) + " rg"); };
    MiniPDF.prototype._stroke = function (rgb) { this._op(n(rgb[0]) + " " + n(rgb[1]) + " " + n(rgb[2]) + " RG"); };

    MiniPDF.prototype._fillRect = function (x, y, w, h, rgb) {
        this._fill(rgb);
        this._op(n(x) + " " + n(y) + " " + n(w) + " " + n(h) + " re f");
    };
    MiniPDF.prototype._rectStroke = function (x, y, w, h, rgb, lw) {
        this._stroke(rgb || [0.83, 0.84, 0.86]);
        this._op(n(lw || 0.6) + " w " + n(x) + " " + n(y) + " " + n(w) + " " + n(h) + " re S");
    };
    MiniPDF.prototype._line = function (x1, y1, x2, y2, rgb, lw) {
        this._stroke(rgb || [0.83, 0.84, 0.86]);
        this._op(n(lw || 0.6) + " w " + n(x1) + " " + n(y1) + " m " + n(x2) + " " + n(y2) + " l S");
    };

    // Draws one line of text whose top sits at yTop. align: "left" | "right" | "center".
    MiniPDF.prototype._textLine = function (text, x, yTop, size, bold, rgb, align) {
        const s = escLit(sanitize(text));
        let bx = x;
        if (align === "right") bx = x - this._measure(sanitize(text), size, bold);
        else if (align === "center") bx = x - this._measure(sanitize(text), size, bold) / 2;
        const by = yTop - size * 0.82;   // baseline from the top of the line box (Helvetica ascent ≈ 0.82 em)
        this._fill(rgb || [0, 0, 0]);
        this._op("BT /" + (bold ? "F2" : "F1") + " " + n(size) + " Tf " + n(bx) + " " + n(by) + " Td (" + s + ") Tj ET");
    };

    // -- layout -------------------------------------------------------------

    MiniPDF.prototype.newPage = function () { this.cur = []; this.pages.push(this.cur); this.y = this.pageH - this.margin; };
    MiniPDF.prototype._ensure = function (h) { if (this.y - h < this.margin) this.newPage(); };
    MiniPDF.prototype.space = function (h) { this.y -= h; };

    MiniPDF.prototype.h1 = function (text) {
        const size = 20, lh = size * 1.25;
        const lines = this._wrap(text, this.contentW, size, true);
        for (const l of lines) { this._ensure(lh); this._textLine(l, this.x, this.y, size, true, [0.1, 0.11, 0.13]); this.y -= lh; }
        this.y -= 4;
    };
    MiniPDF.prototype.meta = function (text) {
        const size = 9, lh = size * 1.5;
        this._ensure(lh);
        this._textLine(text, this.x, this.y, size, false, [0.37, 0.39, 0.41]);
        this.y -= lh + 4;
    };
    MiniPDF.prototype.h2 = function (text) {
        const size = 14, lh = size * 1.3;
        this.y -= 12;                                  // space above
        this._ensure(lh + 8);
        this._textLine(text, this.x, this.y, size, true, [0.1, 0.11, 0.13]);
        this.y -= lh;
        this._line(this.x, this.y + 2, this.x + this.contentW, this.y + 2, [0.83, 0.84, 0.86], 0.8);
        this.y -= 8;
    };
    MiniPDF.prototype.h3 = function (text) {
        const size = 11, lh = size * 1.3;
        this.y -= 6;
        this._ensure(lh);
        this._textLine(text, this.x, this.y, size, true, [0.2, 0.21, 0.23]);
        this.y -= lh + 2;
    };

    // A borderless key/value block: bold-ish gray key on the left, value on the right of a fixed key column.
    MiniPDF.prototype.kvTable = function (rows) {
        if (!rows || !rows.length) return;
        const size = 9.5, lh = size * 1.45, keyW = Math.min(150, this.contentW * 0.4);
        for (const row of rows) {
            const valLines = this._wrap(row[1], this.contentW - keyW, size, false);
            const rowH = Math.max(lh, lh * valLines.length);
            this._ensure(rowH);
            const yTop = this.y;
            this._textLine(row[0], this.x, yTop, size, true, [0.37, 0.39, 0.41]);
            let ty = yTop;
            for (const l of valLines) { this._textLine(l, this.x + keyW, ty, size, false, [0.1, 0.11, 0.13]); ty -= lh; }
            this.y -= rowH;
        }
        this.y -= 8;
    };

    // A bordered two-column data table (label + right-aligned value), with a shaded header row repeated after a
    // page break so a long table stays readable across pages.
    MiniPDF.prototype.dataTable = function (headA, headB, rows) {
        if (!rows || !rows.length) return;
        const size = 9.5, pad = 5, lh = size * 1.3;
        const valW = Math.min(150, this.contentW * 0.32), labW = this.contentW - valW;
        const header = () => {
            const hH = lh + 2 * pad;
            this._ensure(hH);
            const yTop = this.y;
            this._fillRect(this.x, yTop - hH, this.contentW, hH, [0.93, 0.94, 0.95]);
            this._rectStroke(this.x, yTop - hH, this.contentW, hH);
            this._line(this.x + labW, yTop, this.x + labW, yTop - hH);
            this._textLine(headA, this.x + pad, yTop - pad, size, true, [0.2, 0.21, 0.23], "left");
            this._textLine(headB, this.x + this.contentW - pad, yTop - pad, size, true, [0.2, 0.21, 0.23], "right");
            this.y = yTop - hH;
        };
        header();
        for (const row of rows) {
            const labLines = this._wrap(row[0], labW - 2 * pad, size, false);
            const rowH = Math.max(lh, lh * labLines.length) + 2 * pad;
            if (this.y - rowH < this.margin) { this.newPage(); header(); }
            const yTop = this.y;
            this._rectStroke(this.x, yTop - rowH, this.contentW, rowH);
            this._line(this.x + labW, yTop, this.x + labW, yTop - rowH);
            let ty = yTop - pad;
            for (const l of labLines) { this._textLine(l, this.x + pad, ty, size, false, [0.1, 0.11, 0.13], "left"); ty -= lh; }
            this._textLine(String(row[1]), this.x + this.contentW - pad, yTop - pad, size, false, [0.1, 0.11, 0.13], "right");
            this.y = yTop - rowH;
        }
        this.y -= 10;
    };

    // An N-column bordered table (the evaluation grid: one row per algorithm, one column per metric). The first
    // column holds names and gets a wider share; the rest are numeric and right-aligned. The font shrinks as columns
    // are added so a wide table still fits the page, and the header row repeats after a page break.
    MiniPDF.prototype.gridTable = function (headers, rows) {
        if (!headers || !headers.length) return;
        const n = headers.length;
        const size = n <= 5 ? 9 : (n <= 8 ? 8 : 7);
        const lh = size * 1.3, pad = 4;

        // Give the name column up to a third of the width, but never squeeze the numeric ones below a usable share.
        const firstW = Math.min(this.contentW * 0.34, Math.max(80, (this.contentW / n) * 1.8));
        const restW = n > 1 ? (this.contentW - firstW) / (n - 1) : 0;
        const widths = [firstW];
        for (let i = 1; i < n; i++) widths.push(restW);

        const self = this;
        const drawRow = function (cells, bold, fill, isHeader) {
            const wrapped = [];
            let lines = 1;
            for (let i = 0; i < n; i++) {
                const w = self._wrap(cells[i] == null ? "" : String(cells[i]), widths[i] - 2 * pad, size, bold);
                wrapped.push(w);
                if (w.length > lines) lines = w.length;
            }
            const rowH = lines * lh + 2 * pad;
            // isHeader guards the recursion: a header that will not fit is redrawn once on the fresh page, not forever.
            if (self.y - rowH < self.margin) { self.newPage(); if (!isHeader) drawHeader(); }
            const yTop = self.y;
            if (fill) self._fillRect(self.x, yTop - rowH, self.contentW, rowH, [0.93, 0.94, 0.95]);
            self._rectStroke(self.x, yTop - rowH, self.contentW, rowH);
            let cx = self.x;
            for (let i = 0; i < n; i++) {
                if (i > 0) self._line(cx, yTop, cx, yTop - rowH);
                const right = i > 0;
                let ty = yTop - pad;
                for (const ln of wrapped[i]) {
                    self._textLine(ln, right ? cx + widths[i] - pad : cx + pad, ty, size, bold,
                        [0.1, 0.11, 0.13], right ? "right" : "left");
                    ty -= lh;
                }
                cx += widths[i];
            }
            self.y = yTop - rowH;
        };
        const drawHeader = function () { drawRow(headers, true, true, true); };

        drawHeader();
        for (const r of rows) drawRow(r, false, false, false);
        this.y -= 10;
    };

    // Loads a data-URL image, re-encodes it to JPEG (opaque, over white) and registers it as a PDF image XObject.
    // Returns a promise of { name, w, h } (or null on failure). Idempotent per URL.
    MiniPDF.prototype.registerImage = function (dataUrl) {
        if (!dataUrl) return Promise.resolve(null);
        if (this._imgByUrl.has(dataUrl)) return Promise.resolve(this._imgByUrl.get(dataUrl));
        const self = this;
        return new Promise((resolve) => {
            const img = new Image();
            img.onload = function () {
                try {
                    const c = document.createElement("canvas");
                    c.width = img.naturalWidth || img.width;
                    c.height = img.naturalHeight || img.height;
                    const cx = c.getContext("2d");
                    cx.fillStyle = "#ffffff";
                    cx.fillRect(0, 0, c.width, c.height);
                    cx.drawImage(img, 0, 0);
                    const jpeg = c.toDataURL("image/jpeg", 0.92);
                    const rec = { name: "Im" + (self.images.length + 1), w: c.width, h: c.height, bytes: b64ToBytes(jpeg.split(",")[1]) };
                    self.images.push(rec);
                    self._imgByUrl.set(dataUrl, rec);
                    resolve(rec);
                } catch (e) { resolve(null); }
            };
            img.onerror = function () { resolve(null); };
            img.src = dataUrl;
        });
    };

    // Places a registered image (optionally under a small title), scaled to the content width, breaking to a new
    // page when it does not fit and scaling down to the page when it is taller than a whole page.
    MiniPDF.prototype.imageBlock = function (rec, title) {
        if (!rec) return;
        const titleH = title ? 11 * 1.3 + 2 : 0;
        const avail = this.pageH - 2 * this.margin - titleH;   // usable image height on a fresh page
        let w = this.contentW, h = rec.h * (this.contentW / rec.w);
        if (h > avail) { h = avail; w = rec.w * (avail / rec.h); }
        const need = titleH + h + 8;
        if (this.y - need < this.margin) this.newPage();
        if (title) { this._textLine(title, this.x, this.y, 11, true, [0.2, 0.21, 0.23]); this.y -= titleH; }
        const ix = this.x + (this.contentW - w) / 2;   // centre horizontally
        const iy = this.y - h;
        this._op("q " + n(w) + " 0 0 " + n(h) + " " + n(ix) + " " + n(iy) + " cm /" + rec.name + " Do Q");
        this._rectStroke(ix, iy, w, h, [0.83, 0.84, 0.86], 0.6);
        this.y = iy - 10;
    };

    // -- serialization ------------------------------------------------------

    MiniPDF.prototype.build = function () {
        const buf = new ByteBuf();
        buf.push("%PDF-1.4\n%âãÏÓ\n");   // binary marker comment

        // Object numbering: 1 Catalog, 2 Pages, 3 F1, 4 F2, then images, then (Page, Content) per page.
        const numImages = this.images.length, numPages = this.pages.length;
        const imgStart = 5;
        const pageStart = imgStart + numImages;
        const offsets = {};
        const begin = (num) => { offsets[num] = buf.length; buf.push(num + " 0 obj\n"); };
        const end = () => buf.push("\nendobj\n");

        // 1: Catalog
        begin(1); buf.push("<< /Type /Catalog /Pages 2 0 R >>"); end();

        // 2: Pages
        const kids = [];
        for (let p = 0; p < numPages; p++) kids.push((pageStart + p * 2) + " 0 R");
        begin(2); buf.push("<< /Type /Pages /Kids [" + kids.join(" ") + "] /Count " + numPages + " >>"); end();

        // 3, 4: fonts
        begin(3); buf.push("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"); end();
        begin(4); buf.push("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>"); end();

        // image XObjects
        for (let i = 0; i < numImages; i++) {
            const rec = this.images[i];
            begin(imgStart + i);
            buf.push("<< /Type /XObject /Subtype /Image /Width " + rec.w + " /Height " + rec.h +
                " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length " + rec.bytes.length + " >>\nstream\n");
            buf.push(rec.bytes);
            buf.push("\nendstream");
            end();
        }

        // The XObject sub-dictionary is shared by every page (referencing an image a page does not draw is harmless).
        let xobj = "";
        for (let i = 0; i < numImages; i++) xobj += "/" + this.images[i].name + " " + (imgStart + i) + " 0 R ";
        const resources = "<< /Font << /F1 3 0 R /F2 4 0 R >> /XObject << " + xobj + ">> /ProcSet [/PDF /Text /ImageC] >>";

        // pages + content streams
        for (let p = 0; p < numPages; p++) {
            const pageNum = pageStart + p * 2, contentNum = pageNum + 1;
            begin(pageNum);
            buf.push("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + n(this.pageW) + " " + n(this.pageH) + "] /Resources " +
                resources + " /Contents " + contentNum + " 0 R >>");
            end();

            const content = this.pages[p].join("\n") + "\n";
            begin(contentNum);
            buf.push("<< /Length " + content.length + " >>\nstream\n" + content + "endstream");
            end();
        }

        // xref
        const totalObjs = pageStart + numPages * 2;   // one past the last object number
        const xrefOff = buf.length;
        buf.push("xref\n0 " + totalObjs + "\n");
        buf.push("0000000000 65535 f \n");
        for (let num = 1; num < totalObjs; num++) {
            const off = offsets[num] || 0;
            buf.push(("0000000000" + off).slice(-10) + " 00000 n \n");
        }
        buf.push("trailer\n<< /Size " + totalObjs + " /Root 1 0 R >>\nstartxref\n" + xrefOff + "\n%%EOF");

        return buf.toUint8Array();
    };

    window.MiniPDF = MiniPDF;
})();
