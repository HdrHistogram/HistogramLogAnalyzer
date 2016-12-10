/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

import javax.swing.*;

import org.HdrHistogram.HistogramLogAnalyzer.datalayer.TagsHelper;

/*
 * Credit http://stackoverflow.com/questions/60269/how-to-implement-draggable-tab-using-java-swing
 *
 * Modified by Dmitry
 */
class DraggableTabbedPane extends JTabbedPane {

	private static final long serialVersionUID = 1L;
    private boolean dragging = false;
    private Image tabImage = null;
    private Point currentMouseLocation = null;
    private int draggedTabIndex = 0;
    private TabsListener tabsListener = null;

    DraggableTabbedPane(final TabsListener tabsListener) {
        super();
        this.tabsListener = tabsListener;
        addMouseMotionListener(new MouseMotionAdapter() {
          @Override
        public void mouseDragged(MouseEvent e) {

            if(!dragging) {
              int tabNumber = getUI().tabForCoordinate(DraggableTabbedPane.this, e.getX(), e.getY());
              if(tabNumber >= 0) {
                draggedTabIndex = tabNumber;
                Rectangle bounds = getUI().getTabBounds(DraggableTabbedPane.this, tabNumber);
                Image totalImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics totalGraphics = totalImage.getGraphics();
                totalGraphics.setClip(bounds);
                setDoubleBuffered(false);
                paintComponent(totalGraphics);

                tabImage = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
                Graphics graphics = tabImage.getGraphics();
                graphics.drawImage(totalImage, 0, 0, bounds.width, bounds.height, bounds.x, bounds.y, bounds.x + bounds.width, bounds.y+bounds.height, DraggableTabbedPane.this);

                dragging = true;
                repaint();
              }
            } else {
              currentMouseLocation = e.getPoint();
              repaint();
            }

            super.mouseDragged(e);
        }
    });

    addMouseListener(new MouseAdapter() {
          @Override
        public void mouseReleased(MouseEvent e) {

            if(dragging) {
              int tabNumber = getUI().tabForCoordinate(DraggableTabbedPane.this, e.getX(), 10);
              if(tabNumber >= 0) {
                Component comp = getComponentAt(draggedTabIndex);
                String title = getTitleAt(draggedTabIndex);
                removeTabAt(draggedTabIndex);
                insertTab(title, null, comp, null, tabNumber);
                setTabComponentAt(tabNumber, new TabCloseComponent(title, DraggableTabbedPane.this, tabsListener));
                checkFirstTabOpened();
              }
            }

            dragging = false;
            tabImage = null;
          }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if(dragging && currentMouseLocation != null && tabImage != null) {
            g.drawImage(tabImage, currentMouseLocation.x, currentMouseLocation.y, this);
        }
    }

    void plotInputFiles(String[] inputFileNames, Application app) throws IOException {
        boolean multipleFiles = inputFileNames.length > 1;
        boolean firstFile = getTabCount() == 0;
        boolean masterTab = isMasterTabCurrent();
        boolean needNewTab = firstFile || masterTab;

        PlotFilesMode mode;
        if (!multipleFiles) {
            LatencyPanel latencyPanel = new LatencyPanel(inputFileNames, app.getSlaProperties(), app.getMwpProperties());
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
                LatencyPanel latencyPanel = new LatencyPanel(inputFileNames, app.getSlaProperties(), app.getMwpProperties());
                if (needNewTab) {
                    add_to_newtab(latencyPanel);
                } else {
                    add_to_currenttab(latencyPanel);
                }
            } else {
                for (String inputFileName : inputFileNames) {
                    LatencyPanel latencyPanel = new LatencyPanel(inputFileName, app.getSlaProperties(), app.getMwpProperties());
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

    private void add_to_newtab(LatencyPanel latencyPanel) {
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

    private JPanel tab_builder(LatencyPanel latencyPanel) {
        JPanel coverPanel = new JPanel(new GridLayout(1,1));
        coverPanel.add(latencyPanel);
        return coverPanel;
    }

    private void add_to_currenttab(LatencyPanel latencyPanel) {
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

    boolean isTimelineMasterTabOpen() {
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

    void openSLAMasterTab(Application app) {
        SLAPanel slaPanel = new SLAPanel(app.getMainFrame(), app.getSlaProperties());
        addTab(Application.SLA_MASTER_TABNAME, slaPanel);
        setTabComponentAt(getTabCount() - 1, new TabCloseComponent(Application.SLA_MASTER_TABNAME, this, tabsListener));
        setSelectedIndex(getTabCount() - 1);
        checkFirstTabOpened();
    }

    void openMWPMasterTab(Application app) {
        MWPPanel mwpPanel = new MWPPanel(app.getMainFrame(), app.getMwpProperties());
        addTab(Application.MWP_MASTER_TABNAME, mwpPanel);
        setTabComponentAt(getTabCount() - 1, new TabCloseComponent(Application.MWP_MASTER_TABNAME, this, tabsListener));
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