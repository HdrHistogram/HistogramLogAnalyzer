/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.charts;

import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.PercentileObject;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.TimelineObject;
import org.jfree.data.Range;
import org.jfree.data.general.Series;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeries;

import java.util.Date;

class HLATimeSeries extends TimeSeries implements CommonSeries {

    HLATimeSeries(Comparable name) {
        super(name);
    }

    @Override
    public void add(TimelineObject timelineObject, double startTime) {
        double xValue = startTime + timelineObject.getTimelineAxisValue();
        Date date = new Date((long) (xValue * 1000));
        super.addOrUpdate(new FixedMillisecond(date), timelineObject.getLatencyAxisValue());
    }

    @Override
    public void add(double latencyAxisValue, Range domainBounds) {
        double lowerValue = domainBounds.getLowerBound();
        double upperValue = domainBounds.getUpperBound();

        super.addOrUpdate(new FixedMillisecond(new Date((long) lowerValue)), latencyAxisValue);
        super.addOrUpdate(new FixedMillisecond(new Date((long) upperValue)), latencyAxisValue);
    }

    @Override
    public Series get() {
        return this;
    }
}
