/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

public class MWPProperties {

    private List<MWPEntry> mwpEntries = new ArrayList<MWPEntry>();

    public static class MWPEntry {

        private Double percentile;
        private int intervalCount;

        MWPEntry(Double percentile, int intervalCount) {
            this.percentile = percentile;
            this.intervalCount = intervalCount;
        }

        public Double getPercentile() {
            return percentile;
        }

        public int getIntervalCount() {
            return intervalCount;
        }

        @Override
        public boolean equals(Object anObject) {
            if (this == anObject) {
                return true;
            }

            if (anObject instanceof MWPEntry) {
                MWPEntry anEntry = (MWPEntry)anObject;
                return getPercentile().equals(anEntry.getPercentile()) &&
                        getIntervalCount() == anEntry.getIntervalCount();
            }
            return false;
        }
    }

    MWPProperties() {
        mwpEntries.add(DEFAULT_MWP_ENTRY);
    }

    private static final MWPEntry DEFAULT_MWP_ENTRY = new MWPEntry(100.0, 1);

    public static MWPEntry getDefaultMWPEntry() {
        return DEFAULT_MWP_ENTRY;
    }

    public List<MWPEntry> getMWPEntries() {
        return mwpEntries;
    }

    void addMWPEntry(MWPEntry MWPEntry) {
        mwpEntries.add(MWPEntry);
    }

    void clear() {
        mwpEntries.clear();
    }

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    void applyMWP() {
        pcs.firePropertyChange("applyMWP", false, true);
    }

}
