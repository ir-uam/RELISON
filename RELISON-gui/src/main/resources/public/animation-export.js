/* Shared animation sizing, SVG rasterization and bounded-memory GIF encoding. */
(() => {
    function frameSize(sourceWidth, sourceHeight, targetWidth = null) {
        if (!(sourceWidth > 0 && sourceHeight > 0)) throw new Error("Show the visualization before recording.");
        const width = Number.isFinite(targetWidth) && targetWidth > 0 ? targetWidth : sourceWidth;
        const even = value => Math.max(2, Math.round(value / 2) * 2);
        return { width: even(width), height: even(sourceHeight * width / sourceWidth) };
    }

    async function svgFrame(source, sourceWidth, sourceHeight, targetWidth = null) {
        const size = frameSize(sourceWidth, sourceHeight, targetWidth);
        const canvas = document.createElement("canvas");
        canvas.width = size.width; canvas.height = size.height;
        const svg = new DOMParser().parseFromString(source, "image/svg+xml").documentElement;
        if (svg.localName !== "svg" || svg.querySelector("parsererror")) throw new Error("Could not read the animation SVG.");
        if (!svg.hasAttribute("viewBox")) svg.setAttribute("viewBox", `0 0 ${sourceWidth} ${sourceHeight}`);
        svg.setAttribute("width", size.width); svg.setAttribute("height", size.height);
        const url = URL.createObjectURL(new Blob([new XMLSerializer().serializeToString(svg)], { type: "image/svg+xml" }));
        try {
            const image = new Image();
            await new Promise((resolve, reject) => {
                image.onload = resolve;
                image.onerror = () => reject(new Error("Could not render an animation frame."));
                image.src = url;
            });
            canvas.getContext("2d").drawImage(image, 0, 0, size.width, size.height);
            return canvas;
        } finally { URL.revokeObjectURL(url); }
    }

    function videoBitrate(width, height) {
        return Math.round(Math.max(8_000_000, Math.min(80_000_000, width * height * 30 * 0.25)));
    }

    async function gifEncoder() {
        const worker = new Worker("animation-gif-worker.js", { type: "module" });
        const pending = new Map();
        let nextId = 0;
        const fail = error => {
            pending.forEach(({ reject, timer }) => { clearTimeout(timer); reject(error); });
            pending.clear();
        };
        worker.onmessage = ({ data }) => {
            const job = pending.get(data.id);
            if (!job) return;
            pending.delete(data.id); clearTimeout(job.timer);
            if (data.error) job.reject(new Error(data.error));
            else job.resolve(data);
        };
        worker.onerror = event => fail(new Error(event.message || "The GIF encoder failed to load."));
        const request = (type, data = {}, transfers = []) => new Promise((resolve, reject) => {
            const id = ++nextId;
            const timer = setTimeout(() => { pending.delete(id); reject(new Error("The GIF encoder timed out.")); }, 120000);
            pending.set(id, { resolve, reject, timer });
            try { worker.postMessage({ id, type, ...data }, transfers); }
            catch (error) { pending.delete(id); clearTimeout(timer); reject(error); }
        });
        const close = () => { worker.terminate(); fail(new Error("GIF encoding stopped.")); };
        try { await request("start"); }
        catch (error) { close(); throw error; }
        return {
            async addFrame(canvas, delay) {
                const { data } = canvas.getContext("2d").getImageData(0, 0, canvas.width, canvas.height);
                await request("frame", { width: canvas.width, height: canvas.height, delay, rgba: data.buffer }, [data.buffer]);
            },
            async finish() {
                const result = await request("finish");
                return new Blob([result.bytes], { type: "image/gif" });
            },
            close,
        };
    }
    window.relisonAnimationExport = { frameSize, svgFrame, videoBitrate, gifEncoder };
})();
