"use strict";

const NodiMetricHelp = (() => {
    let controller, workspace;
    const documentation = new Map();
    const helpKind = family => ({ recommendation: "model", "diffusion-protocol": "protocol",
        "diffusion-selection": "selection mechanism", "diffusion-expiration": "expiration mechanism",
        "diffusion-propagation": "propagation mechanism", "diffusion-update": "update mechanism",
        "diffusion-sight": "sight mechanism", "diffusion-stop": "stop condition" })[family] || "metric";
    function expressionFor(family, id, section) {
        const topic = (id + " " + section.title).toLowerCase();
        const page = section.source?.page || "";
        if (/gini|entropy|divergence|kld|pearson|assortativity/.test(topic)) return "compare";
        if (family.startsWith("diffusion-")) {
            if (family === "diffusion-stop") return "suggest";
            if (family === "diffusion-expiration") return "curious";
            if (family === "diffusion-update") return "explain";
            return "diffusion";
        }
        if (page === "models-paths.html" || /distance|shortest|path|betweenness|geodesic|closeness|harmonic|eccentricity|diameter|radius|katz|commute|hitting/.test(topic)) return "path";
        if (["models-ir.html", "models-mf.html"].includes(page) || /pagerank|eigenvector|hits|salsa/.test(topic)) return "compute";
        if (/community|partition|modularity|clustering|coreness/.test(topic) || ["community", "community-global", "edge-partition", "edge-partition-global"].includes(family)) return "discover";
        if (page === "models-foaf.html" || /neighbor|neighbour|overlap|embeddedness|reciprocity/.test(topic)) return "explore";
        if (family === "recommendation") return "suggest";
        return "explain";
    }
    function loadDocumentation(family = "node") {
        if (!documentation.has(family)) documentation.set(family, fetch(`help/${family}-metrics.json`)
            .then(response => { if (!response.ok) throw new Error("Documentation request failed"); return response.json(); })
            .then(data => { if (!data?.metrics || !data?.sections) throw new Error("Invalid documentation bundle"); return data; })
            .catch(error => { documentation.delete(family); throw error; }));
        return documentation.get(family);
    }

    function create({ button, selector, panel, bindings = [{ button, selector, family: "node" }], load = loadDocumentation, sanitize = html => {
        if (typeof DOMPurify === "undefined") throw new Error("Documentation renderer is unavailable");
        return DOMPurify.sanitize(html, { USE_PROFILES: { html: true, mathMl: true, svg: true }, ADD_ATTR: ["display", "encoding", "mathvariant"] });
    }, onOpen = () => { if (typeof NodiTutorials !== "undefined") NodiTutorials.dismiss(); } }) {
        const title = panel.querySelector("#node-metric-help-title");
        const intro = panel.querySelector("#node-metric-help-intro");
        const status = panel.querySelector("#node-metric-help-status");
        const content = panel.querySelector("#node-metric-help-content");
        const source = panel.querySelector("#node-metric-help-source");
        const retry = panel.querySelector("#node-metric-help-retry");
        const portrait = panel.querySelector("#node-metric-help-portrait");
        const avatar = panel.querySelector("#node-metric-help-avatar");
        const subtitle = panel.querySelector("#node-metric-help-subtitle");
        const closeButton = panel.querySelector("#node-metric-help-close");
        const choiceWrap = panel.querySelector("#node-metric-help-choice-wrap");
        const choice = panel.querySelector("#node-metric-help-choice");
        let revision = 0, shownId = null, current = bindings[0], choiceBinding = null;

        function showExpression(face) {
            avatar.setAttribute("data-nodi-face", face);
            portrait.src = `img/nodi-2d/nodi-${face}.png`;
        }

        function subject() { return current.selector.multiple ? choice : current.selector; }
        function refreshChoice() {
            choiceWrap.hidden = !current.selector.multiple;
            if (choiceWrap.hidden) return;
            const previous = choiceBinding === current ? choice.value : null;
            const options = [...current.selector.options];
            choice.replaceChildren(...options.map(original => {
                const option = document.createElement("option");
                option.value = original.value; option.textContent = original.textContent;
                return option;
            }));
            const preferred = previous || current.selector.selectedOptions[0]?.value;
            choice.value = options.some(option => option.value === preferred) ? preferred : options[0]?.value || "";
            choice.disabled = !options.length;
            choiceBinding = current;
        }

        async function render() {
            const { family } = current;
            const kind = helpKind(family), selected = subject();
            const version = ++revision, id = selected.value;
            const label = selected.selectedOptions?.[0]?.textContent || id;
            shownId = id;
            title.textContent = label || (kind[0].toUpperCase() + kind.slice(1) + " help");
            subtitle.textContent = `Let’s understand this ${kind}`;
            closeButton.setAttribute("aria-label", `Close ${kind} help`);
            intro.textContent = id ? `Here’s how the project documentation describes this ${kind}.` : `Choose a ${kind} and I’ll show you its documentation.`;
            content.replaceChildren(); source.hidden = true; retry.hidden = true;
            showExpression(id ? "wait" : "curious");
            status.textContent = id ? "Loading documentation…" : "";
            if (!id) return;
            try {
                const data = await load(family);
                if (version !== revision || panel.hidden || subject().value !== id) return;
                const entry = Object.hasOwn(data.metrics, id) ? data.metrics[id] : null;
                const section = entry && Object.hasOwn(data.sections, entry.section) ? data.sections[entry.section] : null;
                if (!section) {
                    intro.textContent = `I don’t have a documented description for this ${kind} yet.`;
                    status.textContent = "Not found in the project documentation.";
                    showExpression("attention");
                    source.href = "help/metrics.html";
                    source.textContent = `Browse documented ${kind}s`;
                } else {
                    // The catalogue supplies identity; explanatory text comes only from project documentation.
                    const html = sanitize(section.html);
                    if (version !== revision || panel.hidden) return;
                    content.innerHTML = html;
                    showExpression(expressionFor(family, id, section));
                    intro.textContent = section.title === label
                        ? `Here’s how the project documentation describes this ${kind}.`
                        : `The documentation describes this ${kind} in the ${section.title} section.`;
                    status.textContent = "";
                    source.href = "help/" + (section.source?.page || data.source?.page || "node-metrics.html") + "#" + section.anchor;
                    source.textContent = "Source: RELISON documentation · " + section.title;
                }
                source.hidden = false;
            } catch (_) {
                if (version !== revision || panel.hidden) return;
                content.replaceChildren();
                intro.textContent = "I couldn’t open the documentation. You can try again.";
                status.textContent = `Could not load ${kind} help.`;
                showExpression("reassure");
                retry.hidden = false;
            }
        }

        function sync() {
            for (const { button, selector, family } of bindings) {
                button.disabled = selector.multiple ? !selector.options.length : !selector.value;
                const label = selector.selectedOptions?.[0]?.textContent || selector.value;
                button.setAttribute("aria-label", selector.multiple ? "Help for diffusion metrics"
                    : label ? "Help for " + label : "Help for the selected " + helpKind(family));
            }
            if (!panel.hidden) {
                refreshChoice();
                if (shownId !== subject().value) void render();
            }
        }
        function open(binding = current) {
            if (binding.selector.multiple ? !binding.selector.options.length : !binding.selector.value) return;
            if (!panel.hidden && binding === current) { close(); return; }
            for (const item of bindings) item.button.setAttribute("aria-expanded", "false");
            current = binding;
            refreshChoice();
            onOpen(); panel.hidden = false; current.button.setAttribute("aria-expanded", "true");
            panel.scrollTop = 0;
            void render(); title.focus({ preventScroll: true });
        }
        function close(restoreFocus = true) {
            if (panel.hidden) return;
            revision++; panel.hidden = true; current.button.setAttribute("aria-expanded", "false");
            if (restoreFocus && current.button.getClientRects().length) current.button.focus();
        }
        for (const binding of bindings) {
            binding.button.addEventListener("click", () => open(binding));
            binding.selector.addEventListener("change", sync);
        }
        closeButton.addEventListener("click", () => close());
        retry.addEventListener("click", () => { void render(); });
        choice.addEventListener("change", () => {
            if (!panel.hidden && current.selector.multiple) void render();
        });
        document.addEventListener("keydown", event => {
            if (event.key !== "Escape" || panel.hidden || document.querySelector("dialog[open]")) return;
            event.preventDefault(); close();
        });
        sync();
        return { open, close, sync };
    }

    return { create,
        init() { controller = create({ panel: document.getElementById("node-metric-help"),
            bindings: [...document.querySelectorAll("[data-metric-help-for]")].map(button => ({ button,
                selector: document.getElementById(button.dataset.metricHelpFor), family: button.dataset.metricHelpFamily })) }); },
        sync() { controller?.sync(); },
        close(restoreFocus = true) { controller?.close(restoreFocus); },
        workspaceChanged(leftWidth, visible, nextWorkspace) {
            document.getElementById("node-metric-help").style.setProperty("--left-panel-width", leftWidth);
            if (!visible || (workspace != null && workspace !== nextWorkspace)) controller?.close(false);
            workspace = nextWorkspace;
        },
    };
})();
