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
                // -f - Specify the name of the file to parse on the command line option [testing]
                if (args[i].equals("-f")) {
                    int nextArg = ++i;
                    if (nextArg >= argsLength) {
                        throw new Exception("Invalid argument: You must specify the name of the latency file after the -f option");
                    }
                    // TODO: Should validate that the file exists, but will be handled later anyway
                    jhViewerConfig.setNameOfJHiccupFileToOpen(args[i]);

                } else if (args[i].equals("-help") || args[i].equals("-h")) {
                    printHelpMessage();
                    System.exit(1);

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
        System.err.println("Usage: java -jar HistogramLogAnalyzer-*.jar [-help | -h] [-jc] [-f nameOfLatencyFileToOpen]");
        System.err.println(
            " [-help | -h]                   Help - prints this message" + _lineSeparator +
            " [-f nameOfLatencyFileToOpen]   File name of the latency log file to open" + _lineSeparator);
    }
}
