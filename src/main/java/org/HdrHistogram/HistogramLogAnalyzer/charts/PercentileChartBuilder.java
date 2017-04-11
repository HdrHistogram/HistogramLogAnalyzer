/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.charts;

import org.HdrHistogram.HistogramLogAnalyzer.applicationlayer.*;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.HistogramModel;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.MaxPercentileIterator;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.PercentileIterator;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.PercentileObject;
import org.HdrHistogram.HistogramLogAnalyzer.properties.*;
import org.jfree.chart.*;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.LegendItemEntity;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.List;

public class PercentileChartBuilder {

    private Double maxLatencyAxisValue = 0.0;
    private Double maxPercentileAxisValue = 0.0;

    public JPanel createPercentileChart(final List<HistogramModel> histogramModels, final AppProperties appProperties,
                                        final ScaleProperties scaleProperties)
    {
        JFreeChart drawable = createPercentileDrawable(histogramModels, appProperties);

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

        // enabling/disabling SLA checkbox changes visibility of this SLA line on chart
        appProperties.getSlaProperties().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("slaShow")) {
                    Boolean b = (Boolean) evt.getNewValue();
                    XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
                    XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();

                    XYSeriesCollection dataset = (XYSeriesCollection) plot.getDataset();
                    for (int i = 0; i < dataset.getSeriesCount(); i++) {
                        XYSeries series = dataset.getSeries(i);
                        String key = (String) series.getKey();
                        if ("SLA".equals(key)) {
                            if (b) {
                                renderer.setSeriesVisible(i, true);
                            } else {
                                renderer.setSeriesVisible(i, false);
                            }
                        }
                    }
                }
            }
        });

        // reseting SLA values updates percentile chart
        appProperties.getSlaProperties().addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("applySLA")) {
                    XYPlot plot = (XYPlot) chartPanel.getChart().getPlot();
                    XYSeries slaSeries = null;
                    // get SLA series
                    XYSeriesCollection dataset = (XYSeriesCollection) plot.getDataset();
                    for (int i = 0; i < dataset.getSeriesCount(); i++) {
                        XYSeries series = dataset.getSeries(i);
                        String key = (String) series.getKey();
                        if ("SLA".equals(key)) {
                            slaSeries = series;
                        }
                    }
                    // update SLA series
                    if (slaSeries != null) {
                        slaSeries.clear();
                        List<SLAProperties.SLAEntry> slaEntries = appProperties.getSlaProperties().getSLAEntries();
                        slaEntries.add(0, new SLAProperties.SLAEntry(0.0, 0.0));
                        int count = slaEntries.size();
                        for (int i = 0; i < count; i++) {
                            SLAProperties.SLAEntry slaEntry = slaEntries.get(i);
                            slaSeries.add(slaEntry.getPercentileAxis(), slaEntry.getLatency());
                            if (i + 1 < count) {
                                SLAProperties.SLAEntry nextSLAEntry = slaEntries.get(i + 1);
                                slaSeries.add(slaEntry.getPercentileAxis(), nextSLAEntry.getLatency());
                            }
                        }
                        slaEntries.remove(0);
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

                    XYSeriesCollection dataset = (XYSeriesCollection) plot.getDataset();
                    for (int i = 0; i < dataset.getSeriesCount(); i++) {
                        XYSeries series = dataset.getSeries(i);
                        String key = (String) series.getKey();
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

        // FIXME: uninstall listeners

        return chartPanel;
    }

    private JFreeChart createPercentileDrawable(List<HistogramModel> histogramModels, AppProperties appProperties) {
        String chartTitle = ConstantsHelper.getChartTitle(HLAChartType.PERCENTILE);
        String xAxisLabel = ConstantsHelper.getXAxisLabel(HLAChartType.PERCENTILE);
        String yAxisLabel = ConstantsHelper.getYAxisLabel(HLAChartType.PERCENTILE);
        String logAxis    = ConstantsHelper.getLogAxisLabel(HLAChartType.PERCENTILE);
        XYSeriesCollection sc = createXYSeriesCollection(histogramModels, appProperties.getSlaProperties());

        JFreeChart drawable = ChartFactory.createXYLineChart(chartTitle,
            xAxisLabel,
            yAxisLabel,
            sc,
            PlotOrientation.VERTICAL,
            true,
            true,
            false);

        drawable.getPlot().setBackgroundPaint(Color.white);
        drawable.getXYPlot().setRangeGridlinePaint(Color.gray);
        drawable.getXYPlot().setDomainGridlinePaint(Color.gray);

        XYPlot plot = (XYPlot) drawable.getPlot();

        LogAxis ll = new LogAxis(logAxis);
        ll.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        ll.setMinorTickMarksVisible(false);
        ll.setBase(10);

        ll.setNumberFormatOverride(new NumberFormat() {
			private static final long serialVersionUID = 6737184345504901251L;

			@Override
			public Number parse(String source, ParsePosition parsePosition) {
				return null;
			}

			@Override
			public StringBuffer format(long number, StringBuffer toAppendTo,
					FieldPosition pos) {
				return toAppendTo;
    		}

			@Override
    		public StringBuffer format(double number, StringBuffer toAppendTo,
					FieldPosition pos) {
				if (number == 1) {
					return new StringBuffer("0.0%");
				}
    			if (number == 10) {
					return new StringBuffer("90.0%");
				}
				if (number == 100) {
					return new StringBuffer("99.0%");
				}
				if (number == 1000) {
					return new StringBuffer("99.9%");
				}
				if (number == 10000) {
					return new StringBuffer("99.99%");
				}
				if (number == 100000) {
					return new StringBuffer("99.999%");
				}
				return toAppendTo;
			}
		});

        ll.setLabelFont(plot.getDomainAxis().getLabelFont());
        plot.setDomainAxis(0, ll);
        plot.getDomainAxis(0).setUpperBound(maxPercentileAxisValue + (maxPercentileAxisValue * 0.4));

        plot.getRangeAxis(0).setRange(0.0, maxLatencyAxisValue + maxLatencyAxisValue * 0.1);
        plot.getRangeAxis().setAutoRange(false);

        // SLA/HPL visibility, line settings etc
        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        XYSeriesCollection dataset = (XYSeriesCollection) plot.getDataset();
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            renderer.setSeriesShapesVisible(i, false);
            renderer.setSeriesLinesVisible(i, true);

            XYSeries series = dataset.getSeries(i);
            String key = (String) series.getKey();
            if (key.equals("SLA")) {
                renderer.setSeriesVisible(i, appProperties.getSlaProperties().isSLAVisible());
                renderer.setSeriesPaint(i, ColorHelper.getSLAColor());
                renderer.setSeriesShapesVisible(i, false);
                renderer.setSeriesStroke(i, new BasicStroke(2.0f));
            } else if (key.endsWith("%") || key.equals("Max")) {
                renderer.setSeriesVisible(i, appProperties.getHplProperties().isHPLVisible());
                renderer.setSeriesPaint(i, ColorHelper.getHPLColor(key));
                renderer.setSeriesStroke(i, new BasicStroke(2.0f));
            } else {
                renderer.setSeriesPaint(i, ColorHelper.getColor(i));
            }
        }

        plot.setDomainGridlinesVisible(false);
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());

        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setRenderer(renderer);

        return drawable;
    }

    private XYSeriesCollection createXYSeriesCollection(List<HistogramModel> histogramModels, SLAProperties slaProperties) {
        XYSeriesCollection ret = new XYSeriesCollection();

        maxLatencyAxisValue = 0.0;
        maxPercentileAxisValue = 0.0;
        boolean multipleFiles = histogramModels.size() > 1;
        boolean multipleTagsEncountered = false;
        String defaultTagKey = ConstantsHelper.getLatencyName() + " by Percentile";
        for (HistogramModel histogramModel : histogramModels) {
            boolean multipleTags = histogramModel.getTags().size() > 1;
            if (multipleTags) {
                multipleTagsEncountered = true;
            }
            for (String tag : histogramModel.getTags()) {
                String key;
                if (multipleFiles) {
                    key = histogramModel.getShortFileName();
                } else {
                    if (multipleTags) {
                        key = tag == null ? "No Tag" : tag;
                    } else {
                        key = defaultTagKey;
                    }
                }
                XYSeries series = new XYSeries(key);

                MaxPercentileIterator mpi = histogramModel.listMaxPercentileObjects(tag);
                PercentileObject mpo = null;
                while (mpi.hasNext()) {
                    mpo = mpi.next();
                    maxLatencyAxisValue = Math.max(maxLatencyAxisValue, mpo.getLatencyAxisValue());
                    maxPercentileAxisValue = Math.max(maxPercentileAxisValue, mpo.getPercentileAxisValue());
                }

                PercentileIterator pi = histogramModel.listPercentileObjects(tag, mpo);
                while (pi.hasNext()) {
                    PercentileObject to = pi.next();
                    series.add(to.getPercentileAxisValue(), to.getLatencyAxisValue());
                }

                ret.addSeries(series);
            }
        }

        // tool doesn't support SLA/HPL for charts with multiple files
        // tool doesn't support SLA/HPL for files with multiple tags
        if (!multipleFiles && !multipleTagsEncountered) {
            XYSeries series = new XYSeries("Max");
            series.add(ret.getDomainLowerBound(false), maxLatencyAxisValue);
            series.add(ret.getDomainUpperBound(false), maxLatencyAxisValue);
            ret.addSeries(series);
        }

        XYSeries series = new XYSeries("SLA");
        List<SLAProperties.SLAEntry> slaEntries = slaProperties.getSLAEntries();
        slaEntries.add(0, new SLAProperties.SLAEntry(0.0, 0.0));
        int count = slaEntries.size();
        for (int i = 0; i < count; i++) {
            SLAProperties.SLAEntry slaEntry = slaEntries.get(i);
            // limit SLA line
            Double percentileAxis = Math.min(slaEntry.getPercentileAxis(), ret.getDomainUpperBound(false));
            series.add(percentileAxis, slaEntry.getLatency());
            if (i + 1 < count) {
                SLAProperties.SLAEntry nextSLAEntry = slaEntries.get(i + 1);
                series.add(slaEntry.getPercentileAxis(), nextSLAEntry.getLatency());
            }
        }
        slaEntries.remove(0);
        ret.addSeries(series);

        return ret;
    }
}
