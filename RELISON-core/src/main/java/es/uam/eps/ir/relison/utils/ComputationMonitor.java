package es.uam.eps.ir.relison.utils;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;

/** Optional execution monitoring. Existing callers use a no-op monitor. Scopes
 * are confined to their calling thread. Parallel code must capture current()
 * before dispatch and use that monitor explicitly in worker lambdas.
 */
public interface ComputationMonitor
{
    ComputationMonitor NONE = (phase, completed, total, unit) -> { };
    ThreadLocal<ComputationMonitor> CURRENT = ThreadLocal.withInitial(() -> NONE);

    void progress(String phase, long completed, long total, String unit);

    default void checkCancelled() {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException();
    }
    default void phase(String phase) { checkCancelled(); progress(phase, 0, -1, ""); }
    static ComputationMonitor current() { return CURRENT.get(); }
    static Scope open(ComputationMonitor monitor) {
        ComputationMonitor previous = CURRENT.get(); CURRENT.set(monitor);
        return () -> { if (previous == NONE) CURRENT.remove(); else CURRENT.set(previous); };
    }
    interface Scope extends AutoCloseable { @Override void close(); }

    /** Count outputs after computation, restoring this phase after nested preparation. */
    default <K, V> Map<K, V> track(Map<K, V> delegate, String phase, long total, String unit) {
        if (this == NONE) return delegate;
        progress(phase, 0, total, unit);
        ComputationMonitor monitor = this;
        return new AbstractMap<K, V>() {
            private long reportedAt = System.nanoTime();
            @Override public Set<Entry<K, V>> entrySet() { return delegate.entrySet(); }
            @Override public V put(K key, V value) {
                monitor.checkCancelled(); V previous = delegate.put(key, value);
                int count = delegate.size();
                long now = System.nanoTime();
                if (count == 1 || count == total || (count & 255) == 0 || now - reportedAt > 100_000_000L) {
                    monitor.progress(phase, count, total, unit); reportedAt = now;
                }
                return previous;
            }
            @Override public V get(Object key) { return delegate.get(key); }
            @Override public int size() { return delegate.size(); }
        };
    }
}
