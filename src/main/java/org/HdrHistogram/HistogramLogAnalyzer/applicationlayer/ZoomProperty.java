/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class ZoomProperty {

    public static class ZoomValue {
        private String lowerBoundString;
        private String upperBoundString;

        public ZoomValue(String lowerBoundString, String upperBoundString) {
            this.lowerBoundString = lowerBoundString;
            this.upperBoundString = upperBoundString;
        }

        public String getLowerBoundString() {
            return lowerBoundString;
        }

        public String getUpperBoundString() {
            return upperBoundString;
        }
    }

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public void zoom(ZoomValue value) {
        pcs.firePropertyChange("zoom", null, value);
    }

}
