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

        if (countAtValue > maxCountAtValue) {
            maxCountAtValue = countAtValue;
        }

        return new BucketObject(latencyValue, countAtValue);
    }

    public double getMaxCountAtValue() {
        return maxCountAtValue;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
