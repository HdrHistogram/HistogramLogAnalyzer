/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.charts;

import org.jfree.data.Range;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

class HLATimeSeriesCollection extends TimeSeriesCollection
        implements CommonSeriesCollection {

    @Override
    public void add(CommonSeries series) {
        super.addSeries((TimeSeries) series);
    }

    @Override
    public Range getDomainBounds() {
        return super.getDomainBounds(false);
    }

    @Override
    public XYDataset getDataset() {
        return this;
    }
}
