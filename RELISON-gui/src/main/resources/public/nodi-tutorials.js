"use strict";

// Tours explain controls and navigate views. Only the user imports, edits or computes data.
const NodiTutorials = (() => {
    const step = (tab, target, title, body, face = "explain", view = "") => ({ tab, target, title, body, face, view });
    const courses = [
        { id: "welcome", title: "Find your way around", description: "Meet the workspaces and keep your work safe.", tab: "import", steps: [
            step("import", "#tabbar", "Hi, I’m Nodi!", "I’m your friendly network guide. These tabs take you from loading a network to exploring, analysing and understanding it. You can try controls as we go, then choose Next when you’re ready.", "welcome"),
            step("import", "#btn-session", "Keep your work", "Use this menu to save a .relison session or open one you saved earlier. Save a session to return to your network and its supported analysis results later.", "suggest"),
            step("import", "#processes-button", "Follow a computation", "Open Processes to see running and recently finished computations. You can check their progress and cancel a run here, even while you explore another workspace.", "curious"),
            step("import", "#settings-button", "Make yourself comfortable", "Settings changes the interface appearance, chart style and colours. The moon or sun button beside it switches between dark and light mode. Come back to Tutorials whenever you want a hand.", "suggest"),
        ] },
        { id: "import", title: "Load your first network", description: "Import a file or generate a network to explore.", tab: "import", steps: [
            step("import", "#file-input", "Start with a network", "Choose an edge list, Pajek or GEXF network, or a saved .relison session. An edge list has a source and target on each row, with an optional weight in the third column.", "welcome"),
            step("import", "#opt-format", "Check the format", "Auto-detect uses the file extension. For edge lists, check the field separator and whether the first row contains headers. GEXF and saved sessions expose their own relevant options.", "curious"),
            step("import", "#btn-load", "Load when you’re ready", "Check the available graph options, then choose Load network. Directed links have a source and target; weighted links carry a value. Choose options that describe your data.", "explain"),
            step("import", "#gen-type", "No file? Generate a network", "Choose a model under Generate network and adjust its parameters. A small random network is a useful place to practise. Choose Generate when you’re ready; loading or generating replaces the active network, so save existing work first.", "suggest"),
            step("import", "#nodi-button", "Choose your next adventure", "After loading a network, open Tutorials and try Explore the network. You’ll learn about views, layouts and appearance. I’ll be right here whenever you need me.", "welcome"),
        ] },
        { id: "network", title: "Explore the network", description: "Views, layouts, appearance and selection.", tab: "network", needsGraph: true, steps: [
            step("network", "#network-renderer", "Choose a view", "Cosmograph offers 2D and 3D views. Sigma offers another 2D view. Geographic map / globe uses latitude and longitude attributes configured under Advanced layout settings.", "welcome"),
            step("network", "#layout-type", "Give your network a shape", "Choose a layout algorithm on the left. Use Start layout or Apply layout below it, depending on the algorithm. Animated layouts can be stopped when you’re happy with their positions.", "curious"),
            step("network", "#node-color-mode", "Let colour tell a story", "The appearance controls on the left change node colour and size. Computed metrics, community assignments and attributes become available where supported. Try a mapping that helps answer your question.", "suggest"),
            step("network", "#right", "Meet your nodes and links", "Click a node or link in the network to inspect it on the right. The selection controls also support search, random selection and shortest paths. Pan and zoom to explore; 3D views also let you rotate.", "curious"),
            step("network", "#network-toolbar", "Share what you see", "Use the Export menu above the canvas for network images or a GEXF graph. Choose PNG for an image or SVG for supported vector output. Use the session menu in the top bar when you want to save your working session.", "suggest"),
        ] },
        { id: "tables", title: "Inspect nodes and edges", description: "Browse data, build filters and export tables.", tab: "tables", needsGraph: true, steps: [
            step("tables", "#tablesubtabbar", "Look underneath the picture", "Nodes and Edges have separate tables. Each row identifies an entity, with attributes and computed values in its columns.", "curious", "nodes"),
            step("tables", "#btn-filter-builder-nodes", "Focus on a question", "Choose Build filters to combine conditions with AND, OR and groups. Review the matching rows and apply your filter when ready. Node and edge filters have separate controls.", "explain", "nodes"),
            step("tables", "#btn-node-columns", "Choose what to see", "Use the column controls to show the attributes and results you need. Tables also offer sorting and pagination for exploring larger networks.", "suggest", "nodes"),
            step("tables", "#btn-export-nodes", "Take the table with you", "Export CSV downloads table data for use in other tools. Switch to Edges for the equivalent link controls. Save a .relison session when you want to return to your analysis in Spectrum.", "suggest", "nodes"),
        ] },
        { id: "communities", title: "Discover communities", description: "Detect groups and explore their connections.", tab: "communities", needsGraph: true, steps: [
            step("communities", "#community-algo", "Find groups in your network", "Choose a community detection algorithm and review its parameters. Detection uses the whole loaded network, independently of the filters in Nodes & Edges.", "curious"),
            step("communities", "#btn-community", "Detect a partition", "Choose Detect to compute the configured partition. Processes tracks the run. Different algorithms and parameter choices can reveal different groupings; use the one that fits your analysis.", "explain"),
            step("communities", "#explorer-partition", "Keep track of your partitions", "Select a stored partition here. The Graph and Table views share the selected community and inspector, so you can explore members and connections from either view.", "welcome"),
            step("communities", "#explorer-metrics", "Measure the groups", "View in Metrics takes the selected partition to the metric workspace. Compute community metrics there to compare group properties and assess the partition.", "suggest"),
        ] },
        { id: "edge-partitions", title: "Explore edge groups", description: "Group links by attributes or communities.", tab: "edge-partitions", needsGraph: true, steps: [
            step("edge-partitions", "#edge-partition-kind", "Look for patterns in links", "Group links using numeric edge-attribute bins, endpoint attributes, or existing communities. Review the controls for the grouping you choose. Community-based choices need a community partition first.", "curious"),
            step("edge-partitions", "#btn-edge-partition", "Detect your edge groups", "Choose Detect edge groups when the configuration is ready. Detection uses the whole loaded network. The partition selector below lets you return to stored groups.", "explain"),
            step("edge-partitions", "#edge-partition-view-metrics", "Compare the groups", "Explore the groups and their members, then use View in Metrics to compute properties for the selected edge partition.", "suggest"),
        ] },
        { id: "metrics", title: "Measure and compare", description: "Compute metrics, inspect rankings and chart results.", tab: "metrics", needsGraph: true, steps: [
            step("metrics", "#subtabbar", "Choose what to measure", "The metric type tabs separate global, node, edge, node-pair and group results. Let’s look at Node metrics. A metric turns a network property into a value you can inspect and compare.", "curious", "nodes"),
            step("metrics", "#vertex-metric", "Configure a metric", "The Compute metrics panel lets you choose a metric and its parameters. Choose Compute to run it. Processes in the top bar tracks its progress; results appear when the computation finishes.", "explain", "compute-nodes"),
            step("metrics", "#metric-nodes-rankings-tab", "Who stands out?", "Rankings orders nodes by a computed metric. Choose a result and adjust top or bottom k to inspect the entities that matter to your question.", "curious", "nodes"),
            step("metrics", "#metric-nodes-distributions-tab", "See the bigger picture", "Distributions summarises how values are spread. Compare lets you examine two metrics, or original and recommendation results when those results are available. Run a metric first to populate these views.", "suggest", "nodes"),
        ] },
        { id: "recommend", title: "Explore recommendations", description: "Run a method and inspect its suggested links.", tab: "recommend", needsGraph: true, steps: [
            step("recommend", "#rec-mode", "Choose the task", "Choose recommendation or link prediction, then select an algorithm and review its parameters. This workspace requires a simple graph; it is unavailable for multigraphs.", "curious"),
            step("recommend", "#btn-rec-apply", "Run your method", "Choose Apply to compute results with your configuration. Watch Processes for progress or cancellation. Computed results can then be selected and inspected.", "explain"),
            step("recommend", "#rec-computed", "Inspect before adding links", "Use the result selector to switch between computed methods. Review their suggested links in the table. Network appearance controls let you display recommendation overlays; adding links is a separate action.", "suggest"),
        ] },
        { id: "diffusion", title: "Follow information spreading", description: "Prepare pieces, run a simulation and inspect it.", tab: "diffusion", needsGraph: true, steps: [
            step("diffusion", ".diff-subtabbar", "Give information a starting point", "Start in Information pieces. Add, upload or generate pieces with creators from your network, and prepare the features your protocol needs. These pieces are the input to the simulation.", "welcome", "pieces"),
            step("diffusion", "#diff-protocol-type", "Choose how information spreads", "Select a preset protocol or configure custom mechanisms. Review their parameters, choose a stop condition and select the metrics you want the simulation to record.", "curious", "pieces"),
            step("diffusion", "#btn-diff-run", "Run the simulation", "Give the run a label if you like, then choose Run simulation. Processes reports its progress. Results become available when the run finishes.", "explain", "pieces"),
            step("diffusion", ".diff-subtabbar", "Replay and investigate", "Graph visualization plays back the selected run. Analysis lets you inspect runs, metrics, nodes, pieces, features and distributions, and compare runs. Saved sessions retain run configurations and metric curves, but not playback simulation files.", "suggest", "pieces"),
        ] },
        { id: "reports", title: "Tell the network’s story", description: "Build a report with text and network snapshots.", tab: "reports", steps: [
            step("", "#btn-report", "Bring your findings together", "The Reports button opens the report workspace. Create a new report or return to an open draft there. Your drafts stay available while this page remains open.", "welcome"),
            step("reports", "#pane-reports", "Build your narrative", "Choose New report if you need a draft, then add text, graph snapshots, top-k tables or metric charts. Text supports visual editing and Markdown. Table and chart cells currently configure placeholders; metric execution is not connected yet.", "explain"),
            step("reports", "#pane-reports", "Capture the network", "In a graph snapshot cell, choose Capture from network. Adjust the network view, then use Capture for report to return with an image. A network is needed for a capture. Add captions and references to connect figures to your text.", "curious"),
            step("reports", "#pane-reports", "Export your report", "Preview your draft, then use Export PDF to choose a page size and orientation. Drafts are held in memory: report saving and loading are not connected yet, and .relison sessions do not include reports. Export before closing or reloading.", "suggest"),
        ] },
    ];
    const storageKey = "relison-nodi-tutorials-v1";

    function createProgress(storage) {
        let data = { progress: {}, completed: [] };
        try {
            const saved = JSON.parse(storage?.getItem(storageKey) || "null");
            for (const course of courses) {
                const index = saved?.progress?.[course.id];
                if (Number.isInteger(index) && index >= 0 && index < course.steps.length) data.progress[course.id] = index;
                if (Array.isArray(saved?.completed) && saved.completed.includes(course.id)) data.completed.push(course.id);
            }
        } catch (_) { /* Storage can be blocked or contain an older/invalid value. */ }
        function persist() { try { storage?.setItem(storageKey, JSON.stringify(data)); } catch (_) { /* In-memory progress still works. */ } }
        return {
            index: id => data.progress[id] ?? 0,
            started: id => Object.hasOwn(data.progress, id),
            completed: id => data.completed.includes(id),
            save(id, index) {
                const course = courses.find(course => course.id === id);
                if (!course || !Number.isInteger(index) || index < 0 || index >= course.steps.length) return;
                data.progress[id] = index; persist();
            },
            finish(id) {
                if (!courses.some(course => course.id === id)) return;
                delete data.progress[id];
                if (!data.completed.includes(id)) data.completed.push(id);
                persist();
            },
        };
    }

    const escape = value => String(value).replace(/[&<>"']/g, ch => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[ch]);
    const faceImage = (face, size) => `<span class="nodi-avatar" data-nodi-face="${face}"><img src="img/nodi-2d/nodi-${face}.png" alt="Nodi, your friendly network guide" width="${size}" height="${size}"/></span>`;
    let controller;

    function create({ getContext, navigate, storage }) {
        const progress = createProgress(storage);
        const motionStorageKey = "relison-nodi-motion-v1";
        const reducedMotion = window.matchMedia?.("(prefers-reduced-motion: reduce)");
        let motionPaused = false;
        try { motionPaused = storage?.getItem(motionStorageKey) === "paused"; } catch (_) { /* The control still works in memory. */ }
        const library = document.createElement("dialog");
        library.id = "nodi-library"; library.className = "nodi-library";
        library.setAttribute("aria-labelledby", "nodi-library-title");
        const guide = document.createElement("aside");
        guide.id = "nodi-guide"; guide.className = "nodi-guide"; guide.hidden = true;
        guide.setAttribute("aria-labelledby", "nodi-step-title");
        guide.setAttribute("aria-describedby", "nodi-step-body");
        const highlight = document.createElement("div");
        highlight.className = "nodi-highlight"; highlight.hidden = true; highlight.setAttribute("aria-hidden", "true");
        document.body.append(library, highlight, guide);
        let active = null, index = 0, finished = false, frame = 0, navigating = false;
        let returnFocus = null;
        const blocked = course => course.needsGraph && !getContext().hasGraph ? "Load a network first" :
            course.id === "recommend" && getContext().multigraph ? "Requires a simple graph" : "";
        const currentStep = () => active?.steps[index];

        function motionLabel() { return reducedMotion?.matches ? "Motion reduced" : motionPaused ? "Animate Nodi" : "Pause Nodi"; }
        function motionIcon() { return reducedMotion?.matches || motionPaused ? "motion_photos_off" : "motion_photos_on"; }
        function motionButton(compact = false) {
            return `<button type="button" class="nodi-motion-toggle" data-nodi="motion" data-compact="${compact}"><span class="material-symbols-outlined" aria-hidden="true">${motionIcon()}</span>${compact ? "" : `<span>${motionLabel()}</span>`}</button>`;
        }
        function syncMotion() {
            const enabled = !motionPaused && !reducedMotion?.matches;
            for (const root of [library, guide, document.getElementById("nodi-welcome")]) {
                root.dataset.nodiMotion = enabled ? "on" : "off";
                const button = root.querySelector('[data-nodi="motion"]');
                if (!button) continue;
                button.disabled = !!reducedMotion?.matches;
                button.setAttribute("aria-label", motionLabel());
                button.title = reducedMotion?.matches ? "Nodi follows your system’s reduced motion setting." : motionLabel();
                button.innerHTML = `<span class="material-symbols-outlined" aria-hidden="true">${motionIcon()}</span>${button.dataset.compact === "true" ? "" : `<span>${motionLabel()}</span>`}`;
            }
        }
        function toggleMotion() {
            if (reducedMotion?.matches) return;
            motionPaused = !motionPaused;
            try { storage?.setItem(motionStorageKey, motionPaused ? "paused" : "on"); } catch (_) { /* Keep the preference for this page. */ }
            // Keep focus, the current step and the surrounding tutorial DOM intact.
            syncMotion();
        }

        function renderLibrary() {
            const context = getContext();
            library.innerHTML = `<header class="nodi-library-head">${faceImage("welcome", 112)}<div class="nodi-speech"><p class="nodi-eyebrow">Nodi · Your friendly network guide</p><h1 id="nodi-library-title">What shall we explore?</h1><p>Pick a tutorial and I’ll walk you through it, one step at a time.</p></div><button type="button" class="nodi-dismiss" data-nodi="close-library" aria-label="Close tutorials"><span class="material-symbols-outlined" aria-hidden="true">close</span></button></header>
                <div class="nodi-library-body"><div class="nodi-library-context"><p>${context.hasGraph ? "You can try things in the interface as we explore together. Choose a tour below to join me." : "Let’s start with a quick tour. Once you load or generate a network, I can show you more."}</p>${motionButton()}</div>
                <div class="nodi-courses">${courses.map(course => {
                    const reason = blocked(course), done = progress.completed(course.id), started = progress.started(course.id);
                    return `<article class="nodi-course" data-suggested="${course.tab === context.tab && !reason}"><h2>${escape(course.title)}</h2><span class="nodi-course-status">${done ? "✓ Completed" : started ? "In progress" : ""}</span><p>${escape(course.description)}</p><span class="nodi-course-meta">${reason || (started ? `Step ${progress.index(course.id) + 1} of ` : "") + course.steps.length + (started ? "" : " steps")}</span><button type="button" data-course="${course.id}" ${reason ? `disabled title="${reason}"` : ""}>${started ? "Continue" : done ? "Replay" : "Start"}</button></article>`;
                }).join("")}</div><p class="nodi-library-note">Take these tours at your own pace. Close a tour to pause it, or replay it anytime. Progress is saved in this browser.</p></div>`;
            syncMotion();
        }

        function openLibrary() {
            if (library.open) return;
            returnFocus = document.activeElement;
            guide.hidden = true; highlight.hidden = true;
            active = null;
            renderLibrary(); library.showModal();
            library.querySelector("[data-nodi='close-library']").focus();
        }

        function closeLibrary() { library.close(); }

        function closeGuide() {
            guide.hidden = true; highlight.hidden = true; active = null;
            document.getElementById("nodi-button").focus();
        }

        function start(id) {
            const course = courses.find(item => item.id === id);
            if (!course || blocked(course)) return false;
            active = course; index = progress.index(id); finished = false;
            library.close(); showStep(); return true;
        }

        function showStep() {
            if (!active) return;
            if (blocked(active)) { openLibrary(); return; }
            progress.save(active.id, index);
            navigating = true;
            try { navigate(currentStep()); } finally { navigating = false; }
            renderGuide();
            const target = document.querySelector(currentStep().target);
            if (isVisible(target)) target.scrollIntoView({ block: "nearest", inline: "nearest", behavior: "instant" });
            schedulePosition();
            guide.querySelector("h2").focus({ preventScroll: true });
        }

        function renderGuide() {
            const current = currentStep();
            guide.hidden = false;
            const mismatch = current.tab && current.tab !== getContext().tab;
            guide.innerHTML = `<div class="nodi-guide-head">${faceImage(finished ? "celebrate" : current.face, 96)}<div class="nodi-speaker"><strong>Nodi</strong><span>Your network guide</span></div><button type="button" class="nodi-dismiss" data-nodi="close-guide" aria-label="Close tutorial and save progress"><span class="material-symbols-outlined" aria-hidden="true">close</span></button></div>
                <div class="nodi-speech"><p class="nodi-eyebrow">${escape(active.title)} · ${finished ? "Complete" : `${index + 1} / ${active.steps.length}`}</p><h2 id="nodi-step-title" tabindex="-1">${finished ? "You’ve got this!" : escape(current.title)}</h2>
                <p id="nodi-step-body" class="nodi-guide-body">${finished ? "Nice exploring! You’ve reached the end of this tour. Keep trying things out, or choose another tutorial and we’ll discover more together." : escape(current.body)}</p>
                <p class="nodi-guide-note" ${mismatch && !finished ? "" : "hidden"}>You’re in another workspace. <button type="button" data-nodi="show-step">Show this step</button></p>
                <progress max="${active.steps.length}" value="${finished ? active.steps.length : index + 1}" aria-label="Tutorial progress"></progress>
                <div class="nodi-guide-footer"><button type="button" class="nodi-all" data-nodi="library">All tutorials</button>${motionButton(true)}<div class="nodi-guide-navigation">${finished ? '<button type="button" class="primary" data-nodi="close-guide">Thanks, Nodi!</button>' : `<button type="button" data-nodi="back" ${index === 0 ? "disabled" : ""}>Back</button><button type="button" class="primary" data-nodi="next">${index === active.steps.length - 1 ? "Finish" : "Next"}</button>`}</div></div></div>`;
            syncMotion();
        }

        function move(offset) {
            if (!active || finished) return;
            if (offset > 0 && index === active.steps.length - 1) {
                progress.finish(active.id); finished = true; highlight.hidden = true;
                renderGuide(); schedulePosition(); guide.querySelector("h2").focus({ preventScroll: true }); return;
            }
            index = Math.max(0, Math.min(active.steps.length - 1, index + offset)); showStep();
        }

        function isVisible(target) {
            return target && target.getClientRects().length && getComputedStyle(target).visibility !== "hidden";
        }

        function position() {
            if (guide.hidden || !active) return;
            const gap = 16, margin = 12, width = window.innerWidth, height = window.innerHeight;
            let x = width - guide.offsetWidth - margin, y = height - guide.offsetHeight - margin;
            const current = currentStep(), target = document.querySelector(current.target);
            const matchedTab = !current.tab || current.tab === getContext().tab;
            highlight.hidden = true;
            if (!finished && matchedTab && isVisible(target)) {
                const rect = target.getBoundingClientRect();
                const left = Math.max(4, rect.left - 4), top = Math.max(4, rect.top - 4);
                const right = Math.min(width - 4, rect.right + 4), bottom = Math.min(height - 4, rect.bottom + 4);
                if (right > left && bottom > top) {
                    highlight.hidden = false;
                    Object.assign(highlight.style, { left: left + "px", top: top + "px", width: right - left + "px", height: bottom - top + "px" });
                    const cardWidth = guide.offsetWidth, cardHeight = guide.offsetHeight;
                    if (right + gap + cardWidth <= width - margin) { x = right + gap; y = top; }
                    else if (left - gap - cardWidth >= margin) { x = left - gap - cardWidth; y = top; }
                    else if (bottom + gap + cardHeight <= height - margin) { x = left; y = bottom + gap; }
                    else if (top - gap - cardHeight >= margin) { x = left; y = top - gap - cardHeight; }
                }
            }
            guide.style.left = Math.max(margin, Math.min(x, width - guide.offsetWidth - margin)) + "px";
            guide.style.top = Math.max(margin, Math.min(y, height - guide.offsetHeight - margin)) + "px";
        }

        function schedulePosition() {
            if (frame) return;
            frame = requestAnimationFrame(() => { frame = 0; position(); });
        }

        function contextChanged() {
            if (navigating) return;
            if (library.open) renderLibrary();
            if (active && !guide.hidden) {
                // A graph can be cleared/replaced while a tour is open; never navigate into an unavailable view.
                if (blocked(active)) { openLibrary(); return; }
                renderGuide(); schedulePosition();
            }
        }

        library.addEventListener("click", event => {
            const button = event.target.closest("button");
            if (button?.dataset.course) start(button.dataset.course);
            if (button?.dataset.nodi === "close-library") closeLibrary();
            if (button?.dataset.nodi === "motion") toggleMotion();
            if (event.target === library) {
                const rect = library.getBoundingClientRect();
                if (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) closeLibrary();
            }
        });
        library.addEventListener("close", () => {
            if (!active) (returnFocus?.isConnected && returnFocus.getClientRects().length ? returnFocus : document.getElementById("nodi-button")).focus();
        });
        guide.addEventListener("click", event => {
            const action = event.target.closest("button")?.dataset.nodi;
            if (action === "close-guide") closeGuide();
            if (action === "library") openLibrary();
            if (action === "show-step") showStep();
            if (action === "back") move(-1);
            if (action === "next") move(1);
            if (action === "motion") toggleMotion();
        });
        document.addEventListener("keydown", event => {
            if (guide.hidden || event.key !== "Escape" || document.querySelector("dialog[open]")) return;
            event.preventDefault(); closeGuide();
        });
        guide.addEventListener("keydown", event => {
            if (event.target.closest("input, textarea, select, [contenteditable]")) return;
            if (event.key === "ArrowRight" || event.key === "ArrowLeft") { event.preventDefault(); move(event.key === "ArrowRight" ? 1 : -1); }
        });
        window.addEventListener("resize", schedulePosition);
        document.addEventListener("scroll", schedulePosition, true);
        if (typeof ResizeObserver !== "undefined") new ResizeObserver(schedulePosition).observe(guide);
        document.getElementById("nodi-button").addEventListener("click", openLibrary);
        document.getElementById("nodi-welcome").addEventListener("click", openLibrary);
        reducedMotion?.addEventListener("change", syncMotion);
        syncMotion();
        return { openLibrary, start, move, closeGuide, contextChanged };
    }

    return { courses, createProgress, create,
        init(options) { controller = create(options); return controller; },
        contextChanged() { controller?.contextChanged(); },
    };
})();
