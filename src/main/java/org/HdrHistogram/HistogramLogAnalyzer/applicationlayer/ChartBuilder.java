/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.ResultSet;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

import javax.swing.BorderFactory;
import javax.swing.ToolTipManager;

import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.DBConnect;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;

import java.util.ArrayList;
import java.util.Collections;

import java.util.logging.Level;

public class ChartBuilder implements MouseListener {

    public static enum ChartType {
        hiccupDurationByPercentile,
        maxHiccupDurationByTimeInterval,
        countOfBucketedHiccupValues
    }

    private DBConnect db;
    private JFreeChart chart;
    private ChartPanel cp;
    public XYSeriesCollection sc;
    private int howmanylines;
    private Double percentile_maxvalue   = 0.0;
    private Double percentile_max_cvalue = 0.0;
    private int color_interval[][] = {{255,0,0},{0,255,0},{0,0,255},{128,0,128},{255,0,0}};
    private int color_percentile[][] = {{65,111,166},{255,0,0},{255,204,102}};

//  private String[] strsymbols = new String[]{"0%", "90%","95%", "99%", "99.9%", "99.99%", "99.999%", "99.9999%", "99.99999%"};
    private Double[] strsymbols;

    private boolean show_sla;

    public void set_strsymbols(Double[] symbols) {
        this.strsymbols = symbols.clone();
    }

    public ChartBuilder(DBConnect db, boolean show_sla, Double[] strsymbols2) {
        this.db = db;
        this.show_sla=show_sla;
        this.strsymbols = strsymbols2.clone();
    }

    public class HiccupBucketCount {
        public double hiccupValue;
        public double count;

        public HiccupBucketCount(double hiccupValueIn, double countIn) {
            hiccupValue = hiccupValueIn;
            count = countIn;
        }

        public void addToCount(double countIn) {
            count += countIn;
        }
    }

    public class PercentileData {
        private String fileName = null;
        private Double percentile_tp_50   = 0.0;
        private Double percentile_tp_90   = 0.0;
        private Double percentile_tp_99   = 0.0;
        private Double percentile_tp_999  = 0.0;
        private Double percentile_tp_9999 = 0.0;
        private Double percentile_tp_max  = 0.0;
        private boolean printedToStdout = false;

        public PercentileData(String fileNameIn, Double percentile_tp_50_in, Double percentile_tp_90_in,
            Double percentile_tp_99_in, Double percentile_tp_999_in,
            Double percentile_tp_9999_in, Double percentile_tp_max_in) {
            fileName = fileNameIn;
            percentile_tp_50   = percentile_tp_50_in;
            percentile_tp_90   = percentile_tp_90_in;
            percentile_tp_99   = percentile_tp_99_in;
            percentile_tp_999  = percentile_tp_999_in;
            percentile_tp_9999 = percentile_tp_9999_in;
            percentile_tp_max  = percentile_tp_max_in;
        }

        public Double get_tp_50() {
            return percentile_tp_50;
        }
        public Double get_tp_90() {
            return percentile_tp_90;
        }
        public Double get_tp_99() {
            return percentile_tp_99;
        }
        public Double get_tp_999() {
            return percentile_tp_999;
        }
        public Double get_tp_9999() {
            return percentile_tp_9999;
        }

        public boolean usePercentileData(String fileNameIn) {
            if ((fileName == null) || (fileNameIn == null)) {
                return false;
            }
            return fileName.equals(fileNameIn);
        }

        public void printedToStdout(boolean alreadyPrinted) {
            printedToStdout = alreadyPrinted;
        }
        public boolean alreadyPrintedToStdout() {
            return printedToStdout;
        }
    }

    public PercentileData percentileData = null;

    private XYSeriesCollection get_dataset(ChartType chartType) {
        sc = new XYSeriesCollection();

        if (chartType == ChartType.hiccupDurationByPercentile) {
            howmanylines = 1;
            for (int i = 0; i < howmanylines; i++) {
                 XYSeries series1 = new XYSeries(ConstantsHelper.getLatencyName() + " by Percentile");
                 XYSeries series2 = new XYSeries("Max");
                 XYSeries series3 = new XYSeries("SLA");
/*
                  XYSeries series4 = new XYSeries("hhh");
*/
                try {
                    String cmd = "select max( cast(value as float) ) as max_value, max(cast(cvalue as float) ) as max_cvalue from j_hst;";
                    ResultSet rs = db.statement.executeQuery(cmd);
                    while (rs.next()) {
                        percentile_maxvalue = rs.getDouble("max_value");
                        percentile_max_cvalue = rs.getDouble("max_cvalue");
                    }

                    cmd = "select * from j_hst where value < " + percentile_maxvalue.toString() + ";";
                    rs = db.statement.executeQuery(cmd);
                    while (rs.next()) {
                        series1.add(rs.getDouble("cvalue"), rs.getDouble("value"));
                        series2.add(rs.getDouble("cvalue"), percentile_maxvalue);
                    }

                    cmd = "select sla_cvalue,sla_hiccuptimeinterval from j_sla_details;";
                    rs = db.statement.executeQuery(cmd);
                    while (rs.next()) {
                        series3.add(rs.getDouble("sla_cvalue"), rs.getDouble("sla_hiccuptimeinterval"));
                    }
/*
                    for (int j = 0; j < strsymbols.length; j++) {
                        series4.add(Math.pow(10, j), (Double)0.0);

                        if (Application.getJHiccupViewerConfiguration().verbose()) {
                            Application.getApplicationLogger().logp(Level.FINEST, "ChartBuilder", "get_dataset", "series 4: [" + j + "]: " + Math.pow(10, j));
                        }
                    }
*/

                } catch (Exception e) {
                }

                sc.addSeries(series1); // cvalue, value
                sc.addSeries(series2); // cvalue, percentile_maxvalue
                sc.addSeries(series3); // sla_cvalue, sla_hiccuptimeinterval
/*				
  				sc.addSeries(series4); // Math.pow(10, j), 0
 */
            }

        } else if (chartType == ChartType.maxHiccupDurationByTimeInterval) {
            boolean printPercentileDataToStdout =
                    Application.getJHiccupViewerConfiguration().printPercentileDataToStdout();
            ArrayList<ElapsedTimeAndValue> maxTimesAndValues = null;
            if (printPercentileDataToStdout) {
                maxTimesAndValues = new ArrayList<ElapsedTimeAndValue>();
            }
        
            howmanylines = 1;
            Double percentile_tp_50   = 0.0;
            Double percentile_tp_90   = 0.0;
            Double percentile_tp_99   = 0.0;
            Double percentile_tp_999  = 0.0;
            Double percentile_tp_9999 = 0.0;
            Double percentile_tp_max  = 0.0;

            for (int i = 0; i < howmanylines; i++) {
                XYSeries series1 = new XYSeries("Max per interval");
                XYSeries series2 = new XYSeries("99%");
                XYSeries series3 = new XYSeries("99.9%");
                XYSeries series4 = new XYSeries("99.99%");
                XYSeries series5 = new XYSeries("Max");
                try {
                    String cmd = "select percentile_tp_50,percentile_tp_90,percentile_tp_99,percentile_tp_999,percentile_tp_9999,percentile_tp_max from j_percentile;";
                    ResultSet rs = db.statement.executeQuery(cmd);
                    while (rs.next()) {
                        percentile_tp_50   = rs.getDouble("percentile_tp_50");
                        percentile_tp_90   = rs.getDouble("percentile_tp_90");
                        percentile_tp_99   = rs.getDouble("percentile_tp_99");
                        percentile_tp_999  = rs.getDouble("percentile_tp_999");
                        percentile_tp_9999 = rs.getDouble("percentile_tp_9999");
                        percentile_tp_max  = rs.getDouble("percentile_tp_max");
                    }
                    cmd = "select percentile_elapsedTime,percentile_ip_max from j_percentile ;";
                    rs = db.statement.executeQuery(cmd);
                    while (rs.next()) {
                        double percentileElapsedTime = rs.getDouble("percentile_elapsedTime");
                        double percentileIntervalMax = rs.getDouble("percentile_ip_max");
                        series1.add(percentileElapsedTime, percentileIntervalMax);

                        if (printPercentileDataToStdout) {
                            ElapsedTimeAndValue elapsedTimeAndValue = new ElapsedTimeAndValue(percentileElapsedTime, percentileIntervalMax);
                            maxTimesAndValues.add(elapsedTimeAndValue);
                        }

                        series2.add(percentileElapsedTime, percentile_tp_99);
                        series3.add(percentileElapsedTime, percentile_tp_999);
                        series4.add(percentileElapsedTime, percentile_tp_9999);
                        series5.add(percentileElapsedTime, percentile_tp_max);
                    }
                } catch (Exception e) {
                }
                sc.addSeries(series1);
                sc.addSeries(series2);
                sc.addSeries(series3);
                sc.addSeries(series4);
                sc.addSeries(series5);
            }

            percentileData = new PercentileData(Application.jHiccupLogFilename(), percentile_tp_50, percentile_tp_90,
                percentile_tp_99, percentile_tp_999,
                percentile_tp_9999, percentile_tp_max);

            if (printPercentileDataToStdout) {
                System.out.println("LogFile: " + Application.jHiccupLogFilename());
                System.out.println(Application.getApplicationInstance().jHiccupLogStartTime());
                System.out.format("50th:    %7.3f%n", percentile_tp_50);
                System.out.format("90th:    %7.3f%n", percentile_tp_90);
                System.out.format("99th:    %7.3f%n", percentile_tp_99);
                System.out.format("99.9th:  %7.3f%n", percentile_tp_999);
                System.out.format("99.99th: %7.3f%n", percentile_tp_9999);

                synchronized (ElapsedTimeAndValue.class) {
                    ElapsedTimeAndValue.setToSortValuesDescending();
                    Collections.sort(maxTimesAndValues);
                }

                int count = 0;
                while (count < maxTimesAndValues.size() && maxTimesAndValues.get(count).getValue() > percentile_tp_9999) {
                    count++;
                }
                System.out.format("NumberGreaterThan99.99: %d%n", count);
                System.out.format("Maximum: %7.3f%n", percentile_tp_max);


                int numberLongestMaxHiccupsToPrint = 
                        Application.getJHiccupViewerConfiguration().numberOfLongestMaxHiccupsToPrintToStdout();
                ArrayList<ElapsedTimeAndValue> maxTimesAndValues2 = new ArrayList<ElapsedTimeAndValue>(numberLongestMaxHiccupsToPrint);
                int iter = 0;
                while (iter < numberLongestMaxHiccupsToPrint && iter < maxTimesAndValues.size()) {
                    maxTimesAndValues2.add(maxTimesAndValues.get(iter));
                    iter++;
                }

                synchronized (ElapsedTimeAndValue.class) {
                    ElapsedTimeAndValue.setToSortElapsedTimesAscending();
                    Collections.sort(maxTimesAndValues2);
                }

                iter = 0;
                while (iter < maxTimesAndValues2.size()) {
                    System.out.format("%d %7.3f %7.3f%n", iter, maxTimesAndValues2.get(iter).getElapsedTime(), maxTimesAndValues2.get(iter).getValue());
                    iter++;
                }

                percentileData.printedToStdout(true);
            }

        } else if (chartType == ChartType.countOfBucketedHiccupValues) {

            XYSeries series1 = new XYSeries("Raw Latency count");
            XYSeries series2 = new XYSeries("99%");
            XYSeries series3 = new XYSeries("99.9%");
            XYSeries series4 = new XYSeries("99.99%");

            // The percentile data for display on the graph and printing to stdout
            double percentile_tp_99   = 0.0D;
            double percentile_tp_999  = 0.0D;
            double percentile_tp_9999 = 0.0D;
            // The maximum hiccup value for printing to stdout
            double maximumHiccupValue = 0.0D;

            double valueToAddForZeroOrOneBasedJHiccupValue = 0.0D;

            try {
                // j_hst(value REAL,Percentile REAL,TotalCountIncludingThisValue REAL, cvalue REAL);";
                String cmd = "select * from j_hst;";
                ResultSet rs = db.statement.executeQuery(cmd);

                // Create a place to store all of the records as we process them
                ArrayList<HiccupBucketCount> hiccupBucketCountList = new ArrayList<HiccupBucketCount>();

                int recordCount = 0;

                boolean processedInitialValue = false;

                double hiccupValue = 0.0D;
                double totalCount  = 0.0D;

                double hiccupValuePrevious = 0.0D; // Set to 0.0 to trigger first addition to the saved values
                double totalCountPrevious  = 0.0D;

                while (rs.next()) {
                    recordCount++;
                    hiccupValue = rs.getDouble("value") + valueToAddForZeroOrOneBasedJHiccupValue;
                    totalCount  = rs.getDouble("TotalCountIncludingThisValue");

                    // Some of the versions of jHiccup have hiccup values that use the raw values
                    // (non-zero based) rather than just the hiccup values. We try to catch those
                    // cases here.
                    if (! processedInitialValue) {
                        if (hiccupValue < 0.5D) {
                            valueToAddForZeroOrOneBasedJHiccupValue = 1.0D;
                            hiccupValue += valueToAddForZeroOrOneBasedJHiccupValue;
                            processedInitialValue = true;
                        }
                    }

                    if (totalCount > totalCountPrevious) {
                        if (hiccupValue > hiccupValuePrevious) {
                            HiccupBucketCount hiccupBucketCount = new HiccupBucketCount(hiccupValue, totalCount - totalCountPrevious);
                            hiccupBucketCountList.add(hiccupBucketCount);

                            if (Application.getJHiccupViewerConfiguration().verbose()) {
                                System.out.println("ChartBuilder.get_dataset 0 record (add ): " + recordCount + " " +
                                    hiccupBucketCount.hiccupValue + " " + hiccupBucketCount.count);
                            }
                        }
                        else {
                            double count = totalCount - totalCountPrevious;
                            HiccupBucketCount hiccupBucketCount = hiccupBucketCountList.get(hiccupBucketCountList.size() - 1);
                            hiccupBucketCount.addToCount(count);

                            if (Application.getJHiccupViewerConfiguration().verbose()) {
                                System.out.println("ChartBuilder.get_dataset 1 record (incr): " + recordCount + " " +
                                    hiccupBucketCount.hiccupValue + " " + hiccupBucketCount.count);
                            }
                        }
                    } else {
                        if (totalCount < totalCountPrevious) {
                            System.err.println("HistogramLogAnalyzer: Record: " + recordCount +
                                " has invalid value: " + totalCount +
                                " lower than previous: " + totalCountPrevious);
                        }
                        if (Application.getJHiccupViewerConfiguration().verbose()) {
                            System.out.println("ChartBuilder.get_dataset 2 record (skip): " + recordCount + " " +
                                hiccupValue + " " + totalCount);
                        }
                    }

                    if (hiccupValue > maximumHiccupValue) {
                        maximumHiccupValue = hiccupValue;
                    }
                    hiccupValuePrevious = hiccupValue;
                    totalCountPrevious = totalCount;
                }

                // If we are displaying pause count then we need to remove the stragglers
                if (Application.getJHiccupViewerConfiguration().showGraphOfCountOfBucketedPauseValues()) {
                    int listSize = hiccupBucketCountList.size();
                    if (listSize > 2) {
                        for (int i = listSize - 1; i > 1; i--) {
                            HiccupBucketCount topHiccupBucketCount = hiccupBucketCountList.get(i);
                            double topBucketsCount = topHiccupBucketCount.count;
                            if (Application.getJHiccupViewerConfiguration().verbose()) {
                                System.out.println("=========== i = " + i + " " + topBucketsCount);
                            }
                            for (int j = i - 1; j >= 0; j--) {
                                HiccupBucketCount hiccupBucketCount = hiccupBucketCountList.get(j);
                                hiccupBucketCount.count -= topBucketsCount;
                                if (hiccupBucketCount.count < 0.0D) {
                                    hiccupBucketCount.count = 0.0D;
                                }
                                if (Application.getJHiccupViewerConfiguration().verbose()) {
                                    System.out.println("===== j = " + j + " " + hiccupBucketCount.count);
                                }
                            }
                        }
                        for (int i = listSize - 1; i >= 0; i--) {
                            HiccupBucketCount hiccupBucketCount = hiccupBucketCountList.get(i);
                            double bucketsCount = hiccupBucketCount.count;
                            if (bucketsCount < 1.0D) {
                                if (Application.getJHiccupViewerConfiguration().verbose()) {
                                    System.out.println("----- remove: " + i + " " + hiccupBucketCount.count);
                                }
                                hiccupBucketCountList.remove(i);
                            }
                        }
                    }
                }

                // We use the maximum count to figure out where to put the percentile lines in the graph
                double maximumHiccupValueCount = 0.0D;

                if (hiccupBucketCountList.size() > 0) {
                    for (HiccupBucketCount hiccupBucketCount : hiccupBucketCountList) {
                        series1.add(hiccupBucketCount.hiccupValue, hiccupBucketCount.count);
                        if (hiccupBucketCount.count > maximumHiccupValueCount ) {
                            maximumHiccupValueCount = hiccupBucketCount.count;
                        }

                        if (Application.getJHiccupViewerConfiguration().verbose()) {
                            System.out.println("ChartBuilder.get_dataset Series1.add: " +
                                hiccupBucketCount.hiccupValue + " " + hiccupBucketCount.count);
                        }

                        if (Application.getJHiccupViewerConfiguration().verbose()) {
                            Application.getApplicationLogger().logp(Level.FINEST, "ChartBuilder", "get_dataset", "Series1.add: " + 
                                hiccupBucketCount.hiccupValue + " " + hiccupBucketCount.count);
                        }
                    }
                }

                // Grab the percentile data for display on the graph
                if ((percentileData != null) && (percentileData.usePercentileData(Application.jHiccupLogFilename()))) {
                        percentile_tp_99   = percentileData.get_tp_99();
                        percentile_tp_999  = percentileData.get_tp_9999();
                        percentile_tp_9999 = percentileData.get_tp_9999();
                } else {
                    String cmdPercentile   = "select percentile_tp_99,percentile_tp_999,percentile_tp_9999 from j_percentile;";
                    ResultSet rsPercentile = db.statement.executeQuery(cmdPercentile);
                    // Need to loop through the entire data set to get to the last record.
                    while (rsPercentile.next()) {
                        percentile_tp_99   = rsPercentile.getDouble("percentile_tp_99");
                        percentile_tp_999  = rsPercentile.getDouble("percentile_tp_999");
                        percentile_tp_9999 = rsPercentile.getDouble("percentile_tp_9999");
                    }
                }

                // Set the value we obtained from the data to the 1 msec base for the raw hiccups, if required;
                percentile_tp_99   += valueToAddForZeroOrOneBasedJHiccupValue; 
                percentile_tp_999  += valueToAddForZeroOrOneBasedJHiccupValue; 
                percentile_tp_9999 += valueToAddForZeroOrOneBasedJHiccupValue; 

                double maximumHiccupValueCountDividedBy3  = maximumHiccupValueCount /  3.3D;
                double maximumHiccupValueCountDividedBy10 = maximumHiccupValueCount / 10.0D;

                // Add the percentile lines
                series2.add(percentile_tp_99, maximumHiccupValueCountDividedBy10);
                series2.add(percentile_tp_99, maximumHiccupValueCountDividedBy3);
                series2.add(percentile_tp_99, maximumHiccupValueCount);

                series3.add(percentile_tp_999, maximumHiccupValueCountDividedBy10);
                series3.add(percentile_tp_999, maximumHiccupValueCountDividedBy3);
                series3.add(percentile_tp_999, maximumHiccupValueCount);

                series4.add(percentile_tp_9999, maximumHiccupValueCountDividedBy10);
                series4.add(percentile_tp_9999, maximumHiccupValueCountDividedBy3);
                series4.add(percentile_tp_9999, maximumHiccupValueCount);

            } catch (Exception e) {
            }

            if (Application.getJHiccupViewerConfiguration().printPercentileDataToStdout() &&
                ((percentileData != null) && (! percentileData.alreadyPrintedToStdout()))) {
                System.out.println("Log file: " + Application.jHiccupLogFilename());
                System.out.println(Application.getApplicationInstance().jHiccupLogStartTime());

                double percentile_tp_50 = percentileData.get_tp_50() + valueToAddForZeroOrOneBasedJHiccupValue; 
                double percentile_tp_90 = percentileData.get_tp_90() + valueToAddForZeroOrOneBasedJHiccupValue; 

                System.out.format("50th:    %7.3f%n", percentile_tp_50);
                System.out.format("90th:    %7.3f%n", percentile_tp_90);
                System.out.format("99th:    %7.3f%n", percentile_tp_99);
                System.out.format("99.9th:  %7.3f%n", percentile_tp_999);
                System.out.format("99.99th: %7.3f%n", percentile_tp_9999);
                System.out.format("Maximum: %7.3f%n", maximumHiccupValue);
                percentileData.printedToStdout(true);
            }

            sc.addSeries(series1);
            sc.addSeries(series2);
            sc.addSeries(series3);
            sc.addSeries(series4);
        }

        return sc;
    }

    private void chartHiccupDurationByPercentile(ChartType chartType) {

        String chartTitle = ConstantsHelper.getChartTitle(ChartType.hiccupDurationByPercentile);
        String xAxisLabel = ConstantsHelper.getXAxisLabel(ChartType.hiccupDurationByPercentile);
        String yAxisLabel = ConstantsHelper.getYAxisLabel(ChartType.hiccupDurationByPercentile);
        String logAxis = ConstantsHelper.getLogAxisLabel(ChartType.hiccupDurationByPercentile);

        chart = ChartFactory.createXYLineChart(chartTitle,
            xAxisLabel,
            yAxisLabel,
            get_dataset(chartType),
            PlotOrientation.VERTICAL,
            true,
            true,
            false);

        chart.getPlot().setBackgroundPaint(Color.white);
        chart.getXYPlot().setRangeGridlinePaint(Color.gray);
        chart.getXYPlot().setDomainGridlinePaint(Color.gray);

        XYPlot plot = (XYPlot) chart.getPlot();

        LogAxis ll = new LogAxis(logAxis);
        ll.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        ((LogAxis)ll).setMinorTickMarksVisible(false);
        ((LogAxis)ll).setBase(10);

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
					return new StringBuffer(new String("0.0%"));
				}
				if (number == 10) {
					return new StringBuffer(new String("90.0%"));
				}
				if (number == 100) {
					return new StringBuffer(new String("99.0%"));
				}
				if (number == 1000) {
					return new StringBuffer(new String("99.9%"));
				}
				if (number == 10000) {
					return new StringBuffer(new String("99.99%"));
				}
				if (number == 100000) {
					return new StringBuffer(new String("99.999%"));
				}
				return toAppendTo;
			}
		});
        plot.setDomainAxis(0, ll);
        //plot.setDomainAxis(ll);
        plot.getDomainAxis(0).setUpperBound(percentile_max_cvalue + (percentile_max_cvalue * 0.4));

        plot.getRangeAxis(0).setRange(0.0, percentile_maxvalue + 10);
        plot.getRangeAxis().setAutoRange(false);

        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        for (int i = 0; i < 3; i++) {
            renderer.setSeriesPaint(i,new Color(color_percentile[i][0],color_percentile[i][1],color_percentile[i][2]));
            renderer.setSeriesStroke(i, new BasicStroke(2.0f));
            renderer.setSeriesShapesVisible(i, false);
        }
        plot.setDomainGridlinesVisible(false);
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());

        renderer.setSeriesItemLabelGenerator(1, new XYItemLabelGenerator() {
            @Override
            public String generateLabel(XYDataset arg0, int arg1, int arg2) {
                if (arg2 == 10) {
                    Double d = arg0.getYValue(arg1, arg2);
                    return "MAX " + d.toString();
                }
                return null;

            }
        });

        renderer.setSeriesItemLabelsVisible(1,true);
        
        if (! show_sla) {
            renderer.setSeriesVisible(2, false);
        }
/*
        renderer.setSeriesItemLabelGenerator(3, new XYItemLabelGenerator() {
            @Override
            public String generateLabel(XYDataset arg0, int arg1, int arg2) {

                if (Application.getJHiccupViewerConfiguration().verbose()) {
                    Application.getApplicationLogger().logp(Level.FINEST, "ChartBuilder", 
                        "chartHiccupDurationByPercentile", "Generate labels: arg1: " + arg1 + " arg2: " + arg2);
                    Application.getApplicationLogger().logp(Level.FINEST, "ChartBuilder", 
                        "chartHiccupDurationByPercentile", "Generate labels: " +  strsymbols[arg2].toString() + "%");
                }

                return strsymbols[arg2].toString() + "%";
            }
        });
*/
        renderer.setSeriesItemLabelsVisible(3, false);
        renderer.setSeriesVisibleInLegend(3, false);
/*
        LogarithmicAxis domainaxis = (LogarithmicAxis) plot.getDomainAxis();
        domainaxis.setTickLabelsVisible(false);
        domainaxis.setTickMarksVisible(false);
*/
        plot.setDomainPannable(true);
        plot.setRangePannable(true);

        plot.setRenderer(renderer);
    }

    private void chartCountOfBucketedHiccupValues(ChartType chartType) {

        String chartTitle = ConstantsHelper.getChartTitle(ChartType.countOfBucketedHiccupValues);
        String xAxisLabel = ConstantsHelper.getXAxisLabel(ChartType.countOfBucketedHiccupValues);
        String yAxisLabel = ConstantsHelper.getYAxisLabel(ChartType.countOfBucketedHiccupValues);

        chart = ChartFactory.createXYLineChart(chartTitle,
            xAxisLabel,
            yAxisLabel,
            get_dataset(chartType),
            PlotOrientation.VERTICAL,
            true,
            true,
            false);
        chart.getPlot().setBackgroundPaint(Color.white);
        chart.getXYPlot().setRangeGridlinePaint(Color.gray);
        chart.getXYPlot().setDomainGridlinePaint(Color.gray);

        XYPlot plot = (XYPlot) chart.getPlot();

        // X axis
        // -> LogarithmicAxis logDomain = new LogarithmicAxis(chartXAxisLabel);
        LogAxis logDomain = new LogAxis(xAxisLabel);
        plot.setDomainAxis(0, logDomain);
        // Y axis
        // -> LogarithmicAxis logRange = new LogarithmicAxis("Number in Bucket");
        LogAxis logRange = new LogAxis(yAxisLabel);
        plot.setRangeAxis(0, logRange);

        plot.getDomainAxis().setAutoRange(true);
        plot.getRangeAxis().setAutoRange(true);

        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        for (int i = 0; i < plot.getSeriesCount(); i++) {
            renderer.setSeriesPaint(i, new Color(color_interval[i][0], color_interval[i][1], color_interval[i][2]));
            if (i == 0) {
                renderer.setSeriesStroke(i, new BasicStroke(1.0f, java.awt.BasicStroke.CAP_SQUARE, java.awt.BasicStroke.JOIN_MITER));
            } else {
                renderer.setSeriesStroke(i, new BasicStroke(2.0f));
                renderer.setSeriesShapesVisible(i, false);

                renderer.setSeriesItemLabelGenerator(i, new XYItemLabelGenerator() {
                    @Override
                    public String generateLabel(XYDataset arg0, int arg1, int arg2) {
                        // Stagger the labels so that they do not overwrite one another
                        // System.out.println("arg 0 1 2 " + arg0 + " " + arg1 + " " + arg2);
                        if (((arg1 == 3) && (arg2 == 0)) || 
                            ((arg1 == 2) && (arg2 == 1)) || 
                            ((arg1 == 1) && (arg2 == 2))) {
                            Double d = arg0.getXValue(arg1, arg2);
                            // return d.toString();
                            return String.format("%.3f", d);
                        } else {
                            return null;
                        }
/*
                        if ((arg1 % 2) == 1) {
                            if (arg2 == 1) {
                                Double d = arg0.getXValue(arg1, arg2);
                                return d.toString();
                            } else {
                                return null;
                            }
                        } else if (arg2 == 0) {
                                Double d = arg0.getXValue(arg1, arg2);
                                return d.toString();
                        } else {
                            return null;
                        }
*/
                    }
                });

                renderer.setSeriesItemLabelsVisible(i, true);
            }
        }

        LegendTitle legend = chart.getLegend();
        legend.setPosition(RectangleEdge.TOP);

        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());

        plot.setDomainGridlinesVisible(false);
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setRenderer(renderer);
    }

    private void chartMaxHiccupDurationByTimeInterval(ChartType chartType) {

        String chartTitle = ConstantsHelper.getChartTitle(ChartType.maxHiccupDurationByTimeInterval);
        String xAxisLabel = ConstantsHelper.getXAxisLabel(ChartType.maxHiccupDurationByTimeInterval);
        String yAxisLabel = ConstantsHelper.getYAxisLabel(ChartType.maxHiccupDurationByTimeInterval);

        chart = ChartFactory.createXYLineChart(chartTitle,
            xAxisLabel,
            yAxisLabel,
            get_dataset(chartType),
            PlotOrientation.VERTICAL,
            true,
            true,
            false);
        chart.getPlot().setBackgroundPaint(Color.white);
        chart.getXYPlot().setRangeGridlinePaint(Color.gray);
        chart.getXYPlot().setDomainGridlinePaint(Color.gray);

        XYPlot plot = (XYPlot) chart.getPlot();
        final XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        for (int i=0; i < plot.getSeriesCount(); i++) {
            renderer.setSeriesPaint(i, new Color(color_interval[i][0], color_interval[i][1], color_interval[i][2]));
            if (i != 0) {
                renderer.setSeriesStroke(i, new BasicStroke(2.0f));
            }
            renderer.setSeriesShapesVisible(i, false);
        }

        LegendTitle legend = chart.getLegend();
        legend.setPosition(RectangleEdge.TOP);
        plot.setDomainGridlinesVisible(false);
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setRenderer(renderer);
    }

    public ChartPanel generic_chart(ChartType chartType) {

        switch (chartType) {
            case hiccupDurationByPercentile:
                chartHiccupDurationByPercentile(ChartType.hiccupDurationByPercentile);
                break;
                    
            case maxHiccupDurationByTimeInterval:
                chartMaxHiccupDurationByTimeInterval(ChartType.maxHiccupDurationByTimeInterval);
                break;
                         
            case countOfBucketedHiccupValues:
                chartCountOfBucketedHiccupValues(ChartType.countOfBucketedHiccupValues);
                break;
                        
            default:
                System.err.println("jHiccupLogAnalyzer: Invalid value passed as chart selector");
                System.exit(1);
                break;
        }

        cp = new ChartPanel(chart, true);
        cp.setPreferredSize(new java.awt.Dimension(800, 600));
        cp.setBackground(Color.gray);
        cp.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(4, 4, 4, 4),
                BorderFactory.createLineBorder(Color.black)));

        ToolTipManager.sharedInstance().registerComponent(cp);
        cp.addMouseListener(this);

        db.close_db();
        return cp;
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
        ToolTipManager ttm = ToolTipManager.sharedInstance();
        ttm.setDismissDelay(10000);
        ttm.setReshowDelay(0);
        ttm.setInitialDelay(0);
    }

    @Override
    public void mouseExited(MouseEvent arg0) {
    }

    @Override
    public void mousePressed(MouseEvent arg0) {
    }

    @Override
    public void mouseReleased(MouseEvent arg0) {
    }
}
