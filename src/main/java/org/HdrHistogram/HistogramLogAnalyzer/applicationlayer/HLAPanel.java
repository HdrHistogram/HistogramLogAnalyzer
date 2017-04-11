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
import org.HdrHistogram.HistogramLogAnalyzer.properties.*;
import org.jfree.chart.ChartPanel;
import org.jfree.data.Range;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
 * Custom JPanel that contains top (timeline) and bottom (percentile) charts
 */
class HLAPanel extends JPanel
{
    private static TimelineChartBuilder timelineChartBuilder = new TimelineChartBuilder();
    private static PercentileChartBuilder percentileChartBuilder = new PercentileChartBuilder();
    private static BucketsChartBuilder bucketsChartBuilder = new BucketsChartBuilder();

    // top "timeline" charts
    private Container topChart = null;
    private JPanel elapsedTimeChart = null;
    private JPanel dateChart = null;

    private Container bottomChart = null;
    private JPanel percentileChart = null;
    private JPanel bucketsChart = null;

    private List< HistogramModel> histogramModels = new ArrayList<>();
    private ScaleProperties scaleProperties = null;
    private ScaleProperties.ScaleEntry scaleEntry = null;

    private String tabTitle;
    private String[] tooltipTexts;

    HLAPanel(String inputFileName, AppProperties appProperties)
        throws IOException
    {
        this(new String[] {inputFileName}, appProperties);
    }

    HLAPanel(String[] inputFileNames, final AppProperties appProperties) throws IOException {
        final ZoomProperties zoomProperty = new ZoomProperties();
        scaleProperties = new ScaleProperties();

        histogramModels = new ArrayList<>();
        for (String inputFileName : inputFileNames) {
            HistogramModel histogramModel =
                    new HistogramModel(inputFileName, null, null, appProperties.getMwpProperties());
            // print statistics to console when requested
            if (Configuration.getInstance().isPPmode()) {
                PrintStatistics.print(histogramModel);
            }
            histogramModels.add(histogramModel);
        }

        initUISettings(histogramModels);

        setLayout(new GridLayout(2, 1));
        setBorder(BorderFactory.createLineBorder(Color.BLACK));

        topChart = new Container();
        bottomChart = new Container();
        topChart.setLayout(new GridLayout(1, 1));
        bottomChart.setLayout(new GridLayout(1, 1));

        add(topChart);
        add(bottomChart);

        createTopChart(appProperties, zoomProperty);
        createBottomChart(histogramModels, appProperties);

        updateTopChart(appProperties.getDateProperties());
        updateBottomChart(appProperties.getViewProperties());

        appProperties.getViewProperties().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("bottomChartTypeChanged")) {
                    updateBottomChart(appProperties.getViewProperties());
                }
            }
        });

        appProperties.getDateProperties().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("setDatesVisible")) {
                    updateTopChart(appProperties.getDateProperties());
                }
            }
        });

        // re-create top charts when MWP enabled
        //
        // tool doesn't support MWP for charts with multiple files
        // tool doesn't support MWP for files with multiple tags
        boolean multipleFiles = histogramModels.size() > 1;
        if (!multipleFiles) {
            final HistogramModel model = histogramModels.get(0);
            boolean multipleTags = model.getTags().size() > 1;
            if (!multipleTags) {
                appProperties.getMwpProperties().addPropertyChangeListener(new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (evt.getPropertyName().equals("applyMWP")) {

                            histogramModels.clear();
                            String inputFileName = model.getInputFileName();
                            MWPProperties mwpProperties = model.getMwpProperties();
                            try {
                                histogramModels.add(new HistogramModel(inputFileName, null, null, mwpProperties));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    createTopChart(appProperties, zoomProperty);
                                    updateTopChart(appProperties.getDateProperties());
                                }
                            });
                        }
                    }
                });
            }
        }

        // zooming on timeline chart updates percentile chart (listener part)
        zoomProperty.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                ZoomProperties.ZoomValue v = (ZoomProperties.ZoomValue) evt.getNewValue();
                // ignore range changes in vertical axis
                if (v.getAxisType() == ZoomProperties.AxisType.RANGE) {
                    return;
                }

                final List<HistogramModel> percetileHistModels = new ArrayList<>();
                for (HistogramModel model : histogramModels) {
                    String inputFileName = model.getInputFileName();
                    Range range = v.getRange();
                    if (appProperties.getDateProperties().isDatesVisible()) {
                        range = DateProperties.dateRangeToRange(model.getStartTimeSec(), v.getRange());
                    }
                    try {
                        HistogramModel newModel = new HistogramModel(inputFileName,
                                range.getLowerBound(), range.getUpperBound(), appProperties.getMwpProperties());
                        percetileHistModels.add(newModel);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        createBottomChart(percetileHistModels, appProperties);
                        updateBottomChart(appProperties.getViewProperties());
                    }
                });
            }
        });

        // FIXME: uninstall listeners
    }

    public ChartPanel getTopChart() {
        return (ChartPanel) ((Container)getComponent(0)).getComponent(0);
    }

    public ChartPanel getBottomChart() {
        return (ChartPanel) ((Container)getComponent(1)).getComponent(0);
    }

    private void createTopChart(AppProperties appProperties, ZoomProperties zoomProperty) {
        elapsedTimeChart =
                timelineChartBuilder.createTimelineChart(
                        histogramModels, appProperties, zoomProperty, scaleProperties, HLAChartType.TIMELINE_ELAPSED_TIME);

        dateChart =
                timelineChartBuilder.createTimelineChart(
                        histogramModels, appProperties, zoomProperty, scaleProperties, HLAChartType.TIMELINE_DATE);
    }

    private void updateTopChart(DateProperties dateProperties) {
        topChart.removeAll();
        topChart.add(dateProperties.isDatesVisible() ? dateChart : elapsedTimeChart);
        topChart.revalidate();
    }

    // use models where "percentile"-range might be enabled
    private void createBottomChart(List<HistogramModel> percentileHistModels, AppProperties appProperties) {
        percentileChart =
                percentileChartBuilder.createPercentileChart(percentileHistModels, appProperties, scaleProperties);

        bucketsChart =
                bucketsChartBuilder.createBucketsChart(percentileHistModels, appProperties);
    }

    private void updateBottomChart(ViewProperties viewProperties) {
        bottomChart.removeAll();
        boolean usePercentileChart = viewProperties.getBottomChartType().equals(HLAChartType.PERCENTILE);
        bottomChart.add(usePercentileChart ? percentileChart : bucketsChart);
        bottomChart.revalidate();
    }


    private void initUISettings(List<HistogramModel> histogramModels) {
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
