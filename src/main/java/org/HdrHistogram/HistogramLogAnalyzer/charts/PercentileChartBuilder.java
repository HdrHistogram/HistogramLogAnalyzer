/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.charts;

import org.HdrHistogram.HistogramLogAnalyzer.applicationlayer.ConstantsHelper;
import org.HdrHistogram.HistogramLogAnalyzer.applicationlayer.LatencyChartType;
import org.HdrHistogram.HistogramLogAnalyzer.applicationlayer.SLAProperties;
import org.HdrHistogram.HistogramLogAnalyzer.applicationlayer.ZoomProperty;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.Parser;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.DBConnect;
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
import java.sql.ResultSet;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.*;
import java.util.List;

public class PercentileChartBuilder {

    private Double percentile_maxvalue   = 0.0;
    private Double percentile_max_cvalue = 0.0;

    public JPanel createPercentileChart(final Set<String> tags, DBConnect db, final SLAProperties slaProperties,
                                        final ZoomProperty zoomProperty, final String hlogFileName)
    {
        JFreeChart drawable = createPercentileDrawable(tags, db, slaProperties);

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

                DBConnect newDb = new DBConnect(((Long) System.currentTimeMillis()).toString());
                Parser pr = new Parser(newDb, hlogFileName, v.getLowerBoundString(), v.getUpperBoundString());
                try {
                    pr.execute();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                JFreeChart drawable = createPercentileDrawable(tags, newDb, slaProperties);
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
                if (evt.getPropertyName().equals("slaReset")) {
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
                            slaSeries.add(slaEntry.getPercentileCount(), slaEntry.getLatency());
                            if (i + 1 < count) {
                                SLAProperties.SLAEntry nextSLAEntry = slaEntries.get(i + 1);
                                slaSeries.add(slaEntry.getPercentileCount(), nextSLAEntry.getLatency());
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

    private JFreeChart createPercentileDrawable(Set<String> tags, DBConnect db, SLAProperties slaProperties) {

        String chartTitle = ConstantsHelper.getChartTitle(LatencyChartType.PERCENTILE);
        String xAxisLabel = ConstantsHelper.getXAxisLabel(LatencyChartType.PERCENTILE);
        String yAxisLabel = ConstantsHelper.getYAxisLabel(LatencyChartType.PERCENTILE);
        String logAxis    = ConstantsHelper.getLogAxisLabel(LatencyChartType.PERCENTILE);

        XYSeriesCollection sc = new XYSeriesCollection();
        for (String tag : tags) {
            sc.addSeries(createXYSeries(tag, db));
        }
        sc.addSeries(createSLAXYSeries(slaProperties));

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
        plot.getDomainAxis(0).setUpperBound(percentile_max_cvalue + (percentile_max_cvalue * 0.4));
        plot.getRangeAxis(0).setRange(0.0, percentile_maxvalue + 10);
        plot.getRangeAxis().setAutoRange(false);

        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        for (int i = 0; i < plot.getSeriesCount() - 1; i++) {
            renderer.setSeriesPaint(i, TimelineChartBuilder.Colors.getColor(i));
            renderer.setSeriesShapesVisible(i, false);
            renderer.setSeriesLinesVisible(i, true);
        }
        int slaIndex = plot.getSeriesCount() - 1;
        renderer.setSeriesVisible(slaIndex, slaProperties.isSLAVisible());
        renderer.setSeriesPaint(slaIndex, TimelineChartBuilder.Colors.getSLAColor());
        renderer.setSeriesShapesVisible(slaIndex, false);
        renderer.setSeriesStroke(slaIndex, new BasicStroke(2.0f));

        plot.setDomainGridlinesVisible(false);
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());

        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setRenderer(renderer);

        return drawable;
    }

    private XYSeries createSLAXYSeries(SLAProperties slaProperties) {
        XYSeries series = new XYSeries("SLA");

        List<SLAProperties.SLAEntry> slaEntries = slaProperties.getSLAEntries();
        slaEntries.add(0, new SLAProperties.SLAEntry(0.0, 0.0));
        int count = slaEntries.size();
        for (int i = 0; i < count; i++) {
            SLAProperties.SLAEntry slaEntry = slaEntries.get(i);
            series.add(slaEntry.getPercentileCount(), slaEntry.getLatency());
            if (i + 1 < count) {
                SLAProperties.SLAEntry nextSLAEntry = slaEntries.get(i + 1);
                series.add(slaEntry.getPercentileCount(), nextSLAEntry.getLatency());
            }
        }
        slaEntries.remove(0);
        return series;
    }

    private XYSeries createXYSeries(String tag, DBConnect db) {

        String defaultTagKey = ConstantsHelper.getLatencyName() + " by Percentile";
        XYSeries series = new XYSeries(tag == null ? defaultTagKey : tag);

        try {
            String cmd = "select max( cast(value as float) ) as max_value, max(cast(cvalue as float) ) as max_cvalue from j_hst"
                            + (tag != null ? " where hst_tag='"+ tag + "'": "") + ";";

            ResultSet rs = db.statement.executeQuery(cmd);
            while (rs.next()) {
                percentile_maxvalue = rs.getDouble("max_value");
                percentile_max_cvalue = rs.getDouble("max_cvalue");
            }

            cmd = "select * from j_hst where value < " + percentile_maxvalue.toString()
                        + (tag != null ? " and hst_tag='"+ tag + "'": "") + ";";
            rs = db.statement.executeQuery(cmd);
            while (rs.next()) {
                double cvalue = rs.getDouble("cvalue");
                double value = rs.getDouble("value");
                series.add(cvalue, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return series;
    }
}
