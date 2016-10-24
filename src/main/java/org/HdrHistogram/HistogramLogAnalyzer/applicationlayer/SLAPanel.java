/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import org.HdrHistogram.HistogramLogAnalyzer.applicationlayer.SLAProperties.SLAEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.List;

class SLAPanel extends JPanel {

    private JFrame mainframe;
    private JPanel SLAslab;
    private SLAProperties slaProperties;

    private static final int SLA_COLUMNS_NUMBER = 3;

    public SLAPanel(JFrame mainframe, SLAProperties slaProperties)
    {
        this.mainframe = mainframe;
        this.slaProperties = slaProperties;
        List<SLAEntry> slaEntries = slaProperties.getSLAEntries();

        setBorder(BorderFactory.createLineBorder(Color.BLACK));

        int rowcount = slaEntries.size();
        SLAslab = new JPanel(new GridLayout(rowcount + 1, SLA_COLUMNS_NUMBER));
        SLAslab.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        SLAslab.add(new JLabel("Row"));
        SLAslab.add(new JLabel("Percentile(%)"));
        SLAslab.add(new JLabel("Hiccup(msec)"));

        for (int i = 0; i < rowcount; i++) {
            SLAslab.add(new JLabel(((Integer) (i + 1)).toString()));
            SLAslab.add(new JTextField(slaEntries.get(i).getPercentile().toString()));
            SLAslab.add(new JTextField(slaEntries.get(i).getLatency().toString()));
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

        add(SLAslab);
        add(ButtonSlab);
    }

    private void sla_apply() {
        if (sla_isvalid() != 0) {
            if (sla_fillvalues()) {
                slaProperties.resetSLA();
                JOptionPane.showMessageDialog(mainframe, "Updated!");
            }
        }
    }

    private void sla_addrows() {
        int rows = SLAslab.getComponentCount() / SLA_COLUMNS_NUMBER - 1;
        String rtarget = JOptionPane.showInputDialog(null, "Add after which row number? (0-" + (rows - 1) + "): ", "jHiccup", 1);
        if (rtarget != null) {
            int userrownumber = Integer.parseInt(rtarget);
            if (!(userrownumber >= 0 && userrownumber < rows)) {
                JOptionPane.showMessageDialog(mainframe, "Incorrect choice. Try again! " + userrownumber, "jHiccup", 1);
                return;
            }
            sla_addat(userrownumber + 1);
        }
    }

    private void sla_delete() {
        int rows = SLAslab.getComponentCount() / SLA_COLUMNS_NUMBER - 1;
        String rtarget = JOptionPane.showInputDialog(null, "Which row to delete? (1-" + (rows-1) + "): ", "jHiccup", 1);
        if (rtarget != null) {
            int userrownumber = Integer.parseInt(rtarget);
            if (!(userrownumber > 0 && userrownumber < rows)) {
                JOptionPane.showMessageDialog(mainframe, "Incorrect choice. Try again!" + userrownumber, "jHiccup", 1);
                return;
            }
            sla_deleteat(userrownumber);
        }
    }

    private int sla_isvalid() {
        int rowcount = SLAslab.getComponentCount() / SLA_COLUMNS_NUMBER;
        for (int i = SLA_COLUMNS_NUMBER; i < rowcount * SLA_COLUMNS_NUMBER; i = i + SLA_COLUMNS_NUMBER) {
            JTextField ptxt = (JTextField) SLAslab.getComponent(i + 1);
            JTextField hicptxt = (JTextField) SLAslab.getComponent(i + 2);
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
                JOptionPane.showMessageDialog(mainframe, "Only numeric values allowed as latency pause times. Try again!");
                hicptxt.grabFocus();
                return 0;
            }
        }
        return 1;
    }

    private void sla_deleteat(int at) {
        int index = (at + 1) * SLA_COLUMNS_NUMBER;
        SLAslab.remove(index--);
        SLAslab.remove(index--);
        SLAslab.remove(index);

        sla_regain();
    }

    private void sla_addat(int at) {
        int index = (at) * SLA_COLUMNS_NUMBER;
        SLAslab.add(new JLabel("Sno"), index++);
        SLAslab.add(new JTextField("*"), index++);
        SLAslab.add(new JTextField("*"), index);

        sla_regain();
    }

    private void sla_regain() {
        int rowcount = SLAslab.getComponentCount() / SLA_COLUMNS_NUMBER;
        for (int i = 3; i < rowcount * SLA_COLUMNS_NUMBER; i = i + SLA_COLUMNS_NUMBER) {
            JLabel lbl = (JLabel) SLAslab.getComponent(i);
            lbl.setText(((Integer) (i / SLA_COLUMNS_NUMBER)).toString());
        }
        SLAslab.setLayout(new GridLayout(rowcount, 3));
    }

    private boolean sla_fillvalues() {
        slaProperties.clear();
        int rowcount = SLAslab.getComponentCount() / 3;
        for (int i = SLA_COLUMNS_NUMBER; i < rowcount * SLA_COLUMNS_NUMBER; i = i
                + SLA_COLUMNS_NUMBER) {
            JTextField ptxt = (JTextField) SLAslab.getComponent(i + 1);
            JTextField htxt = (JTextField) SLAslab.getComponent(i + 2);
            SLAEntry slaEntry = new SLAEntry(Double.parseDouble(ptxt.getText()), Double.parseDouble(htxt.getText()));
            slaProperties.addSLAEntry(slaEntry);
        }
        return true;
    }

}
