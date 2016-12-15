/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

class DnDTabbedPane extends JTabbedPane {

    private BufferedImage dragImage = null;
    private MouseDragTracker mouseTracker = new MouseDragTracker();

    DnDTabbedPane() {
        super();
        installListeners();

        setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
            }
        });
    }

    private void installListeners() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                mouseTracker.mouseDraggedHandler(e);
                repaint();
                super.mouseDragged(e);
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                mouseTracker.mouseReleasedHandler(e);
                repaint();
                super.mouseReleased(e);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if(mouseTracker.isDragging()) {
            int x = mouseTracker.tabStartPoint.x + mouseTracker.currentPoint.x - mouseTracker.startPoint.x;
            int y = mouseTracker.tabStartPoint.y + mouseTracker.currentPoint.y - mouseTracker.startPoint.y;
            g.drawImage(dragImage, x, y, this);
        }
    }

    /*
     * create transparent button with dashed border as drag image
     */
    private void createDragImage(Rectangle bounds) {
        JButton b = new JButton();
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorder(BorderFactory.createDashedBorder(Color.BLACK));
        b.setBounds(0, 0, bounds.width, bounds.height);
        dragImage = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
        b.print(dragImage.createGraphics());
    }

    private void switchTabs(int fromIndex, int toIndex) {
        if (fromIndex == toIndex) {
            return;
        }
        Component comp = getComponentAt(fromIndex);
        TabsListener tabsListener = ((TabCloseComponent)getTabComponentAt(fromIndex)).getTabsListener();
        String title = getTitleAt(fromIndex);
        String tip = getToolTipTextAt(fromIndex);
        removeTabAt(fromIndex);

        insertTab(title, null, comp, tip, toIndex);
        setTabComponentAt(toIndex, new TabCloseComponent(title, DnDTabbedPane.this, tabsListener));

        setSelectedIndex(toIndex);
        repaint();
    }

    private class MouseDragTracker {

        private boolean dragging = false;
        private int draggedTabIndex = 0;

        private Point tabStartPoint;
        private Point startPoint;
        private Point currentPoint;

        private boolean isDragging() {
            return dragging;
        }

        private void startDragging(MouseEvent event) {
            dragging = true;
        }

        private void stopDragging() {
            dragging = false;
        }

        private void mouseReleasedHandler(MouseEvent e) {
            stopDragging();
        }

        private void mouseDraggedHandler(MouseEvent e) {
            int tabIndex = DnDTabbedPane.this.getUI().tabForCoordinate(DnDTabbedPane.this, e.getX(), e.getY());
            if (tabIndex == -1) {
                return;
            }

            if(!dragging) {
                draggedTabIndex = tabIndex;
                Rectangle bounds = getUI().getTabBounds(DnDTabbedPane.this, tabIndex);
                createDragImage(bounds);

                tabStartPoint = new Point(bounds.x, bounds.y);
                startPoint = e.getPoint();
                currentPoint = (Point)startPoint.clone();
                startDragging(e);
            } else {
                currentPoint = e.getPoint();
                checkSwitchTabs(e, tabIndex);
            }
        }

        /*
         * Switch tabs when mouse overcomes third part of right/left tab on X axis
         */
        private void checkSwitchTabs(MouseEvent e, int tabIndex) {
            Rectangle tabBounds = getUI().getTabBounds(DnDTabbedPane.this, tabIndex);
            boolean isNextRight = (tabIndex > draggedTabIndex);

            if (isNextRight) {
                int switchX = tabBounds.x + (tabBounds.width / 3);
                if (e.getX() > switchX) {
                    switchTabs(draggedTabIndex, tabIndex);
                    draggedTabIndex = tabIndex;
                }
            } else {
                int switchX = tabBounds.x + (2 * tabBounds.width / 3);
                if (e.getX() < switchX) {
                    switchTabs(draggedTabIndex, tabIndex);
                    draggedTabIndex = tabIndex;
                }
            }
        }
    }
}