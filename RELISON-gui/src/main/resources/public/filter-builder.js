/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
(function (root) {
    "use strict";
    function operatorsFor(type, operators) {
        const numeric = ["number", "int", "long", "double"].includes(type);
        return operators.filter(operator => !["gt", "gte", "lt", "lte", "between"].includes(operator.value) || numeric)
            .filter(operator => type !== "bool" || ["eq", "neq", "empty", "nonempty"].includes(operator.value));
    }

    function render(container, options) {
        const { expression, columns, operators, typeOf, newCondition, newGroup, onChange } = options;
        const doc = container.ownerDocument;
        const element = (tag, className, text) => {
            const node = doc.createElement(tag);
            if (className) node.className = className;
            if (text) node.textContent = text;
            return node;
        };
        const choice = (select, value, label) => {
            const option = element("option", "", label); option.value = value; select.appendChild(option);
        };
        const button = (label, action) => {
            const node = element("button", "", label); node.type = "button";
            node.addEventListener("click", action); return node;
        };
        const changed = () => onChange(expression);
        const redraw = () => { render(container, options); changed(); };
        function groupEditor(group, depth = 0) {
            const fieldset = element("fieldset", "filter-builder-group");
            fieldset.appendChild(element("legend", "", depth ? "Group (parentheses)" : "Filter expression"));
            group.children.forEach((child, index) => {
                if (index) {
                    const join = element("select", "filter-builder-join");
                    join.setAttribute("aria-label", "Combine with previous condition or group");
                    choice(join, "and", "AND"); choice(join, "or", "OR");
                    join.value = group.joins[index - 1] || "and";
                    join.addEventListener("change", () => { group.joins[index - 1] = join.value; changed(); });
                    fieldset.appendChild(join);
                }
                const row = element("div", "filter-builder-row");
                const remove = button(child.kind === "group" ? "Remove group" : "Remove", () => {
                    group.children.splice(index, 1);
                    group.joins.splice(index ? index - 1 : 0, 1);
                    redraw();
                });
                remove.setAttribute("aria-label", child.kind === "group" ? "Remove filter group" : "Remove filter condition");
                if (child.kind === "group") {
                    row.classList.add("filter-builder-nested");
                    row.append(groupEditor(child, depth + 1), remove);
                } else {
                    const field = element("select", "filter-builder-field");
                    field.setAttribute("aria-label", "Filter field");
                    columns.forEach(column => choice(field, column.key, column.label));
                    field.value = child.col;
                    field.addEventListener("change", () => {
                        child.col = field.value;
                        if (!operatorsFor(typeOf(child.col), operators).some(operator => operator.value === child.op)) child.op = "eq";
                        redraw();
                    });
                    const operator = element("select", "filter-builder-operator");
                    operator.setAttribute("aria-label", "Filter operator");
                    operatorsFor(typeOf(child.col), operators).forEach(item => choice(operator, item.value, item.label));
                    operator.value = child.op;
                    const value = element("input", "filter-builder-value"), upper = element("input", "filter-builder-value");
                    value.type = upper.type = "text";
                    value.value = child.value ?? ""; upper.value = child.value2 ?? "";
                    value.placeholder = "Value"; upper.placeholder = "Upper bound";
                    value.setAttribute("aria-label", "Filter value"); upper.setAttribute("aria-label", "Filter upper bound");
                    const visibility = () => { value.hidden = ["empty", "nonempty"].includes(child.op); upper.hidden = child.op !== "between"; };
                    operator.addEventListener("change", () => { child.op = operator.value; visibility(); changed(); });
                    value.addEventListener("input", () => { child.value = value.value; changed(); });
                    upper.addEventListener("input", () => { child.value2 = upper.value; changed(); });
                    visibility();
                    row.append(field, operator, value, upper, remove);
                }
                fieldset.appendChild(row);
            });
            const actions = element("div", "filter-builder-group-actions");
            const add = node => { if (group.children.length) group.joins.push("and"); group.children.push(node); redraw(); };
            actions.append(button("+ Condition", () => add(newCondition())), button("+ Group", () => add(newGroup())));
            fieldset.appendChild(actions);
            if (!group.children.length) fieldset.appendChild(element("p", "muted", "Empty groups do not restrict the results."));
            return fieldset;
        }
        container.replaceChildren(groupEditor(expression));
    }
    const api = { operatorsFor, render };
    if (typeof module !== "undefined" && module.exports) module.exports = api;
    else root.relisonFilterBuilder = api;
})(typeof window === "undefined" ? globalThis : window);
