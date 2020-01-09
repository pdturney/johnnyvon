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
import java.util.HashMap;
import java.util.Map;

import JohnnyVon.engine.*;

/** A panel that allows you to select one of several different
 * configurations, or edit one to create your own.  Also controls steps per
 * redraw.
 * @author <a href="mailto:raewasch@uwaterloo.ca">Rob Ewaschuk</a>, 
 * <a href="mailto:arnold.smith@nrc.ca">Arnold Smith</a>, 
 * <a href="mailto:peter.turney@nrc.ca">Peter Turney</a>
 * @version 1.0  Copyright &copy; 2002 National Research Council Canada
 */
public class ConfigurationPanel extends JPanel {

	private Simulator simulator;

	private final CodonViewer viewer;

	private JTextArea input;

	private Map settings = new HashMap();

	private final JCheckBox custom;

	private final JComboBox choices;

	private final JButton apply;

	private final JTextField steps = new JTextField(4);

	public ConfigurationPanel(BufferedReader in, final CodonViewer viewer) 
		throws IOException {
		this.viewer = viewer;

		super.setLayout(new BorderLayout());
		JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
				
		JLabel stepsLabel = new JLabel("Steps per draw: ");
		controls.add(stepsLabel);
		controls.add(this.steps);
		stepsLabel.setDisplayedMnemonic('d');
		this.steps.setFocusAccelerator('d');
		this.steps.setText(Integer.toString(viewer.getStepsPerDraw()));

		this.steps.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					try {
						viewer.setStepsPerDraw(Integer.parseInt(steps.getText()));
					} catch (RuntimeException rte) {
						steps.setText(Integer.toString(viewer.getStepsPerDraw()));
					}
				}
			} );


		this.custom = new JCheckBox("Custom", false);
		this.apply = new JButton("Apply");
		this.input = new JTextArea();
		this.input.setEditable(false);
		this.input.setEnabled(false);
		this.choices = new JComboBox(this.loadChoices(in));
		controls.add(this.custom);
		controls.add(this.choices);
		controls.add(this.apply);

		LocalListener listener = new LocalListener();
		this.custom.addActionListener(listener);
		this.choices.addActionListener(listener);
		this.apply.addActionListener(listener);
		this.apply.setMnemonic(KeyEvent.VK_A);
		this.custom.setMnemonic(KeyEvent.VK_M);
		
		this.add(
			new JScrollPane(this.input, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
		this.add(controls, BorderLayout.SOUTH);


	}

	public class LocalListener implements ActionListener {
		
		public void actionPerformed(ActionEvent ae) {
			if (ae.getSource() == custom) {
				boolean selected = custom.isSelected();
				choices.setEnabled(!selected);
				input.setEditable(selected);
				input.setEnabled(selected);
			} else if (ae.getSource() == choices) {
				Object selected = choices.getSelectedItem();
				input.setText((String)settings.get(selected));
			} else if (ae.getSource() == apply) {
				applyCurrent();
			}
		}
	}

	private ComboBoxModel loadChoices(BufferedReader in) throws IOException {
		DefaultComboBoxModel results = new DefaultComboBoxModel();

		boolean first = true;
		
		String name = "";
		while (in.ready() && name != null) {
			name = in.readLine();

			if (name != null && !name.startsWith("#") && name.endsWith("{")) {
				StringBuffer value = new StringBuffer();
				boolean done = false;
				while (!done) {
					String temp = in.readLine();
					if (temp.trim().equals("}")) {
						done = true;
					} else { value.append(temp); value.append('\n'); }
				}
				if (first) { 
					first = false;
					this.input.setText(value.toString());
					this.applyCurrent();
				}
				name = name.substring(0, name.length() - 1);
				this.settings.put(name, value.toString());
				results.addElement(name);
			}
		}
		return results;
	}

	public void applyCurrent() {
		try {
			this.shutdown();
			this.simulator = 
				new Simulator(new BufferedReader(new StringReader(this.input.getText())));
			this.viewer.setSimulator(this.simulator);
			this.simulator.setViewer(this.viewer);
			new Thread(this.simulator).start();
		} catch (Exception e) {
			System.out.println(this.input.getText());
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, 
					"Syntax error in current settings.", "Syntax Error", 
					JOptionPane.ERROR_MESSAGE);
		}
	}

	public void shutdown() { 
		if (this.simulator != null) { this.simulator.shutdown(); }
	}

}
