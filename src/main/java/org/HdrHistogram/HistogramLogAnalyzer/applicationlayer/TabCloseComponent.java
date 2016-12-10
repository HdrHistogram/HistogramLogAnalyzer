/**
 * Written by Azul Systems, and released to the public domain,
 * as explained at http://creativecommons.org/publicdomain/zero/1.0/
 */

package org.HdrHistogram.HistogramLogAnalyzer.applicationlayer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicButtonUI;

@SuppressWarnings("serial")
public class TabCloseComponent extends JPanel {
	private final JTabbedPane pane;
	private final JLabel label;
	private final JButton button = new TabButton();
	private final TabsListener tabsListener;

	/**
	 * @param tabTitle
	 * @param pane
	 */
	public TabCloseComponent(String tabTitle, JTabbedPane pane, TabsListener tabsListener) {
		super(new FlowLayout(FlowLayout.LEFT, 0, 0));
		this.pane = pane;
		this.tabsListener = tabsListener;
		setOpaque(false);
		label = new JLabel(tabTitle);
		add(label);
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
		add(button);
		setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
	}

	/**
	 * Tab Button
	 */
	private class TabButton extends JButton implements ActionListener {
		public TabButton() {
			int size = 17;
			setIcon((new ImageIcon(getClass().getResource("icon_close2.png"))));
			setPreferredSize(new Dimension(size, size));
			setToolTipText("close this tab");
			setUI(new BasicButtonUI());
			setContentAreaFilled(false);
			setFocusable(false);
			setBorder(BorderFactory.createEtchedBorder());
			setBorderPainted(false);
			addMouseListener(buttonMouseListener);
			setRolloverEnabled(true);
			addActionListener(this);
		}

		/**
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
        public void actionPerformed(ActionEvent e) {
		    int index = pane.indexOfTabComponent(TabCloseComponent.this);
            if(JOptionPane.showConfirmDialog(pane.getParent().getParent(),
					"Are you sure you want to close the tab?",pane.getTitleAt(index),JOptionPane.YES_NO_OPTION)==0)
			{
                if (index != -1) {
                    pane.remove(index);
					if (pane.getTabCount() == 0) {
						tabsListener.lastTabClosed();
					}
                }
			}
		}

		/**
		 * @see javax.swing.JButton#updateUI()
		 */
		@Override
        public void updateUI() {
		}
	}

	/**
	 *
	 */
	private final static MouseListener buttonMouseListener = new MouseAdapter() {

		/**
		 * @see java.awt.event.MouseAdapter#mouseEntered(java.awt.event.MouseEvent)
		 */
		@Override
        public void mouseEntered(MouseEvent e) {
			Component component = e.getComponent();
			if (component instanceof AbstractButton) {
				AbstractButton button = (AbstractButton) component;
				button.setBorderPainted(true);
			}
		}

		/**
		 * @see java.awt.event.MouseAdapter#mouseExited(java.awt.event.MouseEvent)
		 */
		@Override
        public void mouseExited(MouseEvent e) {
			Component component = e.getComponent();
			if (component instanceof AbstractButton) {
				AbstractButton button = (AbstractButton) component;
				button.setBorderPainted(false);
			}
		}
	};
}
