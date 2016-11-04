/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.datalayer;

import org.HdrHistogram.*;
import org.HdrHistogram.HistogramLogAnalyzer.applicationlayer.MWPProperties;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class HistogramModel {
    private String inputFileName;
    private String startTime;
    private String endTime;

    private MWPProperties mwpProperties;
    private Set<String> tags;

    private DBManager dbManager;

    public HistogramModel(String inputFileName, String startTime, String endTime, MWPProperties mwpProperties)
            throws IOException
    {
        this.inputFileName = inputFileName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.mwpProperties = mwpProperties;
        this.tags = TagsHelper.listTags(inputFileName);
        this.dbManager = new DBManager();

        init();
    }

    public String getInputFileName() {
        return inputFileName;
    }

    public Set<String> getTags() {
        return tags;
    }

    public MWPProperties getMwpProperties() {
        return mwpProperties;
    }

    private boolean compareTags(String tag, String hTag) {
        return (tag == null ? hTag == null : tag.equals(hTag));
    }

    private EncodableHistogram getIntervalHistogram(HistogramLogReader reader, String tag) {
        EncodableHistogram histogram;
        histogram = getIntervalHistogram(reader);
        while (histogram != null && !compareTags(tag, histogram.getTag())) {
            histogram = getIntervalHistogram(reader);
        }
        return histogram;
    }

    private EncodableHistogram getIntervalHistogram(HistogramLogReader reader) {
        EncodableHistogram histogram;
        if (startTime != null && endTime != null) {
            histogram = reader.nextIntervalHistogram(Double.parseDouble(startTime), Double.parseDouble(endTime));
        } else {
            histogram = reader.nextIntervalHistogram();
        }
        return histogram;
    }

    /*
     * fill database with data
     */
    private void init() throws IOException {
        for (String tag : tags) {
            List<MWPProperties.MWPEntry> mwpEntries = mwpProperties.getMWPEntries();
            for (MWPProperties.MWPEntry mwpEntry : mwpEntries) {
                init(tag, mwpEntry);
            }
            // always build data for non-"moving window" (default MWP) entry, needed for percentile chart
            if (!mwpEntries.contains(MWPProperties.getDefaultMWPEntry())) {
                init(tag, MWPProperties.getDefaultMWPEntry());
            }
        }
    }

    private void init(String tag, MWPProperties.MWPEntry mwpEntry)
            throws IOException
    {
        HistogramLogReader reader = new HistogramLogReader(new File(inputFileName));
        EncodableHistogram intervalHistogram = getIntervalHistogram(reader, tag);

        if (intervalHistogram == null) {
            return; // no interval found
        }

        boolean isMovingWindow = !MWPProperties.getDefaultMWPEntry().equals(mwpEntry);
        Double movingWindowPercentile = mwpEntry.getPercentile();
        int movingWincowIntervalCount = mwpEntry.getIntervalCount();

        Histogram accumulatedRegularHistogram = null;
        DoubleHistogram accumulatedDoubleHistogram = null;

        EncodableHistogram[] movingWindow = new EncodableHistogram[mwpEntry.getIntervalCount()];
        EncodableHistogram movingWindowSumHistogram;
        int movingWindowIndex = 0;

        if (intervalHistogram instanceof DoubleHistogram) {
            accumulatedDoubleHistogram = ((DoubleHistogram) intervalHistogram).copy();
            accumulatedDoubleHistogram.reset();
            accumulatedDoubleHistogram.setAutoResize(true);
            movingWindowSumHistogram = new DoubleHistogram(3);
        } else {
            accumulatedRegularHistogram = ((Histogram ) intervalHistogram).copy();
            accumulatedRegularHistogram.reset();
            accumulatedRegularHistogram.setAutoResize(true);
            movingWindowSumHistogram = new Histogram(3);
        }

        Double outputValueUnitRatio = 1000000.0;
        while (intervalHistogram != null) {

            if (intervalHistogram instanceof DoubleHistogram) {
                if (accumulatedDoubleHistogram == null) {
                    throw new IllegalStateException("Encountered a DoubleHistogram line in a log of Histograms.");
                }
                accumulatedDoubleHistogram.add((DoubleHistogram) intervalHistogram);
            } else {
                if (accumulatedRegularHistogram == null) {
                    throw new IllegalStateException("Encountered a Histogram line in a log of DoubleHistograms.");
                }
                accumulatedRegularHistogram.add((Histogram) intervalHistogram);
            }

            if (isMovingWindow) {
                EncodableHistogram prevHist = movingWindow[movingWindowIndex];
                movingWindow[movingWindowIndex] = intervalHistogram;

                if (movingWindowSumHistogram instanceof DoubleHistogram) {
                    ((DoubleHistogram) movingWindowSumHistogram).add((DoubleHistogram) intervalHistogram);
                    if (prevHist != null) {
                        ((DoubleHistogram) movingWindowSumHistogram).subtract((DoubleHistogram) prevHist);
                    }
                } else {
                    ((Histogram) movingWindowSumHistogram).add((Histogram) intervalHistogram);
                    if (prevHist != null) {
                        ((Histogram) movingWindowSumHistogram).subtract((Histogram) prevHist);
                    }
                }

                movingWindowIndex++;
                movingWindowIndex %= movingWincowIntervalCount;
            }

            double timelineAxisValue = (intervalHistogram.getEndTimeStamp()/1000.0) - reader.getStartTimeSec();
            double latencyAxisValue;

            if (isMovingWindow) {
                if (intervalHistogram instanceof DoubleHistogram) {
                    latencyAxisValue = ((DoubleHistogram) movingWindowSumHistogram).getValueAtPercentile(movingWindowPercentile) / outputValueUnitRatio;
                } else {
                    latencyAxisValue = ((Histogram) movingWindowSumHistogram).getValueAtPercentile(movingWindowPercentile) / outputValueUnitRatio;
                }
            } else {
                latencyAxisValue = intervalHistogram.getMaxValueAsDouble()/ outputValueUnitRatio;
            }

            String moving_window_percentile     = String.valueOf(movingWindowPercentile);
            String moving_window_interval_count = String.valueOf(movingWincowIntervalCount);

            dbManager.insertTimelineObject(
                    new TimelineObject(timelineAxisValue, latencyAxisValue,
                                       tag, moving_window_percentile, moving_window_interval_count));

            intervalHistogram = getIntervalHistogram(reader, tag);
        }

        int percentilesOutputTicksPerHalf = 5;

        // build percentile chart for default MWP entry (non-moving window) only
        if (!isMovingWindow) {
            if (accumulatedRegularHistogram != null) {
                for (HistogramIterationValue iterationValue : accumulatedRegularHistogram.percentiles(percentilesOutputTicksPerHalf)) {
                    double value = iterationValue.getValueIteratedTo() / outputValueUnitRatio;
                    double percentile = iterationValue.getPercentileLevelIteratedTo() / 100.0D;

                    dbManager.insertPercentileObject(
                            new PercentileObject(value, percentile, tag)
                    );
                }
            } else {
                for (DoubleHistogramIterationValue iterationValue : accumulatedDoubleHistogram.percentiles(percentilesOutputTicksPerHalf)) {
                    double value = iterationValue.getValueIteratedTo() / outputValueUnitRatio;
                    double percentile = iterationValue.getPercentileLevelIteratedTo() / 100.0D;

                    dbManager.insertPercentileObject(
                            new PercentileObject(value, percentile, tag)
                    );
                }
            }
        }
    }

    /*
     * queries
     */
    public TimelineIterator listTimelineObjects(boolean multipleTags, String tag, MWPProperties.MWPEntry mwpEntry) {
        return dbManager.listTimelineObjects(multipleTags, tag, mwpEntry);
    }

    public PercentileIterator listPercentileObjects(String tag, PercentileObject maxPO) {
        return dbManager.listPercentileObjects(tag, maxPO);
    }

    public MaxPercentileIterator listMaxPercentileObjects(String tag) {
        return dbManager.listMaxPercentileObjects(tag);
    }

}
