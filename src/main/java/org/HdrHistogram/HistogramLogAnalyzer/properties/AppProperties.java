/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.properties;

public class AppProperties {

    private SLAProperties slaProperties = new SLAProperties();
    private MWPProperties mwpProperties = new MWPProperties();
    private HPLProperties hplProperties = new HPLProperties();
    private ViewProperties viewProperties = new ViewProperties();
    private DateProperties dateProperties = new DateProperties();

    public SLAProperties getSlaProperties() {
        return slaProperties;
    }

    public MWPProperties getMwpProperties() {
        return mwpProperties;
    }

    public HPLProperties getHplProperties() {
        return hplProperties;
    }

    public ViewProperties getViewProperties() {
        return viewProperties;
    }

    public DateProperties getDateProperties() {
        return dateProperties;
    }

}
