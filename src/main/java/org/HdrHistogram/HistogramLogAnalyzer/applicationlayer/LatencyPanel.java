/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import org.HdrHistogram.HistogramLogAnalyzer.charts.BucketsChartBuilder;
import org.HdrHistogram.HistogramLogAnalyzer.charts.PercentileChartBuilder;
import org.HdrHistogram.HistogramLogAnalyzer.charts.TimelineChartBuilder;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.HistogramModel;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.LogGeneratorType;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.MaxPercentileIterator;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.PercentileObject;

import javax.swing.*;
import javax.swing.text.View;
import java.awt.GridLayout;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Custom JPanel that contains top (timeline) and bottom (percentile) charts
 */
class LatencyPanel extends JPanel
{
    private static TimelineChartBuilder timelineChartBuilder = new TimelineChartBuilder();
    private static PercentileChartBuilder percentileChartBuilder = new PercentileChartBuilder();
    private static BucketsChartBuilder bucketsChartBuilder = new BucketsChartBuilder();

    private JPanel bottomChart = null;
    private JPanel percentileChart = null;
    private JPanel bucketsChart = null;

    private List< HistogramModel> histogramModels = new ArrayList<>();
    private ScaleProperties scaleProperties = null;
    private ScaleProperties.ScaleEntry scaleEntry = null;

    private String tabTitle;
    private String[] tooltipTexts;

    LatencyPanel(String inputFileName, SLAProperties slaProperties,
                 MWPProperties mwpProperties, HPLProperties hplProperties,
                 ViewProperties viewProperties)
        throws IOException
    {
        this(new String[] {inputFileName}, slaProperties,
                mwpProperties, hplProperties, viewProperties);
    }

    LatencyPanel(String[] inputFileNames, SLAProperties slaProperties,
                 MWPProperties mwpProperties, HPLProperties hplProperties,
                 final ViewProperties viewProperties)
        throws IOException
    {
        ZoomProperty zoomProperty = new ZoomProperty();
        scaleProperties = new ScaleProperties();

        for (String inputFileName : inputFileNames) {
            HistogramModel histogramModel =
                    new HistogramModel(inputFileName, null, null, mwpProperties);
            histogramModels.add(histogramModel);
        }

        initUISettings();

        setLayout(new GridLayout(2, 1));
        setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JPanel timelineChart =
                timelineChartBuilder.createTimelineChart(histogramModels, zoomProperty,
                         mwpProperties, scaleProperties, hplProperties);

        percentileChart =
                percentileChartBuilder.createPercentileChart(histogramModels,
                        slaProperties, zoomProperty,mwpProperties, scaleProperties, hplProperties);

        bucketsChart =
                bucketsChartBuilder.createTimelineChart(histogramModels, hplProperties, zoomProperty, mwpProperties);

        add(timelineChart);
        updateBottomChart(viewProperties);

        viewProperties.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("bottomChartTypeChanged")) {
                    updateBottomChart(viewProperties);
                }
            }
        });
    }

    private void updateBottomChart(ViewProperties viewProperties) {
        if (bottomChart != null) {
            remove(bottomChart);
        }
        if (viewProperties.getBottomChartType().equals(LatencyChartType.PERCENTILE)) {
            bottomChart = percentileChart;
        } else {
            bottomChart = bucketsChart;
        }
        if (bottomChart != null) {
            add(bottomChart);
        }
    }

    private void initUISettings() {
        // use first model for setting tab title
        HistogramModel histogramModel = histogramModels.get(0);
        LogGeneratorType logGeneratorType = histogramModel.getLogGeneratorType();

        String prefix = "";
        if (!logGeneratorType.equals(LogGeneratorType.UNKNOWN)) {
            prefix = logGeneratorType.getDescription() + " - ";
        }

        tabTitle = prefix + histogramModel.getShortFileName();

        // use all models for setting (multi-line) tooltip text
        tooltipTexts = new String[histogramModels.size()];
        for (int i = 0; i < histogramModels.size(); i++) {
            histogramModel = histogramModels.get(i);
            prefix = "";
            if (!logGeneratorType.equals(LogGeneratorType.UNKNOWN)) {
                prefix = logGeneratorType.getDescription() + " - ";
            }
            tooltipTexts[i] = prefix + histogramModel.getInputFileName();
        }
    }

    String getTabTitle() {
        return tabTitle;
    }

    String[] getTooltipTexts() {
        return tooltipTexts;
    }

    /*
     * returns max values on X/Y axis for this percentile chart
     */
    ScaleProperties.ScaleEntry getScaleEntry() {
        if (scaleEntry == null) {
            double maxLatencyAxisValue = 0.0;
            double maxPercentileAxisValue = 0.0;
            for (HistogramModel histogramModel : histogramModels) {
                for (String tag : histogramModel.getTags()) {
                    MaxPercentileIterator mpi = histogramModel.listMaxPercentileObjects(tag);
                    PercentileObject mpo;
                    while (mpi.hasNext()) {
                        mpo = mpi.next();
                        maxLatencyAxisValue = Math.max(maxLatencyAxisValue, mpo.getLatencyAxisValue());
                        maxPercentileAxisValue = Math.max(maxPercentileAxisValue, mpo.getPercentileAxisValue());
                    }
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
