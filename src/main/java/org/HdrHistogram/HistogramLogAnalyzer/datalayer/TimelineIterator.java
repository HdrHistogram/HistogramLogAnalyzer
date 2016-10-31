/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.datalayer;

import org.HdrHistogram.HistogramLogAnalyzer.applicationlayer.MWPProperties;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.TimelineObject;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TimelineIterator extends ResultSetIterator<TimelineObject> {

    TimelineIterator(ResultSet resultSet) {
        super(resultSet);
    }

    @Override
    public TimelineObject nextObject() {
        double percentileElapsedTime = 0;
        double percentileIntervalMax = 0;
        try {
            percentileElapsedTime = resultSet.getDouble("timelineAxisValue");
            percentileIntervalMax = resultSet.getDouble("latencyAxisValue");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new TimelineObject(percentileElapsedTime, percentileIntervalMax);
    }
}
