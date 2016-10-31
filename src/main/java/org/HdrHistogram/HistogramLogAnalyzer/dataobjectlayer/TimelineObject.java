/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer;

public class TimelineObject {

    public double timelineAxisValue;
    public double latencyAxisValue;

    public String tag;

    public String mwp_percentile;
    public String mwp_intervalCount;

    // "query" object
    public TimelineObject(double timelineAxisValue, double latencyAxisValue) {
        this.timelineAxisValue = timelineAxisValue;
        this.latencyAxisValue = latencyAxisValue;
    }

    // "insert" object
    public TimelineObject(double timelineAxisValue, double latencyAxisValue,
                          String tag, String mwp_percentile, String mwp_intervalCount)
    {
        this.timelineAxisValue = timelineAxisValue;
        this.latencyAxisValue = latencyAxisValue;

        this.tag = tag;

        this.mwp_percentile = mwp_percentile;
        this.mwp_intervalCount = mwp_intervalCount;
    }

    public double getTimelineAxisValue() {
        return timelineAxisValue;
    }

    public double getLatencyAxisValue() {
        return latencyAxisValue;
    }

}
