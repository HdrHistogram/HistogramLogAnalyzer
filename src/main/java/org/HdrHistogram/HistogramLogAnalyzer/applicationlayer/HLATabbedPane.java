/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import org.HdrHistogram.HistogramLogAnalyzer.datalayer.TagsHelper;
import org.HdrHistogram.HistogramLogAnalyzer.panels.MWPPanel;
import org.HdrHistogram.HistogramLogAnalyzer.panels.SLAPanel;
import org.HdrHistogram.HistogramLogAnalyzer.panels.DatePanel;

import javax.swing.*;
import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

public class HLATabbedPane extends DnDTabbedPane {

    private TabsListener tabsListener = null;

    HLATabbedPane(TabsListener tabsListener) {
        super();
        this.tabsListener = tabsListener;
    }

    void plotInputFiles(String[] inputFileNames, Application app) throws IOException {
        boolean multipleFiles = inputFileNames.length > 1;
        boolean firstFile = getTabCount() == 0;
        boolean masterTab = isMasterTabCurrent();
        boolean needNewTab = firstFile || masterTab;

        PlotFilesMode mode;
        if (!multipleFiles) {
            HLAPanel latencyPanel = new HLAPanel(inputFileNames, app.getAppProperties());
            if (needNewTab) {
                add_to_newtab(latencyPanel);
            } else {
                mode = PlotFilesDialog.showDialog(app.getMainFrame(), false);
                if (mode == PlotFilesMode.SAME_TAB) {
                    add_to_currenttab(latencyPanel);
                } else if (mode == PlotFilesMode.MULTIPLE_TABS) {
                    add_to_newtab(latencyPanel);
                }
            }
        } else {
            // tool supports plotting multiple (only untagged) files in the same chart
            if (!containsTaggedFile(inputFileNames)) {
                mode = PlotFilesDialog.showDialog(app.getMainFrame(), true);
            } else {
                mode = PlotFilesDialog.showDialog(app.getMainFrame(), true, true);
            }

            if (mode == PlotFilesMode.SAME_CHART) {
                HLAPanel latencyPanel = new HLAPanel(inputFileNames, app.getAppProperties());
                if (needNewTab) {
                    add_to_newtab(latencyPanel);
                } else {
                    add_to_currenttab(latencyPanel);
                }
            } else {
                for (String inputFileName : inputFileNames) {
                    HLAPanel latencyPanel = new HLAPanel(inputFileName, app.getAppProperties());
                    if (mode == PlotFilesMode.SAME_TAB && !needNewTab) {
                        add_to_currenttab(latencyPanel);
                    } else {
                        add_to_newtab(latencyPanel);
                        needNewTab = false;
                    }
                }

            }
        }
    }

    private boolean containsTaggedFile(String[] inputFileNames) throws FileNotFoundException {
        for (String inputFileName : inputFileNames) {
            Set<String> tags = TagsHelper.listTags(inputFileName);
            if (tags.size() > 1) {
                return true;
            }
        }
        return false;
    }

    private void checkFirstTabOpened() {
        if (getTabCount() == 1) {
            tabsListener.firstTabOpened();
        }
    }

    private void add_to_newtab(HLAPanel latencyPanel) {
        String tabTitle = latencyPanel.getTabTitle();
        insertTab(tabTitle, null, tab_builder(latencyPanel),
                getMultiLineTooltipText(latencyPanel.getTooltipTexts()), getTabCount());
        setTabComponentAt(getTabCount() - 1, new TabCloseComponent(tabTitle, this, tabsListener));
        checkFirstTabOpened();
        setSelectedIndex(getTabCount() - 1);
    }

    private String updateMultilineTooltipText(String oldTooltipText, String[] tooltipTexts) {
        String tooltipText = "";
        for (String tt : tooltipTexts) {
            tooltipText += "<br>" + tt;
        }
        return oldTooltipText.replaceAll("</html>", tooltipText +"</html>");
    }

    private JPanel tab_builder(HLAPanel latencyPanel) {
        JPanel coverPanel = new JPanel(new GridLayout(1,1));
        coverPanel.add(latencyPanel);
        return coverPanel;
    }

    private void add_to_currenttab(HLAPanel latencyPanel) {
        JPanel p1 = (JPanel) getSelectedComponent();
        p1.add(latencyPanel);
        p1.setLayout(new GridLayout(1,2));

        // update tooltip of the current tab
        String newTooltipText =
                updateMultilineTooltipText(getToolTipTextAt(getSelectedIndex()), latencyPanel.getTooltipTexts());
        setToolTipTextAt(getSelectedIndex(), newTooltipText);
    }

    // multi-line tooltip needs to be wrapped in html tags
    private String getMultiLineTooltipText(String[] tooltipTexts) {
        String ret = "<html>";
        for (int i = 0; i < tooltipTexts.length; i++) {
            if (i != 0) {
                ret += "<br>";
            }
            ret += tooltipTexts[i];
        }
        ret += "</html>";
        return ret;
    }

    /*
     * SLA/MWP-related methods
     */
    boolean isSLAMasterTabOpen() {
        if (getTabCount() == 0) {
            return false;
        }
        for (int i = 0; i < getTabCount(); i++) {
            if (getTitleAt(i).contains(Application.SLA_MASTER_TABNAME)) {
                return true;
            }
        }
        return false;
    }

    boolean isMWPMasterTabOpen() {
        if (getTabCount() == 0) {
            return false;
        }
        for (int i = 0; i < getTabCount(); i++) {
            if (getTitleAt(i).contains(Application.MWP_MASTER_TABNAME)) {
                return true;
            }
        }
        return false;
    }

    boolean isTimelineMasterTabOpen() {
        if (getTabCount() == 0) {
            return false;
        }
        for (int i = 0; i < getTabCount(); i++) {
            if (getTitleAt(i).contains(Application.TIME_OPTIONS_MASTER_TABNAME)) {
                return true;
            }
        }
        return false;
    }

    void openSLAMasterTab(Application app) {
        SLAPanel slaPanel = new SLAPanel(app.getMainFrame(), app.getAppProperties().getSlaProperties());
        addTab(Application.SLA_MASTER_TABNAME, slaPanel);
        setTabComponentAt(getTabCount() - 1, new TabCloseComponent(Application.SLA_MASTER_TABNAME, this, tabsListener));
        setSelectedIndex(getTabCount() - 1);
        checkFirstTabOpened();
    }

    void openMWPMasterTab(Application app) {
        MWPPanel mwpPanel = new MWPPanel(app.getMainFrame(), app.getAppProperties().getMwpProperties());
        addTab(Application.MWP_MASTER_TABNAME, mwpPanel);
        setTabComponentAt(getTabCount() - 1, new TabCloseComponent(Application.MWP_MASTER_TABNAME, this, tabsListener));
        setSelectedIndex(getTabCount() - 1);
        checkFirstTabOpened();
    }

    void openTimeOptionsMasterTab(Application app) {
        DatePanel timeOptionsPanel = new DatePanel(app.getMainFrame(), app.getAppProperties().getDateProperties());
        addTab(Application.TIME_OPTIONS_MASTER_TABNAME, timeOptionsPanel);
        setTabComponentAt(getTabCount() - 1, new TabCloseComponent(Application.TIME_OPTIONS_MASTER_TABNAME, this, tabsListener));
        setSelectedIndex(getTabCount() - 1);
        checkFirstTabOpened();
    }

    boolean isMasterTabCurrent() {
        if (getTabCount() != 0) {
            if (getTitleAt(getSelectedIndex()).contains(Application.SLA_MASTER_TABNAME) ||
                    getTitleAt(getSelectedIndex()).contains(Application.MWP_MASTER_TABNAME))
            {
                return true;
            }
        }
        return false;
    }
}
