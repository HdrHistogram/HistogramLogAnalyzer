/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

public class JHiccupViewerCommandLineArguments {

    private String _lineSeparator = System.getProperty("line.separator");

    public JHiccupViewerCommandLineArguments(JHiccupViewerConfiguration jhViewerConfig, String[] args) {

        try {
            int argsLength = args.length;
            for (int i = 0; i < argsLength; ++i) {
                // -v - verbose
                if (args[i].equals("-v")) {
                    jhViewerConfig.setVerbose(true); 

                // -eap - Exit after parsing file [testing]
                } else if (args[i].equals("-eap")) {
                    jhViewerConfig.setExitAfterParsingFile(true); 

                // -f - Specify the name of the file to parse on the command line option [testing]
                } else if (args[i].equals("-f")) {
                    int nextArg = ++i;
                    if (nextArg >= argsLength) {
                        throw new Exception("Invalid argument: You must specify the name of the latency file after the -f option");
                    }
                    // TODO: Should validate that the file exists, but will be handled later anyway
                    jhViewerConfig.setNameOfJHiccupFileToOpen(args[i]);

                } else if (args[i].equals("-help") || args[i].equals("-h")) {
                    printHelpMessage();
                    System.exit(1);

                } else if (args[i].equals("-hh")) {
                    printHelpMessage2();
                    System.exit(1);

                // -jc - show raw jHiccup bucketed count graph
                } else if (args[i].equals("-jc")) {
                    jhViewerConfig.setShowGraphOfCountOfBucketedHiccupValues(true);

                // -pc - show raw pause bucketed count graph
                } else if (args[i].equals("-pc")) {
                    jhViewerConfig.setShowGraphOfCountOfBucketedPauseValues(true);
                    jhViewerConfig.setShowGraphOfCountOfBucketedHiccupValues(true);

                // -pp - print the percentile data to stdout
                } else if (args[i].equals("-pp")) {
                    jhViewerConfig.setPrintPercentileDataToStdout(true);

                // -nlh - print this number of the longest max hiccups to stdout when using -pp
                } else if (args[i].equals("-nlh")) {
                    int nextArg = ++i;
                    if (nextArg >= argsLength) {
                        throw new Exception("Invalid argument: You must specify the number of longest max hiccups you want to print after the -nlh option");
                    }
                    jhViewerConfig.setNumberOfLongestMaxHiccupsToPrintToStdout(args[i]);

                } else {
                    throw new Exception("Invalid argument: " + args[i]);
                }
            }

        } catch (Exception e) {
            if (e.getMessage() != null) {
                System.err.println(e.getMessage());
            } else {
                System.err.println("Command line error");
            }
            printHelpMessage();
            System.exit(1);
        }
    }

    private void printHelpMessage() {
        System.err.println("Usage: java -jar HistogramLogAnalyzer-*.jar [-help | -h] [-jc] [-pp [-nlh numberLongestMaxLatency]] [-f nameOfLatencyFileToOpen]");
        System.err.println(
            " [-help | -h]                   Help - prints this message" + _lineSeparator +
            " [-jc]                          Display the graph of the count of bucketed latency values" + _lineSeparator +
            " [-pp]                          Print the percentile and max data to stdout" + _lineSeparator +
            " [-nlh numberLongestMaxLatency] When using -pp, print this number of the longest max latency to stdout (default: " +
                    JHiccupViewerConfiguration.defaultNumberOfLongestMaxHiccupsToPrintToStdout + ")" + _lineSeparator +
            " [-f nameOfLatencyFileToOpen]   File name of the latency log file to open" + _lineSeparator);
    }

    private void printHelpMessage2() {
        printHelpMessage();
        System.err.println(
            " [-pc]                          Show raw pause bucketed count graph" + _lineSeparator +
            " [-eap]                         Exit after parsing (testing)");
    }
}
