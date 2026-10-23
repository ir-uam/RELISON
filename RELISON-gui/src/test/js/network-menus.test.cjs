/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
const test = require('node:test'), assert = require('node:assert/strict');
const fs = require('node:fs'), path = require('node:path'), vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, '../../main/resources/public/app.js'), 'utf8');
function extract(name) { const start = source.indexOf('function ' + name + '('); return source.slice(start, source.indexOf('\n}', start) + 2); }
function setup(anchor, size = { width: 240, height: 300 }, viewport = { width: 1000, height: 800 }) {
    const events = {}, menuEvents = {}, calls = [];
    const panel = { style: {}, getBoundingClientRect: () => size, setAttribute: (name, value) => calls.push([name, value]),
        showPopover: () => calls.push('show'), hidePopover: () => calls.push('hide') };
    const summary = { getBoundingClientRect: () => ({ width: 60, height: 30, ...anchor }), focus: () => calls.push('focus') };
    const menu = { open: true, querySelector: selector => selector === 'summary' ? summary : panel,
        addEventListener: (name, callback) => { menuEvents[name] = callback; }, contains: target => target === panel || target === summary };
    const context = vm.createContext({ document: { documentElement: { clientWidth: viewport.width, clientHeight: viewport.height },
        querySelectorAll: () => [menu], addEventListener: (name, callback) => { events[name] = callback; } },
        window: { addEventListener() {} }, requestAnimationFrame: () => 1 });
    vm.runInContext(extract('positionNetworkMenu') + '\n' + extract('installNetworkMenus'), context);
    return { context, panel, menu, menuEvents, events, calls };
}
test('menus at either horizontal edge are clamped inside the viewport', () => {
    for (const right of [65, 995]) {
        const h = setup({ right, top: 50, bottom: 80 }); h.context.positionNetworkMenu(h.menu);
        const left = parseFloat(h.panel.style.left);
        assert.ok(left >= 8 && left + 240 <= 992);
    }
});
test('menus open above low anchors and scroll when neither side fits', () => {
    const h = setup({ right: 700, top: 730, bottom: 760 }); h.context.positionNetworkMenu(h.menu);
    assert.equal(h.panel.style.top, '425px');
    const constrained = setup({ right: 300, top: 180, bottom: 210 }, { width: 240, height: 500 }, { width: 400, height: 400 });
    constrained.context.positionNetworkMenu(constrained.menu);
    assert.equal(constrained.panel.style.maxHeight, '177px');
    assert.equal(constrained.panel.style.top, '215px');
});
test('menus use the browser top layer and dismiss without affecting inside controls', () => {
    const h = setup({ right: 300, top: 50, bottom: 80 }); h.context.installNetworkMenus();
    assert.deepEqual(h.calls[0], ['popover', 'manual']);
    h.menuEvents.toggle(); assert.ok(h.calls.includes('show'));
    h.events.pointerdown({ target: h.panel }); assert.equal(h.menu.open, true);
    h.events.pointerdown({ target: {} }); assert.equal(h.menu.open, false);
    h.menuEvents.toggle(); assert.ok(h.calls.includes('hide'));
    h.menu.open = true; h.events.keydown({ key: 'Escape' });
    assert.equal(h.menu.open, false); assert.ok(h.calls.includes('focus'));
});
test('hidden tab anchors close open menus', () => {
    const h = setup({ right: 0, top: 0, bottom: 0, width: 0, height: 0 });
    h.context.positionNetworkMenu(h.menu); assert.equal(h.menu.open, false);
});
