/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.properties;

import org.HdrHistogram.HistogramLogAnalyzer.applicationlayer.HLAChartType;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.HistogramModel;
import org.jfree.data.Range;
import org.jfree.data.time.DateRange;
import sun.util.calendar.ZoneInfo;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class DateProperties {

    private boolean isDatesVisible = false;

    // format-related fields
    private boolean showDate = false;
    private boolean showSeconds = false;
    private boolean showMilliseconds = false;
    private boolean overrideTimezone = false;
    private TimeZone userTimezone = null;

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public boolean isDatesVisible() {
        return isDatesVisible;
    }

    public boolean isChartActive(HLAChartType chartType) {
        return (isDatesVisible ? chartType == HLAChartType.TIMELINE_DATE :
                            chartType == HLAChartType.TIMELINE_ELAPSED_TIME);
    }

    public static DateRange rangeToDateRange(double startTime, Range range) {
        double newLowerBound = startTime + range.getLowerBound();
        double newUpperBound = startTime + range.getUpperBound();
        Date lowerBoundDate = new Date((long) (newLowerBound * 1000));
        Date upperBoundDate = new Date((long) (newUpperBound * 1000));
        return new DateRange(lowerBoundDate, upperBoundDate);
    }

    public static Range dateRangeToRange(double startTime, Range dateRange) {
        double newLowerBound = dateRange.getLowerBound() / 1000.0d;
        double newUpperBounds = dateRange.getUpperBound() / 1000.0d;
        return new Range(newLowerBound - startTime, newUpperBounds - startTime);
    }


    public void setDatesVisible(boolean newValue) {
        isDatesVisible = newValue;
        pcs.firePropertyChange("setDatesVisible", !newValue, newValue);
    }

    public boolean isShowDate() {
        return showDate;
    }

    public void setShowDate(boolean newValue) {
        showDate = newValue;
        pcs.firePropertyChange("setShowDate", !newValue, newValue);
    }

    public boolean isShowSeconds() {
        return showSeconds;
    }

    public void setShowSeconds(boolean newValue) {
        showSeconds = newValue;
        pcs.firePropertyChange("setShowSeconds", !newValue, newValue);
    }

    public boolean isShowMilliseconds() {
        return showMilliseconds;
    }

    public void setShowMilliseconds(boolean newValue) {
        showMilliseconds = newValue;
        pcs.firePropertyChange("setShowMilliseconds", !newValue, newValue);
    }

    public boolean isOverrideTimezone() {
        return overrideTimezone;
    }

    public void setOverrideTimezone(boolean overrideTimezone) {
        this.overrideTimezone = overrideTimezone;
    }

    public void setUserTimezone(String userTimezone) {
        this.userTimezone = TimeZone.getTimeZone(userTimezone);

        pcs.firePropertyChange("setUserTimezone", null, userTimezone);
    }

    public TimeZone getTimeZone(List<HistogramModel> models) {
        if (overrideTimezone) {
            return userTimezone;
        }

        // try to get timezone of first model
        TimeZone timezone = models.get(0).getTimeZone();
        if (timezone != null) {
            return timezone;
        }

        return ZoneInfo.getDefault();
    }

    public static String getShortTimezoneString(TimeZone timeZone, Date date) {
        return timeZone.getDisplayName(timeZone.inDaylightTime(date), TimeZone.SHORT);
    }

    public DateFormat getDateFormat(List<HistogramModel> models) {
        String dateFormat;
        if (isShowDate()) {
            dateFormat = "yyyy/MM/dd HH:mm";
        } else {
            dateFormat = "HH:mm";
        }
        if (isShowSeconds()) {
            dateFormat = dateFormat.concat(":ss");
            if (isShowMilliseconds()) {
                dateFormat = dateFormat.concat(":SSS");
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        sdf.setTimeZone(getTimeZone(models));

        return sdf;
    }

    public static List<String> getAllTimeZones() {
        List<String> ret = new ArrayList<>();
        String[] tzIds = TimeZone.getAvailableIDs();
        for (String tzId : tzIds) {
            if (tzId.length() > 3 && !tzId.startsWith("Etc")) {
                String tzName = TimeZone.getTimeZone(tzId).getDisplayName();
                if (!tzName.startsWith("GMT-") && !tzName.startsWith("GMT+")) {
                    ret.add(tzId);
                }
            }
        }
        return ret;
    }

    public String getLocalTimeZone() {
        return TimeZone.getDefault().getID();
    }

}
