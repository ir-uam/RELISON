/* Encode one RGBA frame at a time; retain only the compressed GIF stream. */
let library, encoder, width, height;
self.onmessage = async ({ data }) => {
    const { id, type } = data;
    try {
        if (type === "start") {
            // Use the published native ESM source. The CDN-transformed bundle
            // exposes only the default constructor and drops the palette helpers.
            const module = await import("https://cdn.jsdelivr.net/npm/gifenc@1.0.3/src/index.js");
            library = [module, module.default].find(candidate => candidate &&
                ["GIFEncoder", "quantize", "applyPalette"].every(name => typeof candidate[name] === "function"));
            if (!library) throw new Error("The GIF module did not expose a compatible encoder.");
            encoder = library.GIFEncoder();
            width = height = null;
        } else if (type === "frame") {
            if (!encoder) throw new Error("The GIF encoder is not initialized.");
            if (width !== null && (width !== data.width || height !== data.height)) throw new Error("Animation frame dimensions changed during recording.");
            width = data.width; height = data.height;
            const rgba = new Uint8Array(data.rgba);
            const palette = library.quantize(rgba, 256, { format: "rgb565" });
            const indexed = library.applyPalette(rgba, palette, "rgb565");
            encoder.writeFrame(indexed, width, height, { palette, delay: data.delay, repeat: 0 });
        } else if (type === "finish") {
            if (!encoder || width === null) throw new Error("No animation frames were captured.");
            encoder.finish();
            const bytes = encoder.bytes().slice();
            self.postMessage({ id, bytes }, [bytes.buffer]);
            encoder = null;
            return;
        } else throw new Error("Unknown GIF encoding request.");
        self.postMessage({ id });
    } catch (error) { self.postMessage({ id, error: error.message || String(error) }); }
};
