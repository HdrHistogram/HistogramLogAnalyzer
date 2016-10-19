/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import org.HdrHistogram.HistogramLogAnalyzer.datalayer.Parser;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.DBConnect;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/*
 * Custom JPanel that contains top and bottom charts
 */
class LatencyPanel extends JPanel implements AxisChangeListener
{
    private DBConnect db;
    private String hlogFileName;
    private String tag;
    private JHiccupViewerConfiguration config;
    private boolean showSLA;
    private ArrayList<Double> sla_db_percentile = new ArrayList<Double>();

    private JPanel topPanel;
    private JPanel bottomPanel;

    LatencyPanel(DBConnect db, String hlogFileName, String tag, JHiccupViewerConfiguration config,
                 boolean showSLA, ArrayList<Double> sla_db_percentile)
    {
        this.db = db;
        this.hlogFileName = hlogFileName;
        this.tag = tag;
        this.config = config;
        this.showSLA = showSLA;
        this.sla_db_percentile = sla_db_percentile;

        this.setLayout(new GridLayout(2, 1));
        this.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        topPanel = makechart_2(ChartBuilder.ChartType.maxHiccupDurationByTimeInterval);
        this.add(topPanel);

        if (config.showGraphOfCountOfBucketedHiccupValues()) {
            bottomPanel = makechart_2(ChartBuilder.ChartType.countOfBucketedHiccupValues);
        } else {
            bottomPanel = makechart_2(ChartBuilder.ChartType.hiccupDurationByPercentile);
        }
        this.add(bottomPanel);
    }

    private ChartPanel generic_chart(ChartBuilder.ChartType chartType) {
        sla_db_percentile.add(0, 0.0);
        ChartBuilder cm = new ChartBuilder(db, showSLA, sla_db_percentile.toArray(new Double[sla_db_percentile.size()]));
        sla_db_percentile.remove(0);

        JFreeChart drawable = cm.createDrawable(chartType);

        ChartPanel cp = new ChartPanel(drawable, true);
        cp.setPreferredSize(new java.awt.Dimension(800, 600));
        cp.setBackground(Color.gray);
        cp.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(4, 4, 4, 4),
                BorderFactory.createLineBorder(Color.black)));

        ToolTipManager.sharedInstance().registerComponent(cp);
        cp.addMouseListener(cm);

        db.close_db();
        return cp;
    }

    private JPanel makechart_2(ChartBuilder.ChartType chartType) {
        JPanel jp = new JPanel(new BorderLayout());
        try {
            ChartPanel cp = generic_chart(chartType);
            if (chartType == ChartBuilder.ChartType.maxHiccupDurationByTimeInterval) {
                XYPlot plot = (XYPlot) cp.getChart().getPlot();
                plot.getDomainAxis().addChangeListener(this);
            }
            jp.add(cp);

        } catch (Exception except) {
            System.err.println("HistogramLogAnalyzer: Image generation exception");
            System.err.println("  Message: " + except.getMessage());
            System.err.println("  Cause:   " + except.getCause());
            except.printStackTrace();

            JLabel label = new JLabel("Failed to create chart: " + except.getMessage());
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setVerticalAlignment(SwingConstants.CENTER);
            jp.add(label);
        }
        return jp;
    }

    @Override
    public void axisChanged(AxisChangeEvent axisChangeEvent) {
        ChartPanel topChart = (ChartPanel) topPanel.getComponent(0);
        XYPlot plot2 = (XYPlot) topChart.getChart().getPlot();
        String lowerString = String.valueOf(plot2.getDomainAxis().getLowerBound());
        String upperString = String.valueOf(plot2.getDomainAxis().getUpperBound());

        // needs re-reading hlog file
        db = new DBConnect(((Long) System.currentTimeMillis()).toString());
        Parser pr = new Parser(db, hlogFileName, tag, lowerString, upperString);
        try {
            pr.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // update drawable for bottom chart
        ChartBuilder cm = new ChartBuilder(db, showSLA, sla_db_percentile.toArray(new Double[sla_db_percentile.size()]));
        JFreeChart drawable;
        if (config.showGraphOfCountOfBucketedHiccupValues()) {
            drawable = cm.createDrawable(ChartBuilder.ChartType.countOfBucketedHiccupValues);
        } else {
            drawable = cm.createDrawable(ChartBuilder.ChartType.hiccupDurationByPercentile);
        }
        ChartPanel bottomChart = (ChartPanel) bottomPanel.getComponent(0);
        bottomChart.setChart(drawable);
    }

    /*
     * SLA-related methods
     */
    void updateSLAVisibility(boolean visible) {
        ChartPanel bottomChart = (ChartPanel) bottomPanel.getComponent(0);
        XYPlot plot = (XYPlot) bottomChart.getChart().getPlot();
        XYLineAndShapeRenderer renderer =
                (XYLineAndShapeRenderer) plot.getRenderer();
        if (visible) {
            renderer.setSeriesVisible(2, true);
        } else {
            renderer.setSeriesVisible(2, false);
        }
        plot.setRenderer(renderer);
    }

    void resetSLA() {
        ChartPanel bottomChart = (ChartPanel) bottomPanel.getComponent(0);
        XYPlot plot = (XYPlot) bottomChart.getChart().getPlot();
        XYSeriesCollection sc = (XYSeriesCollection) plot.getDataset();
        XYSeries sla_series = sc.getSeries(2);

        sla_db_percentile.add(0, 0.0);
        sla_db_percentile.remove(0);

        sla_series.clear();
        ResultSet rs = null;
        try {
            String cmd = "select sla_cvalue,sla_hiccuptimeinterval from j_sla_details;";
            rs = db.statement.executeQuery(cmd);
            while (rs.next()) {
                sla_series.addOrUpdate(
                        rs.getDouble("sla_cvalue"),
                        rs.getDouble("sla_hiccuptimeinterval"));
            }
        } catch (Exception except) {
            System.err.println("HistogramLogAnalyzer: SLA reset graph exception");
            System.err.println("  Message: " + except.getMessage());
            System.err.println("  Cause:   " + except.getCause());
            except.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException except) {
                    System.err.println("HistogramLogAnalyzer: SQL exception");
                    System.err.println("  Message: " + except.getMessage());
                    System.err.println("  Cause:   " + except.getCause());
                    except.printStackTrace();
                }
            }
        }
    }

}
