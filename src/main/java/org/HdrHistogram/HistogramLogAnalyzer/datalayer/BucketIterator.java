/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.datalayer;

import org.HdrHistogram.HistogramLogAnalyzer.applicationlayer.Configuration;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.BucketObject;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.PercentileObject;

import java.util.Iterator;

public class BucketIterator implements Iterator {

    private PercentileIterator pi;

    private boolean processedInitialValue = false;
    private double valueToAddForZeroOrOneBasedJHiccupValue = 0.0D;

    private double maxCountAtValue = 0.0D;

    BucketIterator(PercentileIterator pi) {
        this.pi = pi;
    }

    @Override
    public boolean hasNext() {
        return pi.hasNext();
    }

    @Override
    public Object next() {
        PercentileObject po = pi.next();

        double latencyValue = po.getLatencyAxisValue();
        double countAtValue = po.getCountAtValue();

        if (Configuration.getInstance().getEnableOldStyleBucketChart()) {
            latencyValue += getExtraHiccupValue(latencyValue);
        }

        if (countAtValue > maxCountAtValue) {
            maxCountAtValue = countAtValue;
        }

        return new BucketObject(latencyValue, countAtValue);
    }

    public double getMaxCountAtValue() {
        return maxCountAtValue;
    }

    private double getExtraHiccupValue(double hiccupValue) {
        // Some of the versions of jHiccup have hiccup values that use the raw values
        // (non-zero based) rather than just the hiccup values. We try to catch those
        // cases here.
        if (!processedInitialValue) {
            if (hiccupValue < 0.5D) {
                valueToAddForZeroOrOneBasedJHiccupValue = 1.0D;
                processedInitialValue = true;
            }
        }
        return valueToAddForZeroOrOneBasedJHiccupValue;
    }

    public double getValueToAddForZeroOrOneBasedJHiccupValue() {
        return valueToAddForZeroOrOneBasedJHiccupValue;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
