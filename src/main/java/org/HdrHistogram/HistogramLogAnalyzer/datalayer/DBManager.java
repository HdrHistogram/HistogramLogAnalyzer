/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.datalayer;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.HdrHistogram.HistogramLogAnalyzer.applicationlayer.Application;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.DBConnect;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.ParserDataObjects;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.util.logging.Level;

public class DBManager {

    public static final Double VALUE_FOR_100PERCENT = Double.valueOf(99.99999);

    private DBConnect db = null;

    public String db_insert_j_hst = "INSERT INTO j_hst ";
    public String db_create_j_hst = "CREATE TABLE j_hst(value REAL,Percentile REAL,TotalCountIncludingThisValue REAL, cvalue REAL, hst_tag REAL);";
    public String db_insert_j_percentile = "INSERT INTO j_percentile ";
    public String db_create_j_percentile = "CREATE TABLE j_percentile(percentile_elapsedTime REAL,percentile_ip_count TEXT,percentile_ip_50 REAL,percentile_ip_90 REAL,percentile_ip_max REAL,percentile_tp_count TEXT,percentile_tp_50 REAL,percentile_tp_90 REAL,percentile_tp_99 REAL,percentile_tp_999 REAL,percentile_tp_9999 REAL,percentile_tp_max REAL,percentile_tag REAL);";

    private void create_base_tables() {
        try {
            db.statement.execute(db_create_j_hst);
            db.statement.execute(db_create_j_percentile);

        } catch (Exception except) {
            System.err.println("HistogramLogAnalyzer: Database table creation exception");
            System.err.println("  Message: " + except.getMessage());
            System.err.println("  Cause:   " + except.getCause());
            except.printStackTrace();
        }
    }

    public void parser_insert(ParserDataObjects pDObj) {
        try {
            Double cvalue = 1.0d/(double)(1.0-pDObj.hst_percentile);
            if(cvalue.isInfinite())
                cvalue=(double)0;
            String x = db_insert_j_hst + "VALUES (\"" + pDObj.hst_value
                    + "\", \"" + pDObj.hst_percentile
                    + "\", \"" + pDObj.hst_totalcountincludingthisvalue
                    + "\", \"" + cvalue
                    + "\", \"" + pDObj.hst_tag + "\")";

            db.statement.execute(x);
        } catch (Exception except) {
            System.err.println("HistogramLogAnalyzer: Image generation exception");
            System.err.println("  Message: " + except.getMessage());
            System.err.println("  Cause:   " + except.getCause());
            except.printStackTrace();
        } finally {
            try {
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void parser_percentile_insert(ParserDataObjects pDObj) {
        try {
            String x = db_insert_j_percentile + "VALUES(\"" + pDObj.percentile_elapsedTime + "\"," + 
                    "\"" + pDObj.percentile_ip_count + 
                    "\",\"" + pDObj.percentile_ip_50 + 
                    "\",\"" + pDObj.percentile_ip_90 + 
                    "\",\"" + pDObj.percentile_ip_max + 
                    "\",\"" + pDObj.percentile_tp_count + 
                    "\",\"" + pDObj.percentile_tp_50 + 
                    "\",\"" + pDObj.percentile_tp_90 + 
                    "\",\"" + pDObj.percentile_tp_99 + 
                    "\",\"" + pDObj.percentile_tp_999 + 
                    "\",\"" + pDObj.percentile_tp_9999 + 
                    "\",\"" + pDObj.percentile_tp_max +
                    "\",\"" + pDObj.percentile_tag + "\");";
            db.statement.execute(x);

        } catch (Exception except) {
            System.err.println("HistogramLogAnalyzer: Database exception on percentile data insert");
            System.err.println("  Message: " + except.getMessage());
            System.err.println("  Cause:   " + except.getCause());
            except.printStackTrace();
        } finally {
            try {
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public DBManager(DBConnect db) {
        try {
            this.db = db;
            create_base_tables();
            this.db.close_db();
        } catch (Exception except) {
            System.err.println("HistogramLogAnalyzer: Database and SLA details exception");
            System.err.println("  Message: " + except.getMessage());
            System.err.println("  Cause:   " + except.getCause());
            except.printStackTrace();
        }
    }
}
