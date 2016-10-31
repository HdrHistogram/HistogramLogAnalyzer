/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import org.HdrHistogram.HistogramLogAnalyzer.charts.PercentileChartBuilder;
import org.HdrHistogram.HistogramLogAnalyzer.charts.TimelineChartBuilder;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.HistogramModel;

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

    LatencyPanel(String inputFileName, SLAProperties slaProperties, MWPProperties mwpProperties) {
        ZoomProperty zoomProperty = new ZoomProperty();

        HistogramModel histogramModel = null;
        try {
            histogramModel = new HistogramModel(inputFileName, null, null, mwpProperties);
        } catch (IOException e) {
            e.printStackTrace();
        }

        setLayout(new GridLayout(2, 1));
        setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JPanel timelineChart =
                timelineChartBuilder.createTimelineChart(histogramModel, zoomProperty);
        JPanel percentileChart =
                percentileChartBuilder.createPercentileChart(histogramModel, slaProperties, zoomProperty, mwpProperties);

        add(timelineChart);
        add(percentileChart);
    }
}
