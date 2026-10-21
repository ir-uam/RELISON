/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
(function (root) {
    "use strict";
    const empty = value => value == null || String(value).trim() === "";
    const numeric = type => ["number", "int", "long", "double"].includes(type);
    const pair = (source, target) => JSON.stringify([String(source), String(target)]);

    function filterError(filter, type) {
        if (["gt", "gte", "lt", "lte", "between"].includes(filter.op) && !numeric(type)) return "Use a numeric column for numeric comparisons.";
        if (numeric(type) && !["contains", "notcontains", "empty", "nonempty"].includes(filter.op)) {
            const bounds = filter.op === "between" ? [filter.value, filter.value2] : [filter.value];
            if (bounds.some(value => !empty(value) && !Number.isFinite(Number(value)))) return "Enter a finite number.";
            if (filter.op === "between" && !empty(filter.value) && !empty(filter.value2) && Number(filter.value) > Number(filter.value2)) return "The lower bound must not exceed the upper bound.";
        }
        if (type === "bool" && ["eq", "neq"].includes(filter.op) && !empty(filter.value) && !/^(true|false)$/i.test(String(filter.value))) return "Enter true or false.";
        return "";
    }

    function match(value, filter, type = "string") {
        const { op, value: expected, value2: upper } = filter;
        if (op === "empty") return empty(value);
        if (op === "nonempty") return !empty(value);
        if (filterError(filter, type)) return false;
        if (op === "between") return !empty(value) && Number.isFinite(Number(value)) &&
            (empty(expected) || Number(value) >= Number(expected)) && (empty(upper) || Number(value) <= Number(upper));
        if (empty(expected)) return true;
        const text = value == null ? "" : String(value), wanted = String(expected);
        if (op === "contains") return text.toLowerCase().includes(wanted.toLowerCase());
        if (op === "notcontains") return !text.toLowerCase().includes(wanted.toLowerCase());
        let a = text, b = wanted;
        if (numeric(type)) {
            if (empty(value) || !Number.isFinite(Number(value))) return op === "neq";
            a = Number(value); b = Number(expected);
        } else if (type === "bool") {
            if (!/^(true|false)$/i.test(text)) return op === "neq";
            a = text.toLowerCase(); b = wanted.toLowerCase();
        }
        // String, categorical and identifier equality are exact. Substring search is case-insensitive.
        switch (op) {
            case "eq": return a === b;
            case "neq": return a !== b;
            case "gt": return a > b;
            case "gte": return a >= b;
            case "lt": return a < b;
            case "lte": return a <= b;
            default: return true;
        }
    }

    // Groups expose a connector between each pair of children. AND binds before
    // OR; nested groups supply explicit parentheses. Empty groups are neutral.
    function expressionMatch(expression, valueOf, typeOf, active) {
        function evaluate(node) {
            if (!node) return null;
            if (node.kind === "condition") return active(node) ? match(valueOf(node.col), node, typeOf(node.col)) : null;
            const terms = [[]];
            (node.children || []).forEach((child, index) => {
                if (index && (node.joins?.[index - 1] || node.op) === "or") terms.push([]);
                const value = evaluate(child);
                if (value !== null) terms[terms.length - 1].push(value);
            });
            const populated = terms.filter(term => term.length);
            return populated.length ? populated.some(term => term.every(Boolean)) : null;
        }
        return evaluate(expression) ?? true;
    }

    function expressionConditions(expression) {
        if (!expression) return [];
        return expression.kind === "condition" ? [expression] : (expression.children || []).flatMap(expressionConditions);
    }

    // Preserve the connector attached to each surviving child when fields or
    // clauses are removed. This is also used to migrate renamed attributes.
    function mapExpression(expression, mapper) {
        if (!expression) return null;
        if (expression.kind === "condition") return mapper(expression);
        const children = [], joins = [];
        (expression.children || []).forEach((child, index) => {
            const mapped = mapExpression(child, mapper);
            if (!mapped || mapped.kind === "group" && !mapped.children.length) return;
            if (children.length) joins.push(expression.joins?.[index - 1] || expression.op || "and");
            children.push(mapped);
        });
        return { ...expression, children, joins };
    }

    function expressionText(expression, labelOf, operatorOf) {
        if (!expression) return "";
        if (expression.kind === "condition") {
            const operator = operatorOf(expression.op);
            if (["empty", "nonempty"].includes(expression.op)) return labelOf(expression.col) + " " + operator;
            const text = value => empty(value) ? "…" : JSON.stringify(String(value));
            return labelOf(expression.col) + " " + operator + " " + text(expression.value) +
                (expression.op === "between" ? " and " + text(expression.value2) : "");
        }
        const children = expression.children || [];
        if (!children.length) return "";
        return "(" + children.map((child, index) => (index ? " " + (expression.joins?.[index - 1] || expression.op || "and").toUpperCase() + " " : "") +
            (expressionText(child, labelOf, operatorOf) || "empty group")).join("") + ")";
    }

    function workingSet(graph, nodePasses, edgePasses, edgeFiltered, filtered) {
        const matchingNodes = new Set(graph.nodes().filter(nodePasses));
        const nodes = edgeFiltered ? new Set() : matchingNodes;
        const edges = new Set();
        graph.forEachEdge(edge => {
            const source = graph.source(edge), target = graph.target(edge);
            if (!matchingNodes.has(source) || !matchingNodes.has(target) || !edgePasses(edge)) return;
            edges.add(edge);
            if (edgeFiltered) { nodes.add(source); nodes.add(target); }
        });
        return { nodes, edges, edgeFiltered, filtered };
    }

    function focus(graph, working, selection, helpers) {
        const { type, mode } = selection;
        if (mode === "none") return null;
        const nodes = new Set(), edges = new Set();
        const only = mode.endsWith("-only");
        const result = { nodes, edges, only, type, selected: type === "node" ? selection.node : null };
        if (mode.startsWith("edge-group-")) {
            (selection.edgeGroup || []).forEach(edge => {
                if (!working.edges.has(edge)) return;
                edges.add(edge); nodes.add(graph.source(edge)); nodes.add(graph.target(edge));
            });
            return result;
        }
        if (type === "path") {
            if (!selection.paths.length) return null;
            const pairs = new Set();
            selection.paths.forEach(path => {
                path.forEach(node => { if (working.nodes.has(node)) nodes.add(node); });
                for (let i = 1; i < path.length; i++) {
                    pairs.add(pair(path[i - 1], path[i]));
                    if (!helpers.directed) pairs.add(pair(path[i], path[i - 1]));
                }
            });
            working.edges.forEach(edge => { if (pairs.has(pair(graph.source(edge), graph.target(edge)))) edges.add(edge); });
            return result;
        }
        const edgeSelection = type === "edge";
        if (edgeSelection ? !selection.edge || !graph.hasEdge(selection.edge) : selection.node == null || !graph.hasNode(selection.node)) return null;
        // An excluded target stays in the inspector but never adds entities to the working set.
        if (edgeSelection ? !working.edges.has(selection.edge) : !working.nodes.has(selection.node)) return result;
        const seeds = edgeSelection ? [graph.source(selection.edge), graph.target(selection.edge)] : [selection.node];
        seeds.forEach(node => nodes.add(node));
        if (mode.startsWith("edge-attribute-")) {
            nodes.clear();
            if (!selection.edgeAttribute) return { ...result, unavailable: "Choose an edge attribute." };
            const value = helpers.edgeValue(selection.edge, selection.edgeAttribute);
            const attributeType = helpers.edgeType(selection.edgeAttribute);
            working.edges.forEach(edge => {
                const candidate = helpers.edgeValue(edge, selection.edgeAttribute);
                const matches = empty(value) ? empty(candidate) : match(candidate, { op: "eq", value: String(value) }, attributeType);
                if (matches) { edges.add(edge); nodes.add(graph.source(edge)); nodes.add(graph.target(edge)); }
            });
        } else if (mode.startsWith("ego-")) {
            working.edges.forEach(edge => {
                const source = graph.source(edge), target = graph.target(edge);
                if (seeds.includes(source)) nodes.add(target);
                if (seeds.includes(target)) nodes.add(source);
            });
        } else if (mode.startsWith("community-")) {
            nodes.clear();
            if (!selection.partition) return { ...result, unavailable: "Choose a community partition or node attribute." };
            const groups = new Set(seeds.map(node => helpers.groupKey(helpers.groupValue(selection.partition, node))).filter(value => value != null));
            if (!groups.size) return { ...result, unavailable: "The selected target has no value in this grouping." };
            working.nodes.forEach(node => { if (groups.has(helpers.groupKey(helpers.groupValue(selection.partition, node)))) nodes.add(node); });
        }
        if (mode === "edge-highlight") edges.add(selection.edge);
        else if (mode.startsWith("ego-") || mode.startsWith("community-")) {
            working.edges.forEach(edge => { if (nodes.has(graph.source(edge)) && nodes.has(graph.target(edge))) edges.add(edge); });
        }
        return result;
    }

    function calculate(graph, working, selection, helpers) {
        const selectedFocus = focus(graph, working, selection, helpers);
        const isolate = Boolean(selectedFocus?.only);
        // Empty highlight focus falls back to filter emphasis; actual empty isolation stays empty.
        const emphasis = selectedFocus && (isolate || selectedFocus.nodes.size || selectedFocus.edges.size) ? selectedFocus : null;
        const tableNodes = isolate ? selectedFocus.nodes : working.nodes;
        const tableEdges = isolate ? selectedFocus.edges : working.edges;
        const verdict = (id, kind) => {
            if (isolate && !selectedFocus[kind].has(id)) return "hidden";
            if (!working[kind].has(id)) return "dim";
            if (emphasis && !emphasis[kind].has(id)) return "dim";
            return "normal";
        };
        return { working, focus: selectedFocus, emphasis, isolate, tableNodes, tableEdges,
            nodeVerdict: id => verdict(id, "nodes"), edgeVerdict: id => verdict(id, "edges") };
    }

    const api = { empty, pair, filterError, match, expressionMatch, expressionConditions, mapExpression, expressionText, workingSet, calculate };
    if (typeof module !== "undefined" && module.exports) module.exports = api;
    else root.relisonNetworkView = api;
})(typeof window === "undefined" ? globalThis : window);
