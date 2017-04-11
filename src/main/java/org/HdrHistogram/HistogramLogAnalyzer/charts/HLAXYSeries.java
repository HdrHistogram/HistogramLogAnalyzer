/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.charts;

import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.PercentileObject;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.TimelineObject;
import org.jfree.data.Range;
import org.jfree.data.general.Series;
import org.jfree.data.xy.XYSeries;

class HLAXYSeries extends XYSeries implements CommonSeries {

    HLAXYSeries(Comparable key) {
        super(key);
    }

    @Override
    public void add(TimelineObject timelineObject, double startTime) {
        super.add(timelineObject.getTimelineAxisValue(), timelineObject.getLatencyAxisValue());
    }

    @Override
    public void add(double latencyAxisValue, Range domainBounds) {
        super.add(domainBounds.getLowerBound(), latencyAxisValue);
        super.add(domainBounds.getUpperBound(), latencyAxisValue);
    }

    @Override
    public Series get() {
        return this;
    }
}
