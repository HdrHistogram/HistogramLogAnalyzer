/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;


public class AboutDialog extends JDialog {

    private static String _azulCopyrightString = "Copyright 2016, Azul Systems, Inc. All Rights Reserved.";
    private static String _licenseFilename = "/org/HdrHistogram/HistogramLogAnalyzer/applicationlayer/License.html";

    /**
     * @param parentFrame
     */
    public AboutDialog(JFrame parentFrame) {
        super(parentFrame);

        super.setSize(Math.max((int)(parentFrame.getWidth() * 0.6), 400), Math.max((int)(parentFrame.getHeight() * 0.6), 400));
        super.setModal(true);
        super.setLocationRelativeTo(parentFrame);
        super.setTitle("Help About Histogram Log Analyzer");
        super.getContentPane().add(getMainPane());
    }

    /**
     * @return
     */
    private JComponent getMainPane() {
        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setEditable(false);
        editorPane.setText(getAboutText());
        editorPane.setCaretPosition(0);
        JScrollPane pane = new JScrollPane(editorPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return pane;
    }

    private String getAboutText() {
        StringBuilder sb = new StringBuilder(1000);
        sb.append("<html>");
        sb.append("<h2>Histogram Log Analyzer</h2>");
        sb.append("Version: ").append(Version.version).append("<br/>");
        // sb.append("Release version: ").append(Version.RELEASE_VERSION).append("<br/>");
        sb.append("Build time: ").append(Version.build_time).append("<br/><br/>");
        sb.append(_azulCopyrightString).append("<br/>");
        try {
            String licenseFileLine = "";
            InputStream licenseStream = getClass().getResourceAsStream(_licenseFilename);
            BufferedReader br = new BufferedReader(new InputStreamReader(licenseStream, "US-ASCII"));
            while ((licenseFileLine = br.readLine()) != null) {
                sb.append(licenseFileLine);
            }
        } catch (Exception ex) {
            System.err.println("Unable to find the license file or there is an IO error: " + _licenseFilename);
            System.err.println(ex.getMessage());
        }
        sb.append("</html>");
        return sb.toString();
    }

}
