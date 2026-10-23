/* Only optional street tiles use the internet. All other map assets are local. */
(() => {
    const key = "relison-online-street-maps", listeners = new Set();
    let enabled = false;
    try { enabled = localStorage.getItem(key) === "true"; } catch (_) { /* Offline is the safe default. */ }
    const control = document.getElementById("online-street-maps");
    function notify() { for (const listener of listeners) listener(); }
    function setEnabled(value) {
        enabled = Boolean(value);
        if (control) control.checked = enabled;
        try { localStorage.setItem(key, String(enabled)); } catch (_) { /* Keep the preference for this page. */ }
        notify();
    }
    if (control) { control.checked = enabled; control.addEventListener("change", () => setEnabled(control.checked)); }
    window.addEventListener("offline", notify);
    window.addEventListener("online", notify);
    window.relisonMapSettings = {
        useOnlineTiles: () => enabled && navigator.onLine !== false,
        setEnabled,
        subscribe(listener) { listeners.add(listener); return () => listeners.delete(listener); },
    };
})();
