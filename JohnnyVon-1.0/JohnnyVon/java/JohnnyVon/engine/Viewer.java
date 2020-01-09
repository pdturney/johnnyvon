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

package JohnnyVon.engine;

/** General interface for something that can provide a view onto the Codons. 
 * @author <a href="mailto:raewasch@uwaterloo.ca">Rob Ewaschuk</a>, 
 * <a href="mailto:arnold.smith@nrc.ca">Arnold Smith</a>, 
 * <a href="mailto:peter.turney@nrc.ca">Peter Turney</a>
 * @version 1.0  Copyright &copy; 2002 National Research Council Canada
 */
public interface Viewer {

	public void view(AbstractCodon[] codons, double time, int iterations);

	public void setSimulator(Simulator sim);
}
