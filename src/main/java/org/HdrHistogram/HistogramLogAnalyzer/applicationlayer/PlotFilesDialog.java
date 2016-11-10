/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import javax.swing.*;
import java.awt.*;

class PlotFilesDialog {

    private static String getLabelText(boolean multipleFilesMode, boolean disableSameChart) {
        if (multipleFilesMode) {
            if (!disableSameChart) {
                return  "<html>" +
                        "New files can either be plotted in the same chart, the same tab or multiple tabs.<br>" +
                        "How would you like to plot files?" +
                        "</html>";
            } else {
                return  "<html>" +
                        "New files can either be plotted in the same tab or multiple tabs.<br>" +
                        "How would you like to plot files?" +
                        "</html>";
            }
        } else {
            return  "<html>" +
                    "New file can either be plotted in the current tab or new tab.<br>" +
                    "How would you like to plot file?" +
                    "</html>";
        }
    }

    static PlotFilesMode showDialog(Window owner, boolean multipleFilesMode) {
        return showDialog(owner, multipleFilesMode, false);
    }

    /*
     * Shows a dialog that asks user how to plot new files (in the same chart, the same tab or multiple tabs).
     *
     * multipleFilesMode true if user selected multiple files and the same chart option is needed
     *
     * disableSameChart true if one of selected files contains multiple tags and the same chart option isn't supported
     */
    static PlotFilesMode showDialog(Window owner, boolean multipleFilesMode, boolean disableSameChart) {
        Object[] message = new Object[1];
        JLabel label = new JLabel(getLabelText(multipleFilesMode, disableSameChart));
        message[0] = label;

        String[] options;
        if (multipleFilesMode) {
            if (!disableSameChart) {
                options = new String[]{
                    "Multiple tabs",
                    "Same tab",
                    "Same chart"
                };
            } else {
                options = new String[]{
                    "Multiple tabs",
                    "Same tab"
                };
            }
        } else {
            options = new String[]{
                    "New tab",
                    "Current tab"
            };
        }

        String title = (multipleFilesMode ? "Plot Files" : "Plot File");
        int result = JOptionPane.showOptionDialog(
                owner,
                message,
                title,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[options.length - 1]
        );

        switch(result) {
            case 0:
                return PlotFilesMode.MULTIPLE_TABS;
            case 1:
                return PlotFilesMode.SAME_TAB;
            case 2:
                return PlotFilesMode.SAME_CHART;
            default:
                break;

        }

        // User pressed Close button
        return PlotFilesMode.SAME_CHART;
    }
}
