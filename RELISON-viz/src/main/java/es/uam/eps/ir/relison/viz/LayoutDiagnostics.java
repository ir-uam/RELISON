/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz;


import java.util.Objects;

/** Termination information independent of rendering. */
public final class LayoutDiagnostics
{
    /** Reasons a computation can terminate. */
    public enum Termination { COMPLETED, CONVERGED, CANCELLED, LIMIT_REACHED }

    private final String algorithmId;
    private final Termination termination;

    /**
     * @param algorithmId algorithm identity
     * @param termination termination reason
     */
    public LayoutDiagnostics(String algorithmId, Termination termination)
    {
        this.algorithmId = Objects.requireNonNull(algorithmId);
        this.termination = Objects.requireNonNull(termination);
    }
    /** @return algorithm identity */
    public String getAlgorithmId() { return algorithmId; }
    /** @return termination reason */
    public Termination getTermination() { return termination; }
}
