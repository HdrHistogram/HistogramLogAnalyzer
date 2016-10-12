/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.datalayer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.DBConnect;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.ParserDataObjects;

public class Parser {
    private DBConnect db;
    private ParserDataObjects pDObj;
    public  DBManager DBmgr;
    private String filename;
    private BufferedReader br;
    private String samplingStartTime = "";

    // Check the percentile data do make certain that the data in the file is correct.
    private Double previousPercentileTime = 0.0D; 
    private Double totalPercentileMax     = 0.0D;

    public String getSamplingStartTime() {
        return samplingStartTime;
    }

    public Parser(DBConnect db) {
        this.db = db;
        pDObj = new ParserDataObjects();
        DBmgr = new DBManager(this.db);
    }

    public int executejobs(String filename_hgrm, String filename_log)
    {
        /* Some early versions:
         *
         * hgrm file (1.2.2):
         *
         * First uncommented lines in the histogram file that we need to skip:
         *
         * 1 jHiccup histogram report, Fri Nov 29 09:56:01 GMT 2013 :
         * 2 --------------------
         * 3
         * 4 Value, Percentile, TotalCountIncludingThisValue
         * 5
         * 6 data starts here (skip 5)
         *
         * Later versions follow the same pattern:
         *
         * 1 jHiccup histogram report, Mon Mar 16 13:53:03 PDT 2015 :
         *
         * Log file (1.2.2) has comment characters (and version string, yeah!!), but no comment characters for column headers:
         *
         * 1 #[Logged with jHiccup version 1.2.2] 
         * 2 #[Sampling start time: Mon Mar 16 13:24:42 PDT 2015, (uptime at sampling start: 0.433 seconds)]
         * 3 Time: IntervalPercentiles:count ( 50% 90% Max ) TotalPercentiles:count ( 50% 90% 99% 99.9% 99.99% Max )
         *
         * hgrm file (1.2.5):
         *
         * 1 jHiccup histogram report, Fri Sep 06 09:12:33 MDT 2013 :
         * 2 --------------------
         * 3
         * 4 Value, Percentile, TotalCountIncludingThisValue
         * 5
         * 6         0.00 0.000000000000       7360     (skip 5)
         *
         * Log file (1.2.5):
         *
         * 1 #[Logged with jHiccup version 1.2.5]
         * 2 #[Sampling start time: Thu Sep 05 16:03:58 MDT 2013, (uptime at sampling start: 30.087 seconds)]
         * 3 Time: IntervalPercentiles:count ( 50% 90% Max ) TotalPercentiles:count ( 50% 90% 99% 99.9% 99.99% Max )
         * 4 35.123: I:4503 (   0.094  34.048 266.240 ) T:4503 (   0.094  34.048 222.208 262.144 266.240 266.240 )
         *
         * Switch to both hgrm and log files packaged in one hlog file:
         *
         * hgrm file (2.0.2):
         *
         * 1 #[Overall percentile distribution between 0.000 and <Infinite> seconds (relative to StartTime)]
         * 2 #[StartTime: 1428951642.862 (seconds since epoch), Mon Apr 13 12:00:42 PDT 2015]
         * 3      Value     Percentile TotalCount 1/(1-Percentile)
         * 4
         * 5         0.02 0.000000000000       4492           1.00  (skip 4, and jHiccupLogAnalyzer ignores last field))
         * 6         0.07 0.100000000000    7641309           1.11
         *
         * Log file (2.0.2) ("Sampling start time" changed to "StartTime"):
         *
         * 1 #[Interval percentile log between 0.000 and <Infinite> seconds (relative to StartTime)]
         * 2 #[StartTime: 1428951642.862 (seconds since epoch), Mon Apr 13 12:00:42 PDT 2015]
         * 3 Time: IntervalPercentiles:count ( 50% 90% Max ) TotalPercentiles:count ( 50% 90% 99% 99.9% 99.99% Max )
         * 4 31.093: I:950 (   0.000   0.000   0.000 ) T:950 (   0.000   0.000   0.000   0.000   0.000   0.000 )
         * 5 32.093: I:950 (   0.016   0.016   0.000 ) T:1900 (   0.000   0.016   0.016   0.016   0.016   0.000 )
         *
         * Summary 20150417: 
         *
         * Histogram file has "jHiccup histogram report" (skip 5 lines) or "#[Overall percentile" (skip 4 lines)
         */

        int processHistogramStatus = executejob_hgrm(filename_hgrm);
        if (processHistogramStatus == 1) {
            return 1;
        }
        int processLogStatus = executejob_log(filename_log);
        if (processLogStatus == 1) {
            return 1;
        }
        return 0; // okay
    }

    static final int numberOfLinesToSkip = 5;
    private int executejob_hgrm(String log_filename) {
        try {
            int skiplines = numberOfLinesToSkip;
            this.filename = log_filename;
            int myLine = 0;
            FileInputStream fs = new FileInputStream(filename);
            DataInputStream instrm = new DataInputStream(fs);
            br = new BufferedReader(new InputStreamReader(instrm));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                myLine++;
                if (myLine == 1 && strLine.startsWith("#[Overall percentile")) { 
                    skiplines--; // Only need to skip 4
                }
                if (myLine > skiplines) {
                    strLine = strLine.trim();
                    if ( ! strLine.startsWith("#")) {
                        if (parserjob_hgrm(strLine, myLine) != 0) {
                            System.err.println("HistogramLogAnalyzer: error on line: " + myLine + " in file: " + log_filename);
                            return 1; // error
                        }
                    }
                }
            }
            instrm.close();
            db.close_db();
            return 0;
        } catch (Exception e) {
            System.err.println("HistogramLogAnalyzer: Input file error: " + log_filename);
            e.toString();
            e.printStackTrace();
            return 1;
        }
    }

    private int executejob_log(String log_filename) {
        try {
            this.filename = log_filename;
            FileInputStream fs = new FileInputStream(filename);
            DataInputStream instrm = new DataInputStream(fs);
            br = new BufferedReader(new InputStreamReader(instrm));
            String strLine;
            long lineNumber = 0;
            while ((strLine = br.readLine()) != null) {
                lineNumber++;
                strLine = strLine.trim();
                if ( ! strLine.startsWith("#") && ! strLine.startsWith("Exec")) {
                    if (parserjob_percentile(lineNumber, strLine) != 0) {
                        System.err.println("HistogramLogAnalyzer: error in file: " + log_filename + " line " + lineNumber + ": " + strLine);
                        return 1; // error
                    }
                } else if (strLine.startsWith("#[Sampling start time:") || strLine.startsWith("#[StartTime:")) {
                    String printableSamplingStartTime = strLine.substring(2, strLine.length() - 1);
                    samplingStartTime = printableSamplingStartTime;
                }
            }
            instrm.close();
            db.close_db();
            return 0;
        } catch (Exception e) {
            System.err.println("HistogramLogAnalyzer: Input file error: " + log_filename);
            e.toString();
            e.printStackTrace();
            return 1;
        }
    }

    private int parserjob_percentile(long lineNumber, String line)
    {
        // jHiccup 1.2.2
        // Time: IntervalPercentiles:count ( 50% 90% Max ) TotalPercentiles:count ( 50% 90% 99% 99.9% 99.99% Max )
        // 181.050: I:951 (   1.048   1.056   1.160 ) T:951 (   1.048   1.056   1.056   1.144   1.160   1.160 )
        // jHiccup 1.3.2
        // Time: IntervalPercentiles:count ( 50% 90% Max ) TotalPercentiles:count ( 50% 90% 99% 99.9% 99.99% Max )
        // 1.108:   I:989 (   0.054   0.057   1.400 ) T:989 (   0.054   0.057   0.061   0.137   1.400   1.400 )
        // jHiccup 2.0.2
        // Time: IntervalPercentiles:count ( 50% 90% Max ) TotalPercentiles:count ( 50% 90% 99% 99.9% 99.99% Max )
        // 31.093:  I:950 (   0.000   0.000   0.000 ) T:950 (   0.000   0.000   0.000   0.000   0.000   0.000 )

        try {
            line = line.replaceAll("\\s+", " ");
            // 181.050: I:951 ( 1.048 1.056 1.160 ) T:951 ( 1.048 1.056 1.056 1.144 1.160 1.160 )
            parse_percentile(lineNumber, line);
            DBmgr.parser_percentile_insert(pDObj);
            return 0;
        } catch (Exception e) {
            System.err.println("HistogramLogAnalyzer: Parser error: parserjob_percentile line being parsed with percentile information: " + line);
            e.toString();
            e.printStackTrace();
            return 1;
        }
    }

    private void parse_percentile_header(long lineNumber, String line, String[] lineComponents) 
    {
        String jHiccupHeader = "Time: IntervalPercentiles:count ( 50% 90% Max ) TotalPercentiles:count ( 50% 90% 99% 99.9% 99.99% Max )";
        String[] jHiccupHeaderComponents = jHiccupHeader.split("[ ]");

        if (jHiccupHeaderComponents.length != lineComponents.length) {
            System.err.println("HistogramLogAnalyzer: PERCENTILE DATA CORRUPT - Graphed data will not be accurate");
            System.err.println("HistogramLogAnalyzer: Error in expected fields in log file at line: " + lineNumber);
            System.err.println("HistogramLogAnalyzer:   Expect:  " + jHiccupHeader);
            System.err.println("HistogramLogAnalyzer:   Current: " + line);
        } else {
            for (int i = 0; i < jHiccupHeaderComponents.length; i++) {
                if (! jHiccupHeaderComponents[i].trim().equals(lineComponents[i].trim())) {
                    System.err.println("HistogramLogAnalyzer: PERCENTILE DATA CORRUPT - Graphed data will not be accurate");
                    System.err.println("HistogramLogAnalyzer: Error in expected fields in log file at line: " + lineNumber);
                    System.err.println("HistogramLogAnalyzer:   Expect:  " + jHiccupHeaderComponents[i]);
                    System.err.println("HistogramLogAnalyzer:   Current: " + lineComponents[i]);
                }
            }
        }
        
    }

    private void parse_percentile(long lineNumber, String line)
    {
        // 181.050: I:951 ( 1.048 1.056 1.160 ) T:951 ( 1.048 1.056 1.056 1.144 1.160 1.160 )
        String b[] = line.split("[ ]");
        //    0       1   2   3     4     5   6   7   8   9    10    11    12    13    14  15
        // 181.050: I:951 ( 1.048 1.056 1.160 ) T:951 ( 1.048 1.056 1.056 1.144 1.160 1.160 )

        // Verify that the header is what we expect
        if (b[0].trim().equals("Time:")) {
            parse_percentile_header(lineNumber, line, b); 
            return;
        }

        try {
            String f = b[0].replaceFirst(":", "");
            //    f  
            // 181.050

            pDObj.percentile_elapsedTime = f;

            pDObj.percentile_ip_count    = b[1].split(":")[1];

            pDObj.percentile_ip_50    = b[3];
            pDObj.percentile_ip_90    = b[4];
            pDObj.percentile_ip_max   = b[5];

            pDObj.percentile_tp_count = b[7].split(":")[1];
            pDObj.percentile_tp_50    = b[9];
            pDObj.percentile_tp_90    = b[10];
            pDObj.percentile_tp_99    = b[11];
            pDObj.percentile_tp_999   = b[12];
            pDObj.percentile_tp_9999  = b[13];
            pDObj.percentile_tp_max   = b[14];

            // Check the time because there's a bug in jHiccup that results in the time value
            // value sometimes being mangled at some point in the collection of the data 20131114
            Double thisPercentileTime = Double.parseDouble(pDObj.percentile_elapsedTime);
            if (previousPercentileTime > thisPercentileTime) {
                System.err.println("HistogramLogAnalyzer: PERCENTILE DATA CORRUPT - Graphed data will not be accurate");
                System.err.println("HistogramLogAnalyzer: Error detected with log file's time at line: " + lineNumber);
                System.err.println("HistogramLogAnalyzer:     current  time: " + thisPercentileTime);
                System.err.println("HistogramLogAnalyzer:     previous time: " + previousPercentileTime);
                System.err.println("HistogramLogAnalyzer: Line with error: " + line);
            } else {
                if (thisPercentileTime > previousPercentileTime) {
                    previousPercentileTime = thisPercentileTime;
                }
            }

            // Check the maximum value because there's a bug in jHiccup that results in the max
            // value sometimes decreasing at some point in the collection of the data 20131114
            Double thisTotalPercentileMax = Double.parseDouble(pDObj.percentile_tp_max);
            if (thisTotalPercentileMax < totalPercentileMax) {
                System.err.println("HistogramLogAnalyzer: PERCENTILE DATA CORRUPT - Graphed data will not be accurate");
                System.err.println("HistogramLogAnalyzer: Error detected with maximum value at line: " + lineNumber);
                System.err.println("HistogramLogAnalyzer:     current  Max: " + thisTotalPercentileMax);
                System.err.println("HistogramLogAnalyzer:     previous Max: " + totalPercentileMax);
                System.err.println("HistogramLogAnalyzer: Line with error: " + line);
            } else {
                if (thisTotalPercentileMax > totalPercentileMax) {
                    totalPercentileMax = thisTotalPercentileMax;
                }
            }

        } catch (Exception except) {
            System.err.println("HistogramLogAnalyzer: PERCENTILE DATA CORRUPT - Graphed data will not be accurate");
            System.err.println("HistogramLogAnalyzer: Error detected with log file at line: " + lineNumber);
            System.err.println("HistogramLogAnalyzer: Line with error: " + line);
            except.toString();
            // except.printStackTrace();
        }
    }

    private void parse_histogram(String line) {
        String p[] = line.split(" ");
        pDObj.hst_value = Double.parseDouble(p[0]);
        pDObj.hst_percentile = Double.parseDouble(p[1]);
        pDObj.hst_totalcountincludingthisvalue = Double.parseDouble(p[2]);
    }

    private int parserjob_hgrm(String line, int lineNumber) {
        try {
            line = line.replaceAll("\\s+"," ");
            parse_histogram(line);
            DBmgr.parser_insert(pDObj);
            return 0;
        } catch (Exception except) {
            System.err.println("HistogramLogAnalyzer: Parser error: Histogram file at line number: " + lineNumber);
            System.err.println("HistogramLogAnalyzer: Parser error: Histogram file line: " + line);
            except.toString();
            // except.printStackTrace();
            return 1;
        }
    }
}
