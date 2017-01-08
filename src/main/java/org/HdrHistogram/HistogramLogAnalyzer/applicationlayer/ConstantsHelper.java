/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ConstantsHelper {

    public static String getLatencyName() {
        return "Latency";
    }

    public static String getChartTitle(LatencyChartType chartType) {
        switch (chartType) {
            case PERCENTILE:
                return getLatencyName() + " By Percentile Distribution";
            case TIMELINE:
                return "Maximum " + getLatencyName() + " In Time Interval";
            case BUCKETS:
                return "Raw " + getLatencyName() + " Duration Bucketed Values";
        }
        throw new IllegalArgumentException();
    }

    public static String getXAxisLabel(LatencyChartType chartType) {
        switch (chartType) {
            case PERCENTILE:
                return "Percentile";
            case TIMELINE:
                return "Elapsed Time (sec)";
            case BUCKETS:
                return "Bucketed Raw " + getLatencyName() + " Duration (msec)";
        }
        throw new IllegalArgumentException();
    }

    public static String getYAxisLabel(LatencyChartType chartType) {
        switch (chartType) {
            case PERCENTILE:
                return getLatencyName() + " Duration (msec)";
            case TIMELINE:
                return getLatencyName() + " Duration (msec)";
            case BUCKETS:
                return "Number in buckets";
        }
        throw new IllegalArgumentException();
    }

    public static String getLogAxisLabel(LatencyChartType chartType) {
        switch (chartType) {
            case PERCENTILE:
                return getLatencyName() + " by Percentile";
        }
        throw new IllegalArgumentException();
    }
}
