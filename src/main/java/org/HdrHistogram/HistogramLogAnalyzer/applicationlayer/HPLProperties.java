/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/*
 * HPL stands for horizontal percentile lines
 */
public class HPLProperties {

    private boolean isHPLVisible = false;

    public static double[] getPercentiles() {
        return new double[] { 0.99D, 0.999D, 0.9999D };
    }

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public boolean isHPLVisible() {
        return isHPLVisible;
    }

    void toggleHPLVisibility(boolean newValue) {
        isHPLVisible = newValue;
        pcs.firePropertyChange("hplShow", !newValue, newValue);
    }

}
