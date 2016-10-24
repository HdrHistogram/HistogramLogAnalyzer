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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Parser {
    private String hlogFileName;
    private String startTime;
    private String endTime;

    private ParserDataObjects pDObj;
    private DBManager DBmgr;
    private Map<String, AbstractHistogram> accumulatedHistograms;

    public Parser(DBConnect db, String hlogFileName, String startTime, String endTime) {
        this.hlogFileName = hlogFileName;
        this.startTime = startTime;
        this.endTime = endTime;

        pDObj = new ParserDataObjects();
        DBmgr = new DBManager(db);
    }

    private EncodableHistogram getNextIntervalHistogram(HistogramLogReader reader)
    {
        EncodableHistogram histogram;
        if (startTime != null && endTime != null) {
            histogram = reader.nextIntervalHistogram(Double.parseDouble(startTime), Double.parseDouble(endTime));
        } else {
            histogram = reader.nextIntervalHistogram();
        }
        return histogram;
    }

    public Set<String> getTags() {
        return accumulatedHistograms.keySet();
    }

    public void execute() throws IOException {
        HistogramLogReader reader = new HistogramLogReader(new File(hlogFileName));
        EncodableHistogram histogram = getNextIntervalHistogram(reader);
        accumulatedHistograms = new HashMap<String, AbstractHistogram>();

        if (histogram == null) {
            return; // no interval found
        }

        Double outputValueUnitRatio = 1000000.0;
        while (histogram != null) {
            AbstractHistogram accumulatedHistogram = accumulatedHistograms.get(histogram.getTag());
            if (accumulatedHistogram == null) {
                accumulatedHistogram = ((AbstractHistogram) histogram).copy();
                accumulatedHistogram.reset();
                accumulatedHistogram.setAutoResize(true);
                accumulatedHistograms.put(histogram.getTag(), accumulatedHistogram);
            }

            accumulatedHistogram.add((AbstractHistogram) histogram);

            pDObj.percentile_elapsedTime = String.valueOf((histogram.getEndTimeStamp()/1000.0) - reader.getStartTimeSec());
            pDObj.percentile_ip_max      = String.valueOf(histogram.getMaxValueAsDouble()/ outputValueUnitRatio);
            pDObj.percentile_tp_count    = String.valueOf(accumulatedHistogram.getTotalCount());
            pDObj.percentile_tp_50       = String.valueOf(accumulatedHistogram.getValueAtPercentile(50.0) / outputValueUnitRatio);
            pDObj.percentile_tp_90       = String.valueOf(accumulatedHistogram.getValueAtPercentile(90.0) / outputValueUnitRatio);
            pDObj.percentile_tp_99       = String.valueOf(accumulatedHistogram.getValueAtPercentile(99.0) / outputValueUnitRatio);
            pDObj.percentile_tp_999      = String.valueOf(accumulatedHistogram.getValueAtPercentile(99.9) / outputValueUnitRatio);
            pDObj.percentile_tp_9999     = String.valueOf(accumulatedHistogram.getValueAtPercentile(99.99) / outputValueUnitRatio);
            pDObj.percentile_tp_max      = String.valueOf(accumulatedHistogram.getMaxValue() / outputValueUnitRatio);
            pDObj.percentile_tag = histogram.getTag();

            DBmgr.parser_percentile_insert(pDObj);
            histogram = getNextIntervalHistogram(reader);
        }

        for (String tag : accumulatedHistograms.keySet()) {
            int percentilesOutputTicksPerHalf = 5;
            for (HistogramIterationValue iterationValue : accumulatedHistograms.get(tag).percentiles(percentilesOutputTicksPerHalf)) {
                pDObj.hst_value = iterationValue.getValueIteratedTo() / outputValueUnitRatio;
                pDObj.hst_percentile = iterationValue.getPercentileLevelIteratedTo() / 100.0D;
                pDObj.hst_totalcountincludingthisvalue = iterationValue.getTotalCountToThisValue();
                pDObj.hst_tag = tag;

                DBmgr.parser_insert(pDObj);
            }
        }

        return;
    }
}
