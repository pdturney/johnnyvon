/* JohnnyVon -- An implementation of self-replicating automata 
   in two-dimensional continuous space.
   Copyright (C) 2002 National Research Council Canada

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

Authors:
	Robert Ewaschuk - raewasch@uwaterloo.ca
	Arnold Smith - arnold.smith@nrc.ca
	Peter Turney - peter.turney@nrc.ca

Postal Contact:
	Peter Turney
	Institute for Information Technology
	National Research Council Canada
	M-50, Montreal Road
	Ottawa, ON, Canada
	K1A 0R6

*/

package JohnnyVon.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.io.*;
import java.net.*;

import JohnnyVon.engine.Simulator;

/** A window that contains a CodonViewer and a ConfigurationPanel on two
 * different tabs.  It also has a menu for controlling various options, and
 * generally martials the various actions that take place on the GUI.
 * @author <a href="mailto:raewasch@uwaterloo.ca">Rob Ewaschuk</a>, 
 * <a href="mailto:arnold.smith@nrc.ca">Arnold Smith</a>, 
 * <a href="mailto:peter.turney@nrc.ca">Peter Turney</a>
 * @version 1.0  Copyright &copy; 2002 National Research Council Canada
 */
public class JohnnyVonDisplay extends JFrame {

	public static final String FALLBACK_TEXT = "Default Chain of 8 {\n"
			+ "# Select \"Custom\" to enable editting here.\n"
			+ "#\n"
			+ "# Size of container (units).  Container extends this far from the\n"
			+ "# origin on each axis.\n"
			+ "80\n"
			+ "# Number of Codons total\n"
			+ "88\n"
			+ "# Number of specified codons.  (Remainder will be generated randomly.)\n"
			+ "# This must be at most the number of lines specifying codons below.\n"
			+ "8\n"
			+ "# Starting settings for each codon.  Must have at least as many\n"
			+ "# as the number right above here.\n"
			+ "# x, y, vx, vy, angle, angular_velocity, type\n"
			+ "# (x, y) = location.  (x and y must be between within -(size of container)\n"
			+ "# and +(size of container).\n"
			+ "# (vx, vy) = initial linear velocity.\n"
			+ "# angle = initial angle (radians, CCW from East.)\n"
			+ "# angular_velocity = angular_velocity (radians/sec)\n"
			+ "# type = the bit of information encoded by this codon.  0 or 1.\n"
			+ "-56 4 0 0 90 0 1\n"
			+ "-42 4 0 0 90 0 0\n"
			+ "-28 4 0 0 90 0 1\n"
			+ "-14 4 0 0 90 0 0\n"
			+ "0 4 0 0 90 0 1\n"
			+ "14 4 0 0 90 0 0\n"
			+ "28 4 0 0 90 0 1\n"
			+ "42 4 0 0 90 0 0\n"
			+ "}\n\n";

	private final CodonViewer viewer;

	private final JMenuBar menu;

	private final JTabbedPane mainPane;

	private final JLabel status;
		
	private final JViewport viewport = new JViewport();
	private final JScrollPane viewerPane;
	private final ConfigurationPanel configPane;

	private final JButton playPauseButton;
	private final JButton stopButton;
	private final JButton stepButton;
	private final Icon playIcon;
	private final Icon pauseIcon;
	private final Icon stopIcon;
	private final Icon stepIcon;

  // do not start running immediately -- give user time to configure the settings
	private boolean paused = true;  

	public JohnnyVonDisplay(BufferedReader reader, Closer closer) throws IOException {
		super("JohnnyVon");		

		this.status = new JLabel("Initializing...");
		
		this.viewerPane = new JScrollPane(viewport, 
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		this.viewer = new CodonViewer(this.status, viewport);
		this.viewport.setView(this.viewer);
		this.configPane = new ConfigurationPanel(reader, this.viewer);

		this.mainPane = new JTabbedPane(JTabbedPane.TOP);
		this.mainPane.add("View", this.viewerPane);
		this.mainPane.add("Setup", this.configPane);

		// Not supported in JDK 1.3
		//this.mainPane.setMnemonicAt(0, KeyEvent.VK_W);
		//this.mainPane.setMnemonicAt(1, KeyEvent.VK_U);
		
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(mainPane);

		JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));

		ImageIcon pauseIcon = null;
		ImageIcon playIcon = null;
		ImageIcon stopIcon = null;
		ImageIcon stepIcon = null;

		try {
			ClassLoader loader = this.getClass().getClassLoader(); 
			URL url;

		 	url = loader.getResource("JohnnyVon/support/pause.gif"); 
			if (url != null)
				pauseIcon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(url)); 
			
			url = loader.getResource("JohnnyVon/support/play.gif"); 
			if (url != null)
				playIcon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(url)); 

			url = loader.getResource("JohnnyVon/support/stop.gif"); 
			if (url != null)
				stopIcon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(url)); 

			url = loader.getResource("JohnnyVon/support/step.gif"); 
			if (url != null)
				stepIcon = new ImageIcon(Toolkit.getDefaultToolkit().getImage(url)); 

		} catch (Exception e) {
			System.out.println("Failed to load pause image.");
			e.printStackTrace();
			
		}
		this.pauseIcon = pauseIcon;
		this.playIcon = playIcon;
		this.stopIcon = stopIcon;
		this.stepIcon = stepIcon;

		Action playPauseAction = new TogglePauseAction();
		this.playPauseButton = new JButton(playPauseAction);
		
		if (this.pauseIcon != null && this.playIcon != null) {
			this.playPauseButton.setText("");
			if (this.paused == true) this.playPauseButton.setIcon(this.playIcon);
			else this.playPauseButton.setIcon(this.pauseIcon);
			this.playPauseButton.setBorder(BorderFactory.createBevelBorder(
						BevelBorder.RAISED));
		}

		if (this.stepIcon != null) {
			this.stepButton = new JButton(new StepAction(this.stepIcon));
			this.stepButton.setText("");
			this.stepButton.setBorder(BorderFactory.createBevelBorder(
						BevelBorder.RAISED));
		} else this.stepButton = new JButton(new StepAction());
	
		if (this.stopIcon != null) {
			this.stopButton = new JButton(new StopAction(this.stopIcon));
			this.stopButton.setText("");
			this.stopButton.setBorder(BorderFactory.createBevelBorder(
						BevelBorder.RAISED));
		} else this.stopButton = new JButton(new StopAction());
			
		bottom.add(this.playPauseButton);
		bottom.add(this.stopButton);
		bottom.add(this.stepButton);
		bottom.add(status);

		this.menu = new JMenuBar();
		this.buildMenues(closer, playPauseAction);

		this.getContentPane().add(bottom, BorderLayout.SOUTH);

		this.pack();

	  this.viewer.zoomToFit();
	}

	private final void buildMenues(Closer closer, Action playPauseAction) {

		JMenu file = new JMenu("File");
		file.setMnemonic(KeyEvent.VK_F);
		
		this.addMenuItem(file, new AboutAction(), KeyEvent.VK_A, KeyEvent.VK_H);
		file.add(new JSeparator());
		this.addMenuItem(file, new CloseAction(closer), KeyEvent.VK_C, KeyEvent.VK_X);

		JCheckBoxMenuItem temp;
		
		JMenu viewMenu = new JMenu("View");
		viewMenu.setMnemonic(KeyEvent.VK_V);

		this.addMenuItem(viewMenu, new ZoomInAction(), 
				KeyEvent.VK_I, KeyEvent.VK_I);
		this.addMenuItem(viewMenu, new ZoomOutAction(), 
				KeyEvent.VK_O, KeyEvent.VK_O);
		this.addMenuItem(viewMenu, new ZoomToFitAction(), 
				KeyEvent.VK_Z, KeyEvent.VK_Z);
		viewMenu.add(new JSeparator());
		this.addMenuItem(viewMenu, playPauseAction,
			 	KeyEvent.VK_P, KeyEvent.VK_P);
		viewMenu.add(new JSeparator());
		this.addMenuItem(viewMenu, new ToggleShowArmsAction(), 
				KeyEvent.VK_A, KeyEvent.VK_R).setSelected(true);
		this.addMenuItem(viewMenu, new ToggleShowFieldsAction(), 
				KeyEvent.VK_S, KeyEvent.VK_E).setSelected(true);
		this.addMenuItem(viewMenu, new ToggleShowForcesAction(), 
				KeyEvent.VK_F, KeyEvent.VK_S).setSelected(true);
		/* No worky...FIXME ? /
		viewMenu.add(new JSeparator());
		viewMenu.add(new ToggleFullWindowModeAction()); //*/
		viewMenu.add(new JSeparator());
		this.addMenuItem(viewMenu, new ToggleColorsAction(), 
				KeyEvent.VK_C, KeyEvent.VK_C); 
		
		this.menu.add(file);
		this.menu.add(viewMenu);
		super.setJMenuBar(this.menu);
	}

	private JMenuItem addMenuItem(JMenu target, Action action, int mnemonic,
			int accel) {
		JMenuItem item = new JMenuItem(action);
		item.setMnemonic(mnemonic);
		item.setAccelerator(KeyStroke.getKeyStroke(accel, ActionEvent.ALT_MASK));
		target.add(item);
		return item;
	}

	public void setPaused(boolean paused) { 
		this.paused = paused;
		if (this.playIcon != null) {
			if (paused) this.playPauseButton.setIcon(this.playIcon);
			else this.playPauseButton.setIcon(this.pauseIcon);
		} else {
			// Fall back to text labels.
			if (paused) this.playPauseButton.setText("Play ");
			else this.playPauseButton.setText("Pause");
		}
		this.viewer.setPaused(paused); 
	}

	private class ZoomInAction extends AbstractAction {
		public ZoomInAction() { super("Zoom In"); }
		public void actionPerformed(ActionEvent ae) { viewer.zoomIn(); }
	}

	private class ZoomOutAction extends AbstractAction {
		public ZoomOutAction() { super("Zoom Out"); }
		public void actionPerformed(ActionEvent ae) { viewer.zoomOut(); }
	}

	private class ZoomToFitAction extends AbstractAction {
		public ZoomToFitAction() { super("Zoom In To Fit"); }
		public void actionPerformed(ActionEvent ae) { viewer.zoomToFit(); }
	}

	private class TogglePauseAction extends AbstractAction {
		public TogglePauseAction() { super("Pause"); }
		public void actionPerformed(ActionEvent ae) { 
			JohnnyVonDisplay.this.setPaused(!JohnnyVonDisplay.this.paused);
		}
						
	}

	private class StopAction extends AbstractAction {
		public StopAction(Icon icon) { super("Stop", icon); }
		public StopAction() { super("Stop"); }
		public void actionPerformed(ActionEvent ae) { 
			JohnnyVonDisplay.this.setPaused(true);
			JohnnyVonDisplay.this.configPane.applyCurrent();
		}
	}

	private class StepAction extends AbstractAction {
		public StepAction(Icon icon) { super("Step", icon); }
		public StepAction() { super("Step"); }
		public void actionPerformed(ActionEvent ae) { 
			JohnnyVonDisplay.this.setPaused(true);
			JohnnyVonDisplay.this.viewer.step();
		}
	}

	private class ToggleShowArmsAction extends AbstractAction {
		public ToggleShowArmsAction() { super("Show Arms"); }
		public void actionPerformed(ActionEvent ae) { viewer.toggleArms(); }
	}

	private class ToggleShowForcesAction extends AbstractAction {
		public ToggleShowForcesAction() { super("Show Forces"); }
		public void actionPerformed(ActionEvent ae) { viewer.toggleForces(); }
	}

	private class ToggleShowFieldsAction extends AbstractAction {
		public ToggleShowFieldsAction() { super("Show Fields"); }
		public void actionPerformed(ActionEvent ae) { viewer.toggleFields(); }
	}

	private class ToggleColorsAction extends AbstractAction {
		private boolean blackBackground = true;
		public ToggleColorsAction() { super("Toggle Colors"); }
		public void actionPerformed(ActionEvent ae) { 
			this.blackBackground = !this.blackBackground;
			if (this.blackBackground) {
				viewer.setColors(Color.black, Color.white);
			} else {
				viewer.setColors(Color.white, Color.black);
			}
		}
	}

	private class ToggleFullWindowModeAction extends AbstractAction {
		private boolean enabled = false;
		public ToggleFullWindowModeAction() { 
			super("Full Window");
			JohnnyVonDisplay.this.viewer.addMouseListener( 
					new MouseAdapter() { 
						public void mouseClicked(MouseEvent me) { swap(); } }
				);
	 	}

		
		public void actionPerformed(ActionEvent ae) {
			this.swap();
		}

		private void swap() {
			this.enabled = !this.enabled;
			JohnnyVonDisplay.this.menu.setVisible(!this.enabled);
			if (this.enabled) {
				JohnnyVonDisplay.this.getContentPane().remove(mainPane);
				JohnnyVonDisplay.this.viewerPane.setViewport(null);
				//JohnnyVonDisplay.
//				JohnnyVonDisplay.this.viewport.setView(null);
				JohnnyVonDisplay.this.mainPane.remove(viewerPane);
				JohnnyVonDisplay.this.getContentPane().add(viewport);
			} else {
				JohnnyVonDisplay.this.getContentPane().remove(viewport);
				JohnnyVonDisplay.this.viewerPane.setViewport(viewport);
				JohnnyVonDisplay.this.mainPane.add(viewerPane);
				JohnnyVonDisplay.this.getContentPane().add(mainPane);
//				JohnnyVonDisplay.this.viewport.setView(viewer);
			}
			JohnnyVonDisplay.this.viewer.zoomToFit();

		}
	}

	/** An interface that is passed into this display to call when the display
	 * window is closed or File/Quit is selected from the Menu.  In an
	 * application, this might exit, whereas in an applet, it might merely call
	 * stop() on the applet itself.
	 * @author <a href="mailto:raewasch@uwaterloo.ca">Rob Ewaschuk</a>, 
	 * <a href="mailto:arnold.smith@nrc.ca">Arnold Smith</a>, 
	 * <a href="mailto:peter.turney@nrc.ca">Peter Turney</a>
	 * @version 1.0  Copyright &copy; 2002 National Research Council Canada
	 */
	public interface Closer {
		public void close(JohnnyVonDisplay display);
	}

	private class CloseAction extends AbstractAction {
		private final Closer closer;
		public CloseAction(Closer closer) { 
			super("Close"); 
			this.closer = closer;
		}
		
		public void actionPerformed(ActionEvent ae) {
			this.closer.close(JohnnyVonDisplay.this);
		//	JohnnyVonDisplay.this.setVisible(false);
		}
	}

	private class AboutAction extends AbstractAction {
		private AboutWindow aboutWindow;
		public AboutAction() { super("About..."); }
		public void actionPerformed(ActionEvent ae) { 
			if (this.aboutWindow == null) { this.aboutWindow = new AboutWindow(); }
			this.aboutWindow.setVisible(true);
		}
	}
}
