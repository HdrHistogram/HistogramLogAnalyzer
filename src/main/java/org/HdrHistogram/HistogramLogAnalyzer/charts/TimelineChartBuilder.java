/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.charts;

import org.HdrHistogram.HistogramLogAnalyzer.applicationlayer.*;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.DBConnect;
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
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.List;

public class TimelineChartBuilder {

    public JPanel createTimelineChart(Set<String> tags, DBConnect db, final ZoomProperty zoomProperty) {
        JFreeChart drawable = createTimelineDrawable(tags, db);

        ChartPanel chartPanel = new ChartPanel(drawable, true);
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

        // FIXME: uninstall listeners

        return chartPanel;
    }

    static class Colors {

        private static List<Color> colors = new ArrayList<Color>();
        static {
            colors.add(Color.BLUE);
            colors.add(Color.GREEN);
            colors.add(Color.RED);
        }

        static Color getColor(int i) {
            Color c = colors.get(i);
            if (c == null) {
                Random r = new Random();
                c = new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255));
                colors.add(c);
            }
            return c;
        }

        static Color getSLAColor() {
            return Color.YELLOW;
        }
    }

    private JFreeChart createTimelineDrawable(Set<String> tags, DBConnect db) {
        String chartTitle = ConstantsHelper.getChartTitle(LatencyChartType.TIMELINE);
        String xAxisLabel = ConstantsHelper.getXAxisLabel(LatencyChartType.TIMELINE);
        String yAxisLabel = ConstantsHelper.getYAxisLabel(LatencyChartType.TIMELINE);

        XYSeriesCollection sc = new XYSeriesCollection();
        for (String tag : tags) {
            sc.addSeries(createXYSeries(tag, db));
        }

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
        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        for (int i = 0; i < plot.getSeriesCount(); i++) {
            renderer.setSeriesPaint(i, Colors.getColor(i));
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

    private XYSeries createXYSeries(String tag, DBConnect db) {
        String defaultTagKey = "Max per interval";
        XYSeries series1 = new XYSeries(tag == null ? defaultTagKey : tag);
        try {
            String cmd = "select percentile_elapsedTime,percentile_ip_max from j_percentile"
                       + (tag != null ? " where percentile_tag='"+ tag + "'": "") + ";";

            ResultSet rs = db.statement.executeQuery(cmd);
            while (rs.next()) {
                double percentileElapsedTime = rs.getDouble("percentile_elapsedTime");
                double percentileIntervalMax = rs.getDouble("percentile_ip_max");
                series1.add(percentileElapsedTime, percentileIntervalMax);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return series1;
    }
}