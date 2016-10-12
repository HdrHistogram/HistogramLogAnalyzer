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
    private String xml_SLAfilename = "SLAdetails.xml";

    public String db_insert_j_hst = "INSERT INTO j_hst ";
    public String db_create_j_hst = "CREATE TABLE j_hst(value REAL,Percentile REAL,TotalCountIncludingThisValue REAL, cvalue REAL);";
    public String db_insert_j_percentile = "INSERT INTO j_percentile ";
    public String db_create_j_percentile = "CREATE TABLE j_percentile(percentile_elapsedTime REAL,percentile_ip_count TEXT,percentile_ip_50 REAL,percentile_ip_90 REAL,percentile_ip_max REAL,percentile_tp_count TEXT,percentile_tp_50 REAL,percentile_tp_90 REAL,percentile_tp_99 REAL,percentile_tp_999 REAL,percentile_tp_9999 REAL,percentile_tp_max REAL);";

    public String db_create_j_sla_details = "CREATE TABLE j_sla_details(rowid INTEGER PRIMARY KEY AUTOINCREMENT,sla_percentile REAL,sla_cvalue REAL,sla_hiccuptimeinterval REAL);";

    private ArrayList<Double> percentile = new ArrayList<Double>();
    private ArrayList<Double> hiccupvalue = new ArrayList<Double>();

    public void prepare_SLA(DBConnect db, Double[] percentile, Double[] hiccupvalue) {
        if (Application.getJHiccupViewerConfiguration().verbose()) {
            Application.getApplicationLogger().entering("DBManager", "prepare_SLA");
            for (int j = 0; j < percentile.length; j++) {
                Application.getApplicationLogger().logp(Level.FINEST, "DBManager", "prepare_SLA", percentile[j] + " " + hiccupvalue[j]);
            }
        }

        try {
        	for (int i = 0; i < percentile.length; i++) {
				Double pr = percentile[i];
				Double cvalue = 1 / (1 - pr / 100);
				if (cvalue.isInfinite())
					cvalue = 1000 / (1 - (99.999) / 100);
				cvalue = (double) Math.round(cvalue);
				Double lw_hicpval = hiccupvalue[i];
				Double up_hicpval = (i + 1 < hiccupvalue.length) ? hiccupvalue[i + 1] : lw_hicpval;

				String x = "insert into j_sla_details(sla_percentile,sla_cvalue,sla_hiccuptimeinterval) VALUES(\"" + pr + "\",\""
						+ cvalue + "\",\"" + lw_hicpval + "\");";
				if (i != 0)
					db.statement.execute(x);
				x = "insert into j_sla_details(sla_percentile,sla_cvalue,sla_hiccuptimeinterval) VALUES(\"" + pr + "\",\""
						+ cvalue + "\",\"" + up_hicpval + "\");";
				if (i + 1 != hiccupvalue.length)
					db.statement.execute(x);
			}
        	/*
            if (percentile[0] != 0.0d) {
                String x = "insert into j_sla_details(sla_percentile,sla_cvalue,sla_hiccuptimeinterval) VALUES(\"" + 0.0 + "\",\""
                        + 1 + "\",\"" + hiccupvalue[0] + "\");";
                db.statement.execute(x);

                if (Application.getJHiccupViewerConfiguration().verbose()) {
                    Application.getApplicationLogger().logp(Level.FINEST, "DBManager", "prepare_SLA", "add: 0.0  1 " +  hiccupvalue[0]);
                }
            }
            double previousprValue = 0.0d;
            for (int i = 0; i < percentile.length; i++) {
                Double previousPercentile = percentile[i];
                if (Application.getJHiccupViewerConfiguration().verbose()) {
                    Application.getApplicationLogger().logp(Level.FINEST, "DBManager", "prepare_SLA", "iteration: " + i + " previousPercentile: " + previousPercentile);
                }

                if (previousprValue == 0.0d || previousPercentile < 100.0d) {
                    previousprValue = previousPercentile;
                }
                if (previousPercentile == 100.0d) {
                    previousPercentile = VALUE_FOR_100PERCENT;
                }
                Double cvalue = 1.0d / (double)(1.0 - (previousPercentile / 100.0d));
                if (cvalue.isInfinite()) {
                    //cvalue = 1.0 / (1.0d - (((100.0d - previousprValue)*0.9) + previousprValue) / 100.0d);
                }

                Double lw_hicpval = hiccupvalue[i];
                Double up_hicpval = (i + 1 < hiccupvalue.length) ? hiccupvalue[i + 1] : lw_hicpval;

                String x = "insert into j_sla_details(sla_percentile,sla_cvalue,sla_hiccuptimeinterval) VALUES(\"" + previousPercentile + "\",\""
                        + cvalue + "\",\"" + lw_hicpval + "\");";

                db.statement.execute(x);

                if (Application.getJHiccupViewerConfiguration().verbose()) {
                    Application.getApplicationLogger().logp(Level.FINEST, "DBManager", "prepare_SLA", "add: " + previousPercentile + " " + cvalue + " " + lw_hicpval);
                }

                if (i + 1 != hiccupvalue.length) {
                    x = "insert into j_sla_details(sla_percentile,sla_cvalue,sla_hiccuptimeinterval) VALUES(\"" + previousPercentile + "\",\""
                            + cvalue + "\",\"" + up_hicpval + "\");";
                    db.statement.execute(x);

                    if (Application.getJHiccupViewerConfiguration().verbose()) {
                        Application.getApplicationLogger().logp(Level.FINEST, "DBManager", "prepare_SLA", "add: " + previousPercentile + " " + cvalue + " " + up_hicpval);
                    }
                }
            }*/

        } catch (Exception except) {
            System.err.println("HistogramLogAnalyzer: SLA limits processing exception");
            System.err.println("  Message: " + except.getMessage());
            System.err.println("  Cause:   " + except.getCause());
            except.printStackTrace();
        }
    }

    private int read_SLAdetails(String xmlfilename) {
        percentile.add(0.0);
        hiccupvalue.add(0.0);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db1 = dbf.newDocumentBuilder();
            Document dom = db1.parse(getClass().getResourceAsStream(xmlfilename));
            NodeList nlist = dom.getElementsByTagName("SLAPercentile");
            for (int temp = 0; temp < nlist.getLength(); temp++) {
                Element e = (Element) nlist.item(temp);
                percentile.add(Double.parseDouble(nlist.item(temp).getFirstChild().getNodeValue()));
                hiccupvalue.add(Double.parseDouble(e.getAttribute("acceptablehicupp_msec")));

                if (Application.getJHiccupViewerConfiguration().verbose()) {
                    Application.getApplicationLogger().logp(Level.FINEST, "DBManager", "read_SLAdetails", 
                        "percentile: " + Double.parseDouble(nlist.item(temp).getFirstChild().getNodeValue()) + " hiccup value: " +
                        Double.parseDouble(e.getAttribute("acceptablehicupp_msec")));
                }
            }
            prepare_SLA(db, percentile.toArray(new Double[percentile.size()]),
                    hiccupvalue.toArray(new Double[hiccupvalue.size()]));
            percentile.clear();
            hiccupvalue.clear();
            return nlist.getLength();

        } catch (ParserConfigurationException pce) {
            System.err.println("HistogramLogAnalyzer: XML parsing exception for file: " + xmlfilename);
            System.err.println("  Message: " + pce.getMessage());
            System.err.println("  Cause:   " + pce.getCause());
            pce.printStackTrace();
        } catch (SAXException se) {
            System.err.println("HistogramLogAnalyzer: SAX parser exception while processing XML file: " + xmlfilename);
            System.err.println("  Message: " + se.getMessage());
            System.err.println("  Cause:   " + se.getCause());
            se.printStackTrace();
        } catch (IOException ioe) {
            System.err.println("HistogramLogAnalyzer: XML file I/O exception for file: " + xmlfilename);
            System.err.println("  Message: " + ioe.getMessage());
            System.err.println("  Cause:   " + ioe.getCause());
            ioe.printStackTrace();
            System.exit(1);
        }

        return 0;
    }

    private void create_base_tables() {
        try {
            db.statement.execute(db_create_j_hst);
            db.statement.execute(db_create_j_percentile);
            db.statement.execute(db_create_j_sla_details);

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
                    + "\", \"" + pDObj.hst_percentile + "\", \"" + pDObj.hst_totalcountincludingthisvalue + "\", \"" + cvalue + "\")";
            db.statement.execute(x);

            if (Application.getJHiccupViewerConfiguration().verbose()) {
                Application.getApplicationLogger().logp(Level.FINEST, "DBManager", "parser_insert",
                    "insert:" + pDObj.hst_value + " " + pDObj.hst_percentile + " " + pDObj.hst_totalcountincludingthisvalue + " " +  cvalue);
            }

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
                    "\",\"" + pDObj.percentile_tp_max + "\");";
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
            read_SLAdetails(xml_SLAfilename);
            this.db.close_db();
        } catch (Exception except) {
            System.err.println("HistogramLogAnalyzer: Database and SLA details exception");
            System.err.println("  Message: " + except.getMessage());
            System.err.println("  Cause:   " + except.getCause());
            except.printStackTrace();
        }
    }
}
