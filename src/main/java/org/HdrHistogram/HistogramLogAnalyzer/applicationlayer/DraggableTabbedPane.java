/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;

import javax.swing.JTabbedPane;


public class DraggableTabbedPane extends JTabbedPane {

	private static final long serialVersionUID = 1L;
private boolean dragging = false;
  private Image tabImage = null;
  private Point currentMouseLocation = null;
  private int draggedTabIndex = 0;

  public DraggableTabbedPane() {
    super();
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
            setTabComponentAt(tabNumber, new TabCloseComponent(title, DraggableTabbedPane.this));
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
}