/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.charts;

import org.HdrHistogram.HistogramLogAnalyzer.applicationlayer.*;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.HistogramModel;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.TimelineIterator;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.TimelineObject;
import org.jfree.chart.*;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.LegendItemEntity;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;

import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TimelineChartBuilder {

    public JPanel createTimelineChart(final List<HistogramModel> models, final ZoomProperty zoomProperty,
                                      final ScaleProperties scaleProperties)
    {
        JFreeChart drawable = createTimelineDrawable(models);

        final ChartPanel chartPanel = new ChartPanel(drawable, true);
        chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));
        chartPanel.setBackground(Color.gray);
        chartPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(4, 4, 4, 4),
                BorderFactory.createLineBorder(Color.black)));

        ToolTipManager.sharedInstance().registerComponent(chartPanel);
        chartPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                ToolTipManager ttm = ToolTipManager.sharedInstance();
                ttm.setDismissDelay(10000);
                ttm.setReshowDelay(0);
                ttm.setInitialDelay(0);
            }
        });

        // clicking on legend item for a tag changes visibility of this tag on chart
        chartPanel.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent chartMouseEvent) {
                ChartEntity chartEntity = chartMouseEvent.getEntity();
                if (chartEntity instanceof LegendItemEntity) {
                    LegendItemEntity legendItemEntity = (LegendItemEntity)chartEntity;
                    XYPlot plot = (XYPlot) chartMouseEvent.getChart().getPlot();
                    XYLineAndShapeRenderer renderer =
                            (XYLineAndShapeRenderer) plot.getRenderer();

                    XYSeriesCollection dataset = (XYSeriesCollection) plot.getDataset();
                    for (int i = 0; i < dataset.getSeriesCount(); i++) {
                        XYSeries series = dataset.getSeries(i);
                        String key = (String) series.getKey();
                        Boolean flag = renderer.getSeriesLinesVisible(i);
                        if (key.equals(legendItemEntity.getSeriesKey())) {
                            renderer.setSeriesLinesVisible(i, !flag);
                        }
                    }
                }
            }
            @Override
            public void chartMouseMoved(ChartMouseEvent chartMouseEvent) {
            }
        });

        // tool doesn't support MWP for charts with multiple files
        // tool doesn't support MWP for files with multiple tags
        boolean multipleFiles = models.size() > 1;
        if (!multipleFiles) {
            final HistogramModel model = models.get(0);
            boolean multipleTags = model.getTags().size() > 1;
            if (!multipleTags) {
                model.getMwpProperties().addPropertyChangeListener(new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (evt.getPropertyName().equals("applyMWP")) {
                            List<HistogramModel> newModels = new ArrayList<>();
                            String inputFileName = model.getInputFileName();
                            MWPProperties mwpProperties = model.getMwpProperties();
                            try {
                                newModels.add(new HistogramModel(inputFileName, null, null, mwpProperties));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            JFreeChart drawable = createTimelineDrawable(newModels);
                            chartPanel.setChart(drawable);
                        }
                        }
                });
            }
        }

        // zooming on timeline chart updates percentile chart (fire part)
        final XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
        plot.getDomainAxis().addChangeListener(new AxisChangeListener() {
            @Override
            public void axisChanged(AxisChangeEvent axisChangeEvent) {
                String lowerBoundString = String.valueOf(plot.getDomainAxis().getLowerBound());
                String upperBoundString = String.valueOf(plot.getDomainAxis().getUpperBound());
                zoomProperty.zoom(new ZoomProperty.ZoomValue(lowerBoundString, upperBoundString));
            }
        });

        scaleProperties.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("applyScale")) {
                    ScaleProperties.ScaleEntry se = (ScaleProperties.ScaleEntry) evt.getNewValue();
                    XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
                    plot.getRangeAxis(0).setRange(0.0, se.getMaxYValue() + se.getMaxYValue() * 0.1);
                }
            }
        });

        // FIXME: uninstall listeners

        return chartPanel;
    }

    private JFreeChart createTimelineDrawable(List<HistogramModel> models)
    {
        String chartTitle = ConstantsHelper.getChartTitle(LatencyChartType.TIMELINE);
        String xAxisLabel = ConstantsHelper.getXAxisLabel(LatencyChartType.TIMELINE);
        String yAxisLabel = ConstantsHelper.getYAxisLabel(LatencyChartType.TIMELINE);
        XYSeriesCollection seriesCollection = createXYSeriesCollection(models);

        JFreeChart drawable = ChartFactory.createXYLineChart(chartTitle,
                xAxisLabel,
                yAxisLabel,
                seriesCollection,
                PlotOrientation.VERTICAL,
                true,
                true,
                false);

        drawable.getPlot().setBackgroundPaint(Color.white);
        drawable.getXYPlot().setRangeGridlinePaint(Color.gray);
        drawable.getXYPlot().setDomainGridlinePaint(Color.gray);

        XYPlot plot = (XYPlot) drawable.getPlot();
        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        for (int i = 0; i < plot.getSeriesCount(); i++) {
            renderer.setSeriesPaint(i, ColorHelper.getColor(i));
            renderer.setSeriesShapesVisible(i, false);
            renderer.setSeriesLinesVisible(i, true);
        }

        LegendTitle legend = drawable.getLegend();
        legend.setPosition(RectangleEdge.TOP);
        plot.setDomainGridlinesVisible(false);
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setRenderer(renderer);

        return drawable;
    }

    private static final String DEFAULT_KEY = "Max per interval";

    private XYSeriesCollection createXYSeriesCollection(List<HistogramModel> histogramModels) {
        XYSeriesCollection ret = new XYSeriesCollection();

        // tool doesn't support MWP for charts with multiple files
        // tool doesn't support MWP for files with multiple tags
        boolean multipleFiles = histogramModels.size() > 1;
        if (!multipleFiles) {
            HistogramModel histogramModel = histogramModels.get(0);
            Set<String> tags = histogramModel.getTags();

            boolean multipleTags = tags.size() > 1;
            if (!multipleTags) {
                MWPProperties mwpProperties = histogramModel.getMwpProperties();
                List<MWPProperties.MWPEntry> mwpEntries = mwpProperties.getMWPEntries();
                for (MWPProperties.MWPEntry mwpEntry : mwpEntries) {
                    String key;
                    if (mwpEntry.isDefaultEntry()) {
                        key = DEFAULT_KEY;
                    } else {
                        key = mwpEntry.toString();
                    }
                    XYSeries series = new XYSeries(key);

                    TimelineIterator ti = histogramModel.listTimelineObjects(false, null, mwpEntry);
                    while (ti.hasNext()) {
                        TimelineObject to = ti.next();
                        series.add(to.getTimelineAxisValue(), to.getLatencyAxisValue());
                    }
                    ret.addSeries(series);
                }
                return ret;
            }
        }

        for (HistogramModel histogramModel : histogramModels) {
            Set<String> tags = histogramModel.getTags();
            for (String tag : tags) {
                String key;
                if (multipleFiles) {
                    key = histogramModel.getShortFileName();
                } else {
                    key = tag == null ? DEFAULT_KEY : tag;
                }
                XYSeries series = new XYSeries(key);

                TimelineIterator ti = histogramModel.listTimelineObjects(true, tag, null);
                while (ti.hasNext()) {
                    TimelineObject to = ti.next();
                    series.add(to.getTimelineAxisValue(), to.getLatencyAxisValue());
                }
                ret.addSeries(series);
            }
        }

        return ret;
    }
}
