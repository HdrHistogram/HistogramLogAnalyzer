/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class ViewProperties {

    // Percentile by default but can be changed to Buckets via "View" settings
    private LatencyChartType bottomChartType = LatencyChartType.PERCENTILE;

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    LatencyChartType getBottomChartType() {
        return bottomChartType;
    }

    void toogleBottomChartType(LatencyChartType newBottomChartType) {
        if (bottomChartType != newBottomChartType) {
            LatencyChartType oldBottomChartType = bottomChartType;
            bottomChartType = newBottomChartType;
            pcs.firePropertyChange("bottomChartTypeChanged", oldBottomChartType, bottomChartType);
        }
    }
}
