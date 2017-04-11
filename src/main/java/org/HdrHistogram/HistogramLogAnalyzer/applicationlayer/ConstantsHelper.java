/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

public class ConstantsHelper {

    public static String getLatencyName() {
        return "Latency";
    }

    public static String getChartTitle(HLAChartType chartType) {
        switch (chartType) {
            case PERCENTILE:
                return getLatencyName() + " By Percentile Distribution";
            case TIMELINE_ELAPSED_TIME:
            case TIMELINE_DATE:
                return "Maximum " + getLatencyName() + " In Time Interval";
            case BUCKETS:
                return "Raw " + getLatencyName() + " Duration Bucketed Values";
        }
        throw new IllegalArgumentException();
    }

    public static String getXAxisLabel(HLAChartType chartType) {
        return getXAxisLabel(chartType, null);
    }

    public static String getXAxisLabel(HLAChartType chartType, String timezone) {
        switch (chartType) {
            case PERCENTILE:
                return "Percentile";
            case TIMELINE_ELAPSED_TIME:
                return "Elapsed Time (sec)";
            case TIMELINE_DATE:
                return timezone + " time";
            case BUCKETS:
                return "Bucketed Raw " + getLatencyName() + " Duration (msec)";
        }
        throw new IllegalArgumentException();
    }

    public static String getYAxisLabel(HLAChartType chartType) {
        switch (chartType) {
            case PERCENTILE:
                return getLatencyName() + " Duration (msec)";
            case TIMELINE_ELAPSED_TIME:
            case TIMELINE_DATE:
                return getLatencyName() + " Duration (msec)";
            case BUCKETS:
                return "Number in buckets";
        }
        throw new IllegalArgumentException();
    }

    public static String getLogAxisLabel(HLAChartType chartType) {
        switch (chartType) {
            case PERCENTILE:
                return getLatencyName() + " by Percentile";
        }
        throw new IllegalArgumentException();
    }
}
