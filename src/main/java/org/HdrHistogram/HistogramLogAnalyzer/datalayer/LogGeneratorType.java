/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.datalayer;

public enum LogGeneratorType {

    CASSANDRA_STRESS("Cassandra Stress"),
    JHICCUP("jHiccup"),
    UNKNOWN("Unknown");

    private String description;

    LogGeneratorType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
