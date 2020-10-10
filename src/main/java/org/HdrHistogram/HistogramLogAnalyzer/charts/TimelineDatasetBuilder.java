/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.charts;

import org.HdrHistogram.HistogramLogAnalyzer.applicationlayer.HLAChartType;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.HistogramModel;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.MaxPercentileIterator;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.TimelineIterator;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.PercentileObject;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.TimelineObject;
import org.HdrHistogram.HistogramLogAnalyzer.properties.MWPProperties;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

class TimelineDatasetBuilder {

    private CommonSeries createSeries(String name, HLAChartType chartType) {
        if (chartType == HLAChartType.TIMELINE_DATE) {
            return new HLATimeSeries(name);
        } else {
            return new HLAXYSeries(name);
        }
    }

    private CommonSeriesCollection createSeriesCollection(HLAChartType chartType) {
        if (chartType == HLAChartType.TIMELINE_DATE) {
            return new HLATimeSeriesCollection();
        } else {
            return new HLAXYSeriesCollection();
        }
    }

    private static final String DEFAULT_KEY = "Max per interval";

    CommonSeriesCollection build(List<HistogramModel> histogramModels, HLAChartType chartType) {

        CommonSeriesCollection ret = createSeriesCollection(chartType);

        // tool doesn't support MWP/HPL for charts with multiple files
        // tool doesn't support MWP/HPL for files with multiple tags
        boolean multipleFiles = histogramModels.size() > 1;
        if (!multipleFiles) {
            HistogramModel histogramModel = histogramModels.get(0);
            Set<String> tags = histogramModel.getTags();

            boolean multipleTags = tags.size() > 1;
            if (!multipleTags) {
                String tag = null;
                if(!tags.isEmpty()) {
                    tag = tags.iterator().next();
                }
                MWPProperties mwpProperties = histogramModel.getMwpProperties();
                List<MWPProperties.MWPEntry> mwpEntries = mwpProperties.getMWPEntries();
                for (MWPProperties.MWPEntry mwpEntry : mwpEntries) {
                    String key;
                    if (mwpEntry.isDefaultEntry()) {
                        key = DEFAULT_KEY;
                    } else {
                        key = mwpEntry.toString();
                    }

                    double startTime = histogramModel.getStartTimeSec();
                    CommonSeries series = createSeries(key, chartType);
                    TimelineIterator ti = histogramModel.listTimelineObjects(false, tag, mwpEntry);
                    while (ti.hasNext()) {
                        TimelineObject to = ti.next();
                        series.add(to, startTime);
                    }
                    ret.add(series);
                }

                // HPL lines
                Iterator<PercentileObject> pi = histogramModel.listHPLPercentileObjects(tag);
                while (pi.hasNext()) {
                    PercentileObject po = pi.next();
                    String key = String.valueOf(po.getPercentileValue() * 100);
                    key = !key.contains(".") ? key : key.replaceAll("0*$", "").replaceAll("\\.$", "");

                    CommonSeries series = createSeries(key + "%", chartType);
                    series.add(po.getLatencyAxisValue(), ret.getDomainBounds());
                    ret.add(series);
                }

                // Max line
                Double maxLatencyAxisValue = 0.0;
                MaxPercentileIterator mpi = histogramModel.listMaxPercentileObjects(null);
                PercentileObject mpo;
                while (mpi.hasNext()) {
                    mpo = mpi.next();
                    maxLatencyAxisValue = Math.max(maxLatencyAxisValue, mpo.getLatencyAxisValue());
                }

                CommonSeries series = createSeries("Max", chartType);
                series.add(maxLatencyAxisValue, ret.getDomainBounds());
                ret.add(series);

                return ret;
            }
        }

        for (HistogramModel histogramModel : histogramModels) {
            boolean multipleTags = histogramModel.getTags().size() > 1;
            for (String tag : histogramModel.getTags()) {
                String key;
                if (multipleFiles) {
                    key = histogramModel.getShortFileName();
                } else {
                    if (multipleTags) {
                        key = tag == null ? "No Tag" : tag;
                    } else {
                        key = DEFAULT_KEY;
                    }
                }

                CommonSeries series = createSeries(key, chartType);
                TimelineIterator ti = histogramModel.listTimelineObjects(true, tag, null);
                while (ti.hasNext()) {
                    TimelineObject to = ti.next();
                    series.add(to, histogramModel.getStartTimeSec());
                }
                ret.add(series);
            }
        }
        return ret;
    }
}
