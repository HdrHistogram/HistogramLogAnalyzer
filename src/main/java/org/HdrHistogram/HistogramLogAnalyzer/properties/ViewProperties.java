/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.properties;

import org.HdrHistogram.HistogramLogAnalyzer.applicationlayer.HLAChartType;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class ViewProperties {

    // Percentile by default but can be changed to Buckets via "View" settings
    private HLAChartType bottomChartType = HLAChartType.PERCENTILE;

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public HLAChartType getBottomChartType() {
        return bottomChartType;
    }

    public void toogleBottomChartType(HLAChartType newBottomChartType) {
        if (bottomChartType != newBottomChartType) {
            HLAChartType oldBottomChartType = bottomChartType;
            bottomChartType = newBottomChartType;
            pcs.firePropertyChange("bottomChartTypeChanged", oldBottomChartType, bottomChartType);
        }
    }
}
