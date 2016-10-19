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
import java.awt.GridLayout;
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

import java.sql.ResultSet;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.WindowConstants;
import javax.swing.JPopupMenu;

import org.HdrHistogram.HistogramLogAnalyzer.datalayer.Parser;
import org.HdrHistogram.HistogramLogAnalyzer.datalayer.DBManager;
import org.HdrHistogram.HistogramLogAnalyzer.dataobjectlayer.DBConnect;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.plot.XYPlot;

public class Application implements ActionListener, Runnable {

    private ArrayList<DBConnect> dbarry = new ArrayList<DBConnect>();
    private DBConnect db;
    private Parser pr;
    private JFrame mainframe;
    private DraggableTabbedPane tabbedPane;
    private JPanel toppanel, bottompanel, SLAslab;
    private JToolBar tool;
    private JCheckBox chk_sla;
    private JMenu menu_tags;

    private static final String APP_TITLE = "Histogram Log Analyzer";
    private static final String sla_tabname = "SLA Master";

    private JPanel mainPanel;
    private JLabel welcomeLabel;
    private ChartBuilder cm;

    private static final int SLA_tbl_cols        =   3;

    private static final int margin_mid          =  20;
    private static final int margin_page         =  10;
    private static final int brdr_thickness      =   1;
    private static final int snapshot_w          = 667;
    private static final int snapshot_h          = 389;
    private static final int snapshot_title_size =  40;

    private ArrayList<Double> sla_db_percentile = new ArrayList<Double>();
    private ArrayList<Double> sla_db_hicpvalue  = new ArrayList<Double>();

    private static Application applicationInstance = null;

    // File chooser
    private ZFileChooser fc;
    // private String _jHiccupLogFilename = null; // The name of the jHiccup log file that is currently open
    private static String localfile         = "none";
    private String snapshot_filename = null;
    private static final String _jHiccupLogFileLastSelectedFilesDirectory = ".histogramloganalyzer.lastjhiccuplogdirectory";

    public static String jHiccupLogFilename() {
        if (applicationInstance != null) {
            return localfile;
        } else {
            return "none";
        }
    } 

    public String jHiccupLogStartTime() {
        if (applicationInstance != null && pr != null) {
            return pr.getSamplingStartTime();
        } else {
            return "none";
        }
    } 

    public static Application getApplicationInstance() {
        return applicationInstance;
    }
 
    // The directory we were in when we started jHiccupLogAnalyzer
    private static String _currentWorkingDirectory = null;

    // Properties and command line arguments processing
    private static final String nameOfjHiccupLogAnalyzerPropertiesFile = ".histogramloganalyzer.properties";
    private static JHiccupViewerConfiguration jHiccupViewerConfiguration = null; // Overall configuration including properties and command line arguments

    public static JHiccupViewerConfiguration getJHiccupViewerConfiguration() {
        return jHiccupViewerConfiguration;
    }

    // Logging
    private static final Logger appLogger = Logger.getLogger(Application.class.getName()); 
    private static final Date startTime = new Date();
    private static final String startTimeString = 
        (new SimpleDateFormat("yyyyMMdd_HHmmss")).format(startTime) + "_" + 
        (String.format("%03d", (startTime.getTime() % 1000)));

    // jHiccup Log Analyzer's log file to which it writes logging messages; used to determine if initialized
    private static volatile String _jHiccupLogAnalyzersLogFile = null;

    public static Logger getApplicationLogger() {
        return appLogger;
    }

    private int isSLAtab_open() {
        if (tabbedPane.getTabCount() != 0) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if (tabbedPane.getTitleAt(i).contains(sla_tabname)) {
                    return 1;
                }
            }
            return 0;
        } else {
            return 1;
        }
    }

    private int is_at_SLAtab() {
        if (tabbedPane.getTabCount() != 0) {
            if (tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()).contains(sla_tabname)) {
                return 1;
            }
            return 0;
        } else {
            return 0;
        }
    }

    private void openHlogFile(String hlogFileName) {
        try {
            List<String> tags = TagsHelper.listTags(hlogFileName);
            String tag = showTagsChooser(tags, hlogFileName);
            menu_tags.add(createTagsMenuItems(tags, hlogFileName));

            openHlogFile(hlogFileName, tag);
        } catch (Throwable throwable) {
            showMessage("Failed to process Histogram Log file: " + hlogFileName);
            System.err.println("Failed to process Histogram Log file: " + hlogFileName);
            throwable.printStackTrace();
        }
    }

    private void openHlogFile(String hlogFileName, String tag) {
        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.entering("Application", "openHlogFile");
        }

        try {
            run(hlogFileName, tag);
        } catch (Throwable throwable) {
            showMessage("Failed to process Histogram Log file: " + hlogFileName);
            System.err.println("Failed to process Histogram Log file: " + hlogFileName);
            throwable.printStackTrace();
        }

        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.exiting("Application", "openHlogFile");
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
            System.err.println("HistogramLogAnalyzer: Image generation exception");
            System.err.println("  Message: " + except.getMessage());
            System.err.println("  Cause:   " + except.getCause());
            except.printStackTrace();
        }
        return inputfile;
    }

    class aFilter extends javax.swing.filechooser.FileFilter {
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

    public String open_filechooser() {
        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.entering("Application", "open_filechooser");
        }

        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setPreferredSize(new Dimension(1024,640));
        fc.setDialogTitle("Open Latency Log File"); // Provides context for the user
        // fc.setIcon();
        int rtn = fc.showOpenDialog();
        if (rtn == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fc.getSelectedFile();
            saveLastDirectory(selectedFile.getParent());
            if (jHiccupViewerConfiguration.verbose()) {
                appLogger.exiting("Application", "open_filechooser");
            }
            return selectedFile.getPath();
        }

        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.exiting("Application", "open_filechooser");
        }

        return null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("action_exit"))
            exit_routine();

        else if (e.getActionCommand().equals("action_cmp_close")) {
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
            String fileToOpen = open_filechooser();
            // fileToOpen is null if any option other than APPROVE_OPTION selected
            if (fileToOpen != null) {
                openHlogFile(fileToOpen);
            }

        } else if (e.getActionCommand().equals("action_sla_tab")) {
            if (isSLAtab_open() == 0) {
                add_slatab();
            }

        } else if (e.getActionCommand().equals("action_sla_add")) {
            sla_addrows();

        } else if (e.getActionCommand().equals("action_sla_delete")) {
            sla_delete();

        } else if (e.getActionCommand().equals("action_sla_onoff")) {
            if (tabbedPane.getTabCount() != 0) {
                JCheckBox jchkbox = (JCheckBox) e.getSource();
                sla_turn_onoff(jchkbox.isSelected());
            }

        } else if (e.getActionCommand().equals("action_sla_apply")) {
            if (sla_isvalid() != 0) {
                if (sla_fillvalues()) {
                    sla_pushtodb();
                    sla_reset_graph();
                    JOptionPane.showMessageDialog(mainframe, "Updated!");
                }
            }

        } else if (e.getActionCommand().equals("action_maxrange")) {
            if (tabbedPane.getTabCount() != 0 && is_at_SLAtab() == 0) {
                JPanel p1 = (JPanel) tabbedPane.getSelectedComponent();
                for (int k = 0; k < p1.getComponentCount(); k++) {
                    JPanel latencyPanel = (JPanel) p1.getComponent(k);

                    JPanel jp1 = (JPanel) latencyPanel.getComponent(0);
                    JPanel jp2 = (JPanel) latencyPanel.getComponent(1);
                    ChartPanel cp1 = (ChartPanel) jp1.getComponent(0);
                    ChartPanel cp2 = (ChartPanel) jp2.getComponent(0);
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
            // Should not reach here
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
            for (int j = 0; j < imgs.length; j++) {
                g.drawImage(imgs[j], width, 0, null);
                width += imgs[j].getWidth();
            }
            ImageIO.write(img3_combined, "PNG", new File(snapfile));
            for (int i = 0; i < f3.length; i++)
                f3[i].delete();

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

            JPanel jp1 = (JPanel) latencyPanel.getComponent(0);
            JPanel jp2 = (JPanel) latencyPanel.getComponent(1);
            ChartPanel cp1 = (ChartPanel) jp1.getComponent(0);
            ChartPanel cp2 = (ChartPanel) jp2.getComponent(0);
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

    public void showMessage(String msg) {
        JOptionPane.showMessageDialog(mainframe, msg);
    }

    private JPanel getMainPanel() {
        if (mainPanel == null) {
            mainPanel = new JPanel(new CardLayout());
            mainPanel.add("welcome", getWelcomeLabel());
            mainPanel.add("mainPanel", bottompanel);
        }
        return mainPanel;
    }

    public void showMainPanel() {
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
        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.entering("Application", "create_menubar");
        }

        JMenuBar menuBar = new JMenuBar();
        mainframe.setJMenuBar(menuBar);
        JMenu menu_file = new JMenu("File");
        menu_file.setMnemonic(KeyEvent.VK_F);

        menu_tags = new JMenu("Tags");
        menu_tags.setMnemonic(KeyEvent.VK_T);

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

        JMenuItem menu_file_exit = new JMenuItem("Exit");
        menu_file_exit.setMnemonic(KeyEvent.VK_X);
        menu_file_exit.setAccelerator(KeyStroke.getKeyStroke('Q', Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        menu_file_exit.setActionCommand("action_exit");

        menuBar.add(menu_file);
        menuBar.add(menu_tags);
        menuBar.add(menu_help);

        menu_file.add(menu_file_open);
        menu_file.addSeparator();
        menu_file.add(menu_file_exit);

        menu_file_open.addActionListener(this);
        menu_file_exit.addActionListener(this);

        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.exiting("Application", "create_menubar");
        }
    }

    private String showTagsChooser(List<String> tags, String title) {
        if (tags == null || tags.size() == 0)
            return null;

        return (String)JOptionPane.showInputDialog(
                mainframe,
                "Choose tag to open",
                title,
                JOptionPane.QUESTION_MESSAGE,
                null,
                tags.toArray(new String[tags.size()]),
                tags.get(0));
    }

    private JMenu createTagsMenuItems(List<String> tags, String hlogFile) {
        if (tags == null) {
            JMenu menu = new JMenu(new File(hlogFile).getName());
            menu.setToolTipText(hlogFile);
            JMenuItem menuItem = new JMenuItem("[NO TAG (default)]");
            menu.add(menuItem);
            return menu;
        }

        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JMenuItem jmi = (JMenuItem)e.getSource();
                JPopupMenu jpm = (JPopupMenu) jmi.getParent();
                JMenu menu = (JMenu)jpm.getInvoker();

                String file = menu.getToolTipText();
                String tag = jmi.getText();
                // clicking on tag opens new file
                openHlogFile(file, tag);
            }
        };

        JMenu menu = new JMenu(new File(hlogFile).getName());
        menu.setToolTipText(hlogFile);
        for (String tag : tags) {
            JMenuItem menuItem = new JMenuItem(tag);
            menu.add(menuItem);
            menuItem.addActionListener(listener);
        }

        return menu;
    }

    private void create_toolbar() {
        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.entering("Application", "create_toolbar");
        }

        tool = new JToolBar();
        tool.setFloatable(false);
        tool.setRollover(true);

        JButton bttopen = new JButton("Open");
        bttopen.setActionCommand("action_openfile");
        bttopen.setIcon(new ImageIcon(getClass().getResource("icon_open.png")));
        bttopen.setMnemonic(KeyEvent.VK_O);

        bttopen.addActionListener(this);
        tool.add(bttopen, BorderLayout.WEST);
        JButton bttphoto = new JButton("Snapshot");
        bttphoto.setActionCommand("action_snapshot");
        bttphoto.setIcon(new ImageIcon(getClass().getResource("icon_photo.png")));
        bttphoto.setToolTipText("Take Snapshot");
        bttphoto.setMnemonic(KeyEvent.VK_P);
        bttphoto.addActionListener(this);

        tool.add(bttphoto, BorderLayout.WEST);
        JButton bttsla = new JButton(sla_tabname);
        bttsla.setActionCommand("action_sla_tab");
        bttsla.setIcon(new ImageIcon(getClass().getResource("icon_sla.png")));
        bttsla.setToolTipText(sla_tabname);
        bttsla.setMnemonic(KeyEvent.VK_M);
        bttsla.addActionListener(this);

        tool.add(bttsla, BorderLayout.WEST);
        chk_sla = new JCheckBox("Show SLA");
        chk_sla.setActionCommand("action_sla_onoff");
        chk_sla.setToolTipText("Enable/Disable SLA");
        chk_sla.setMnemonic(KeyEvent.VK_S);
        chk_sla.addActionListener(this);
        tool.add(chk_sla, BorderLayout.WEST);

        JButton bttSetMAXRange = new JButton("MAXRange");
        bttSetMAXRange.setActionCommand("action_maxrange");
        bttSetMAXRange.setIcon(new ImageIcon(getClass().getResource(
                "icon_maxrange.png")));
        bttSetMAXRange.setToolTipText("Set MAX Range to both");
        bttSetMAXRange.setMnemonic(KeyEvent.VK_M);
        bttSetMAXRange.addActionListener(this);
        tool.add(bttSetMAXRange, BorderLayout.WEST);

        toppanel.add(tool);

        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.exiting("Application", "create_toolbar");
        }
    }

    public void prepare_ui() {
        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.entering("Application", "prepare_ui");
        }

        mainframe = new JFrame(APP_TITLE);
        mainframe.setIconImage((new ImageIcon(getClass().getResource("azul_logo.png")).getImage()));
        mainframe.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent arg0) {
                exit_routine();
            }
        });
        new FileDrop(mainframe, new FileDrop.Listener() {

            @Override
            public void filesDropped(File[] files) {
                try {
                    localfile = files[0].getCanonicalPath();
                    openHlogFile(localfile);
                } catch (IOException except) {
                    System.err.println("HistogramLogAnalyzer: File dropped in window exception");
                    System.err.println("  Message: " + except.getMessage());
                    System.err.println("  Cause:   " + except.getCause());
                    except.printStackTrace();
                }
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

        String nameOfFileToOpen = jHiccupViewerConfiguration.nameOfJHiccupFileToOpen();
        if (nameOfFileToOpen != null) {
            openHlogFile(nameOfFileToOpen);
        }

        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.exiting("Application", "prepare_ui");
        }
    }

    private boolean sla_fillvalues() {
		if (jHiccupViewerConfiguration.verbose()) {
			appLogger.entering("Application", "sla_fillvalues");
		}
		sla_db_percentile.clear();
		sla_db_hicpvalue.clear();

		int rowcount = SLAslab.getComponentCount() / 3;
		for (int i = SLA_tbl_cols; i < rowcount * SLA_tbl_cols; i = i
				+ SLA_tbl_cols) {
			JTextField ptxt = (JTextField) SLAslab.getComponent(i + 1);
			JTextField htxt = (JTextField) SLAslab.getComponent(i + 2);
			sla_db_percentile.add(Double.parseDouble(ptxt.getText()));
			sla_db_hicpvalue.add(Double.parseDouble(htxt.getText()));
		}
		if (jHiccupViewerConfiguration.verbose()) {
			appLogger.exiting("Application", "sla_fillvalues");
		}
		return true;
		/*
        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.entering("Application", "sla_fillvalues");
        }

        sla_db_percentile.clear();
        sla_db_hicpvalue.clear();

        int rowcount = SLAslab.getComponentCount() / 3;
        int currentRow = 0;
        for (int i = SLA_tbl_cols; i < rowcount * SLA_tbl_cols; i = i + SLA_tbl_cols, currentRow++) {
            JTextField ptxt = (JTextField) SLAslab.getComponent(i + 1);
            JTextField htxt = (JTextField) SLAslab.getComponent(i + 2);

            double sla_db_percentileValue = Double.parseDouble(ptxt.getText());

            if (currentRow == 0) {
                if (sla_db_percentileValue < 0.0d || sla_db_percentileValue > 100.0d) {
                    JOptionPane.showMessageDialog(mainframe, "The percentile value in the first row must be between 0 and 100."
                            , "jHiccup", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            } else {
                if (sla_db_percentileValue < sla_db_percentile.get(currentRow-1) || sla_db_percentileValue > 100.0d) {
                    JOptionPane.showMessageDialog(mainframe, "The percentile value in row " + (currentRow+1) +
                                        " must be greater than or equal to the value in row " + currentRow + " and less than 100."
                            , "jHiccup", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
            sla_db_percentile.add(sla_db_percentileValue);

            double sla_db_hicpvalueValue = Double.parseDouble(htxt.getText());
            if (currentRow == 0) {
                if (sla_db_hicpvalueValue <= 0.0d) {
                    JOptionPane.showMessageDialog(mainframe, "The hiccup value in the first row must greater than 0."
                            , "jHiccup", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            } else {
                if (sla_db_hicpvalueValue < sla_db_hicpvalue.get(currentRow-1)) {
                    JOptionPane.showMessageDialog(mainframe, "The hiccup value in row " + (currentRow+1) +
                            " must be greater than or equal to the value in row " + currentRow + "."
                            , "jHiccup", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
            sla_db_hicpvalue.add(sla_db_hicpvalueValue);
        }

        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.exiting("Application", "sla_fillvalues");
        }

        return true;
        */
    }

    private void sla_turn_onoff(boolean b) {
        if (tabbedPane.getTabCount() != 0) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if (!tabbedPane.getTitleAt(i).contains(sla_tabname)) {
                    JPanel p1 = (JPanel) tabbedPane.getComponentAt(i);
                    for (int k = 0; k < p1.getComponentCount(); k++) {
                        JPanel latencyPanel = (JPanel) p1.getComponent(k);
                        ((LatencyPanel)latencyPanel).updateSLAVisibility(b);
                    }
                }
            }
        }
    }

    private void sla_reset_graph() {
        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.entering("Application", "sla_reset_graph");
        }

        if (tabbedPane.getTabCount() != 0) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if (!tabbedPane.getTitleAt(i).contains(sla_tabname)) {
                    JPanel p1 = (JPanel) tabbedPane.getComponentAt(i);
                    for (int k = 0; k < p1.getComponentCount(); k++) {
                        JPanel latencyPanel = (JPanel) p1.getComponent(k);
                        ((LatencyPanel)latencyPanel).resetSLA();
                    }
                }
            }
        }

        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.exiting("Application", "sla_reset_graph");
        }
    }

    private double getSLAMasterPercentileValue(double percentValue) {
        if (percentValue == 100.0d) {
            percentValue = DBManager.VALUE_FOR_100PERCENT;
        }
        return  (1.0d/(1.0d-(percentValue/100.0d)));
    }

    private static final String deleteFromJSLADetails = "delete from j_sla_details;";
    private static final String deleteFromSQLiteSequence = "delete from sqlite_sequence where name='j_sla_details';";

    private void sla_pushtodb() {
        for (int i = 0; i < dbarry.size(); i++) {
            try {
                DBConnect local_db = dbarry.get(i);
                local_db.statement.execute(deleteFromJSLADetails);
                local_db.statement.execute(deleteFromSQLiteSequence);
                sla_db_percentile.add(0, 0.0);
                sla_db_hicpvalue.add(0, 0.0);
                pr.DBmgr.prepare_SLA(local_db,
                    sla_db_percentile.toArray(new Double[sla_db_percentile.size()]),
                    sla_db_hicpvalue.toArray(new Double[sla_db_hicpvalue.size()]));
            } catch (Exception except) {
                System.err.println("HistogramLogAnalyzer: Database exception");
                System.err.println("  Message: " + except.getMessage());
                System.err.println("  Cause:   " + except.getCause());
                except.printStackTrace();
            }
        }
    }

    private void add_slatab() {
        tabbedPane.addTab(sla_tabname, SLATab());
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, new TabCloseComponent(sla_tabname, tabbedPane));
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }

    private void sla_getfromdb() {
        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.entering("Application", "sla_getfromdb");
        }
    	if(sla_db_percentile.size()!=0)
		{
			sla_pushtodb();
			return;
		}
		sla_db_percentile.clear();
		sla_db_hicpvalue.clear();
        appLogger.logp(Level.FINEST, "Application", "sla_getfromdb", "start work");

		try {
			String cmd = "select * from (select *,rowid as k from j_sla_details where rowid>1) where k%2=0;";
			ResultSet rs = db.statement.executeQuery(cmd);
			while (rs.next()) {
				sla_db_percentile.add(rs.getDouble("sla_percentile"));
				sla_db_hicpvalue.add(rs.getDouble("sla_hiccuptimeinterval"));
			}
		} catch (Exception except) {
          System.err.println("HistogramLogAnalyzer: SLA data retrieval from DB exception");
          System.err.println("  Message: " + except.getMessage());
          System.err.println("  Cause:   " + except.getCause());
          except.printStackTrace();
		}
        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.exiting("Application", "sla_getfromdb");
        }
		/*
        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.entering("Application", "sla_getfromdb");
        }

        if (sla_db_percentile.size() != 0) {
            sla_pushtodb();
            return;
        }
        sla_db_percentile.clear();
        sla_db_hicpvalue.clear();

        appLogger.logp(Level.FINEST, "Application", "sla_getfromdb", "start work");

        ResultSet rs = null;
        double count = 1;
        try {
            String cmd = "select * from (select *,rowid as k from j_sla_details where rowid>1) where k%2=0;";
            rs = db.statement.executeQuery(cmd);
            while (rs.next()) {
                if (jHiccupViewerConfiguration.verbose()) {
                    appLogger.logp(Level.FINEST, "Application", "sla_getfromdb", " count: " + count + " percentile: " + rs.getDouble("sla_percentile"));
                }

                if (rs.getDouble("sla_percentile") == 0.0) {
                    // sla_db_percentile.add(10.0);

                    if (jHiccupViewerConfiguration.verbose()) {
                        appLogger.logp(Level.FINEST, "Application", "sla_getfromdb", "count: " + count + " percentile: 10.0 added");
                    }
                } else {
                    sla_db_percentile.add(rs.getDouble("sla_percentile"));
                    sla_db_hicpvalue.add(count);
                }
                // sla_db_hicpvalue.add(count);
                count++;
            }

        } catch (Exception except) {
            System.err.println("jHiccupLogAnalyzer: SLA data retrieval from DB exception");
            System.err.println("  Message: " + except.getMessage());
            System.err.println("  Cause:   " + except.getCause());
            except.printStackTrace();
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException except) {
                    System.err.println("jHiccupLogAnalyzer: SLA data retrieval Result Set close exception");
                    System.err.println("  Message: " + except.getMessage());
                    System.err.println("  Cause:   " + except.getCause());
                    except.printStackTrace();
                }
            }
        }

        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.exiting("Application", "sla_getfromdb");
        }
        */
    }

    private void sla_delete() {
        int rows = SLAslab.getComponentCount() / SLA_tbl_cols - 1;
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

    private void sla_regain() {
        int rowcount = SLAslab.getComponentCount() / SLA_tbl_cols;
        for (int i = 3; i < rowcount * SLA_tbl_cols; i = i + SLA_tbl_cols) {
            JLabel lbl = (JLabel) SLAslab.getComponent(i);
            lbl.setText(((Integer) (i / SLA_tbl_cols)).toString());
        }
        SLAslab.setLayout(new GridLayout(rowcount, 3));
    }

    private void sla_deleteat(int at) {
        int index = (at + 1) * SLA_tbl_cols;
        SLAslab.remove(index--);
        SLAslab.remove(index--);
        SLAslab.remove(index);

        sla_regain();
    }

    private void sla_addat(int at) {
        int index = (at) * SLA_tbl_cols;
        SLAslab.add(new JLabel("Sno"), index++);
        SLAslab.add(new JTextField("*"), index++);
        SLAslab.add(new JTextField("*"), index);

        sla_regain();
    }

    private void sla_addrows() {
        int rows = SLAslab.getComponentCount() / SLA_tbl_cols - 1;
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

    private int sla_isvalid() {
        int rowcount = SLAslab.getComponentCount() / SLA_tbl_cols;
        for (int i = SLA_tbl_cols; i < rowcount * SLA_tbl_cols; i = i + SLA_tbl_cols) {
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
                JOptionPane.showMessageDialog(mainframe, "Only numeric values allowed as hiccup pause times. Try again!");
                hicptxt.grabFocus();
                return 0;
            }
        }
        return 1;
    }

    private JPanel SLATab() {
        JPanel cPanel = new JPanel();
        cPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        int rowcount = sla_db_percentile.size();
        SLAslab = new JPanel(new GridLayout(rowcount + 1, SLA_tbl_cols));
        SLAslab.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        SLAslab.add(new JLabel("Row"));
        SLAslab.add(new JLabel("Percentile(%)"));
        SLAslab.add(new JLabel("Hiccup(msec)"));

        for (int i = 0; i < rowcount; i++) {
            SLAslab.add(new JLabel(((Integer) (i + 1)).toString()));
            SLAslab.add(new JTextField(sla_db_percentile.get(i).toString()));
            SLAslab.add(new JTextField(sla_db_hicpvalue.get(i).toString()));
        }

        JPanel ButtonSlab = new JPanel(new GridLayout(3, 2));
        JButton bttsla_add = new JButton("Add");
        bttsla_add.setActionCommand("action_sla_add");
        bttsla_add.setMnemonic(KeyEvent.VK_A);
        bttsla_add.setIcon(new ImageIcon(getClass().getResource("icon_add.png")));
        bttsla_add.setToolTipText("Add an SLA Percentile");
        bttsla_add.addActionListener(this);

        JButton bttsla_delete = new JButton("Delete");
        bttsla_delete.setActionCommand("action_sla_delete");
        bttsla_delete.setMnemonic(KeyEvent.VK_D);
        bttsla_delete.setIcon(new ImageIcon(getClass().getResource("icon_delete.png")));
        bttsla_delete.setToolTipText("Delete an SLA Percentile");
        bttsla_delete.addActionListener(this);

        JButton bttsla_apply = new JButton("Apply");
        bttsla_apply.setActionCommand("action_sla_apply");
        bttsla_apply.setIcon(new ImageIcon(getClass().getResource("icon_apply.png")));
        bttsla_apply.setMnemonic(KeyEvent.VK_P);
        bttsla_apply.setToolTipText("Apply to all graphs");
        bttsla_apply.addActionListener(this);

        ButtonSlab.add(bttsla_add);
        ButtonSlab.add(bttsla_delete);
        ButtonSlab.add(bttsla_apply);

        cPanel.add(SLAslab);
        cPanel.add(ButtonSlab);

        return cPanel;
    }

    private JPanel tab_builder(JPanel latencyPanel) {
        JPanel coverPanel = new JPanel(new GridLayout(1,1));
        coverPanel.add(latencyPanel);
        return coverPanel;
    }

    private void add_ithere(JPanel latencyPanel) {
        JPanel p1 = (JPanel) tabbedPane.getSelectedComponent();
        p1.add(latencyPanel);
        p1.setLayout(new GridLayout(1,2));
    }

    private void add_tabs(String hlogFileName, String tag) {
        JPanel latencyPanel =
            new LatencyPanel(db, hlogFileName, tag, jHiccupViewerConfiguration, chk_sla.isSelected(), sla_db_percentile);
        if (tabbedPane.getTabCount() != 0 && is_at_SLAtab() == 0) {
            if (JOptionPane.showConfirmDialog(mainframe, "Open within the current tab?", localfile, JOptionPane.YES_NO_OPTION) == 0) {
                add_ithere(latencyPanel);
                return;
            }
        }
        String shortTabTitle = ConstantsHelper.getTabTitle(true);
        String fullTabTitle = ConstantsHelper.getTabTitle(false);
        tabbedPane.insertTab(shortTabTitle, null, tab_builder(latencyPanel), fullTabTitle, tabbedPane.getTabCount());
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, new TabCloseComponent(shortTabTitle, tabbedPane));
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
    }

    public void clean_all(String filename) {
        localfile = filename;
        mainframe.setTitle(APP_TITLE);
    }

    public void run(String hlogFileName, String tag) throws IOException {
        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.entering("Application", "run");
        }

        updateLabel("Please wait... Processing jHicuup file " + hlogFileName);
        clean_all(hlogFileName);

        ConstantsHelper.detectLogGeneratorTool(hlogFileName, tag);

        db = new DBConnect(((Long) System.currentTimeMillis()).toString());
        pr = new Parser(this.db, hlogFileName, tag, null, null);
        if (pr.execute() != 0) {
            updateLabel("Failed to process jHicupp file " + hlogFileName);
            showMessage("Cannot parse the input file");
            clean_all("no file is open");

            if (jHiccupViewerConfiguration.verbose()) {
                appLogger.exiting("Application", "run", "failed to open file: " + hlogFileName);
            }
            return;
        }
        dbarry.add(db);
        sla_getfromdb();

        showMainPanel();
        add_tabs(hlogFileName, tag);

        if (jHiccupViewerConfiguration.exitAfterParsingFile()) {
            System.exit(1);
        }

        // mainframe.setEnabled(true);
        tabbedPane.repaint();
        SetCursor_ready();

        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.exiting("Application", "run");
        }
    }

    private void SetCursor_ready() {
        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.entering("Application", "SetCursor_ready");
        }

        mainframe.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.exiting("Application", "SetCursor_ready");
        }

    }

    private void exit_routine() {
        if (db != null) {
            db.close_db();
        }
        System.exit(0);
    }

    @Override
    public void run() {
        prepare_ui();
    }

   public static synchronized void initializeLogging() {
        if (_jHiccupLogAnalyzersLogFile == null) { // Only initialize once, never "turn-off" once initialized so that we can avoid strange states
            String tempDirectory = System.getProperty("java.io.tmpdir");
            String userName = System.getProperty("user.name");
            _jHiccupLogAnalyzersLogFile = tempDirectory + File.separator + "HistogramLogAnalyzer_" + userName + "_" + startTimeString + ".log";
            Handler fileHandler = null;
            try {
                fileHandler = new FileHandler(_jHiccupLogAnalyzersLogFile);
            } catch (Exception except) {
                System.err.println("HistogramLogAnalyzer: Unable to open the log file: " + _jHiccupLogAnalyzersLogFile);
                System.err.println("  Message: " + except.getMessage());
                System.err.println("  Cause:   " + except.getCause());
                except.printStackTrace();
                // TODO: add a pop-up to notify the user
                System.exit(1);
            }
            Logger.getLogger("org.hdrhistogram.histogramloganalyzer").addHandler(fileHandler);
            Logger.getLogger("org.hdrhistogram.histogramloganalyzer").setLevel(Level.FINEST);
        }
    }

    private void saveLastDirectory(String lastSelectedDirectory) {
        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.entering("Application", "saveLastDirectory");
        }

        String userHome = System.getProperty("user.home");
        File saveFile = new File(userHome + File.separator + _jHiccupLogFileLastSelectedFilesDirectory);
        try {
            BufferedWriter bufWriter = new BufferedWriter(new FileWriter(saveFile));
            bufWriter.write(lastSelectedDirectory); 
            bufWriter.close(); 
        } catch (Exception except) {
            // Do not do anything if there's an error
        }

        if (jHiccupViewerConfiguration.verbose()) {
            appLogger.exiting("Application", "saveLastDirectory");
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

    public static synchronized String getCurrentWorkingDirectory() {
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
            System.err.println("HistogramLogAnalyzer: UI Theme failure");
            System.err.println("HistogramLogAnalyzer: Snapshot generation exception");
            System.err.println("  Message: " + except.getMessage());
            System.err.println("  Cause:   " + except.getCause());
            except.printStackTrace();
        }

        // Process the properties file and the command-line arguments
        jHiccupViewerConfiguration = new JHiccupViewerConfiguration();
        String userHome = System.getProperty("user.home");
        JHiccupViewerProperties jhvpInUserHome = new JHiccupViewerProperties(jHiccupViewerConfiguration, userHome + File.separator + nameOfjHiccupLogAnalyzerPropertiesFile);
        JHiccupViewerProperties jhvpInLocalDirectory = new JHiccupViewerProperties(jHiccupViewerConfiguration, getCurrentWorkingDirectory() + 
          File.separator + nameOfjHiccupLogAnalyzerPropertiesFile);
        JHiccupViewerCommandLineArguments jhvcla = new JHiccupViewerCommandLineArguments(jHiccupViewerConfiguration, args);

        // Set-up logging
        if (jHiccupViewerConfiguration.verbose()) {
            initializeLogging();
        }

        applicationInstance = new Application();
        SwingUtilities.invokeLater(applicationInstance);
    }
}
