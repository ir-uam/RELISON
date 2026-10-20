package es.uam.eps.ir.relison.sna.community.detection.attributes;

import java.math.BigDecimal;
import java.util.Objects;

/** A lower-inclusive, upper-exclusive numeric attribute interval. */
public final class NumericAttributeBin
{
    private final BigDecimal lowerBound;
    private final BigDecimal upperBound;

    public NumericAttributeBin(BigDecimal lowerBound, BigDecimal upperBound)
    {
        if (lowerBound == null || upperBound == null || lowerBound.compareTo(upperBound) >= 0)
            throw new IllegalArgumentException("A numeric bin must have a lower bound below its upper bound");
        this.lowerBound = lowerBound.stripTrailingZeros();
        this.upperBound = upperBound.stripTrailingZeros();
    }

    public BigDecimal getLowerBound()
    {
        return lowerBound;
    }

    public BigDecimal getUpperBound()
    {
        return upperBound;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if (!(other instanceof NumericAttributeBin)) return false;
        NumericAttributeBin bin = (NumericAttributeBin) other;
        return lowerBound.compareTo(bin.lowerBound) == 0 && upperBound.compareTo(bin.upperBound) == 0;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(lowerBound, upperBound);
    }

    @Override
    public String toString()
    {
        return "[" + lowerBound.toPlainString() + ", " + upperBound.toPlainString() + ")";
    }
}
