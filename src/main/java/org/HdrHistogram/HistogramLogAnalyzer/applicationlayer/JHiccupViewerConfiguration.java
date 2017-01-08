/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

public class JHiccupViewerConfiguration {

    private static final JHiccupViewerConfiguration instance = new JHiccupViewerConfiguration();

    private String     _nameOfJHiccupFileToOpen;
    private boolean enableOldStyleBucketChart = false;

    private JHiccupViewerConfiguration() {
        String value = System.getProperty("enableOldStyleBucketChart");
        if ("true".equals(value)) {
            enableOldStyleBucketChart = true;
        }
    }

    public boolean getEnableOldStyleBucketChart() {
        return enableOldStyleBucketChart;
    }

    public static JHiccupViewerConfiguration getInstance() {
        return instance;
    }

    public String nameOfJHiccupFileToOpen() {
        return _nameOfJHiccupFileToOpen;
    }

    public void setNameOfJHiccupFileToOpen(String valueIn) {
        _nameOfJHiccupFileToOpen = valueIn;
    }


}
