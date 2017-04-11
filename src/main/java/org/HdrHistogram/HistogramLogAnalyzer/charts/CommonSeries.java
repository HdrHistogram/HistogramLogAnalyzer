/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.charts;

import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.PercentileObject;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.TimelineObject;
import org.jfree.data.Range;
import org.jfree.data.general.Series;

interface CommonSeries {

    void add(TimelineObject timelineObject, double startTime);

    void add(double latencyAxisValue, Range domainBounds);

    Series get();
}
