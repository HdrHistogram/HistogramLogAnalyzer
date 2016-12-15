/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.plot.XYPlot;

public class Application implements ActionListener, Runnable {

    private SLAProperties slaProperties = new SLAProperties();
    private MWPProperties mwpProperties = new MWPProperties();
    private HPLProperties hplProperties = new HPLProperties();

    private JFrame mainframe;
    private LatencyTabbedPane tabbedPane;
    private JPanel toppanel, bottompanel;

    JFrame getMainFrame() {
        return mainframe;
    }

    private static final String APP_TITLE = "Histogram Log Analyzer";
    static final String SLA_MASTER_TABNAME = "SLA Master";
    private static final String SLA_TOOLTIP_TEXT = "Service level agreement settings";

    static final String MWP_MASTER_TABNAME = "MWP Master";
    private static final String MWP_TOOLTIP_TEXT = "Moving window percentile settings";

    private static final String NORMALIZE_TOOLTIP_TEXT =
            "Enable/disable normalizing across charts in the current tab";

    private JPanel mainPanel;
    private JLabel welcomeLabel;

    private static final int margin_mid          =  20;
    private static final int margin_page         =  10;
    private static final int brdr_thickness      =   1;
    private static final int snapshot_w          = 667;
    private static final int snapshot_h          = 389;
    private static final int snapshot_title_size =  40;

    // File chooser
    private ZFileChooser fc;
    private String snapshot_filename = null;
    private static final String _jHiccupLogFileLastSelectedFilesDirectory = ".histogramloganalyzer.lastjhiccuplogdirectory";

    // The directory we were in when we started jHiccupLogAnalyzer
    private static String _currentWorkingDirectory = null;

    // Properties and command line arguments processing
    private static JHiccupViewerConfiguration jHiccupViewerConfiguration = null; // Overall configuration including properties and command line arguments

    private void openFiles(File[] inputFiles) {
        try {
            run(inputFiles);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private File getaborder(int thickness, File inputfile) {
        try {
            BufferedImage image = ImageIO.read(inputfile);
            BufferedImage brdimg = new BufferedImage(image.getWidth()
                    + thickness * 2, image.getHeight() + thickness * 2,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics gx = brdimg.createGraphics();
            gx.setColor(Color.GRAY);
            gx.fillRect(0, 0, brdimg.getWidth(), brdimg.getHeight());
            gx.setColor(Color.WHITE);
            gx.fillRect(thickness, thickness,
                    brdimg.getWidth() - thickness * 2, brdimg.getHeight()
                            - thickness * 2);
            gx.drawImage(image, thickness, thickness, null);
            ImageIO.write(brdimg, "PNG", inputfile);
        } catch (Exception except) {
            except.printStackTrace();
        }
        return inputfile;
    }

    private class aFilter extends javax.swing.filechooser.FileFilter {
        @Override
        public boolean accept(File file) {
            if (file.isDirectory()) {
                return true;
            }
            String filename = file.getName();
            return filename.endsWith(".png");
        }

        @Override
        public String getDescription() {
            return "*.PNG Files";
        }
    }

    private int check_snapshotfile() {
        JFileChooser fcc = new JFileChooser(fc.getSelectedFile());
        fcc.addChoosableFileFilter(new aFilter());
        int result = fcc.showSaveDialog(mainframe);
        if (result != 0)
            return 0;
        snapshot_filename = fcc.getSelectedFile().toString();
        if (!snapshot_filename.contains(".png"))
            snapshot_filename += ".png";
        return 1;
    }

    private File[] chooseInputFiles() {
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setPreferredSize(new Dimension(1024,640));
        fc.setMultipleMode(true);
        fc.setDialogTitle("Open Latency Log File"); // Provides context for the user
        // fc.setIcon();
        int rtn = fc.showOpenDialog();
        if (rtn == JFileChooser.APPROVE_OPTION) {
            File[] inputFiles = fc.getSelectedFiles();
            saveLastDirectory(inputFiles[0].getParent());
            return inputFiles;
        }

        return null;
    }

    private void snapshot_groupfiles(File[] f3, String snapfile) {
        try {
            BufferedImage[] imgs = new BufferedImage[f3.length];
            int width = 0, height = 0;
            for (int i = 0; i < imgs.length; i++) {
                imgs[i] = ImageIO.read(f3[i]);
                width += imgs[i].getWidth();
                height = imgs[i].getHeight();
            }
            BufferedImage img3_combined = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_ARGB);

            Graphics g = img3_combined.getGraphics();
            width = 0;
            for (BufferedImage img : imgs) {
                g.drawImage(img, width, 0, null);
                width += img.getWidth();
            }
            ImageIO.write(img3_combined, "PNG", new File(snapfile));
            for (File aF3 : f3) {
                aF3.delete();
            }

        } catch (Exception except) {
            System.err.println("HistogramLogAnalyzer: Image generation exception");
            System.err.println("  Message: " + except.getMessage());
            System.err.println("  Cause:   " + except.getCause());
            except.printStackTrace();
        }
    }

    private void get_snapshot(String snapfile) {
        JPanel p1 = (JPanel) tabbedPane.getSelectedComponent();
        File[] f3 = new File[p1.getComponentCount()];
        for (int k = 0; k < p1.getComponentCount(); k++) {
            JPanel latencyPanel = (JPanel) p1.getComponent(k);

            ChartPanel cp1 = (ChartPanel) latencyPanel.getComponent(0);
            ChartPanel cp2 = (ChartPanel) latencyPanel.getComponent(1);
            //String snaptitle = txt.getText();

            File f1 = new File(((Long) System.currentTimeMillis()).toString()
                    + cp1.getChart().getTitle());
            File f2 = new File(((Long) System.currentTimeMillis()).toString()
                    + cp2.getChart().getTitle());
            f3[k] = new File(((Long) System.currentTimeMillis()).toString()
                    + ((Integer) k).toString());
            try {
                ChartUtilities.saveChartAsPNG(f1, cp1.getChart(), snapshot_w, snapshot_h);
                ChartUtilities.saveChartAsPNG(f2, cp2.getChart(), snapshot_w, snapshot_h);
                // load source images
                BufferedImage img1 = ImageIO.read(getaborder(brdr_thickness, f1));
                BufferedImage img2 = ImageIO.read(getaborder(brdr_thickness, f2));

                int c_w = img1.getWidth() + 2 * margin_page;
                int c_h = 2 * img1.getHeight() + snapshot_title_size + margin_page + 2 * margin_mid;

                BufferedImage img3_combined = new BufferedImage(c_w, c_h, BufferedImage.TYPE_INT_ARGB);

                BufferedImage img4_text = new BufferedImage(img1.getWidth() + 2 * margin_page, snapshot_title_size,BufferedImage.TYPE_INT_ARGB);

                File f_tmp = new File("temp_file");
                ImageIO.write(img4_text, "PNG", f_tmp);
                img4_text = ImageIO.read(f_tmp);
                Graphics g1 = img4_text.createGraphics();
                g1.setColor(Color.WHITE);
                g1.fillRect(0, 0, img4_text.getWidth(), img4_text.getHeight());
                g1.setColor(Color.BLACK);
                g1.setFont(new Font("Arial", Font.BOLD, 18));
                //g1.drawString(snaptitle,img4_text.getWidth() / 2 - 10*snaptitle.length()/2,snapshot_title_size / 2);
                ImageIO.write(img4_text, "PNG", f_tmp);

                Graphics g = img3_combined.getGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, c_w, c_h);
                g.drawImage(img4_text, 0, 0, null);
                g.drawImage(img1, margin_page, snapshot_title_size, null);
                g.drawImage(img2, margin_page, img1.getHeight() + snapshot_title_size + margin_mid, null);

                ImageIO.write(img3_combined, "PNG", f3[k]);
                f1.delete();
                f2.delete();
                f_tmp.delete();

            } catch (Exception except) {
                System.err.println("jHiccupLogAnalyzer: Snapshot generation exception");
                System.err.println("  Message: " + except.getMessage());
                System.err.println("  Cause:   " + except.getCause());
                except.printStackTrace();
            }
        }
        snapshot_groupfiles(f3,snapfile);
    }

    private JPanel getMainPanel() {
        if (mainPanel == null) {
            mainPanel = new JPanel(new CardLayout());
            mainPanel.add("welcome", getWelcomeLabel());
            mainPanel.add("mainPanel", bottompanel);
        }
        return mainPanel;
    }

    private void showMainPanel() {
        ((CardLayout) getMainPanel().getLayout()).show(getMainPanel(),
                "mainPanel");
    }

    private JLabel getWelcomeLabel() {
        if (welcomeLabel == null) {
            welcomeLabel = new JLabel(
                    "To open a file use File -> Open or Drag and Drop a latency log file");
            welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        }
        return welcomeLabel;
    }

    private void updateLabel(final String message) {
        if (SwingUtilities.isEventDispatchThread()) {
            getWelcomeLabel().setText(message);
        } else {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    updateLabel(message);
                }
            });
        }
    }

    /*
     * Action handlers
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();

        switch (actionCommand) {
            case "openHandler":
                openHandler();
                break;
            case "snapshotHandler":
                snapshotHandler();
                break;
            case "configureSLAHandler":
                configureSLAHandler();
                break;
            case "configureMWPHandler":
                configureMWPHandler();
                break;
            case "exitHandler":
                exitHandler();
                break;
            case "normalizeHandler":
                normalizeHandler();
                break;
            case "maxRangeHandler":
                maxRangeHandler();
                break;
            case "showSLAButton":
                showSLAButton(e);
                break;
            case "showMWPButton":
                showMWPButton(e);
                break;
            case "showHPLButton":
                showHPLButton(e);
                break;
            case "aboutHandler":
                aboutHandler();
                break;
            default:
                throw new IllegalArgumentException("Illegal argument: " + e.getActionCommand());
        }

        tabbedPane.repaint();
    }

    private void openHandler() {
        File[] inputFiles = chooseInputFiles();
        // fileToOpen is null if any option other than APPROVE_OPTION selected
        if (inputFiles != null) {
            openFiles(inputFiles);
        }
    }

    private void snapshotHandler() {
        if (tabbedPane.getTabCount() != 0) {
            int result = check_snapshotfile();
            if (result == 0)
                return;
            get_snapshot(snapshot_filename);
            JOptionPane.showMessageDialog(mainframe, "Successfully saved as: " + snapshot_filename);
        } else {
            JOptionPane.showMessageDialog(mainframe, "No data for snapshot");
        }
    }

    private void configureSLAHandler() {
        if (!tabbedPane.isSLAMasterTabOpen()) {
            tabbedPane.openSLAMasterTab(Application.this);
        }
    }

    private void configureMWPHandler() {
        if (!tabbedPane.isTimelineMasterTabOpen()) {
            tabbedPane.openMWPMasterTab(Application.this);
        }
    }

    private void exitHandler() {
        mainframe.dispose();
    }

    private void normalizeHandler() {
        boolean enableNormalizing = normalizeMenuItem.isSelected();
        JPanel currentPanel = (JPanel) tabbedPane.getSelectedComponent();
        if (currentPanel == null) {
            return;
        }
        if (enableNormalizing) {
            // find max and use it
            double maxYValue = 0.0;
            for (int i = 0; i < currentPanel.getComponentCount(); i++) {
                LatencyPanel latencyPanel = (LatencyPanel) currentPanel.getComponent(i);
                ScaleProperties.ScaleEntry scaleEntry = latencyPanel.getScaleEntry();
                maxYValue = Math.max(maxYValue, scaleEntry.getMaxYValue());
            }
            ScaleProperties.ScaleEntry normalizedScaleEntry =
                    new ScaleProperties.ScaleEntry(0.0, maxYValue); // ignore X
            for (int i = 0; i < currentPanel.getComponentCount(); i++) {
                LatencyPanel latencyPanel = (LatencyPanel) currentPanel.getComponent(i);
                latencyPanel.scale(normalizedScaleEntry);
            }
        } else {
            // use default max values
            for (int i = 0; i < currentPanel.getComponentCount(); i++) {
                LatencyPanel latencyPanel = (LatencyPanel) currentPanel.getComponent(i);
                latencyPanel.scale();
            }
        }
    }

    private void maxRangeHandler() {
        if (tabbedPane.getTabCount() != 0 && !tabbedPane.isMasterTabCurrent()) {
            JPanel p1 = (JPanel) tabbedPane.getSelectedComponent();
            for (int k = 0; k < p1.getComponentCount(); k++) {
                JPanel latencyPanel = (JPanel) p1.getComponent(k);

                ChartPanel cp1 = (ChartPanel) latencyPanel.getComponent(0);
                ChartPanel cp2 = (ChartPanel) latencyPanel.getComponent(1);
                XYPlot plot1 = (XYPlot) cp1.getChart().getPlot();
                XYPlot plot2 = (XYPlot) cp2.getChart().getPlot();

                Double maxxi = 0.0;

                if (maxxi < plot1.getRangeAxis().getRange().getUpperBound()) {
                    maxxi = plot1.getRangeAxis().getRange().getUpperBound();
                } else if (maxxi < plot2.getRangeAxis().getRange().getUpperBound()) {
                    maxxi = plot2.getRangeAxis().getRange().getUpperBound();
                }
                plot1.getRangeAxis().setRange(0.0, maxxi);
                plot2.getRangeAxis().setRange(0.0, maxxi);
            }
        }
    }

    private void showSLAButton(ActionEvent e) {
        boolean b;
        Object source = e.getSource();
        if (source instanceof JCheckBox) {
            b = ((JCheckBox) source).isSelected();
        } else if (source instanceof JCheckBoxMenuItem) {
            b = ((JCheckBoxMenuItem) source).isSelected();
        } else {
            throw new RuntimeException("unknown source type: "+source);
        }
        slaProperties.toggleSLAVisibility(b);
        showSLAMenuItem.setSelected(b);
        showSLAButton.setSelected(b);
    }

    private void showMWPButton(ActionEvent e) {
        boolean b;
        Object source = e.getSource();
        if (source instanceof JCheckBox) {
            b = ((JCheckBox) source).isSelected();
        } else if (source instanceof JCheckBoxMenuItem) {
            b = ((JCheckBoxMenuItem) source).isSelected();
        } else {
            throw new RuntimeException("unknown source type: "+source);
        }
        mwpProperties.toggleMWPVisibility(b);
        showMWPMenuItem.setSelected(b);
        showMWPButton.setSelected(b);
    }

    private void showHPLButton(ActionEvent e) {
        boolean b;
        Object source = e.getSource();
        if (source instanceof JCheckBox) {
            b = ((JCheckBox) source).isSelected();
        } else if (source instanceof JCheckBoxMenuItem) {
            b = ((JCheckBoxMenuItem) source).isSelected();
        } else {
            throw new RuntimeException("unknown source type: "+source);
        }
        hplProperties.toggleHPLVisibility(b);
        showHPLMenuItem.setSelected(b);
    }

    private void aboutHandler() {
        AboutDialog dialog = new AboutDialog(mainframe);
        dialog.setVisible(true);
    }

    private JMenuItem snapshotMenuItem;
    private JMenuItem configureSLAMenuItem;
    private JMenuItem configureMWPMenuItem;

    private JCheckBoxMenuItem normalizeMenuItem;
    private JCheckBoxMenuItem maxRangeMenuItem;
    private JCheckBoxMenuItem showSLAMenuItem;
    private JCheckBoxMenuItem showMWPMenuItem;
    private JCheckBoxMenuItem showHPLMenuItem;

    private void enableMenuItems(boolean b) {
        snapshotMenuItem.setEnabled(b);
        configureSLAMenuItem.setEnabled(b);
        configureMWPMenuItem.setEnabled(b);
        normalizeMenuItem.setEnabled(b);
        maxRangeMenuItem.setEnabled(b);
        showSLAMenuItem.setEnabled(b);
        showMWPMenuItem.setEnabled(b);
        showHPLMenuItem.setEnabled(b);
    }

    private void create_menubar() {
        JMenuBar menuBar = new JMenuBar();
        mainframe.setJMenuBar(menuBar);
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        JMenuItem openMenuItem = new JMenuItem("Open", new ImageIcon(getClass().getResource("icon_open.png")));
        openMenuItem.setMnemonic(KeyEvent.VK_O);
        openMenuItem.setAccelerator(KeyStroke.getKeyStroke('O', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        openMenuItem.setActionCommand("openHandler");
        openMenuItem.addActionListener(this);

        snapshotMenuItem = new JMenuItem("Snapshot", new ImageIcon(getClass().getResource("icon_photo.png")));
        snapshotMenuItem.setMnemonic(KeyEvent.VK_P);
        snapshotMenuItem.setAccelerator(KeyStroke.getKeyStroke('P', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        snapshotMenuItem.setActionCommand("snapshotHandler");
        snapshotMenuItem.addActionListener(this);

        configureSLAMenuItem = new JMenuItem("Configure SLA", new ImageIcon(getClass().getResource("icon_sla.png")));
        configureSLAMenuItem.setActionCommand("configureSLAHandler");
        configureSLAMenuItem.addActionListener(this);

        // FIXME: new icon for MWP
        configureMWPMenuItem = new JMenuItem("Configure MWP", new ImageIcon(getClass().getResource("icon_sla.png")));
        configureMWPMenuItem.setActionCommand("configureMWPHandler");
        configureMWPMenuItem.addActionListener(this);

        JMenuItem exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.setMnemonic(KeyEvent.VK_X);
        exitMenuItem.setAccelerator(KeyStroke.getKeyStroke('X', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        exitMenuItem.setActionCommand("exitHandler");
        exitMenuItem.addActionListener(this);

        normalizeMenuItem = new JCheckBoxMenuItem("Normalize Y axis");
        normalizeMenuItem.setMnemonic(KeyEvent.VK_N);
        normalizeMenuItem.setAccelerator(KeyStroke.getKeyStroke('N', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        normalizeMenuItem.setToolTipText(NORMALIZE_TOOLTIP_TEXT);
        normalizeMenuItem.setActionCommand("normalizeHandler");
        normalizeMenuItem.addActionListener(this);

        maxRangeMenuItem = new JCheckBoxMenuItem("MaxRange");
        maxRangeMenuItem.setMnemonic(KeyEvent.VK_M);
        maxRangeMenuItem.setAccelerator(KeyStroke.getKeyStroke('M', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        maxRangeMenuItem.setToolTipText("Set MAX Range to both");
        maxRangeMenuItem.setActionCommand("maxRangeHandler");
        maxRangeMenuItem.addActionListener(this);

        showSLAMenuItem = new JCheckBoxMenuItem("Show SLA");
        showSLAMenuItem.setMnemonic(KeyEvent.VK_S);
        showSLAMenuItem.setAccelerator(KeyStroke.getKeyStroke('S', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        showSLAMenuItem.setToolTipText("Enable/Disable SLA");
        showSLAMenuItem.setActionCommand("showSLAButton");
        showSLAMenuItem.addActionListener(this);

        showMWPMenuItem = new JCheckBoxMenuItem("Show MWP");
        showMWPMenuItem.setToolTipText("Enable/Disable MWP");
        showMWPMenuItem.setActionCommand("showMWPButton");
        showMWPMenuItem.addActionListener(this);

        showHPLMenuItem = new JCheckBoxMenuItem("Show percentile lines");
        showHPLMenuItem.setToolTipText("Enable/Disable percentile lines");
        showHPLMenuItem.setActionCommand("showHPLButton");
        showHPLMenuItem.addActionListener(this);

        JMenuItem aboutMenuItem = new JMenuItem("About");
        aboutMenuItem.setActionCommand("aboutHandler");
        aboutMenuItem.addActionListener(this);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);

        fileMenu.add(openMenuItem);
        fileMenu.add(snapshotMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(configureSLAMenuItem);
        fileMenu.add(configureMWPMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);

        viewMenu.add(normalizeMenuItem);
        viewMenu.add(maxRangeMenuItem);
        viewMenu.addSeparator();
        viewMenu.add(showSLAMenuItem);
        viewMenu.add(showMWPMenuItem);
        viewMenu.add(showHPLMenuItem);

        helpMenu.add(aboutMenuItem);
    }

    private JButton snapshotButton;
    private JButton slaMasterButton;
    private JButton mwpMasterButton;
    private JCheckBox showSLAButton;
    private JCheckBox showMWPButton;
    private JButton maxRangeButton;

    private void enableToolbarButtons(boolean b) {
        snapshotButton.setEnabled(b);
        slaMasterButton.setEnabled(b);
        mwpMasterButton.setEnabled(b);
        showSLAButton.setEnabled(b);
        showMWPButton.setEnabled(b);
        maxRangeButton.setEnabled(b);
    }

    private void create_toolbar() {
        JToolBar tool = new JToolBar();
        tool.setFloatable(false);
        tool.setRollover(true);

        JButton openButton = new JButton("Open");
        openButton.setIcon(new ImageIcon(getClass().getResource("icon_open.png")));
        openButton.setMnemonic(KeyEvent.VK_O);
        openButton.setActionCommand("openHandler");
        openButton.addActionListener(this);
        tool.add(openButton, BorderLayout.WEST);

        snapshotButton = new JButton("Snapshot");
        snapshotButton.setIcon(new ImageIcon(getClass().getResource("icon_photo.png")));
        snapshotButton.setToolTipText("Take Snapshot");
        snapshotButton.setMnemonic(KeyEvent.VK_P);
        snapshotButton.setActionCommand("snapshotHandler");
        snapshotButton.addActionListener(this);
        tool.add(snapshotButton, BorderLayout.WEST);

        slaMasterButton = new JButton(SLA_MASTER_TABNAME);
        slaMasterButton.setIcon(new ImageIcon(getClass().getResource("icon_sla.png")));
        slaMasterButton.setToolTipText(SLA_TOOLTIP_TEXT);
        slaMasterButton.setActionCommand("configureSLAHandler");
        slaMasterButton.addActionListener(this);
        tool.add(slaMasterButton, BorderLayout.WEST);

        mwpMasterButton = new JButton(MWP_MASTER_TABNAME);
        // FIXME: new icon for MWP
        mwpMasterButton.setIcon(new ImageIcon(getClass().getResource("icon_sla.png")));
        mwpMasterButton.setToolTipText(MWP_TOOLTIP_TEXT);
        mwpMasterButton.setActionCommand("configureMWPHandler");
        mwpMasterButton.addActionListener(this);
        tool.add(mwpMasterButton, BorderLayout.WEST);

        showSLAButton = new JCheckBox("Show SLA");
        showSLAButton.setMnemonic(KeyEvent.VK_S);
        showSLAButton.setToolTipText("Enable/Disable SLA");
        showSLAButton.setActionCommand("showSLAButton");
        showSLAButton.addActionListener(this);
        tool.add(showSLAButton, BorderLayout.WEST);

        showMWPButton = new JCheckBox("Show MWP");
        showMWPButton.setToolTipText("Enable/Disable MWP");
        showMWPButton.setActionCommand("showMWPButton");
        showMWPButton.addActionListener(this);
        tool.add(showMWPButton, BorderLayout.WEST);

        maxRangeButton = new JButton("MaxRange");
        maxRangeButton.setActionCommand("maxRangeHandler");
        maxRangeButton.setIcon(new ImageIcon(getClass().getResource("icon_maxrange.png")));
        maxRangeButton.setToolTipText("Set MAX Range to both");
        maxRangeButton.setMnemonic(KeyEvent.VK_M);
        maxRangeButton.addActionListener(this);
        tool.add(maxRangeButton, BorderLayout.WEST);

        enableToolbarButtons(false);
        enableMenuItems(false);
        toppanel.add(tool);
    }

    private void installListeners() {
        TabsListener tabsListener = new TabsListener() {
            @Override
            public void firstTabOpened() {
                enableToolbarButtons(true);
                enableMenuItems(true);

                showMWPButton.setEnabled(mwpProperties.isShowMWPUnlocked());
                showMWPMenuItem.setEnabled(mwpProperties.isShowMWPUnlocked());
            }

            @Override
            public void lastTabClosed() {
                enableToolbarButtons(false);
                enableMenuItems(false);
            }
        };
        tabbedPane = new LatencyTabbedPane(tabsListener);

        mwpProperties.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals("applyMWP")) {
                    showMWPButton.setEnabled(mwpProperties.isShowMWPUnlocked());
                    showMWPMenuItem.setEnabled(mwpProperties.isShowMWPUnlocked());
                }
            }
        });
    }

    private void prepare_ui() {
        mainframe = new JFrame(APP_TITLE);
        mainframe.setIconImage((new ImageIcon(getClass().getResource("azul_logo.png")).getImage()));
        mainframe.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent arg0) {
                mainframe.dispose();
            }
        });
        mainframe.setDropTarget(new DropTarget() {
            public synchronized void drop(DropTargetDropEvent dtde) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                try {
                    List<File> files =
                        (List<File>)dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    openFiles(files.toArray(new File[files.size()]));
                } catch (UnsupportedFlavorException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        create_menubar();
        fc = new ZFileChooser(mainframe, new File(getPreviousRunsDirectoryForJHiccupLogFileOpenOrUseCurrentWorkingDirectory()));

        installListeners();

        toppanel = new JPanel(new BorderLayout());
        create_toolbar();
        bottompanel = new JPanel(new BorderLayout());
        mainframe.getContentPane().setLayout(new BorderLayout());
        mainframe.getContentPane().add(toppanel, BorderLayout.NORTH);
        mainframe.getContentPane().add(getMainPanel(), BorderLayout.CENTER);

        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension dim = toolkit.getScreenSize();
        mainframe.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        mainframe.setBounds((dim.width * 30) / 100, (dim.height * 5) / 100,
                (dim.width * 40) / 100, (dim.height * 80) / 100);
        mainframe.setVisible(true);

        bottompanel.add(tabbedPane);

        mainPanel.add("mainPanel", bottompanel);

        String inputFileName = jHiccupViewerConfiguration.nameOfJHiccupFileToOpen();
        if (inputFileName != null) {
            File[] inputFiles = new File[] {new File(inputFileName)};
            openFiles(inputFiles);
        }
    }

    private static String[] getInputFileNames(File[] inputFiles) {
        String[] inputFileNames = new String[inputFiles.length];
        for (int i = 0; i < inputFiles.length; i++) {
            inputFileNames[i] = inputFiles[i].getAbsolutePath();
        }
        return inputFileNames;
    }

    SLAProperties getSlaProperties() {
        return slaProperties;
    }

    MWPProperties getMwpProperties() {
        return mwpProperties;
    }

    HPLProperties getHplProperties() {
        return hplProperties;
    }

    private void run(File[] inputFiles) throws IOException {
        updateLabel("Please wait... Processing log files");
        showMainPanel();

        tabbedPane.plotInputFiles(getInputFileNames(inputFiles), this);
        tabbedPane.repaint();

        mainframe.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    @Override
    public void run() {
        prepare_ui();
    }

    private void saveLastDirectory(String lastSelectedDirectory) {
        String userHome = System.getProperty("user.home");
        File saveFile = new File(userHome + File.separator + _jHiccupLogFileLastSelectedFilesDirectory);
        try {
            BufferedWriter bufWriter = new BufferedWriter(new FileWriter(saveFile));
            bufWriter.write(lastSelectedDirectory); 
            bufWriter.close(); 
        } catch (Exception except) {
            // Do not do anything if there's an error
        }
    }

    private String getPreviousRunsDirectoryForJHiccupLogFileOpenOrUseCurrentWorkingDirectory() {
        String userHome = System.getProperty("user.home");
        File saveFile = new File(userHome + File.separator + _jHiccupLogFileLastSelectedFilesDirectory);
        try {
            if (saveFile.exists() && saveFile.isFile()) {
                BufferedReader bufReader = new BufferedReader(new FileReader(saveFile));
                String lastSavedDirectory = bufReader.readLine();
                bufReader.close();
                if (lastSavedDirectory != null) {
                    File dir = new File(lastSavedDirectory);
                    if (dir.exists() && dir.isDirectory()) {
                        return lastSavedDirectory; 
                    }
                }
            }
        } catch (Exception except) {
            // Do not do anything if there's an error - just use the current working directory
        }
        return getCurrentWorkingDirectory();
    }

    private static synchronized String getCurrentWorkingDirectory() {
        if (_currentWorkingDirectory == null) {
            _currentWorkingDirectory = System.getProperty("user.dir");
        }
        return _currentWorkingDirectory; 
    }

    public static void main(String args[]) {
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception except) {
            except.printStackTrace();
        }

        // Process the properties file and the command-line arguments
        jHiccupViewerConfiguration = new JHiccupViewerConfiguration();
        new JHiccupViewerCommandLineArguments(jHiccupViewerConfiguration, args);

        SwingUtilities.invokeLater(new Application());
    }
}
