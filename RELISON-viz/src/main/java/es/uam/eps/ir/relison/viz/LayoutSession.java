/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz;
/**
 * Caller-scheduled computation. Step and snapshot use one thread; cancel may use another.
 * @param <U> node type
 */
public interface LayoutSession<U> {
    /** @param iterations non-negative maximum iterations to advance */
    void step(int iterations);
    /** @return immutable current coordinates, status and progress */
    LayoutResult<U> snapshot();
    /** Requests cancellation. */
    void cancel();
    /** @return whether terminated */
    boolean isFinished();
}
