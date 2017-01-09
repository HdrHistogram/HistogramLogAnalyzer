package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import org.HdrHistogram.HistogramLogAnalyzer.datalayer.HistogramModel;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.MaxPercentileIterator;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.TimelineIterator;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.PercentileObject;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.TimelineObject;

import java.io.IOException;
import java.util.*;

class PrintStatistics {

    // prints statistics to console
    static void print(HistogramModel histogramModel) throws IOException {
        for (String tag : histogramModel.getTags()) {
            print(histogramModel, tag);
        }
    }

    static void print(HistogramModel histogramModel, String tag) throws IOException {
        String tagString = (tag != null) ? "[tag=" + tag + "]" : "";
        System.out.println("LogFile"+ tagString + ": " + histogramModel.getInputFileName());

        double startTime = histogramModel.getLatestStartTime();
        System.out.format(Locale.US, "StartTime: %.3f (seconds since epoch), %s\n",
                            startTime, (new Date((long) (startTime * 1000))).toString());

        // 99.99'ile value
        double valueFor9999 = 0;

        Iterator<PercentileObject> pi = histogramModel.listHPLPercentileObjects(tag);
        while (pi.hasNext()) {
            PercentileObject po = pi.next();
            String percentileString = String.valueOf(po.getPercentileValue() * 100);
            if ("99.99".equals(percentileString)) {
                valueFor9999 = po.getLatencyAxisValue();
            }
            percentileString = !percentileString.contains(".") ?
                    percentileString : percentileString.replaceAll("0*$", "").replaceAll("\\.$", "");
            percentileString = percentileString + "th:";
            System.out.format("%-8s" + " %7.3f%n", percentileString, po.getLatencyAxisValue());
        }

        // number of latency points greater than 99.99'ile
        int counter = 0;
        MWPProperties.MWPEntry mwpEntry = MWPProperties.getDefaultMWPEntry();
        TimelineIterator ti = histogramModel.listTimelineObjects(false, tag, mwpEntry);
        while (ti.hasNext()) {
            TimelineObject to = ti.next();
            if (to.getLatencyAxisValue() > valueFor9999) {
                counter++;
            }
        }
        System.out.format("NumberGreaterThan99.99: %d%n", counter);

        // maximum latency value
        Double maxLatencyAxisValue = 0.0;
        MaxPercentileIterator mpi = histogramModel.listMaxPercentileObjects(tag);
        PercentileObject mpo;
        while (mpi.hasNext()) {
            mpo = mpi.next();
            maxLatencyAxisValue = Math.max(maxLatencyAxisValue, mpo.getLatencyAxisValue());
        }
        System.out.format("Maximum: %7.3f%n", maxLatencyAxisValue);

        // top N (10 by default) latency points on timeline chart (sort by timeline)
        List<TimelineObject> topLatencyObjects = new ArrayList<>();
        ti = histogramModel.listTimelineObjects(false, tag, mwpEntry,
                Configuration.getInstance().getNlhValue());
        while (ti.hasNext()) {
            topLatencyObjects.add(ti.next());
        }

        Collections.sort(topLatencyObjects, new Comparator<TimelineObject>() {
            @Override
            public int compare(TimelineObject o1, TimelineObject o2) {
                return Double.compare(o1.getTimelineAxisValue(), o2.getTimelineAxisValue());
            }
        });
        counter = 0;
        for (TimelineObject to : topLatencyObjects) {
            System.out.format("%d %7.3f %7.3f%n", counter, to.getTimelineAxisValue(), to.getLatencyAxisValue());
            counter++;
        }
        System.out.println();

    }
}
