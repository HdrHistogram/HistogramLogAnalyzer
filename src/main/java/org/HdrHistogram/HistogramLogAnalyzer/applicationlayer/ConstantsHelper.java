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

    private enum LogGeneratorTool {CASSANDRA_STRESS, JHICCUP, UNKNOWN}
    private static LogGeneratorTool logGenTool;
    private static String currentInputFileName;

    private static final String CASSANDRA_STRESS_TOOL_NAME = "Cassandra Stress";
    private static final String JHICCUP_TOOL_NAME = "jHiccup";

    static void detectLogGeneratorTool(String inputFileName)
            throws IOException
    {
        BufferedReader reader = null;
        currentInputFileName = inputFileName;
        try {
            reader = new BufferedReader(new FileReader(new File(currentInputFileName)));
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
            title += new File(currentInputFileName).getName();
        } else {
            title += currentInputFileName;
        }
        return title;
    }

    public static String getLatencyName() {
        return "Latency";
    }

    public static String getChartTitle(LatencyChartType chartType) {
        switch (chartType) {
            case PERCENTILE:
                return getLatencyName() + " By Percentile Distribution";
            case TIMELINE:
                return "Maximum " + getLatencyName() + " In Time Interval";
        }
        throw new IllegalArgumentException();
    }

    public static String getXAxisLabel(LatencyChartType chartType) {
        switch (chartType) {
            case PERCENTILE:
                return "Percentile";
            case TIMELINE:
                return "Elapsed Time (sec)";
        }
        throw new IllegalArgumentException();
    }

    public static String getYAxisLabel(LatencyChartType chartType) {
        switch (chartType) {
            case PERCENTILE:
                return getLatencyName() + " Duration (msec)";
            case TIMELINE:
                return getLatencyName() + " Duration (msec)";
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
