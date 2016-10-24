/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import org.HdrHistogram.HistogramLogAnalyzer.charts.PercentileChartBuilder;
import org.HdrHistogram.HistogramLogAnalyzer.charts.TimelineChartBuilder;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.Parser;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.DBConnect;

import javax.swing.*;
import java.awt.GridLayout;
import java.awt.Color;
import java.io.IOException;
import java.util.Set;

/*
 * Custom JPanel that contains top (timeline) and bottom (percentile) charts
 */
class LatencyPanel extends JPanel
{
    private static TimelineChartBuilder timelineChartBuilder = new TimelineChartBuilder();

    private static PercentileChartBuilder percentileChartBuilder = new PercentileChartBuilder();

    LatencyPanel(String hlogFileName, SLAProperties slaProperties) {
        ZoomProperty zoomProperty = new ZoomProperty();

        DBConnect db = new DBConnect(((Long) System.currentTimeMillis()).toString());
        Parser pr = new Parser(db, hlogFileName, null, null);
        try {
            pr.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Set<String> tags = pr.getTags();

        setLayout(new GridLayout(2, 1));
        setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JPanel timelineChart = timelineChartBuilder.createTimelineChart(tags, db, zoomProperty);
        JPanel percentileChart = percentileChartBuilder.createPercentileChart(tags, db, slaProperties, zoomProperty, hlogFileName);

        add(timelineChart);
        add(percentileChart);
    }
}
