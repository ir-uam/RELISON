/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz;


import java.util.Objects;

/** Termination information independent of rendering. */
public final class LayoutDiagnostics
{
    /** Reasons a computation can terminate. */
    public enum Termination { RUNNING, COMPLETED, CONVERGED, CANCELLED, LIMIT_REACHED }

    private final String algorithmId;
    private final Termination termination;
    private final int iterations;
    private final double maximumDisplacement;

    /**
     * @param algorithmId algorithm identity
     * @param termination termination reason
     */
    public LayoutDiagnostics(String algorithmId, Termination termination)
    {
        this(algorithmId, termination, 0, 0);
    }
    /**
     * @param algorithmId identity
     * @param termination status
     * @param iterations completed iterations
     * @param maximumDisplacement maximum movement in the last completed iteration
     */
    public LayoutDiagnostics(String algorithmId, Termination termination, int iterations, double maximumDisplacement) {
        if (iterations < 0 || !Double.isFinite(maximumDisplacement) || maximumDisplacement < 0) throw new IllegalArgumentException("Invalid diagnostics");
        this.iterations = iterations;
        this.maximumDisplacement = maximumDisplacement;
        this.algorithmId = Objects.requireNonNull(algorithmId);
        this.termination = Objects.requireNonNull(termination);
    }
    /** @return completed iterations */
    public int getIterations() { return iterations; }
    /** @return largest movement in last iteration */
    public double getMaximumDisplacement() { return maximumDisplacement; }
    /** @return algorithm identity */
    public String getAlgorithmId() { return algorithmId; }
    /** @return termination reason */
    public Termination getTermination() { return termination; }
}
