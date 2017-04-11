/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.charts;

import org.jfree.data.Range;
import org.jfree.data.general.Series;
import org.jfree.data.xy.XYDataset;

interface CommonSeriesCollection {

    void add(CommonSeries series);

    Range getDomainBounds();

    XYDataset getDataset();
}
