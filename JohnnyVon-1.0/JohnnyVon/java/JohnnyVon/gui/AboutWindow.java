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
import javax.swing.event.*;

/** A simple "about" window for displaying licence, authors and a quick
 * description. 
 * @author <a href="mailto:raewasch@uwaterloo.ca">Rob Ewaschuk</a>, 
 * <a href="mailto:arnold.smith@nrc.ca">Arnold Smith</a>, 
 * <a href="mailto:peter.turney@nrc.ca">Peter Turney</a>
 * @version 1.0  Copyright &copy; 2002 National Research Council Canada
 */
public class AboutWindow extends JFrame {

	/** Create an about window. */
	public AboutWindow() {
		super("About");

		// Not supported in JDK 1.3
		//JTabbedPane pane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.WRAP_TAB_LAYOUT);

		JTabbedPane pane = new JTabbedPane(JTabbedPane.TOP);

		// Create the content for each pane
		JTextArea licence = new JTextArea(
	"   JohnnyVon -- An implementation of self-replicating automata "
	+ "in two-dimensional continuous space.\n"
	+ "    Copyright (C) 2002 National Research Council Canada\n\n"
	+ "This program is free software; you can redistribute it and/or "
	+ "modify it under the terms of the GNU General Public License "
	+ "as published by the Free Software Foundation; either version 2 "
	+ "of the License, or (at your option) any later version.\n\n"
	+ "This program is distributed in the hope that it will be useful, "
	+ "but WITHOUT ANY WARRANTY; without even the implied warranty of "
	+ "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the "
	+ "GNU General Public License for more details.\n\n"
	+ "You should have received a copy of the GNU General Public License "
	+ "along with this program; if not, write to the Free Software "
	+ "Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.\n");

		licence.setEditable(false);
		licence.setLineWrap(true);
		licence.setWrapStyleWord(true);

		JTextArea general = new JTextArea( "Overview\n"
		+ "\n"
		+ "JohnnyVon is a project to produce self-replicating automata in a continuous 2D space.\n"
		+ "\n"
		+ "It simulates a simple 2D physics-ish universe, with widgets called Codons that interact under a fairly simple, plausible set of rules, and have the replication of long strings of these Codons as an emergent behaviour of these rules.\n"

);
		general.setEditable(false);
		general.setLineWrap(true);
		general.setWrapStyleWord(true);
				
		JTextArea authors = new JTextArea("JohnnyVon was developed by the following people:\n"
		+ "Robert Ewaschuk - raewasch@uwaterloo.ca\n"
		+ "Arnold Smith - arnold.smith@nrc.ca\n"
		+ "Peter Turney - peter.turney@nrc.ca\n"
		+ "\n"
		+ "National Research Council of Canada:\n"
		+ "http://www.nrc.ca/\n"
		+ "\n"
		+ "Institute for Information Technology:\n"
		+ "http://www.iit.nrc.ca/\n"
		+ "\n"
		+ "Interactive Information Group:\n"
		+ "http://www.iit.nrc.ca/II_public/index.html");

		authors.setEditable(false);
		authors.setLineWrap(true);
		authors.setWrapStyleWord(true);
				
		// Shorthands.
		int scrollNever = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER;
		int scrollAsNeeded = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED;
		// Scroll bars vertically when you need them.
		pane.add("Licence", new JScrollPane(licence, scrollAsNeeded, scrollNever));
		pane.add("Authors", new JScrollPane(authors, scrollAsNeeded, scrollNever));
		pane.add("General", new JScrollPane(general, scrollAsNeeded, scrollNever));

		// add the main tabbed panel to this frame as its child.
		this.getContentPane().add(pane);

		// Set our size, and add a closer that makes it invisible.
		super.setSize(500, 500);
		super.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent we) {
					AboutWindow.this.setVisible(false);
				}
			} );
	}


}

