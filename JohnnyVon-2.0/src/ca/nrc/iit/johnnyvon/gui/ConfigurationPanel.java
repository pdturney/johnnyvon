/* JohnnyVon -- An implementation of self-replicating automata 
   in two-dimensional continuous space.
   Copyright (C) 2002-2004 National Research Council Canada

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
	Robert Ewaschuk - rob@infinitepigeons.org
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

package ca.nrc.iit.johnnyvon.gui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.io.*;
import java.util.*;

import ca.nrc.iit.johnnyvon.engine.*;

/** A panel that allows you to select one of several different
 * configurations, or edit one to create your own.  Also controls steps per
 * redraw.
 *
 * @author <a href="mailto:rob@infinitepigeons.org">Rob Ewaschuk</a>, 
 * <a href="mailto:arnold.smith@nrc.ca">Arnold Smith</a>, 
 * <a href="mailto:peter.turney@nrc.ca">Peter Turney</a>
 * @version 2.0  Copyright &copy; 2002-2004 National Research Council Canada
 */
public class ConfigurationPanel extends JPanel {

	/** This is the simulator logic, for applying configuration changes. */
	private Simulator _simulator;

	/** This is the viewer system, for applying visual changes */
	private final CodonViewer _viewer;

	/** Properties input.  This is a nasty hack so we don't have to bother with
	 * a more complex input system.  It's simple and crappy. */
	private JTextArea _input = new JTextArea();

	/** The various settings contained in the input file. */
	private final JComboBox _choices = new JComboBox();

	/** Button to apply a new choice in _choices, or changes in _input */
	private final JButton _apply = new JButton("Apply");

	/** The properties that were originally loaded.  We may modify these. */
	private Properties _properties;

	public ConfigurationPanel(Properties properties, CodonViewer viewer) throws IOException {

		this._viewer = viewer;
		this._properties = properties;

		// Create local widgets
		JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		this._input.setEditable(true);
		this._input.setEnabled(true);
		this._input.setRows(20);
		this._input.setText(this.getText());
		this._apply.setMnemonic(KeyEvent.VK_A);
		this._choices.setModel(this.getChoices());

		this.addActionListeners();

		// Layout widgets
		super.setLayout(new BorderLayout());

		controls.add(this._choices);
		controls.add(this._apply);

		this.add(new JScrollPane(this._input, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
		this.add(controls, BorderLayout.SOUTH);

		// Check for a default setting, and apply it if possible.
		String def = properties.getProperty("Default");
		if(def != null) {
			this._choices.setSelectedItem(def);
		}

		// Whether there's a specified default or not, we should apply the current
		// setting.
		applyCurrent();

	}

	/** Get the properties in _properties as a string */
	private String getText() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			_properties.store(baos, "");
			String[] values = new String(baos.toByteArray(), "ISO8859-1").split("\n");
			Arrays.sort(values);
			StringBuffer temp = new StringBuffer();
			for (int i = 0; i < values.length; i++) {
				values[i] = values[i].replaceAll(";", ";\\\\\n");
				values[i] = values[i].replaceAll("\\\\t", "\t");
				temp.append(values[i]);
				temp.append("\n");
			}
					
			return temp.toString();
		} catch (IOException ioe) {
			throw new RuntimeException("IOException in GUI.", ioe);
		}
	}

	private void addActionListeners() {


		/*
		this._choices.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				Object selected = _choices.getSelectedItem();
				try {
				} catch (IOException ioe) {
					throw new RuntimeException("IOException in GUI.", ioe);
				}
			}
		});
		*/

		this._apply.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				applyCurrent();
			}
		});


	}

	private ComboBoxModel getChoices() throws IOException {
		DefaultComboBoxModel results = new DefaultComboBoxModel();

		Iterator keys = this._properties.keySet().iterator();
		
		while (keys.hasNext()) {
			String name = (String)keys.next();
			if (!name.equals("Default") && name.indexOf(".") == -1) {
				results.addElement(name);
			}
		}
		return results;
	}

	public void applyCurrent() {

		try {
			// This is stupid.
			ByteArrayInputStream bais = new ByteArrayInputStream(this._input.getText().getBytes("ISO8859-1"));
			this._properties.load(bais);

			this.shutdown();
			this._simulator = new Simulator(this._properties, this._choices.getSelectedItem().toString());
			
			this._viewer.setSimulator(this._simulator);
			this._simulator.setViewer(this._viewer);
			new Thread(this._simulator, "JohnnyVon Simulator").start();
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, 
					"Syntax error in current settings.", "Syntax Error", 
					JOptionPane.ERROR_MESSAGE);
		}
	}

	public void shutdown() { 
		if (this._simulator != null) { this._simulator.shutdown(); }
	}

}
