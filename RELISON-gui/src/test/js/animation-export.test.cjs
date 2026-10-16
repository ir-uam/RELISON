const test = require("node:test"), assert = require("node:assert/strict");
const fs = require("node:fs"), path = require("node:path"), vm = require("node:vm");
const read = name => fs.readFileSync(path.join(__dirname, "../../main/resources/public", name), "utf8");
const plain = value => JSON.parse(JSON.stringify(value));

function utility(extra = {}) {
    const context = vm.createContext({ window: {}, Blob, ...extra });
    vm.runInContext(read("animation-export.js"), context);
    return context.window.relisonAnimationExport;
}

test("HD and 4K export sizes exceed a small viewport, retain aspect ratio and use even codec dimensions", () => {
    const api = utility();
    assert.deepEqual(plain(api.frameSize(800, 450, 1920)), { width: 1920, height: 1080 });
    assert.deepEqual(plain(api.frameSize(800, 450, 3840)), { width: 3840, height: 2160 });
    assert.deepEqual(plain(api.frameSize(800, 600, 3840)), { width: 3840, height: 2880 });
    assert.deepEqual(plain(api.frameSize(1000, 700, null)), { width: 1000, height: 700 });
    const odd = api.frameSize(937, 613, 1920);
    assert.equal(odd.width % 2, 0); assert.equal(odd.height % 2, 0);
    assert.throws(() => api.frameSize(0, 0, 3840), /Show the visualization/);
    assert.ok(api.videoBitrate(3840, 2160) > api.videoBitrate(1920, 1080));
    assert.ok(api.videoBitrate(3840, 2160) >= 50_000_000);
});

test("vector frames rasterize at 4K intrinsic resolution and release their blob URLs", async () => {
    let blob, revoked, draw;
    const attrs = { viewBox: "0 0 800 450" };
    const api = utility({
        document: { createElement: () => ({ getContext: () => ({ drawImage: (...args) => { draw = args; } }) }) },
        DOMParser: class { parseFromString() { return { documentElement: {
            localName: "svg", querySelector: () => null, hasAttribute: key => key in attrs,
            setAttribute: (key, value) => { attrs[key] = value; },
        } }; } },
        XMLSerializer: class { serializeToString() { return JSON.stringify(attrs); } },
        Image: class { set src(value) { queueMicrotask(() => this.onload()); } },
        URL: { createObjectURL: value => { blob = value; return "blob:frame"; }, revokeObjectURL: value => { revoked = value; } },
    });
    const frame = await api.svgFrame("<svg/>", 800, 450, 3840);
    assert.equal(frame.width, 3840); assert.equal(frame.height, 2160);
    const serialized = JSON.parse(await blob.text());
    assert.equal(serialized.width, 3840); assert.equal(serialized.viewBox, "0 0 800 450");
    assert.deepEqual(draw.slice(1), [0, 0, 3840, 2160]);
    assert.equal(revoked, "blob:frame");
});

function recorders() {
    const downloads = [], events = [], streams = [], options = [];
    let failCapture = false;
    class Recorder {
        static isTypeSupported() { return true; }
        constructor(stream, settings) { this.state = "inactive"; options.push(settings); }
        start() { this.state = "recording"; }
        pause() { this.state = "paused"; events.push("pause"); }
        resume() { this.state = "recording"; events.push("resume"); }
        stop() { this.state = "inactive"; this.ondataavailable({ data: new Blob(["encoded"]) }); this.onstop(); }
    }
    const api = utility();
    api.gifEncoder = async () => ({
        addFrame: async frame => { events.push("encode" + frame.index); await Promise.resolve(); events.push("encoded" + frame.index); },
        finish: async () => new Blob(["GIF89a"]), close: () => events.push("close"),
    });
    const context = vm.createContext({
        window: { MediaRecorder: Recorder, relisonAnimationExport: api }, MediaRecorder: Recorder, Blob,
        document: { createElement: () => ({ getContext: () => ({ drawImage() {} }), captureStream: () => {
            const stream = { stopped: false, getTracks: () => [{ stop: () => { stream.stopped = true; } }] };
            streams.push(stream); return stream;
        } }) },
        setTimeout: fn => { queueMicrotask(fn); },
        setStatus() {}, download: (...args) => downloads.push(args),
    });
    const source = read("app.js");
    const functions = vm.runInContext(source.slice(source.indexOf("const FRAME_DELAY_MS"), source.indexOf("// Caps a frame list"))
        + "\n({recordGif,recordWebm})", context);
    let index;
    const spec = { name: "rotation", button: { disabled: false }, frameArgs: [0, 1, 2], delayMs: 100,
        setFrame: value => { index = value; events.push("frame" + index); },
        captureFrame: () => { if (failCapture && index > 0) throw new Error("capture failed"); return { width: 3840, height: 2160, index }; },
        restore: () => events.push("restore"),
    };
    return { ...functions, spec, events, streams, options, downloads, fail: () => { failCapture = true; } };
}

test("GIF encoding finishes each frame before capturing another and restores the view", async () => {
    const h = recorders(); await h.recordGif(h.spec);
    assert.deepEqual(h.events, ["frame0", "encode0", "encoded0", "frame1", "encode1", "encoded1", "frame2", "encode2", "encoded2", "close", "restore"]);
    assert.equal(h.downloads[0][0], "rotation.gif"); assert.equal(h.spec.button.disabled, false);
});

test("GIF worker accepts complete named or default module APIs", async () => {
    for (const useDefault of [true, false]) {
        const messages = [], encoded = [];
        const api = {
            GIFEncoder: () => ({ writeFrame: (...args) => encoded.push(args), finish() {}, bytes: () => Uint8Array.of(71, 73, 70) }),
            quantize: rgba => { assert.equal(rgba.length, 16); return [[0, 0, 0], [255, 255, 255]]; },
            applyPalette: () => Uint8Array.of(0, 1, 0, 1),
        };
        const context = vm.createContext({ self: { postMessage: message => messages.push(message) }, Uint8Array,
            loadModule: async () => useDefault ? { default: api } : api });
        vm.runInContext(read("animation-gif-worker.js").replace(/await import\("[^\"]+"\)/, "await loadModule()"), context);
        await context.self.onmessage({ data: { id: 1, type: "start" } });
        assert.equal(messages[0].error, undefined);
        await context.self.onmessage({ data: { id: 2, type: "frame", width: 2, height: 2, delay: 100, rgba: new ArrayBuffer(16) } });
        await context.self.onmessage({ data: { id: 3, type: "finish" } });
        assert.equal(encoded.length, 1); assert.equal(encoded[0][3].delay, 100);
        assert.deepEqual(Array.from(messages.at(-1).bytes), [71, 73, 70]);
        assert.ok(messages.every(message => !message.error));
    }
});

test("GIF worker reports incomplete default-constructor bundles before capturing frames", async () => {
    const messages = [];
    const context = vm.createContext({ self: { postMessage: message => messages.push(message) },
        loadModule: async () => ({ default: function GIFEncoder() {} }) });
    vm.runInContext(read("animation-gif-worker.js").replace(/await import\("[^\"]+"\)/, "await loadModule()"), context);
    await context.self.onmessage({ data: { id: 1, type: "start" } });
    assert.match(messages[0].error, /compatible encoder/);
});

test("4K WebM requests a high bitrate, pauses during rendering and stops capture tracks", async () => {
    const h = recorders(); await h.recordWebm(h.spec);
    assert.ok(h.options[0].videoBitsPerSecond >= 50_000_000);
    assert.match(h.options[0].mimeType, /video\/webm/);
    assert.equal(h.events.filter(event => event === "pause").length, 2);
    assert.equal(h.events.filter(event => event === "resume").length, 2);
    assert.ok(h.streams[0].stopped); assert.equal(h.events.at(-1), "restore");
    assert.equal(h.downloads[0][0], "rotation.webm");
});

test("failed WebM capture releases the stream and restores controls without downloading a partial video", async () => {
    const h = recorders(); h.fail(); await h.recordWebm(h.spec);
    assert.ok(h.streams[0].stopped); assert.equal(h.events.at(-1), "restore");
    assert.equal(h.spec.button.disabled, false); assert.equal(h.downloads.length, 0);
});

test("diffusion Cosmograph rerenders at 4K and restores its live pixel ratio, including on failure", async () => {
    const source = read("diffusion-cosmograph.js");
    const method = source.slice(source.indexOf("async captureFrame(targetWidth)"), source.indexOf("getDimensions() { return dimensions; }"));
    for (const fail of [false, true]) {
        let ratio = 1, capturedWidth;
        const canvas = { width: 800, height: 450, clientWidth: 800, clientHeight: 450 };
        const settings = [];
        const instance = {
            getCanvas: () => canvas, getConfig: () => ({ pixelRatio: ratio }),
            setConfigPartial: async config => { ratio = config.pixelRatio; settings.push(ratio); },
            resize: () => { canvas.width = 800 * ratio; canvas.height = 450 * ratio; },
        };
        const context = vm.createContext({ instance, container: {}, graphApi: {},
            window: { relisonAnimationExport: utility() },
            document: { documentElement: {}, createElement: () => ({ getContext: () => ({ fillRect() {}, drawImage: input => {
                capturedWidth = input.width; if (fail) throw new Error("drawing failed");
            } }) }) },
            getComputedStyle: () => ({ getPropertyValue: () => "#111111" }),
            requestAnimationFrame: fn => queueMicrotask(fn),
        });
        const capture = vm.runInContext("({" + method + "}).captureFrame", context);
        if (fail) await assert.rejects(capture(3840), /drawing failed/);
        else {
            const frame = await capture(3840);
            assert.equal(frame.width, 3840); assert.equal(frame.height, 2160);
        }
        assert.equal(capturedWidth, 3840); assert.deepEqual(settings, [4.8, 1]);
        assert.equal(ratio, 1); assert.equal(canvas.width, 800);
    }
});
