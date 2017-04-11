/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.properties;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

public class MWPProperties {

    private List<MWPEntry> mwpEntries = new ArrayList<MWPEntry>();

    private boolean isMWPVisible = false;

    public static class MWPEntry {

        private Double percentile;
        private long windowLength; // in msec

        public MWPEntry(Double percentile, long windowLength) {
            this.percentile = percentile;
            this.windowLength = windowLength;
        }

        public Double getPercentile() {
            return percentile;
        }

        public long getWindowLength() {
            return windowLength;
        }

        public boolean isDefaultEntry() {
            return DEFAULT_MWP_ENTRY.equals(this);
        }

        @Override
        public boolean equals(Object anObject) {
            if (this == anObject) {
                return true;
            }

            if (anObject instanceof MWPEntry) {
                MWPEntry anEntry = (MWPEntry)anObject;
                return getPercentile().equals(anEntry.getPercentile()) &&
                        getWindowLength() == anEntry.getWindowLength();
            }
            return false;
        }

        public String toString() {
            double windowLengthInSeconds = (double) getWindowLength() / 1000;
            String secondsWording = windowLengthInSeconds == 1 ? " second" : " seconds";

            if (windowLengthInSeconds == (long) windowLengthInSeconds) {
                return " " + getPercentile() + "%'ile, " +
                        String.format("%d", (long) windowLengthInSeconds) + secondsWording;
            } else {
                return " " + getPercentile() + "%'ile, " +
                        String.format("%s", windowLengthInSeconds) + secondsWording;
            }
        }
    }

    public MWPProperties() {
        mwpEntries.add(DEFAULT_MWP_ENTRY);
    }

    // FIXME: default entry still contains interval count (but not used)
    private static final MWPEntry DEFAULT_MWP_ENTRY = new MWPEntry(100.0, 1);

    public static MWPEntry getDefaultMWPEntry() {
        return DEFAULT_MWP_ENTRY;
    }

    public List<MWPEntry> getMWPEntries() {
        return mwpEntries;
    }

    /*
     * entries accessible via MWP master tab (default entry not accessible)
     */
    public List<MWPEntry> getAccessibleMWPEntry() {
        List<MWPEntry> ret = new ArrayList<>();
        for (MWPEntry mwpEntry : getMWPEntries()) {
            if (!DEFAULT_MWP_ENTRY.equals(mwpEntry)) {
                ret.add(mwpEntry);
            }
        }
        return ret;
    }

    public void addMWPEntry(MWPEntry MWPEntry) {
        mwpEntries.add(MWPEntry);
    }

    public void reset() {
        mwpEntries.clear();
        mwpEntries.add(DEFAULT_MWP_ENTRY);
    }

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public boolean isMWPVisible() {
        return isMWPVisible;
    }

    public void toggleMWPVisibility(boolean newValue) {
        isMWPVisible = newValue;
        pcs.firePropertyChange("mwpShow", !newValue, newValue);
    }

    public void applyMWP() {
        pcs.firePropertyChange("applyMWP", false, true);
    }

    public boolean isShowMWPUnlocked() {
        return mwpEntries.size() > 1;
    }

}
