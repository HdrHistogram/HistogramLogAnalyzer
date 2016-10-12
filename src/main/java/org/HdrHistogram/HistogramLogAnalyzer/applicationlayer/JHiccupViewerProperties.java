/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * JHiccupViewerProperties is the class that encapsulates the handling of the properties
 * file used by jHiccupLogAnalyzer.
 */

public class JHiccupViewerProperties {
    private String     _filename;  // Filename of configuration file
    private Properties _prop;      // Properties loaded from file
    private static final String propertyPrefix = "org.hdrhistogram.histogramloganalyzer.";
    private static final String propertyFieldSeparator = ".";
    
    public JHiccupViewerProperties(JHiccupViewerConfiguration jhViewerConfig, String filename) {
        _filename = filename;
        _prop = new Properties();
        File file = new File(this._filename);
        
        try {
            if (file.exists() && file.isFile()) {
                InputStream in = new FileInputStream(file);
                _prop.load(in);
                in.close();
            }
        } catch (Exception e) {
            // We don't have much choice because we are in initialization, so just document and exit
            System.err.println("HistogramLogAnalyzer: Error opening properties file: " + _filename);
            e.printStackTrace();
            System.exit(1);
        }

        // Supported properties that can be set in the properties file
        String propVerbose = propertyPrefix + JHiccupViewerConfiguration.strVerbose;
        String propPrintPercentileDataToStdout = propertyPrefix + JHiccupViewerConfiguration.strPrintPercentileDataToStdout;
        String propNumberOfLongestMaxHiccupsToPrintToStdout = propertyPrefix + JHiccupViewerConfiguration.strNumberOfLongestMaxHiccupsToPrintToStdout;

        boolean trueOrFalse = false;
        int     intValue = 0;

        for (Object key : _prop.keySet()) {
            String strkey = key.toString();
            if (strkey.equals(propVerbose)) {
                String trueOrFalseString = (String) _prop.get(key);
                try {
                    trueOrFalse = Boolean.parseBoolean(trueOrFalseString);
                } catch (Exception e) {
                    handlePropertyException(e, propVerbose, trueOrFalseString);
                }
                jhViewerConfig.setVerbose(trueOrFalse); 

            } else if (strkey.equals(propPrintPercentileDataToStdout)) {
                String trueOrFalseString = (String) _prop.get(key);
                try {
                    trueOrFalse = Boolean.parseBoolean(trueOrFalseString);
                } catch (Exception e) {
                    handlePropertyException(e, propPrintPercentileDataToStdout, trueOrFalseString);
                }
                jhViewerConfig.setPrintPercentileDataToStdout(trueOrFalse); 

            } else if (strkey.equals(propNumberOfLongestMaxHiccupsToPrintToStdout)) {
                String intValueString = (String) _prop.get(key);
                try {
                    intValue = Integer.parseInt(intValueString);
                } catch (Exception e) {
                    handlePropertyException(e, propNumberOfLongestMaxHiccupsToPrintToStdout, intValueString);
                }
                jhViewerConfig.setNumberOfLongestMaxHiccupsToPrintToStdout(intValue);
            }
        }
    }
    
    public boolean propertiesAvailable() {
        return _prop.size() != 0;
    }

    private float getAFloatProperty(Object key, String propertyName) {
        String floatString = (String) _prop.get(key);
        try {
            float floatValue = Float.parseFloat(floatString);
            return floatValue;
        } catch (Exception e) {
            handlePropertyException(e, propertyName, floatString);
        }
        return 0.0F;
    }

    private int getAnIntProperty(Object key, String propertyName) {
        String intString = (String) _prop.get(key);
        try {
            int intValue = Integer.parseInt(intString);
            return intValue;
        } catch (Exception e) {
            handlePropertyException(e, propertyName, intString);
        }
        return 0;
    }

    public void handlePropertyException(Exception except, String property, String value) {
        // We don't have much choice because we are in initialization, so just document and exit
        System.err.println("HistogramLogAnalyzer: Error parsing properties file: " + _filename);
        System.err.println("HistogramLogAnalyzer:   Property is: " + property);
        System.err.println("HistogramLogAnalyzer:   Value is: " + value);
        except.printStackTrace();
        System.exit(1);
    }

    public static void main(String args[]) {
        if (args.length == 0) {
            System.err.println("JHiccupViewerProperties takes one option, the name of the configuration file: .histogramloganalyzer.properties");
        }
        JHiccupViewerConfiguration jhViewerConfig = new JHiccupViewerConfiguration();
        JHiccupViewerProperties myConfig = new JHiccupViewerProperties(jhViewerConfig, args[0]);
	System.out.println("HistogramLogAnalyzer: verbose value: " + jhViewerConfig.verbose());
	System.out.println("HistogramLogAnalyzer: LatencyFileToOpen value: " + jhViewerConfig.nameOfJHiccupFileToOpen());
	System.out.println("HistogramLogAnalyzer: exitAfterParsingFile: " + jhViewerConfig.exitAfterParsingFile());
	System.out.println("HistogramLogAnalyzer: showGraphOfCountOfBucketedLatencyValues: " + jhViewerConfig.showGraphOfCountOfBucketedHiccupValues());
    }
}
