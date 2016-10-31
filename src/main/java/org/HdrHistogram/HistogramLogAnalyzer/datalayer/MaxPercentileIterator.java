/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.datalayer;

import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.PercentileObject;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MaxPercentileIterator extends ResultSetIterator<PercentileObject> {

    MaxPercentileIterator(ResultSet resultSet) {
        super(resultSet);
    }

    @Override
    public PercentileObject nextObject() {
        double latencyAxisValue = 0;
        double percentileAxisValue = 0;
        try {
            latencyAxisValue = resultSet.getDouble("MaxLatencyAxisValue");
            percentileAxisValue = resultSet.getDouble("MaxPercentileAxisValue");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new PercentileObject(latencyAxisValue, percentileAxisValue);
    }
}
