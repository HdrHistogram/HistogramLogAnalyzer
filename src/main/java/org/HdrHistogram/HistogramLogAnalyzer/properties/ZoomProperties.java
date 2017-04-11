/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.properties;

import org.HdrHistogram.HistogramLogAnalyzer.applicationlayer.HLAChartType;
import org.jfree.data.Range;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class ZoomProperties {

    public enum AxisType { DOMAIN, RANGE }


    public static class ZoomValue {

        private Range range;
        private AxisType axisType;

        // it's either date or elapsed-time bounds
        private HLAChartType chartType;

        public ZoomValue(AxisType axisType, Range range, HLAChartType chartType) {
            this.axisType = axisType;
            this.range = range;
            this.chartType = chartType;
        }

        public AxisType getAxisType() {
            return axisType;
        }

        public Range getRange() {
            return range;
        }

        public HLAChartType getChartType() {
            return chartType;
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
