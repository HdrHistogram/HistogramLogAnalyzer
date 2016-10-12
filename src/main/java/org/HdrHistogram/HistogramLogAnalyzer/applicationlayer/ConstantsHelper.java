/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

class ConstantsHelper {

    private enum LogGeneratorTool {CASSANDRA_STRESS, JHICCUP, UNKNOWN}
    private static LogGeneratorTool logGenTool;
    private static String currentHlogFileName;
    private static String currentTag;

    private static final String CASSANDRA_STRESS_TOOL_NAME = "Cassandra Stress";
    private static final String JHICCUP_TOOL_NAME = "jHiccup";

    static void detectLogGeneratorTool(String hlogFileName, String tag)
            throws IOException
    {
        BufferedReader reader = null;
        currentHlogFileName = hlogFileName;
        currentTag = tag;
        try {
            reader = new BufferedReader(new FileReader(new File(currentHlogFileName)));
            String line = reader.readLine();
            if (line.contains(CASSANDRA_STRESS_TOOL_NAME)) {
                logGenTool = LogGeneratorTool.CASSANDRA_STRESS;
            } else  if (line.contains(JHICCUP_TOOL_NAME)) {
                logGenTool = LogGeneratorTool.JHICCUP;
            } else {
                logGenTool = LogGeneratorTool.UNKNOWN;
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    static String getTabTitle(boolean useShortFileName) {
        String title = "";
        switch(logGenTool) {
            case CASSANDRA_STRESS:
                title = CASSANDRA_STRESS_TOOL_NAME + " - ";
                break;
            case JHICCUP:
                title = JHICCUP_TOOL_NAME + " - ";
                break;
        }
        if (useShortFileName) {
            title += new File(currentHlogFileName).getName();
        } else {
            title += currentHlogFileName;
        }
        if (currentTag != null) {
            title += "[" + currentTag + "]";
        }
        return title;
    }

    static String getLatencyName() {
        if (logGenTool == LogGeneratorTool.JHICCUP) {
            return "Hiccup";
        } else  if (logGenTool == LogGeneratorTool.CASSANDRA_STRESS) {
            if (currentTag != null) {
                if (currentTag.endsWith("-rt")) {
                    return "Response Time";
                } else if (currentTag.endsWith("-st")) {
                    return "Service Time";
                } else if (currentTag.endsWith("-wt")) {
                    return "Wait Time";
                }
            }
        }
        return "Latency";
    }

    static String getChartTitle(ChartBuilder.ChartType chartType) {
        switch (chartType) {
            case hiccupDurationByPercentile:
                return getLatencyName() + " By Percentile Distribution";
            case maxHiccupDurationByTimeInterval:
                return "Maximum " + getLatencyName() + " In Time Interval";
            case countOfBucketedHiccupValues:
                if (Application.getJHiccupViewerConfiguration().showGraphOfCountOfBucketedPauseValues()) {
                    return "Pause Duration Bucketed Values";
                } else {
                    return "Raw " + getLatencyName() + " Duration Bucketed Values";
                }
        }
        throw new IllegalArgumentException();
    }

    static String getXAxisLabel(ChartBuilder.ChartType chartType) {
        switch (chartType) {
            case hiccupDurationByPercentile:
                return "Percentile";
            case maxHiccupDurationByTimeInterval:
                return "Elapsed Time (sec)";
            case countOfBucketedHiccupValues:
                if (Application.getJHiccupViewerConfiguration().showGraphOfCountOfBucketedPauseValues()) {
                    return "Bucketed Pause Duration (millisec)";
                } else {
                    return "Bucketed Raw " + getLatencyName() + " Duration (millisec)";
                }
        }
        throw new IllegalArgumentException();
    }

    static String getYAxisLabel(ChartBuilder.ChartType chartType) {
        switch (chartType) {
            case hiccupDurationByPercentile:
                return getLatencyName() + " Duration (msec)";
            case maxHiccupDurationByTimeInterval:
                return getLatencyName() + " Duration (msec)";
            case countOfBucketedHiccupValues:
                return "Number in Bucket";
        }
        throw new IllegalArgumentException();
    }

    static String getLogAxisLabel(ChartBuilder.ChartType chartType) {
        switch (chartType) {
            case hiccupDurationByPercentile:
                return getLatencyName() + " by Percentile";
        }
        throw new IllegalArgumentException();
    }
}
