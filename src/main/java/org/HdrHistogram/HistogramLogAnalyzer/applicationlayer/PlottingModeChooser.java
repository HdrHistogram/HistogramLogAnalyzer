/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class PlottingModeChooser {

    private final static String[] MODE_DESCRIPTIONS = new String[] {
            "Plot %s in the same chart",
            "Plot %s in the same tab",
            "Plot %s in %s"
    };

    private final static String[] MODE_IMAGE_FILESNAMES = new String[] {
            "samechart.png",
            "sametab.png",
            "multipletabs.png"
    };

    enum PlottingMode {
        SAME_CHART(MODE_DESCRIPTIONS[0], MODE_IMAGE_FILESNAMES[0]),
        SAME_TAB(MODE_DESCRIPTIONS[1], MODE_IMAGE_FILESNAMES[1]),
        NEW_TAB(MODE_DESCRIPTIONS[2], MODE_IMAGE_FILESNAMES[2]);

        private final String description;
        private final String imageFileName;

        PlottingMode(String description, String imageFileName) {
            this.description = description;
            this.imageFileName = imageFileName;
        }

        String getDescription() {
            return description;
        }

        String getImageFileName() {
            return imageFileName;
        }
    }

    private static class RadioButtonPanel extends JPanel {
        private static PlottingMode selectedPlottingMode;

        RadioButtonPanel(final PlottingMode plottingMode, ButtonGroup group,
                         final boolean selected, boolean multipleFiles) {
            this.setLayout(new BorderLayout());

            String buttonText = String.format(plottingMode.getDescription(),
                    multipleFiles ? "files" : "file",
                    multipleFiles ? "multiple tabs" : "new tab");
            buttonText = String.format("%-35s", buttonText);
            JRadioButton radioButton = new JRadioButton(buttonText);
            if (selected) {
                selectedPlottingMode = plottingMode;
            }
            radioButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    selectedPlottingMode = plottingMode;
                }
            });
            this.add(radioButton, BorderLayout.WEST);
            group.add(radioButton);
            radioButton.setSelected(selected);
            ImageIcon ii = new ImageIcon(getClass().getResource(plottingMode.getImageFileName()));
            this.add(new JLabel(ii), BorderLayout.EAST);
        }

        static PlottingMode getSelectedPlottingMode() {
            return selectedPlottingMode;
        }
    }


    static PlottingMode showChooser(Application app, boolean multipleFiles, PlottingMode... plottingModes) {
        JPanel panel = new JPanel();
        ButtonGroup group = new ButtonGroup();
        panel.setLayout(new GridLayout(plottingModes.length, 1));
        for (int i = 0; i < plottingModes.length; i++) {
            RadioButtonPanel radioButtonPanel =
                    new RadioButtonPanel(plottingModes[i], group, i == 0, multipleFiles);
            radioButtonPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            panel.add(radioButtonPanel);

        }
        String QUESTION_STRING = "Choose mode";
        JOptionPane.showOptionDialog(app.getMainFrame(), panel, QUESTION_STRING,
                JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION,
                null, null, null);

        return RadioButtonPanel.getSelectedPlottingMode();
    }


}
