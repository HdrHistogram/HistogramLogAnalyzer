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

    public JPanel createPercentileChart(final HistogramModel histogramModel, final SLAProperties slaProperties,
                                        final ZoomProperty zoomProperty, final MWPProperties MWPProperties)
    {
        JFreeChart drawable = createPercentileDrawable(histogramModel, slaProperties);

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

        // zooming on timeline chart updates percentile chart (listener part)
        zoomProperty.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                ZoomProperty.ZoomValue v = (ZoomProperty.ZoomValue) evt.getNewValue();
                HistogramModel newModel = null;
                try {
                    String inputFileName = histogramModel.getInputFileName();
                    newModel = new HistogramModel(inputFileName, v.getLowerBoundString(), v.getUpperBoundString(), MWPProperties);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                JFreeChart drawable = createPercentileDrawable(newModel, slaProperties);
                chartPanel.setChart(drawable);
            }
        });

        // enabling/disabling SLA checkbox changes visibility of this SLA line on chart
        slaProperties.addPropertyChangeListener(new PropertyChangeListener() {
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
        slaProperties.addPropertyChangeListener(new PropertyChangeListener() {
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
                        List<SLAProperties.SLAEntry> slaEntries = slaProperties.getSLAEntries();
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

        // FIXME: uninstall listeners

        return chartPanel;
    }

    private JFreeChart createPercentileDrawable(HistogramModel histogramModel, SLAProperties slaProperties) {

        String chartTitle = ConstantsHelper.getChartTitle(LatencyChartType.PERCENTILE);
        String xAxisLabel = ConstantsHelper.getXAxisLabel(LatencyChartType.PERCENTILE);
        String yAxisLabel = ConstantsHelper.getYAxisLabel(LatencyChartType.PERCENTILE);
        String logAxis    = ConstantsHelper.getLogAxisLabel(LatencyChartType.PERCENTILE);
        XYSeriesCollection sc = createXYSeriesCollection(histogramModel, slaProperties);

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

			/**
			 *
			 */
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
        plot.setDomainAxis(0, ll);
        plot.getDomainAxis(0).setUpperBound(maxPercentileAxisValue + (maxPercentileAxisValue * 0.4));
        plot.getRangeAxis(0).setRange(0.0, maxLatencyAxisValue + 10);
        plot.getRangeAxis().setAutoRange(false);

        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        for (int i = 0; i < plot.getSeriesCount() - 1; i++) {
            renderer.setSeriesPaint(i, ColorHelper.getColor(i));
            renderer.setSeriesShapesVisible(i, false);
            renderer.setSeriesLinesVisible(i, true);
        }
        int slaIndex = plot.getSeriesCount() - 1;
        renderer.setSeriesVisible(slaIndex, slaProperties.isSLAVisible());
        renderer.setSeriesPaint(slaIndex, ColorHelper.getSLAColor());
        renderer.setSeriesShapesVisible(slaIndex, false);
        renderer.setSeriesStroke(slaIndex, new BasicStroke(2.0f));

        plot.setDomainGridlinesVisible(false);
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());

        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setRenderer(renderer);

        return drawable;
    }

    private XYSeriesCollection createXYSeriesCollection(HistogramModel histogramModel, SLAProperties slaProperties) {
        XYSeriesCollection ret = new XYSeriesCollection();

        for (String tag : histogramModel.getTags()) {
            String defaultTagKey = ConstantsHelper.getLatencyName() + " by Percentile";
            XYSeries series = new XYSeries(tag == null ? defaultTagKey : tag);

            MaxPercentileIterator mpi = histogramModel.listMaxPercentileObjects(tag);
            PercentileObject mpo = null;
            while (mpi.hasNext()) {
                mpo = mpi.next();
                maxLatencyAxisValue = mpo.getLatencyAxisValue();
                maxPercentileAxisValue = mpo.getPercentileAxisValue();
            }

            PercentileIterator pi = histogramModel.listPercentileObjects(tag, mpo);
            while (pi.hasNext()) {
                PercentileObject to = pi.next();
                series.add(to.getPercentileAxisValue(), to.getLatencyAxisValue());
            }

            ret.addSeries(series);
        }

        XYSeries series = new XYSeries("SLA");

        List<SLAProperties.SLAEntry> slaEntries = slaProperties.getSLAEntries();
        slaEntries.add(0, new SLAProperties.SLAEntry(0.0, 0.0));
        int count = slaEntries.size();
        for (int i = 0; i < count; i++) {
            SLAProperties.SLAEntry slaEntry = slaEntries.get(i);
            series.add(slaEntry.getPercentileAxis(), slaEntry.getLatency());
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
