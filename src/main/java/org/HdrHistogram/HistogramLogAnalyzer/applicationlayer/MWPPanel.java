/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.concurrent.TimeUnit;

class MWPPanel extends JPanel {

    private JFrame mainframe;
    private JPanel panel;
    private MWPProperties MWPProperties;

    private static final int TIMELINE_COLUMNS_NUMBER = 3;


    MWPPanel(JFrame mainframe, MWPProperties MWPProperties) {
        this.mainframe = mainframe;
        this.MWPProperties = MWPProperties;
        List<MWPProperties.MWPEntry> mwpEntries =
                MWPProperties.getAccessibleMWPEntry();

        setBorder(BorderFactory.createLineBorder(Color.BLACK));

        int rowcount = mwpEntries.size();
        panel = new JPanel(new GridLayout(rowcount + 1, TIMELINE_COLUMNS_NUMBER));
        panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        panel.add(new JLabel("Row"));
        panel.add(new JLabel("Percentile(%)"));
        panel.add(new JLabel("Window length(seconds)"));

        for (int i = 0; i < rowcount; i++) {
            panel.add(new JLabel(((Integer) (i + 1)).toString()));
            panel.add(new JTextField(mwpEntries.get(i).getPercentile().toString()));
            panel.add(new JTextField(Long.toString(
                    TimeUnit.MILLISECONDS.toSeconds(mwpEntries.get(i).getWindowLength()))));
        }

        JPanel ButtonSlab = new JPanel(new GridLayout(3, 2));
        JButton bttsla_add = new JButton("Add");
        bttsla_add.setMnemonic(KeyEvent.VK_A);
        bttsla_add.setIcon(new ImageIcon(getClass().getResource("icon_add.png")));
        bttsla_add.setToolTipText("Add an SLA Percentile");
        bttsla_add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sla_addrows();
            }
        });

        JButton bttsla_delete = new JButton("Delete");
        bttsla_delete.setMnemonic(KeyEvent.VK_D);
        bttsla_delete.setIcon(new ImageIcon(getClass().getResource("icon_delete.png")));
        bttsla_delete.setToolTipText("Delete an SLA Percentile");
        bttsla_delete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sla_delete();
            }
        });

        JButton bttsla_apply = new JButton("Apply");
        bttsla_apply.setIcon(new ImageIcon(getClass().getResource("icon_apply.png")));
        bttsla_apply.setMnemonic(KeyEvent.VK_P);
        bttsla_apply.setToolTipText("Apply to all graphs");
        bttsla_apply.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sla_apply();
            }
        });

        ButtonSlab.add(bttsla_add);
        ButtonSlab.add(bttsla_delete);
        ButtonSlab.add(bttsla_apply);

        add(panel);
        add(ButtonSlab);
    }

    private void sla_apply() {
        if (sla_isvalid() != 0) {
            if (sla_fillvalues()) {
                MWPProperties.applyMWP();
            }
        }
    }

    private void sla_addrows() {
        int rows = panel.getComponentCount() / TIMELINE_COLUMNS_NUMBER;
        sla_addat(rows);
    }

    private void sla_delete() {
        int rows = panel.getComponentCount() / TIMELINE_COLUMNS_NUMBER - 1;
        if (rows == 1) {
            JOptionPane.showMessageDialog(mainframe, "Last row, unable to delete");
            return;
        }

        String rtarget = JOptionPane.showInputDialog(null, "Which row to delete? (1-" + rows + "): ");
        if (rtarget != null) {
            int userrownumber = Integer.parseInt(rtarget);
            if (!(userrownumber > 0 && userrownumber <= rows)) {
                JOptionPane.showMessageDialog(mainframe, "Incorrect choice. Try again!" + userrownumber);
                return;
            }
            sla_deleteat(userrownumber);
        }
    }

    private int sla_isvalid() {
        int rowcount = panel.getComponentCount() / TIMELINE_COLUMNS_NUMBER;
        for (int i = TIMELINE_COLUMNS_NUMBER; i < rowcount * TIMELINE_COLUMNS_NUMBER; i = i + TIMELINE_COLUMNS_NUMBER) {
            JTextField ptxt = (JTextField) panel.getComponent(i + 1);
            JTextField hicptxt = (JTextField) panel.getComponent(i + 2);
            try {
                Double.parseDouble(ptxt.getText());
            } catch (Exception ee) {
                JOptionPane.showMessageDialog(mainframe, "Only numeric values allowed as percentile values. Try again!");
                ptxt.grabFocus();
                return 0;
            }
            try {
                Double.parseDouble(hicptxt.getText());
            } catch (Exception ee) {
                JOptionPane.showMessageDialog(mainframe, "Only numeric values allowed as window length. Try again!");
                hicptxt.grabFocus();
                return 0;
            }
        }
        return 1;
    }

    private void sla_deleteat(int at) {
        int index = (at + 1) * TIMELINE_COLUMNS_NUMBER;
        panel.remove(--index);
        panel.remove(--index);
        panel.remove(--index);

        sla_regain();
    }

    private void sla_addat(int at) {
        int index = (at) * TIMELINE_COLUMNS_NUMBER;
        panel.add(new JLabel("Sno"), index++);
        panel.add(new JTextField("*"), index++);
        panel.add(new JTextField("*"), index);

        sla_regain();
    }

    private void sla_regain() {
        int rowcount = panel.getComponentCount() / TIMELINE_COLUMNS_NUMBER;
        for (int i = 3; i < rowcount * TIMELINE_COLUMNS_NUMBER; i = i + TIMELINE_COLUMNS_NUMBER) {
            JLabel lbl = (JLabel) panel.getComponent(i);
            lbl.setText(((Integer) (i / TIMELINE_COLUMNS_NUMBER)).toString());
        }
        panel.setLayout(new GridLayout(rowcount, 3));
        this.repaint();
    }

    private boolean sla_fillvalues() {
        MWPProperties.reset();
        int rowcount = panel.getComponentCount() / 3;
        for (int i = TIMELINE_COLUMNS_NUMBER; i < rowcount * TIMELINE_COLUMNS_NUMBER; i = i + TIMELINE_COLUMNS_NUMBER) {
            JTextField ptxt = (JTextField) panel.getComponent(i + 1);
            JTextField htxt = (JTextField) panel.getComponent(i + 2);
            double windowLengthInSeconds = Double.parseDouble(htxt.getText());
            MWPProperties.MWPEntry MWPEntry =
                    new MWPProperties.MWPEntry(Double.parseDouble(ptxt.getText()),
                                                (long) (windowLengthInSeconds * 1000));
            MWPProperties.addMWPEntry(MWPEntry);
        }
        return true;
    }
}
