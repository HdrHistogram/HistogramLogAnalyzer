/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SLAProperties {

    private List<SLAEntry> slaEntries = new ArrayList<SLAEntry>();

    private boolean isSLAVisible = false;

    public static class SLAEntry {
        private Double percentile;
        private Double latency;

        public SLAEntry(Double percentile, Double latency) {
            this.percentile = percentile;
            this.latency = latency;
        }

        public Double getPercentile() {
            return percentile;
        }

        public Double getLatency() {
            return latency;
        }

        // 1/1-percentile
        public Double getPercentileCount() {
            Double cvalue = 1 / (1 - getPercentile() / 100);
            if (cvalue.isInfinite()) {
                cvalue = 1000 / (1 - (99.999) / 100);
            }
            cvalue = (double) Math.round(cvalue);
            return cvalue;
        }
    }

    private String SLA_FILENAME = "SLAdetails.xml";

    public SLAProperties() {
        try {
            readPropertiesFromFile();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
    }

    private void readPropertiesFromFile() throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db1 = dbf.newDocumentBuilder();
        InputStream is = getClass().getResourceAsStream(SLA_FILENAME);
        Document dom = db1.parse(is);
        NodeList nlist = dom.getElementsByTagName("SLAPercentile");
        for (int temp = 0; temp < nlist.getLength(); temp++) {
            Element e = (Element) nlist.item(temp);
            Double percentile = Double.parseDouble(nlist.item(temp).getFirstChild().getNodeValue());
            Double latency = Double.parseDouble(e.getAttribute("acceptablehicupp_msec"));
            slaEntries.add(new SLAEntry(percentile, latency));
        }
    }

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public boolean isSLAVisible() {
        return isSLAVisible;
    }

    public void toggleSLAVisibility(boolean newValue) {
        isSLAVisible = newValue;
        pcs.firePropertyChange("slaShow", !newValue, newValue);
    }

    public void resetSLA() {
        pcs.firePropertyChange("slaReset", false, true);
    }

    public List<SLAEntry> getSLAEntries() {
        return slaEntries;
    }

    public void clear() {
        slaEntries.clear();
    }

    public void addSLAEntry(SLAEntry slaEntry) {
        slaEntries.add(slaEntry);
    }
}
