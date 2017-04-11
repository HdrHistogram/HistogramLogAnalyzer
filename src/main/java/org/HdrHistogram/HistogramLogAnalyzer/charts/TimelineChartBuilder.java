/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.charts;

import org.HdrHistogram.HistogramLogAnalyzer.applicationlayer.*;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.HistogramModel;
import org.HdrHistogram.HistogramLogAnalyzer.properties.*;

import org.jfree.chart.*;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.LegendItemEntity;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;

import org.jfree.data.Range;
import org.jfree.data.xy.*;
import org.jfree.ui.RectangleEdge;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class TimelineChartBuilder {

    public JPanel createTimelineChart(final List<HistogramModel> models, final AppProperties appProperties,
                                      final ZoomProperties zoomProperty, final ScaleProperties scaleProperties,
                                      final HLAChartType chartType)
    {
        JFreeChart drawable = createTimelineDrawable(models, zoomProperty, appProperties, chartType);

        final DateProperties dateProperties = appProperties.getDateProperties();

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

                    XYDataset dataset = plot.getDataset();
                    for (int i = 0; i < dataset.getSeriesCount(); i++) {
                        String key = (String)dataset.getSeriesKey(i);
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

        // enabling/disabling MWP checkbox changes visibility of this MWP lines on chart
        appProperties.getMwpProperties().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("mwpShow")) {
                    Boolean b = (Boolean) evt.getNewValue();
                    XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
                    XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();

                    XYDataset dataset = plot.getDataset();
                    for (int i = 0; i < dataset.getSeriesCount(); i++) {
                        String key = (String)dataset.getSeriesKey(i);
                        if (key.contains("%'ile")) {
                            renderer.setSeriesVisible(i, b);
                        }
                    }
                }
            }
        });

        // toggling HPL menu item changes visibility of HPL lines on chart
        appProperties.getHplProperties().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("hplShow")) {
                    Boolean b = (Boolean) evt.getNewValue();
                    XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
                    XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();

                    XYDataset dataset = plot.getDataset();
                    for (int i = 0; i < dataset.getSeriesCount(); i++) {
                        String key = (String)dataset.getSeriesKey(i);
                        if (key.endsWith("%") || key.equals("Max")) {
                            renderer.setSeriesVisible(i, b);
                        }
                    }
                }
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

        zoomProperty.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                ZoomProperties.ZoomValue v = (ZoomProperties.ZoomValue) evt.getNewValue();
                // ignore notifications from same chart
                if (v.getChartType() == chartType) {
                    return;
                }

                XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
                if (v.getAxisType() == ZoomProperties.AxisType.RANGE) {
                    plot.getRangeAxis().setRange(v.getRange());
                    return;
                }

                Range newDomainRange;
                // match date chart with first model
                double startTime = models.get(0).getStartTimeSec();
                if (v.getChartType() == HLAChartType.TIMELINE_ELAPSED_TIME) {
                    newDomainRange = DateProperties.rangeToDateRange(startTime, v.getRange());
                } else {
                    newDomainRange = DateProperties.dateRangeToRange(startTime, v.getRange());
                }
                plot.getDomainAxis().setRange(newDomainRange);
            }
        });

        if (chartType == HLAChartType.TIMELINE_DATE) {
            dateProperties.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getPropertyName().equals("setShowDate") ||
                        evt.getPropertyName().equals("setUserTimezone") ||
                        evt.getPropertyName().equals("setShowSeconds") ||
                        evt.getPropertyName().equals("setShowMilliseconds"))
                    {
                        XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
                        DateAxis axis = (DateAxis) plot.getDomainAxis();
                        axis.setDateFormatOverride(dateProperties.getDateFormat(models));

                        TimeZone timezone = appProperties.getDateProperties().getTimeZone(models);
                        Date startTime = models.get(0).getStartTime();
                        String xAxisLabel = ConstantsHelper.getXAxisLabel(chartType,
                                DateProperties.getShortTimezoneString(timezone, startTime));
                        chartPanel.getChart().getXYPlot().getDomainAxis().setLabel(xAxisLabel);
                    }
                }
            });
        }

        // FIXME: uninstall listeners

        return chartPanel;
    }

    /*
     * defaults for TimeSeries and XYLine charts are slightly different
     * use these tweaks to align these charts so that switching
     * between these charts doesn't change plots.
     */
    private void adjustDrawable(JFreeChart drawable, HLAChartType chartType) {
        if (chartType == HLAChartType.TIMELINE_DATE) {
            ((NumberAxis)drawable.getXYPlot().getRangeAxis()).setAutoRangeIncludesZero(true);
        } else {
            ((NumberAxis)drawable.getXYPlot().getDomainAxis()).setAutoRangeStickyZero(false);
        }

        final XYPlot plot = drawable.getXYPlot();
        ValueAxis v = plot.getDomainAxis();
        v.setLowerMargin(0.05D);
        v.setUpperMargin(0.05D);
    }

    private JFreeChart createTimelineDrawable(final List<HistogramModel> models,
                                              final ZoomProperties zoomProperty,
                                              final AppProperties appProperties, final HLAChartType chartType)
    {
        TimeZone timeZone = appProperties.getDateProperties().getTimeZone(models);

        String chartTitle = ConstantsHelper.getChartTitle(chartType);
        Date startTime = models.get(0).getStartTime();
        String xAxisLabel = ConstantsHelper.getXAxisLabel(chartType, DateProperties.getShortTimezoneString(timeZone, startTime));
        String yAxisLabel = ConstantsHelper.getYAxisLabel(chartType);

        CommonSeriesCollection seriesCollection = new TimelineDatasetBuilder().build(models, chartType);
        XYDataset dataset = seriesCollection.getDataset();

        JFreeChart drawable;
        if (chartType == HLAChartType.TIMELINE_DATE) {
            drawable = ChartFactory.createTimeSeriesChart(chartTitle,
                    xAxisLabel,
                    yAxisLabel,
                    dataset,
                    true,
                    true,
                    false);
        } else {
            drawable = ChartFactory.createXYLineChart(chartTitle,
                    xAxisLabel,
                    yAxisLabel,
                    dataset,
                    PlotOrientation.VERTICAL,
                    true,
                    true,
                    false);
        }
        adjustDrawable(drawable, chartType);

        final XYPlot plot = drawable.getXYPlot();
        drawable.getPlot().setBackgroundPaint(Color.white);
        drawable.getXYPlot().setRangeGridlinePaint(Color.gray);
        drawable.getXYPlot().setDomainGridlinePaint(Color.gray);

        // zooming on timeline chart updates percentile chart (fire part)
        // also updates another timeline chart
        plot.getDomainAxis().addChangeListener(new AxisChangeListener() {
            @Override
            public void axisChanged(AxisChangeEvent axisChangeEvent) {
                if (appProperties.getDateProperties().isChartActive(chartType)) {
                    Range range = plot.getDomainAxis().getRange();
                    zoomProperty.zoom(new ZoomProperties.ZoomValue(ZoomProperties.AxisType.DOMAIN, range, chartType));
                }
            }
        });
        plot.getRangeAxis().addChangeListener(new AxisChangeListener() {
            @Override
            public void axisChanged(AxisChangeEvent axisChangeEvent) {
                if (appProperties.getDateProperties().isChartActive(chartType)) {
                    Range range = plot.getRangeAxis().getRange();
                    zoomProperty.zoom(new ZoomProperties.ZoomValue(ZoomProperties.AxisType.RANGE, range, chartType));
                }
            }
        });

        if (chartType == HLAChartType.TIMELINE_DATE) {
            DateAxis axis = (DateAxis) plot.getDomainAxis();
            DateFormat df = appProperties.getDateProperties().getDateFormat(models);
            axis.setDateFormatOverride(df);
            axis.setTimeZone(df.getTimeZone());
        }

        // MWP/HPL visibility, line settings
        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            renderer.setSeriesShapesVisible(i, false);
            renderer.setSeriesLinesVisible(i, true);

            String key = (String)dataset.getSeriesKey(i);
            if (key.contains("%'ile")) {
                renderer.setSeriesVisible(i, appProperties.getMwpProperties().isMWPVisible());
                renderer.setSeriesPaint(i, ColorHelper.getColor(i));
            } else if (key.endsWith("%") || key.equals("Max")) {
                renderer.setSeriesVisible(i, appProperties.getHplProperties().isHPLVisible());
                renderer.setSeriesPaint(i, ColorHelper.getHPLColor(key));
                renderer.setSeriesStroke(i, new BasicStroke(2.0f));
            } else {
                renderer.setSeriesPaint(i, ColorHelper.getColor(i));
            }
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
}
