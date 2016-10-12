/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

public class JHiccupViewerConfiguration {

    // All configurable options (try to keep them in order in the various sections)
    private boolean    _verbose;
    private String     _nameOfJHiccupFileToOpen;
    private boolean    _exitAfterParsingFile;
    private boolean    _showGraphOfCountOfBucketedHiccupValues;
    private boolean    _showGraphOfCountOfBucketedPauseValues;
    private boolean    _printPercentileDataToStdout;
    private int        _numberOfLongestMaxHiccupsToPrintToStdout;

    // Strings to use for the properties file
    public static final String strVerbose = "Verbose";
    public static final String strNameOfJHiccupFileToOpen = "NameOfJHiccupFileToOpen";
    public static final String strExitAfterParsingFile = "ExitAfterParsingFile";
    public static final String strShowGraphOfCountOfBucketedHiccupValues = "ShowGraphOfCountOfBucketedHiccupValues";
    public static final String strShowGraphOfCountOfBucketedPauseValues = "ShowGraphOfCountOfBucketedPauseValues";
    public static final String strPrintPercentileDataToStdout = "PrintPercentileDataToStdout";
    public static final String strNumberOfLongestMaxHiccupsToPrintToStdout = "NumberOfLongestMaxHiccupsToPrintToStdout";
    public static final int    defaultNumberOfLongestMaxHiccupsToPrintToStdout = 15;

    public JHiccupViewerConfiguration() {
        // Set the default values
        _verbose                 = false;
        _nameOfJHiccupFileToOpen = null;
        _exitAfterParsingFile    = false;
        _showGraphOfCountOfBucketedHiccupValues = false;
        _showGraphOfCountOfBucketedPauseValues  = false;
        _printPercentileDataToStdout            = false;
        _numberOfLongestMaxHiccupsToPrintToStdout = defaultNumberOfLongestMaxHiccupsToPrintToStdout;
    }

    // Get values
    public boolean verbose() {
        return _verbose;
    }
    public String nameOfJHiccupFileToOpen() {
        return _nameOfJHiccupFileToOpen;
    }
    public boolean exitAfterParsingFile() {
        return _exitAfterParsingFile;
    }
    public boolean showGraphOfCountOfBucketedHiccupValues() {
        return _showGraphOfCountOfBucketedHiccupValues;
    }
    public boolean showGraphOfCountOfBucketedPauseValues() {
        return _showGraphOfCountOfBucketedPauseValues;
    }
    public boolean printPercentileDataToStdout() {
        return _printPercentileDataToStdout;
    }
    public int numberOfLongestMaxHiccupsToPrintToStdout() {
        return _numberOfLongestMaxHiccupsToPrintToStdout;
    }

    // Set values
    public void setVerbose(boolean valueIn) {
        // We need to ensure that logging has been initialized before turning it on
        if (valueIn) {
            Application.initializeLogging();
        }
        _verbose = valueIn;
    }
    public void setNameOfJHiccupFileToOpen(String valueIn) {
        _nameOfJHiccupFileToOpen = valueIn;
    }
    public void setExitAfterParsingFile(boolean valueIn) {
        _exitAfterParsingFile = valueIn;
    }
    public void setShowGraphOfCountOfBucketedHiccupValues(boolean valueIn) {
        _showGraphOfCountOfBucketedHiccupValues = valueIn;
    }
    public void setShowGraphOfCountOfBucketedPauseValues(boolean valueIn) {
        _showGraphOfCountOfBucketedPauseValues = valueIn;
    }
    public void setPrintPercentileDataToStdout(boolean valueIn) {
        _printPercentileDataToStdout = valueIn;
    }
    public void setNumberOfLongestMaxHiccupsToPrintToStdout(int valueIn) {
        int value = valueIn > -1 ? valueIn : 0;
        _numberOfLongestMaxHiccupsToPrintToStdout = value;
    }
    public void setNumberOfLongestMaxHiccupsToPrintToStdout(String valueIn) {
        try {
            int intValue = Integer.parseInt(valueIn);
            setNumberOfLongestMaxHiccupsToPrintToStdout(intValue);
        } catch (Exception e) {
            handleException(e, strNumberOfLongestMaxHiccupsToPrintToStdout, valueIn);
        }
    }

    public void handleException(Exception except, String commandLineOption, String value) {
        // We don't have much choice because we are in initialization, so just document and exit
        System.err.println("HistogramLogAnalyzer: Error parsing command line option");
        System.err.println("HistogramLogAnalyzer:   Command line option is: " + commandLineOption);
        System.err.println("HistogramLogAnalyzer:   Value is: " + value);
        except.printStackTrace();
        System.exit(1);
    }

}
