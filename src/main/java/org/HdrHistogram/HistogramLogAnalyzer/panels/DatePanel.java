/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.panels;

import org.HdrHistogram.HistogramLogAnalyzer.properties.DateProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class DatePanel extends JPanel {

    public DatePanel(JFrame mainframe, final DateProperties dateProperties) {
        setBorder(BorderFactory.createLineBorder(Color.BLACK));
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        final JCheckBox secondsCheckBox = new JCheckBox("Display time with seconds");
        final JCheckBox millisecondsCheckBox = new JCheckBox("Display time with milliseconds");
        final JCheckBox dateCheckBox = new JCheckBox("Show date");
        final JCheckBox timezoneCheckbox = new JCheckBox("Set time zone");
        final JComboBox<String> timezoneCombobox = new JComboBox();

        secondsCheckBox.setSelected(dateProperties.isShowSeconds());
        secondsCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                dateProperties.setShowSeconds(secondsCheckBox.isSelected());
                millisecondsCheckBox.setEnabled(secondsCheckBox.isSelected());
            }
        });

        millisecondsCheckBox.setEnabled(secondsCheckBox.isSelected());
        millisecondsCheckBox.setSelected(dateProperties.isShowMilliseconds());
        millisecondsCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                dateProperties.setShowMilliseconds(millisecondsCheckBox.isSelected());
            }
        });

        dateCheckBox.setSelected(dateProperties.isShowDate());
        dateCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                dateProperties.setShowDate(dateCheckBox.isSelected());
            }
        });

        timezoneCheckbox.setSelected(dateProperties.isOverrideTimezone());
        timezoneCheckbox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                timezoneCombobox.setEnabled(timezoneCheckbox.isSelected());
                dateProperties.setOverrideTimezone(timezoneCheckbox.isSelected());
                dateProperties.setUserTimezone((String)timezoneCombobox.getSelectedItem());
            }
        });

        for (String tz : dateProperties.getAllTimeZones()) {
            timezoneCombobox.addItem(tz);
        }
        String localtz = dateProperties.getLocalTimeZone();
        timezoneCombobox.setEnabled(timezoneCheckbox.isSelected());
        timezoneCombobox.setSelectedItem(localtz);
        timezoneCombobox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                dateProperties.setUserTimezone((String)timezoneCombobox.getSelectedItem());
            }
        });

        secondsCheckBox.setAlignmentX(LEFT_ALIGNMENT);
        millisecondsCheckBox.setAlignmentX(LEFT_ALIGNMENT);
        timezoneCheckbox.setAlignmentX(LEFT_ALIGNMENT);
        dateCheckBox.setAlignmentX(LEFT_ALIGNMENT);
        timezoneCombobox.setAlignmentX(LEFT_ALIGNMENT);

        panel.add(secondsCheckBox);
        panel.add(millisecondsCheckBox);
        panel.add(dateCheckBox);
        panel.add(timezoneCheckbox);
        panel.add(timezoneCombobox);

        add(panel);
    }
}