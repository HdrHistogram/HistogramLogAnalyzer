/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.datalayer;

import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.PercentileObject;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PercentileIterator extends ResultSetIterator<PercentileObject> {

    PercentileIterator(ResultSet resultSet) {
        super(resultSet);
    }

    @Override
    public PercentileObject nextObject() {
        double percentileAxisValue = 0;
        double percentileValue = 0;
        double latencyAxisValue = 0;
        try {
            percentileAxisValue = resultSet.getDouble("percentileAxisValue");
            percentileValue = resultSet.getDouble("percentileValue");
            latencyAxisValue = resultSet.getDouble("latencyAxisValue");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new PercentileObject(latencyAxisValue, percentileAxisValue, percentileValue);
    }
}
