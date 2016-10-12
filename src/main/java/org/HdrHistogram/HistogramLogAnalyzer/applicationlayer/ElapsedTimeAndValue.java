/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

public class ElapsedTimeAndValue implements Comparable<ElapsedTimeAndValue> {

    private double elapsedTime = 0.0F;
    private double value       = 0.0F;
    private static boolean sortValuesDescending      = false;
    private static boolean sortElapsedTimesAscending = false;

    public ElapsedTimeAndValue(double elapsedTimeIn, double valueIn) {
        super();
        elapsedTime = elapsedTimeIn;
        value = valueIn;
    }

    public double getElapsedTime() {
        return elapsedTime;
    }

    public double getValue() {
        return value;
    }

    public static synchronized void setToSortValuesDescending() {
        sortValuesDescending      = true;
        sortElapsedTimesAscending = false;
    }

    public static synchronized void setToSortElapsedTimesAscending() {
        sortValuesDescending      = false;
        sortElapsedTimesAscending = true;
    }

    public int compareTo(ElapsedTimeAndValue elapsedTimeAndValueIn) { 
        double diff = 0.0F;
        if (sortValuesDescending) {
            diff = elapsedTimeAndValueIn.getValue() - value;
        } else if (sortElapsedTimesAscending) {
            diff = elapsedTime - elapsedTimeAndValueIn.getElapsedTime();
        }
        if (diff > 0.0F) {
            return 1;
        }
        else if (diff < 0.0F) {
            return -1; 
        }
        return 0;
    }
}
