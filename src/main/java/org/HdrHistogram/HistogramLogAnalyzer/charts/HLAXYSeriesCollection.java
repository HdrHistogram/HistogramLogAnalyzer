/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.charts;

import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

class HLAXYSeriesCollection extends XYSeriesCollection
        implements CommonSeriesCollection {

    @Override
    public void add(CommonSeries series) {
        super.addSeries((XYSeries) series);
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
