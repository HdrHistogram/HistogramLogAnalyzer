/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.charts;

import org.HdrHistogram.HistogramLogAnalyzer.applicationlayer.*;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.BucketIterator;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.HistogramModel;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.MaxPercentileIterator;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.BucketObject;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.PercentileObject;
import org.HdrHistogram.HistogramLogAnalyzer.properties.AppProperties;
import org.HdrHistogram.HistogramLogAnalyzer.properties.ZoomProperties;
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
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.List;

public class BucketsChartBuilder {

    public JPanel createBucketsChart(final List<HistogramModel> models, final AppProperties appProperties) {
        JFreeChart drawable = createBucketsDrawable(models, appProperties);

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
                        boolean hplKey = key.endsWith("%") || key.endsWith("Max");
                        if (hplKey) {
                            renderer.setSeriesVisible(i, b);
                        }
                    }
                }
            }
        });

        // FIXME: uninstall listeners

        return chartPanel;
    }

    private JFreeChart createBucketsDrawable(List<HistogramModel> histogramModels, AppProperties appProperties)
    {
        String chartTitle = ConstantsHelper.getChartTitle(HLAChartType.BUCKETS);
        String xAxisLabel = ConstantsHelper.getXAxisLabel(HLAChartType.BUCKETS);
        String yAxisLabel = ConstantsHelper.getYAxisLabel(HLAChartType.BUCKETS);

        XYSeriesCollection sc = createXYSeriesCollection(histogramModels);

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

        XYSeriesCollection dataset = (XYSeriesCollection) plot.getDataset();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        LogAxis logDomain = new LogAxis(xAxisLabel);
        logDomain.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        logDomain.setMinorTickMarksVisible(false);
        logDomain.setBase(10);
        DecimalFormat df = new DecimalFormat("0.###");
        df.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.ENGLISH));
        logDomain.setNumberFormatOverride(df);

        logDomain.setLabelFont(plot.getDomainAxis().getLabelFont());
        plot.setDomainAxis(0, logDomain);

        LogAxis logRange = new LogAxis(yAxisLabel);
        logRange.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        logRange.setMinorTickMarksVisible(false);
        logRange.setBase(10);
        df = new DecimalFormat("#,###,###");
        df.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.ENGLISH));
        logRange.setNumberFormatOverride(df);

        logRange.setLabelFont(plot.getRangeAxis().getLabelFont());
        plot.setRangeAxis(0, logRange);

        for (int i = 0; i < plot.getSeriesCount(); i++) {

            XYSeries series = dataset.getSeries(i);
            String key = (String) series.getKey();

            renderer.setSeriesLinesVisible(i, true);
            if (key.endsWith("%") || key.equals("Max")) {
                renderer.setSeriesVisible(i, appProperties.getHplProperties().isHPLVisible());
                renderer.setSeriesPaint(i, ColorHelper.getHPLColor(key));
                renderer.setSeriesStroke(i, new BasicStroke(2.0f));
            } else {
                renderer.setSeriesPaint(i, ColorHelper.getColor(i));
            }

            boolean hplKey = key.endsWith("%") || key.endsWith("Max");
            if (!hplKey) {
                renderer.setSeriesStroke(i, new BasicStroke(1.0f, java.awt.BasicStroke.CAP_SQUARE, java.awt.BasicStroke.JOIN_MITER));
                renderer.setSeriesShapesVisible(i, false);
            } else {
                renderer.setSeriesStroke(i, new BasicStroke(2.0f));
                renderer.setSeriesShapesVisible(i, false);
                renderer.setSeriesItemLabelsVisible(i, true);
            }
        }

        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());

        plot.setDomainGridlinesVisible(false);
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setRenderer(renderer);

        return drawable;

    }

    private static final String DEFAULT_KEY = "Raw latency count";

    private XYSeriesCollection createXYSeriesCollection(List<HistogramModel> histogramModels) {
        XYSeriesCollection ret = new XYSeriesCollection();

        // tool doesn't support MWP/HPL for charts with multiple files
        // tool doesn't support MWP/HPL for files with multiple tags
        boolean multipleFiles = histogramModels.size() > 1;
        if (!multipleFiles) {
            HistogramModel histogramModel = histogramModels.get(0);
            Set<String> tags = histogramModel.getTags();

            boolean multipleTags = tags.size() > 1;
            if (!multipleTags) {
                XYSeries series = new XYSeries(DEFAULT_KEY);
                BucketIterator bi = histogramModel.listBucketObjects(null);
                while (bi.hasNext()) {
                    BucketObject bo = (BucketObject) bi.next();
                    double hiccupValue = bo.getLatencyValue();
                    double totalCount = bo.getCountAtValue();
                    if (totalCount == 0) {
                        continue;
                    }
                    series.add(hiccupValue, totalCount);
                }
                ret.addSeries(series);

                // HPL lines (vertical lines actually but let's use HPL for consistency)
                Iterator<PercentileObject> pi = histogramModel.listHPLPercentileObjects(null);
                while (pi.hasNext()) {
                    PercentileObject po = pi.next();
                    String key = String.valueOf(po.getPercentileValue() * 100);
                    key = !key.contains(".") ? key : key.replaceAll("0*$", "").replaceAll("\\.$", "");
                    series = new XYSeries(key +"%");

                    double latencyValue = po.getLatencyAxisValue();

                    series.add(latencyValue, ret.getRangeLowerBound(false));
                    series.add(latencyValue, ret.getRangeUpperBound(false));
                    ret.addSeries(series);
                }

                // Max line
                Double maxLatencyAxisValue = 0.0;
                MaxPercentileIterator mpi = histogramModel.listMaxPercentileObjects(null);
                PercentileObject mpo;
                while (mpi.hasNext()) {
                    mpo = mpi.next();
                    maxLatencyAxisValue = Math.max(maxLatencyAxisValue, mpo.getLatencyAxisValue());
                }
                series = new XYSeries("Max");
                series.add(ret.getDomainUpperBound(false), ret.getRangeLowerBound(false));
                series.add(ret.getDomainUpperBound(false), ret.getRangeUpperBound(false));
                ret.addSeries(series);

                return ret;
            }
        }

        for (HistogramModel histogramModel : histogramModels) {
            boolean multipleTags = histogramModel.getTags().size() > 1;
            for (String tag : histogramModel.getTags()) {
                String key;
                if (multipleFiles) {
                    key = histogramModel.getShortFileName();
                } else {
                    if (multipleTags) {
                        key = tag == null ? "No Tag" : tag;
                    } else {
                        key = DEFAULT_KEY;
                    }
                }
                XYSeries series = new XYSeries(key);

                BucketIterator bi = histogramModel.listBucketObjects(tag);
                while (bi.hasNext()) {
                    BucketObject bo = (BucketObject) bi.next();
                    double hiccupValue = bo.getLatencyValue();
                    double totalCount = bo.getCountAtValue();
                    if (totalCount == 0) {
                        continue;
                    }
                    series.add(hiccupValue, totalCount);
                }
                ret.addSeries(series);
            }
        }


        return ret;
    }
}
