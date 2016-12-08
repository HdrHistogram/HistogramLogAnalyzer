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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.FileWriter;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.plot.XYPlot;

public class Application implements ActionListener, Runnable {

    private SLAProperties slaProperties = new SLAProperties();
    private MWPProperties mwpProperties = new MWPProperties();

    private JFrame mainframe;
    private DraggableTabbedPane tabbedPane;
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

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("action_cmp_close")) {
            JButton btt = (JButton) e.getSource();
            JPanel overpanel = (JPanel) btt.getParent().getParent().getParent();
            if (overpanel.getComponentCount() != 1) {
                JTextField t = (JTextField) btt.getParent().getComponent(0);
                if (JOptionPane.showConfirmDialog(mainframe, "Are you sure?", "Close file: " + t.getText(), JOptionPane.YES_NO_OPTION) == 0) {
                    for (int i = 0; i < overpanel.getComponentCount(); i++) {
                        if (overpanel.getComponent(i).equals(btt.getParent().getParent())) {
                            overpanel.remove(i);
                        }
                    }
                }
            } else {
                JOptionPane.showMessageDialog(mainframe, "Close the tab instead");
            }

        } else if (e.getActionCommand().equals("action_openfile")) {
            File[] inputFiles = chooseInputFiles();
            // fileToOpen is null if any option other than APPROVE_OPTION selected
            if (inputFiles != null) {
                openFiles(inputFiles);
            }
        } else if (e.getActionCommand().equals("action_maxrange")) {
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

        } else if (e.getActionCommand().equals("action_snapshot")) {
            if (tabbedPane.getTabCount() != 0) {
                    int result = check_snapshotfile();
                    if (result == 0)
                        return;
                    get_snapshot(snapshot_filename);
                    JOptionPane.showMessageDialog(mainframe, "Successfully saved as: " + snapshot_filename);
            } else {
                JOptionPane.showMessageDialog(mainframe, "No data for snapshot");
            }

        } else {
            throw new IllegalArgumentException("Illegal argument: " + e.getActionCommand());
        }

        tabbedPane.repaint();
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

    private void create_menubar() {
        JMenuBar menuBar = new JMenuBar();
        mainframe.setJMenuBar(menuBar);
        JMenu menu_file = new JMenu("File");
        menu_file.setMnemonic(KeyEvent.VK_F);
        JMenu menu_view = new JMenu("View");
        menu_view.setMnemonic(KeyEvent.VK_V);
        JMenu menu_help = new JMenu("Help");
        menu_help.setMnemonic(KeyEvent.VK_H);

        final JMenuItem menuitem_abt_version = new JMenuItem("About");
        menu_help.add(menuitem_abt_version);
        menuitem_abt_version.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    AboutDialog dialog = new AboutDialog(mainframe);
                    dialog.setVisible(true);
                }
         });

        JMenuItem menu_file_open = new JMenuItem("Open", new ImageIcon(getClass().getResource("icon_open.png")));
        menu_file_open.setActionCommand("action_openfile");
        menu_file_open.setMnemonic(KeyEvent.VK_O);
        menu_file_open.setAccelerator(KeyStroke.getKeyStroke('O', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menu_file_open.addActionListener(this);

        JMenuItem menu_file_exit = new JMenuItem("Exit");
        menu_file_exit.setMnemonic(KeyEvent.VK_X);
        menu_file_exit.setAccelerator(KeyStroke.getKeyStroke('Q', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menu_file_exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainframe.dispose();
            }
        });

        final JCheckBoxMenuItem menu_normalize = new JCheckBoxMenuItem("Normalize Y axis");
        menu_normalize.setMnemonic(KeyEvent.VK_N);
        menu_normalize.setToolTipText(NORMALIZE_TOOLTIP_TEXT);
        menu_normalize.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean enableNormalizing = menu_normalize.isSelected();
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
        });

        menuBar.add(menu_file);
        menuBar.add(menu_view);
        menuBar.add(menu_help);

        menu_file.add(menu_file_open);
        menu_file.addSeparator();
        menu_file.add(menu_file_exit);

        menu_view.add(menu_normalize);
    }

    private JButton buttonOpenFile;
    private JButton buttonSnapshot;
    private JButton slaMasterButton;
    private JButton timelineMasterButton;
    private JCheckBox showSLAButton;
    private JButton maxRangeButton;

    private void enableToolbarButtons(boolean b) {
        buttonSnapshot.setEnabled(b);
        slaMasterButton.setEnabled(b);
        timelineMasterButton.setEnabled(b);
        showSLAButton.setEnabled(b);
        maxRangeButton.setEnabled(b);
    }

    private void create_toolbar() {
        JToolBar tool = new JToolBar();
        tool.setFloatable(false);
        tool.setRollover(true);

        buttonOpenFile = new JButton("Open");
        buttonOpenFile.setActionCommand("action_openfile");
        buttonOpenFile.setIcon(new ImageIcon(getClass().getResource("icon_open.png")));
        buttonOpenFile.setMnemonic(KeyEvent.VK_O);
        buttonOpenFile.addActionListener(this);
        tool.add(buttonOpenFile, BorderLayout.WEST);

        buttonSnapshot = new JButton("Snapshot");
        buttonSnapshot.setActionCommand("action_snapshot");
        buttonSnapshot.setIcon(new ImageIcon(getClass().getResource("icon_photo.png")));
        buttonSnapshot.setToolTipText("Take Snapshot");
        buttonSnapshot.setMnemonic(KeyEvent.VK_P);
        buttonSnapshot.addActionListener(this);
        tool.add(buttonSnapshot, BorderLayout.WEST);

        slaMasterButton = new JButton(SLA_MASTER_TABNAME);
        slaMasterButton.setIcon(new ImageIcon(getClass().getResource("icon_sla.png")));
        slaMasterButton.setToolTipText(SLA_TOOLTIP_TEXT);
        slaMasterButton.setMnemonic(KeyEvent.VK_S);
        slaMasterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!tabbedPane.isSLAMasterTabOpen()) {
                    tabbedPane.openSLAMasterTab(Application.this);
                }
            }
        });
        tool.add(slaMasterButton, BorderLayout.WEST);

        timelineMasterButton = new JButton(MWP_MASTER_TABNAME);
        // FIXME: new icon for MWP
        timelineMasterButton.setIcon(new ImageIcon(getClass().getResource("icon_sla.png")));
        timelineMasterButton.setToolTipText(MWP_TOOLTIP_TEXT);
        timelineMasterButton.setMnemonic(KeyEvent.VK_T);
        timelineMasterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!tabbedPane.isTimelineMasterTabOpen()) {
                    tabbedPane.openMWPMasterTab(Application.this);
                }
            }
        });
        tool.add(timelineMasterButton, BorderLayout.WEST);

        showSLAButton = new JCheckBox("Show SLA");
        showSLAButton.setToolTipText("Enable/Disable SLA");
        showSLAButton.setMnemonic(KeyEvent.VK_S);
        showSLAButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                slaProperties.toggleSLAVisibility(((JCheckBox)e.getSource()).isSelected());
            }
        });

        tool.add(showSLAButton, BorderLayout.WEST);

        maxRangeButton = new JButton("MaxRange");
        maxRangeButton.setActionCommand("action_maxrange");
        maxRangeButton.setIcon(new ImageIcon(getClass().getResource(
                "icon_maxrange.png")));
        maxRangeButton.setToolTipText("Set MAX Range to both");
        maxRangeButton.setMnemonic(KeyEvent.VK_M);
        maxRangeButton.addActionListener(this);
        tool.add(maxRangeButton, BorderLayout.WEST);

        enableToolbarButtons(false);
        toppanel.add(tool);
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
        new FileDrop(mainframe, new FileDrop.Listener() {
            @Override
            public void filesDropped(File[] files) {
                openFiles(files);
            }
        });
        create_menubar();
        fc = new ZFileChooser(mainframe, new File(getPreviousRunsDirectoryForJHiccupLogFileOpenOrUseCurrentWorkingDirectory()));

        tabbedPane = new DraggableTabbedPane();

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

    private void run(File[] inputFiles) throws IOException {
        updateLabel("Please wait... Processing log files");
        showMainPanel();

        tabbedPane.plotInputFiles(getInputFileNames(inputFiles), this);
        tabbedPane.repaint();

        mainframe.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        enableToolbarButtons(true);
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
