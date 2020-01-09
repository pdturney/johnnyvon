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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.Reader;

import javax.swing.JFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.net.URL;

/** An application launcher for JohnnyVon.  Reads command line parameters,
 * then tries to load the input from a default location, lastly falling back
 * onto a compiled-in default configuration.  
 * @author <a href="mailto:raewasch@uwaterloo.ca">Rob Ewaschuk</a>, 
 * <a href="mailto:arnold.smith@nrc.ca">Arnold Smith</a>, 
 * <a href="mailto:peter.turney@nrc.ca">Peter Turney</a>
 * @version 1.0  Copyright &copy; 2002 National Research Council Canada
 */
public class JohnnyVonApplication {

	public static void main(String[] args) {
		try {
			Reader reader;
			boolean stdin = false;
			
			if (args.length >= 1 && (args[0].equals("--read"))) {
				System.out.print("Reading data from standard in..");
				stdin = true;
				reader = new InputStreamReader(System.in);
			} else {
				// First, try to fall back to the default input file.
				URL url = JohnnyVonApplication.class.getClassLoader()
					.getResource("JohnnyVon/support/input.txt"); 
				if (url != null) {
					reader = new InputStreamReader(url.openStream());
				} else {
					// If all else fails, use the fallback text.
					System.out.println(
						"Using default data.  Run with --read to read data.");
					// any argument means fallback.
					reader = new StringReader(JohnnyVonDisplay.FALLBACK_TEXT);
				}
			}

			JFrame frame = new JohnnyVonDisplay(new BufferedReader(reader), 
					new JohnnyVonDisplay.Closer() {
						public void close(JohnnyVonDisplay display) {
							System.exit(0);
						}
					});

			if (stdin) System.out.println(".done.");

			frame.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent we) {
						System.exit(0);
					}
				} );
			
			frame.show();

		} catch (Exception e) {
			System.out.println("Failed.  See below for details. (" + e + ")");
			e.printStackTrace();
		}

	}

}
