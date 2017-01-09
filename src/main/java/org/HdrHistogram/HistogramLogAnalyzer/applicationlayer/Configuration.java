/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

/*
 * The class contains various CLI parameters, it's not thread safe.
 */
public class Configuration {

    private static Configuration instance;

    private boolean helpMode = false;
    private String inputFileName;

    // temp option to highlight changes in Buckets chart
    private boolean enableOldStyleBucketChart = false;

    // options related to console mode
    private boolean ppMode = false;
    private int nlhValue = 15;
    private boolean nlhSpecified = false;
    private boolean eapMode = false;

    private Configuration() {
        String value = System.getProperty("enableOldStyleBucketChart");
        if ("true".equals(value)) {
            enableOldStyleBucketChart = true;
        }
    }

    public boolean getEnableOldStyleBucketChart() {
        return enableOldStyleBucketChart;
    }

    public static Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }

    String getInputFileName() {
        return inputFileName;
    }

    boolean isPPmode() {
        return ppMode;
    }

    boolean isEAPmode() {
        return eapMode;
    }

    int getNlhValue() {
        return nlhValue;
    }

    void parseArgs(String[] args) {

        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals("-help") || args[i].equals("-h")) {
                helpMode = true;
            } else if (args[i].equals("-f")) {
                inputFileName = args[++i];
            } else if (args[i].equals("-pp")) {
                ppMode = true;
            } else if (args[i].equals("-nlh")) {
                nlhSpecified = true;
                nlhValue = Integer.parseInt(args[++i]);
            } else if (args[i].equals("-eap")) {
                eapMode = true;
            } else {
                System.err.println("Invalid args: " + args[i] +
                                   "\n");
                printHelpMessage();
                System.exit(1);
            }
        }

        if (helpMode) {
            printHelpMessage();
            System.exit(1);
        }

        if (ppMode && inputFileName == null) {
            throw new RuntimeException("-pp option requires input file");
        }

        if ((nlhSpecified || eapMode) && !ppMode) {
            throw new RuntimeException("-nlh/-eap options require specifying -pp option");
        }
    }

    private void printHelpMessage() {
        System.err.println(
            "HistogramLogAnalyzer, version " + Version.version + "\n" +
            "Usage: java -jar HistogramLogAnalyzer.jar " +
                "[-h|-help] [-pp [-nlh NUMBER]] [-eap] [-f FILE]\n" +
            "\n" +
            "Options:\n" +
            " -h|-help                       prints this message\n" +
            " -f FILE                        file name of histogram log file to process\n" +
            " -pp                            print percentile and max data to stdout\n" +
            " -nlh NUMBER                    print N top latency values to stdout\n" +
            "                                the default is to print " + nlhValue + " top latency values\n" +
            " -eap                           exit after parsing file\n");
    }

}
