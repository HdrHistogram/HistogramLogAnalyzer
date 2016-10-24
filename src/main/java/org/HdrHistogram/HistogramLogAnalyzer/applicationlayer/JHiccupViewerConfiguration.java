/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

public class JHiccupViewerConfiguration {

    // All configurable options (try to keep them in order in the various sections)
    private String     _nameOfJHiccupFileToOpen;

    public JHiccupViewerConfiguration() {
        // Set the default values
        _nameOfJHiccupFileToOpen = null;
    }

    // Get values
    public String nameOfJHiccupFileToOpen() {
        return _nameOfJHiccupFileToOpen;
    }

    public void setNameOfJHiccupFileToOpen(String valueIn) {
        _nameOfJHiccupFileToOpen = valueIn;
    }
}
