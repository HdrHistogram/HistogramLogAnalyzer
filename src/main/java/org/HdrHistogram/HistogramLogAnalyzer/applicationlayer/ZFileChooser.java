/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import java.awt.Dimension;
import java.awt.FileDialog;
import java.io.File;
import java.io.FilenameFilter;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileFilter;

/**
 * ZFileChooser.java - ZFileChooser.java
 *
 * @since   : 1.0 (Aug 18, 2012)
 */
public final class ZFileChooser {

    private FileDialog fileDialog;
    private JFileChooser fileChooser;
    private JFrame mainFrame;
    private File defaultDir;

    /**
     * @param mainFrame
     */
    public ZFileChooser(JFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    /**
     * @param mainFrame
     */
    public ZFileChooser(JFrame mainFrame, File defaultDir) {
        this.mainFrame = mainFrame;
        this.defaultDir = defaultDir;
    }

    /**
     * @return option selected by the user
     *         mac:       CANCEL_OPTION, APPROVE_OPTION
     *         all other: CANCEL_OPTION, APPROVE_OPTION, ...
     */
    public int showSaveDialog() {
        if (isMac()) {
            getFileDialog().setMode(FileDialog.SAVE);
            getFileDialog().setVisible(true);
            String file = getFileDialog().getFile();
            if (file == null) {
                return JFileChooser.CANCEL_OPTION;
            }
            return JFileChooser.APPROVE_OPTION;
        } else {
            return getFileChooser().showSaveDialog(mainFrame);
        }
    }

    /**
     * @return option selected by the user
     *         mac:       CANCEL_OPTION, APPROVE_OPTION
     *         all other: CANCEL_OPTION, APPROVE_OPTION, ...
     */
    public int showOpenDialog() {
        if (isMac()) {
            getFileDialog().setMode(FileDialog.LOAD);
            getFileDialog().setVisible(true);
            String file = getFileDialog().getFile();
            if (file == null) {
                return JFileChooser.CANCEL_OPTION;
            }
            return JFileChooser.APPROVE_OPTION;
        } else {
            return getFileChooser().showOpenDialog(mainFrame);
        }
    }

    /**
     * @return File object that contains information about the file selected in
     *         the dialog
     */
    public File getSelectedFile() {
        if (isMac()) {
            String dir = getFileDialog().getDirectory();
            String file = getFileDialog().getFile();
            return new File(dir, file);
        } else {
            return getFileChooser().getSelectedFile();
        }
    }

    File[] getSelectedFiles() {
        if (isMac()) {
            return getFileDialog().getFiles();
        } else {
            return getFileChooser().getSelectedFiles();
        }
    }

    /**
     * @param fil is the File object that contains the information to which to set
     *        the file field
     */
    public void setSelectedFile(File fil) {
        if (isMac()) {
            // TODO: getFileDialog().setFile(fil);
        } else {
            getFileChooser().setSelectedFile(fil);
        }
    }

    /**
     * @return File object that contains information about the directory displayed
     *         in the dialog
     */
    public File getCurrentDirectory() {
        if (isMac()) {
            return new File(getFileDialog().getDirectory());
        } else {
            return getFileChooser().getCurrentDirectory();
        }
    }

    /**
     * @param dir is the File object that contains the information to which to set
     *        the directory field
     */
    public void setCurrentDirectory(File dir) {
        if (isMac()) {
            // TODO: getFileDialog().setDirectory(dir.something());
        } else {
            getFileChooser().setCurrentDirectory(dir);
        }
    }

    /**
     * @param dialogTitle is the title to display in the dialog
     */
    public void setDialogTitle(String dialogTitle) {
        if (isMac()) {
            // TODO: getFileDialog().setDialogTitle(dialogTitle);
        } else {
            getFileChooser().setDialogTitle(dialogTitle);
        }
    }

    /**
     * @return FileDialog
     */
    private FileDialog getFileDialog() {
        if (fileDialog == null) {
            fileDialog = new FileDialog(mainFrame, "Histogram Log Analyzer");
            udateDefaultDir(defaultDir);
        }
        return fileDialog;
    }

    private JFileChooser getFileChooser() {
        if (fileChooser == null) {
            fileChooser = new JFileChooser(defaultDir);
        }
        return fileChooser;
    }

    /**
     * @param aFilter
     */
    public void addChoosableFileFilter(final FileFilter aFilter) {
        if (isMac()) {
            getFileDialog().setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name != null ? aFilter.accept(new File(dir, name)) : aFilter.accept(dir);
                }
            });

        }
        else {
            getFileChooser().addChoosableFileFilter(aFilter);
        }

    }

    private static boolean isMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf("mac") >= 0);
    }

    /**
     * @param filesOnly
     */
    public void setFileSelectionMode(int filesOnly) {
        if (isMac()) {

        } else {
            getFileChooser().setFileSelectionMode(filesOnly);
        }
    }

    /**
     * @param dimension
     */
    public void setPreferredSize(Dimension dimension) {
        if (isMac()) {
            getFileDialog().setPreferredSize(dimension);
        } else {
            getFileChooser().setPreferredSize(dimension);
        }
    }

    /**
     * @param dDir
     */
    private void udateDefaultDir(File dDir) {
        if (isMac()) {
            if (dDir != null) {
                if (dDir.isDirectory()) {
                    getFileDialog().setDirectory(dDir.toString());
                } else if (dDir.isFile()) {
                    getFileDialog().setFile(dDir.toString());
                }
            }
        }
    }

    void setMultipleMode(boolean enable) {
        if (isMac()) {
            getFileDialog().setMultipleMode(enable);
        } else {
            getFileChooser().setMultiSelectionEnabled(enable);
        }
    }
}
