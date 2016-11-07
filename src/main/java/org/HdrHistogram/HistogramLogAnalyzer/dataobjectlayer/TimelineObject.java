/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer;

public class TimelineObject {

    private double timelineAxisValue;
    private double latencyAxisValue;

    private String tag;

    private String mwp_percentile;
    private String mwp_windowLength;

    // "query" object
    public TimelineObject(double timelineAxisValue, double latencyAxisValue) {
        this.timelineAxisValue = timelineAxisValue;
        this.latencyAxisValue = latencyAxisValue;
    }

    // "insert" object
    public TimelineObject(double timelineAxisValue, double latencyAxisValue,
                          String tag, String mwp_percentile, String mwp_windowLength)
    {
        this.timelineAxisValue = timelineAxisValue;
        this.latencyAxisValue = latencyAxisValue;

        this.tag = tag;

        this.mwp_percentile = mwp_percentile;
        this.mwp_windowLength = mwp_windowLength;
    }

    public double getTimelineAxisValue() {
        return timelineAxisValue;
    }

    public double getLatencyAxisValue() {
        return latencyAxisValue;
    }

    public String getTag() {
        return tag;
    }

    public String getMwpPercentile() {
        return mwp_percentile;
    }

    public String getMwpWindowLength() {
        return mwp_windowLength;
    }
}
