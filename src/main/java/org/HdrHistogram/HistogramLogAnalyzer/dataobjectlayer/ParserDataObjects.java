/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer;

public class ParserDataObjects {
	public double hst_value;
	public double hst_percentile;
	public double hst_totalcountincludingthisvalue;

	public String percentile_elapsedTime;
	public String percentile_ip_count;
	public String percentile_ip_50;
	public String percentile_ip_90;
	public String percentile_ip_max;
	public String percentile_tp_count;
	public String percentile_tp_50;
	public String percentile_tp_90;
	public String percentile_tp_99;
	public String percentile_tp_999;
	public String percentile_tp_9999;
	public String percentile_tp_max;
}