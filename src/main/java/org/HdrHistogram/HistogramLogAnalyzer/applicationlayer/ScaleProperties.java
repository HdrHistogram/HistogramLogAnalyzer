/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class ScaleProperties {

    public static class ScaleEntry {
        private Double maxXValue;
        private Double maxYValue;

        public ScaleEntry(double maxXValue, double maxYValue) {
            this.maxXValue = maxXValue;
            this.maxYValue = maxYValue;
        }

        public double getMaxXValue() {
            return maxXValue;
        }

        public double getMaxYValue() {
            return maxYValue;
        }
    }

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public void applyScale(ScaleEntry se) {
        pcs.firePropertyChange("applyScale", null, se);
    }
}
