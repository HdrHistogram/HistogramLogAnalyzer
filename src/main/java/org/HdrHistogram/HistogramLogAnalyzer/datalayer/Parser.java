/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.datalayer;

import org.HdrHistogram.*;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.DBConnect;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.ParserDataObjects;

import java.io.File;
import java.io.IOException;

public class Parser {
    private String hlogFileName;
    private String tag;
    private String startTime;
    private String endTime;

    private ParserDataObjects pDObj;
    public DBManager DBmgr;

    public Parser(DBConnect db, String hlogFileName, String tag, String startTime, String endTime) {
        this.hlogFileName = hlogFileName;
        this.tag = tag;
        this.startTime = startTime;
        this.endTime = endTime;

        pDObj = new ParserDataObjects();
        DBmgr = new DBManager(db);
    }

    private Double outputValueUnitRatio = 1000000.0; // default to msec units for output.
    private int percentilesOutputTicksPerHalf = 5;

    private boolean tagsMatch(String tagA, String tagB) {
        return tagA == null && tagB == null || tagA != null && tagA.equals(tagB);
    }

    private EncodableHistogram getNextIntervalHistogramForTag(HistogramLogReader reader)
    {
        EncodableHistogram histogram;
        if (startTime != null && endTime != null) {
            histogram = reader.nextIntervalHistogram(Double.parseDouble(startTime), Double.parseDouble(endTime));
        } else {
            histogram = reader.nextIntervalHistogram();
        }
        while (histogram != null && !tagsMatch(tag, histogram.getTag())) {
            if (startTime != null && endTime != null) {
                histogram = reader.nextIntervalHistogram(Double.parseDouble(startTime), Double.parseDouble(endTime));
            } else {
                histogram = reader.nextIntervalHistogram();
            }
        }
        return histogram;
    }

    public int execute() throws IOException {
        HistogramLogReader reader = new HistogramLogReader(new File(hlogFileName));
        EncodableHistogram histogram = getNextIntervalHistogramForTag(reader);
        AbstractHistogram accumulatedHistogram = null;

        if (histogram == null) {
            return 0; // no interval found
        }

        accumulatedHistogram = ((AbstractHistogram) histogram).copy();
        accumulatedHistogram.reset();
        accumulatedHistogram.setAutoResize(true);

        while (histogram != null) {
            accumulatedHistogram.add((AbstractHistogram) histogram);

            pDObj.percentile_elapsedTime = String.valueOf((histogram.getEndTimeStamp()/1000.0) - reader.getStartTimeSec());
            pDObj.percentile_ip_max      = String.valueOf(histogram.getMaxValueAsDouble()/outputValueUnitRatio);
            pDObj.percentile_tp_count    = String.valueOf(accumulatedHistogram.getTotalCount());
            pDObj.percentile_tp_50       = String.valueOf(accumulatedHistogram.getValueAtPercentile(50.0) / outputValueUnitRatio);
            pDObj.percentile_tp_90       = String.valueOf(accumulatedHistogram.getValueAtPercentile(90.0) / outputValueUnitRatio);
            pDObj.percentile_tp_99       = String.valueOf(accumulatedHistogram.getValueAtPercentile(99.0) / outputValueUnitRatio);
            pDObj.percentile_tp_999      = String.valueOf(accumulatedHistogram.getValueAtPercentile(99.9) / outputValueUnitRatio);
            pDObj.percentile_tp_9999     = String.valueOf(accumulatedHistogram.getValueAtPercentile(99.99) / outputValueUnitRatio);
            pDObj.percentile_tp_max      = String.valueOf(accumulatedHistogram.getMaxValue() / outputValueUnitRatio);

            DBmgr.parser_percentile_insert(pDObj);
            histogram = getNextIntervalHistogramForTag(reader);
        }

        for (HistogramIterationValue iterationValue : accumulatedHistogram.percentiles(percentilesOutputTicksPerHalf)) {
            pDObj.hst_value = iterationValue.getValueIteratedTo() / outputValueUnitRatio;
            pDObj.hst_percentile = iterationValue.getPercentileLevelIteratedTo() / 100.0D;
            pDObj.hst_totalcountincludingthisvalue = iterationValue.getTotalCountToThisValue();
            DBmgr.parser_insert(pDObj);
        }

        return 0;
    }

    public String getSamplingStartTime() {
        return startTime;
    }

}
