/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer;

public class BucketObject {

    private double latencyValue;
    private double countAtValue;

    public BucketObject(double hiccupValue, double countAtValue) {
        this.latencyValue = hiccupValue;
        this.countAtValue = countAtValue;
    }

    public double getLatencyValue() {
        return latencyValue;
    }

    public double getCountAtValue() {
        return countAtValue;
    }
}
