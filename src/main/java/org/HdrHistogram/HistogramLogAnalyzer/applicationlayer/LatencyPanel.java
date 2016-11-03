/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import org.HdrHistogram.HistogramLogAnalyzer.charts.PercentileChartBuilder;
import org.HdrHistogram.HistogramLogAnalyzer.charts.TimelineChartBuilder;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.HistogramModel;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.MaxPercentileIterator;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.PercentileObject;

import javax.swing.*;
import java.awt.GridLayout;
import java.awt.Color;
import java.io.IOException;

/*
 * Custom JPanel that contains top (timeline) and bottom (percentile) charts
 */
class LatencyPanel extends JPanel
{
    private static TimelineChartBuilder timelineChartBuilder = new TimelineChartBuilder();

    private static PercentileChartBuilder percentileChartBuilder = new PercentileChartBuilder();

    private HistogramModel histogramModel = null;
    private ScaleProperties scaleProperties = null;
    private ScaleProperties.ScaleEntry scaleEntry = null;

    LatencyPanel(String inputFileName, SLAProperties slaProperties,
                 MWPProperties mwpProperties)
    {
        ZoomProperty zoomProperty = new ZoomProperty();
        scaleProperties = new ScaleProperties();

        histogramModel = null;
        try {
            histogramModel = new HistogramModel(inputFileName, null, null, mwpProperties);
        } catch (IOException e) {
            e.printStackTrace();
        }

        setLayout(new GridLayout(2, 1));
        setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JPanel timelineChart =
                timelineChartBuilder.createTimelineChart(histogramModel, zoomProperty, scaleProperties);
        JPanel percentileChart =
                percentileChartBuilder.createPercentileChart(histogramModel,
                        slaProperties, zoomProperty,mwpProperties, scaleProperties);

        add(timelineChart);
        add(percentileChart);
    }

    /*
     * returns max values on X/Y axis for this percentile chart
     */
    ScaleProperties.ScaleEntry getScaleEntry() {
        if (scaleEntry == null) {
            double maxLatencyAxisValue = 0.0;
            double maxPercentileAxisValue = 0.0;
            for (String tag : histogramModel.getTags()) {
                MaxPercentileIterator mpi = histogramModel.listMaxPercentileObjects(tag);
                PercentileObject mpo;
                while (mpi.hasNext()) {
                    mpo = mpi.next();
                    maxLatencyAxisValue = mpo.getLatencyAxisValue();
                    maxPercentileAxisValue = mpo.getPercentileAxisValue();
                }
            }
            scaleEntry = new ScaleProperties.ScaleEntry(maxPercentileAxisValue, maxLatencyAxisValue);
        }
        return scaleEntry;
    }

    void scale() {
        scaleProperties.applyScale(getScaleEntry());
    }

    void scale(ScaleProperties.ScaleEntry scaleEntry) {
        scaleProperties.applyScale(scaleEntry);
    }
}
