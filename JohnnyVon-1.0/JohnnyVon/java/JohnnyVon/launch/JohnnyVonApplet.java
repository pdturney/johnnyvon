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

package JohnnyVon.launch;

import JohnnyVon.engine.*;
import JohnnyVon.gui.*;

import java.applet.Applet;
import javax.swing.JFrame;
import javax.swing.JApplet;
import javax.swing.JOptionPane;
import javax.swing.JLabel;

import java.io.StringReader;
import java.io.BufferedReader;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

/** JohnnyVon as an applet.  Tries to get data_url parameter (which comes
 * from a param tag within the applet tag) for an absolute url, or data_path
 * for a url relative to the document that loaded this applet, or by loading
 * from the JohnnyVon/support/input.txt from the .jar file that the applet
 * was loading from, or finally falling back on a compiled-in default. 
 * @author <a href="mailto:raewasch@uwaterloo.ca">Rob Ewaschuk</a>, 
 * <a href="mailto:arnold.smith@nrc.ca">Arnold Smith</a>, 
 * <a href="mailto:peter.turney@nrc.ca">Peter Turney</a>
 * @version 1.0  Copyright &copy; 2002 National Research Council Canada
 */
public class JohnnyVonApplet extends JApplet {

	private JohnnyVonDisplay frame;

	public void init() {

		Reader reader = null;

		try {
			String sourceDataURL = super.getParameter("data_url");
			if (sourceDataURL == null) {
				String sourceDataFile = super.getParameter("data_path");
				if (sourceDataFile != null) {
					// Bad hack.  There's no proper (easy) way to do this.
					String temp = this.getDocumentBase().toString();
					temp = temp.substring(0, temp.lastIndexOf("/") + 1);
					sourceDataURL = temp + sourceDataFile;
					//sourceDataURL = "http://" + this.getDocumentBase().getHost() + sourceDataFile;
				} 
			}

			URL target;

			if (sourceDataURL != null) {
		 		target = new URL(sourceDataURL);
			} else {
				target = JohnnyVonApplet.class.getClassLoader()
					.getResource("JohnnyVon/support/input.txt"); 
			}
			// assert: sourceDataURL != null;
			
			reader = new InputStreamReader(target.openStream());
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, 
					"Error loading data.  Using default data.  Please notify the site"
					+ " administrator.  (See Java Console for details.)");
			System.out.println("Failed to fetch data.  Using fallback text.");
			e.printStackTrace();
			reader = new StringReader(JohnnyVonDisplay.FALLBACK_TEXT);
		}
		
		try {
			this.frame = new JohnnyVonDisplay(new BufferedReader(reader),
				new JohnnyVonDisplay.Closer() {
					public void close(JohnnyVonDisplay display) {
						JohnnyVonApplet.this.stop();
					}
				} );
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, 
				"Fatal Error launching applet.\nPlease contact the administrator of this site."
				+ "(This is probably a syntax error in the provided data.\nSee the"
				+ "Java console for more information.)",
				"Error!", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}

		this.getContentPane().add(
				new JLabel("JohnnyVon should be open\n in a separate window.",
				JLabel.CENTER));
	}

	public void start() {
		this.frame.show();
		// do not start running immediately -- give user time to configure the settings
		this.frame.setPaused(true);
	}

	public void stop() {
		this.frame.hide();
		this.frame.setPaused(true);
	}

	public void destroy() {
	
	}

}
